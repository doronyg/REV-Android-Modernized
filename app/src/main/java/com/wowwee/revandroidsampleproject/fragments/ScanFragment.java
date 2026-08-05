package com.wowwee.revandroidsampleproject.fragments;

import static android.os.Looper.getMainLooper;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;
import com.wowwee.revandroidsampleproject.MainActivity;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.utils.BroadcastReceiverUtils;
import com.wowwee.revandroidsampleproject.utils.REVPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScanFragment extends BaseViewFragment {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "REV-ScanFragment";
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler handler;

    // Connect logic
    private Timer tapTimer;
    private static final int CONNECTION_IDLE = 0;
    private static final int CONNECTION_SCANNING = 1;
    private static final int CONNECTION_SCAN_HOLD = 2;
    private static final int CONNECTION_CONNECTING = 3;
    private static final int CONNECTION_CONNECTED = 4;
    private int connectionState = CONNECTION_IDLE;
    private long connectTimestamp;
    private long closestTimestamp;
    private REVRobot closestRev = null;
    private int scanStatusTick = 0;
    private static final long PAIRED_FALLBACK_DELAY_MS = 2500L;
    private static final long SCAN_STARTUP_INITIAL_DELAY_MS = 500L;
    private static final long SCAN_STARTUP_FINAL_DELAY_MS = 1000L;
    private final ArrayList<BluetoothDevice> pairedRevCandidates = new ArrayList<BluetoothDevice>();
    private long pairedCandidatesUpdatedAtMs = 0L;
    private boolean pairedFallbackAttempted = false;
    private final Runnable scanStartupStepOneRunnable = this::runScanStartupStepOne;
    private final Runnable scanStartupStepTwoRunnable = this::runScanStartupStepTwo;
    private boolean isRevFinderReceiverRegistered = false;

    public ScanFragment() {
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_scan;
    }

    //================================================================================
    // Override
    //================================================================================

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null)
            return null;

        View view = super.onCreateView(inflater, container, savedInstanceState);
        handler = new Handler(getMainLooper());

        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume() start.");
        super.onResume();
        if (handler == null) {
            handler = new Handler(getMainLooper());
        }
        resetResumeState();

        if (!ensurePermissionsReady()) {
            return;
        }
        if (!initBluetooth()) {
            Log.e(TAG, "initBluetooth() failed; scan flow aborted.");
            return;
        }
        registerFinderReceiver();
        if (tryResumeWithConnectedRev()) {
            return;
        }
        if (!ensureBluetoothAdapterAvailableAndEnabled()) {
            return;
        }

        startScanStartupSequence();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelScanStartupSequence();
        scanLeDevice(false);
        pairedFallbackAttempted = false;
        unregisterFinderReceiver();
        if (tapTimer != null) {
            tapTimer.cancel();
            tapTimer = null;
        }
    }

    private void resetResumeState() {
        pairedRevCandidates.clear();
        pairedCandidatesUpdatedAtMs = 0L;
        pairedFallbackAttempted = false;
    }

    private boolean ensurePermissionsReady() {
        if (hasRequiredBluetoothPermissions()) {
            return true;
        }

        Log.w(TAG, "Required Bluetooth permissions missing on resume; requesting through activity.");
        if (getFragmentActivity() instanceof MainActivity) {
            ((MainActivity) getFragmentActivity()).ensureBluetoothPermissionsAndStart();
        }
        return false;
    }

    private void registerFinderReceiver() {
        boolean wasRegistered = isRevFinderReceiverRegistered;
        isRevFinderReceiverRegistered = BroadcastReceiverUtils.registerReceiver(
                getFragmentActivity(),
                mRevFinderBroadcastReceiver,
                REVRobotFinder.getRevRobotFinderIntentFilter(),
                isRevFinderReceiverRegistered,
                false
        );
        if (!wasRegistered && isRevFinderReceiverRegistered) {
            Log.d(TAG, "Registered REV finder broadcast receiver.");
        }
    }

    private void unregisterFinderReceiver() {
        BroadcastReceiverUtils.unregisterReceiver(
                getFragmentActivity(),
                mRevFinderBroadcastReceiver,
                isRevFinderReceiverRegistered,
                TAG
        );
        isRevFinderReceiverRegistered = false;
    }

    private boolean tryResumeWithConnectedRev() {
        REVRobot connectedRev = REVRobotFinder.getInstance().firstConnectedREV();
        if (connectedRev == null) {
            return false;
        }

        Log.d(TAG, "onResume(): REV already connected, opening DriveView directly.");
        REVPlayer.getInstance().setPlayerRev(connectedRev);
        setConnectionState(CONNECTION_CONNECTED);
        return true;
    }

    private boolean ensureBluetoothAdapterAvailableAndEnabled() {
        BluetoothManager btManager = (BluetoothManager) getFragmentActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager != null ? btManager.getAdapter() : null;
        Log.d(TAG, "BluetoothManager instance=" + (btManager == null ? "null" : "non-null") + ", adapter=" + (btAdapter == null ? "null" : "non-null"));

        if (btAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null in onResume. Device may not support Bluetooth LE, adapter service may be unavailable, or permission gate still failing.");
            return false;
        }

        boolean enabled = btAdapter.isEnabled();
        if (!enabled) {
            Log.d(TAG, "Bluetooth adapter present but disabled; requesting user to enable.");
            try {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } catch (ActivityNotFoundException ax) {
                Log.w(TAG, "ACTION_REQUEST_ENABLE not available; falling back to manual activation prompt.", ax);
                askBluetoothActivationManually();
            } catch (AndroidRuntimeException ax) {
                Log.w(TAG, "ACTION_REQUEST_ENABLE failed at runtime; falling back to manual activation prompt.", ax);
                askBluetoothActivationManually();
            }
        }
        return enabled;
    }

    private void startScanStartupSequence() {
        cancelScanStartupSequence();
        handler.postDelayed(scanStartupStepOneRunnable, SCAN_STARTUP_INITIAL_DELAY_MS);
    }

    private void runScanStartupStepOne() {
        if (!isAdded() || getFragmentActivity() == null) {
            return;
        }

        REVRobotFinder.getInstance().clearFoundREVList();
        scanLeDevice(false);
        handler.postDelayed(scanStartupStepTwoRunnable, SCAN_STARTUP_FINAL_DELAY_MS);
    }

    private void runScanStartupStepTwo() {
        if (!isAdded() || getFragmentActivity() == null) {
            return;
        }

        scanLeDevice(true);
        startTapTimer();
        setConnectionState(CONNECTION_SCANNING);
    }

    private void startTapTimer() {
        if (tapTimer != null) {
            tapTimer.cancel();
        }

        tapTimer = new Timer();
        tapTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                tapTimerAction();
            }
        }, 0, 500);
    }

    private void cancelScanStartupSequence() {
        if (handler == null) {
            return;
        }

        handler.removeCallbacks(scanStartupStepOneRunnable);
        handler.removeCallbacks(scanStartupStepTwoRunnable);
    }

    private boolean hasRequiredBluetoothPermissions() {
        Context context = getFragmentActivity();
        if (context == null) {
            Log.w(TAG, "Permission check aborted: fragment activity is null.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int scanPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN);
            int connectPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT);
            Log.d(TAG, "Permission check S+: BLUETOOTH_SCAN=" + (scanPermission == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED")
                    + ", BLUETOOTH_CONNECT=" + (connectPermission == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            return scanPermission == PackageManager.PERMISSION_GRANTED
                    && connectPermission == PackageManager.PERMISSION_GRANTED;
        }

        int coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        Log.d(TAG, "Permission check pre-S: ACCESS_COARSE_LOCATION=" + (coarseLocation == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        return coarseLocation == PackageManager.PERMISSION_GRANTED;

    }

    private boolean hasBluetoothConnectPermission() {
        Context context = getFragmentActivity();
        if (context == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    //================================================================================
    // Bluetooth
    //================================================================================

    private void askBluetoothActivationManually() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getFragmentActivity());

        builder.setCancelable(true);

        builder.setMessage("bluetooth_enable_question");
        builder.setTitle("bluetooth_enable_dialog_title");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!hasBluetoothConnectPermission()) {
                            if (getFragmentActivity() instanceof MainActivity) {
                                ((MainActivity) getFragmentActivity()).ensureBluetoothPermissionsAndStart();
                            }
                            return;
                        }

                        try {
                            BluetoothAdapter.getDefaultAdapter().enable();
                        } catch (SecurityException ex) {
                            Log.w(getClass().getName(), "Bluetooth enable blocked by missing permission.", ex);
                        }
                    }}
        );

        builder.setNegativeButton(android.R.string.no, (dialog, which) -> {});

        builder.show();
    }

    private boolean initBluetooth(){
        final BluetoothManager bluetoothManager = (BluetoothManager) getFragmentActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        Log.d(TAG, "initBluetooth(): BluetoothManager=" + (bluetoothManager == null ? "null" : "non-null"));
        if (bluetoothManager == null) {
            Log.e(TAG, "BluetoothManager is null.");
            return false;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();
        Log.d(TAG, "initBluetooth(): adapter=" + (mBluetoothAdapter == null ? "null" : "non-null"));
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null. Device may not support Bluetooth LE.");
            return false;
        }

        REVRobotFinder.getInstance().setBluetoothAdapter(mBluetoothAdapter);
        REVRobotFinder.getInstance().setApplicationContext(getFragmentActivity());
        Log.d(TAG, "initBluetooth(): adapter and context passed to REVRobotFinder.");
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "scanLeDevice(" + enable + ") skipped: Bluetooth adapter unavailable.");
            mScanning = false;
            return;
        }

        if (enable) {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.w(TAG, "scanLeDevice(true) skipped: Bluetooth adapter is disabled.");
                mScanning = false;
                return;
            }
            Log.d(TAG, "scanLeDevice(true): starting continuous REV scan.");
            // Stops scanning after a pre-defined scan period.
//        	final double scanTime = System.currentTimeMillis();
            mScanning = true;
            REVRobotFinder.getInstance().scanForREVContinuous();
        }else{
            Log.d(TAG, "scanLeDevice(false): stopping continuous REV scan.");
            mScanning = false;
            REVRobotFinder.getInstance().stopScanForREVContinuous();
        }
    }

    public boolean IsScanning() {
        return mScanning;
    }

    //================================================================================
    // Timer action
    //================================================================================

    private void tapTimerAction() {
        if(connectionState == CONNECTION_SCANNING || connectionState == CONNECTION_SCAN_HOLD) {
            List<REVRobot> revFound = REVRobotFinder.getInstance().getRevFoundList();
            // Older threshold was very strict and caused frequent "stuck scanning" on modern phones.
            int closeRSSI = -85;
            long connectWait = 1500;

            if ((scanStatusTick++ % 8) == 0) {
                int strongestRssi = -127;
                String strongestName = "<none>";
                for (REVRobot r : revFound) {
                    if (r != null && r.rssi > strongestRssi) {
                        strongestRssi = r.rssi;
                        strongestName = r.getName();
                    }
                }
                Log.d(TAG, "tapTimerAction(): foundCount=" + revFound.size() + ", strongest=" + strongestName + "(" + strongestRssi + "), threshold=" + closeRSSI + ", closestRev=" + (closestRev != null ? closestRev.getName() + "(" + closestRev.rssi + ")" : "<none>"));
            }

            if(closestRev != null) {
                if(closestRev.rssi >= closeRSSI) {
                    // Check timestamp
                    if(System.currentTimeMillis() - closestTimestamp >= connectWait) {
                        Log.d(TAG, "tapTimerAction(): connecting to " + closestRev.getName() + " rssi=" + closestRev.rssi);
                        setConnectionState(CONNECTION_CONNECTING);
                        closestRev.setCallbackInterface(this);
                        closestRev.connect(getFragmentActivity());
                        scanLeDevice(false);
                    }
                }
                else {
                    closestRev = null;
                }
            }

            if(closestRev == null) {
                for(REVRobot rev : revFound) {
                    if(rev.rssi >= closeRSSI) {
                        closestRev = rev;
                        closestTimestamp = System.currentTimeMillis();
                        Log.d(TAG, "tapTimerAction(): candidate selected " + rev.getName() + " rssi=" + rev.rssi);
                        break;
                    }
                }
            }

            if (closestRev == null && revFound.isEmpty() && !pairedFallbackAttempted && !pairedRevCandidates.isEmpty()) {
                long ageMs = System.currentTimeMillis() - pairedCandidatesUpdatedAtMs;
                if (ageMs >= PAIRED_FALLBACK_DELAY_MS) {
                    BluetoothDevice fallbackDevice = pairedRevCandidates.get(0);
                    Log.d(TAG, "tapTimerAction(): no live advertisements found, trying paired fallback connect to " + safeDeviceLabel(fallbackDevice));
                    REVRobot fallbackRev = new REVRobot(fallbackDevice, new ArrayList<AdRecord>(), null);
                    fallbackRev.setCallbackInterface(this);
                    pairedFallbackAttempted = true;
                    setConnectionState(CONNECTION_CONNECTING);
                    fallbackRev.connect(getFragmentActivity());
                    scanLeDevice(false);
                    return;
                }
            }

            if(connectionState == CONNECTION_SCANNING && closestRev != null) {
                setConnectionState(CONNECTION_SCAN_HOLD);
            }
            else if(connectionState == CONNECTION_SCAN_HOLD && closestRev == null) {
                setConnectionState(CONNECTION_SCANNING);
            }
        }
    }

    //================================================================================
    // Connect Logic
    //================================================================================

    private void setConnectionState(int state) {
        if(connectionState != state) {
            connectionState = state;
            switch (connectionState) {
                default:
                case CONNECTION_SCANNING:
                    Log.d(getClass().getName(), "CONNECTION_SCANNING");
                    break;
                case CONNECTION_SCAN_HOLD:
                    Log.d(getClass().getName(), "CONNECTION_SCAN_HOLD");
                    connectTimestamp = System.currentTimeMillis();
                    break;
                case CONNECTION_CONNECTING:
                    break;
                case CONNECTION_CONNECTED: {
                    long connectDeltaTime = System.currentTimeMillis() - connectTimestamp;
                    long delay = 1200 - connectDeltaTime;
                    if(delay < 0) {
                        delay = 0;
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(getClass().getName(), "CONNECTION_CONNECTED");
                        }
                    }, delay);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Proceed to DriveView
                            getFragmentActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(getClass().getName(),"Go to game page.");
                                    FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), new DriveViewFragment(), R.id.view_id_content, false);
                                }
                            });
                        }
                    }, (delay + 1000));

                }
                break;
            }
        }
    }

    //================================================================================
    // REVRobotFinder broadcast receiver
    //================================================================================

    private final BroadcastReceiver mRevFinderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (REVRobotFinder.REVRobotFinder_REVFound.equals(action)){
                if (intent.getExtras() == null) {
                    return;
                }

                BluetoothDevice device = (BluetoothDevice)(intent.getExtras().get("BluetoothDevice"));
                if (device == null) {
                    return;
                }

                String deviceName = "<unknown>";
                try {
                    deviceName = device.getName();
                } catch (SecurityException ex) {
                    Log.w(getClass().getName(), "Bluetooth device name blocked by missing permission.", ex);
                }
                Log.d(getClass().getName(), "RevScanFragment broadcast receiver found REV: " + deviceName);
                pairedFallbackAttempted = false;
            } else if (REVRobotFinder.REVRobotFinder_REVPairedFound.equals(action)) {
                if (intent.getExtras() == null) {
                    return;
                }

                Object pairedObj = intent.getExtras().get("PairedBluetoothDevices");
                if (!(pairedObj instanceof ArrayList)) {
                    Log.w(TAG, "REVPairedFound broadcast received without ArrayList payload");
                    return;
                }

                pairedRevCandidates.clear();
                ArrayList<?> rawList = (ArrayList<?>) pairedObj;
                for (Object item : rawList) {
                    if (item instanceof BluetoothDevice) {
                        pairedRevCandidates.add((BluetoothDevice) item);
                    }
                }
                pairedCandidatesUpdatedAtMs = System.currentTimeMillis();

                Log.d(TAG, "REVPairedFound: candidates=" + pairedRevCandidates.size());
                for (BluetoothDevice d : pairedRevCandidates) {
                    Log.d(TAG, "REVPairedFound candidate: " + safeDeviceLabel(d));
                }
            }
        }
    };

    //================================================================================
    // REVRobot callback
    //================================================================================

    @Override
    public void revDeviceReady(REVRobot rev) {
        Log.d(getClass().getName(), "revDeviceReady!");

        // REV connected
        closestRev = null;

        // Set player rev
        REVPlayer.getInstance().setPlayerRev(rev);

        // Set connection state
        if (getFragmentActivity() != null) {
            getFragmentActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Animate connected
                    setConnectionState(CONNECTION_CONNECTED);
                }
            });
        }
    }

    @Override
    public void revDeviceDisconnected(REVRobot rev) {
        Log.d(getClass().getName(), "revDeviceDisconnected!");
        pairedFallbackAttempted = false;
        setConnectionState(CONNECTION_SCANNING);
    }

    private String safeDeviceLabel(BluetoothDevice device) {
        if (device == null) {
            return "<null-device>";
        }
        String address = device.getAddress();
        try {
            String name = device.getName();
            return (name != null ? name : "<unnamed>") + " [" + address + "]";
        } catch (SecurityException ex) {
            return "<permission-denied-name> [" + address + "]";
        }
    }
}
