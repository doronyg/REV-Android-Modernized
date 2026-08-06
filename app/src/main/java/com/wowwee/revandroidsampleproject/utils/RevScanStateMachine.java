package com.wowwee.revandroidsampleproject.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.AndroidRuntimeException;
import android.util.Log;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.ref.WeakReference;

public class RevScanStateMachine {

    public interface Listener {
        void onPermissionsRequired();
        void onBluetoothEnableRequired();
        void onScanStatusChanged(String status, boolean showRetry);
        void onDiscoveryRecommended();
        void onNavigateToDriverMode(REVRobot connectedRev);
    }

    private static final String TAG = "REV-ScanStateMachine";

    private static final int CONNECTION_IDLE = 0;
    private static final int CONNECTION_SCANNING = 1;
    private static final int CONNECTION_SCAN_HOLD = 2;
    private static final int CONNECTION_CONNECTING = 3;
    private static final int CONNECTION_CONNECTED = 4;

    private static final long PAIRED_FALLBACK_DELAY_MS = 2500L;
    private static final long SCAN_STARTUP_INITIAL_DELAY_MS = 500L;
    private static final long SCAN_STARTUP_FINAL_DELAY_MS = 1000L;
    private static final long DISCOVERY_RECOMMEND_MS = 10000L;
    private static final long SCAN_TIMEOUT_MS = 30000L;

    private static RevScanStateMachine instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<BluetoothDevice> pairedRevCandidates = new ArrayList<BluetoothDevice>();
    private WeakReference<Activity> activityRef = new WeakReference<Activity>(null);
    private WeakReference<Listener> listenerRef = new WeakReference<Listener>(null);
    private REVRobot.REVRobotInterface robotCallback;

    private BluetoothAdapter bluetoothAdapter;
    private Timer tapTimer;
    private int connectionState = CONNECTION_IDLE;
    private long connectTimestamp;
    private long closestTimestamp;
    private REVRobot closestRev;
    private int scanStatusTick;
    private long pairedCandidatesUpdatedAtMs;
    private boolean pairedFallbackAttempted;
    private boolean isRevFinderReceiverRegistered;
    private boolean hasSeenCandidateInSession;

    private final Runnable scanStartupStepOneRunnable = this::runScanStartupStepOne;
    private final Runnable scanStartupStepTwoRunnable = this::runScanStartupStepTwo;
    private final Runnable discoveryRecommendRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectionState != CONNECTION_SCANNING || hasSeenCandidateInSession) {
                return;
            }

            notifyStatus("No device yet. You can open discovery mode.", false);
             Listener scanListener = getListener();
             if (scanListener != null) {
                 scanListener.onDiscoveryRecommended();
             }
         }
    };

    private final Runnable scanTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectionState == CONNECTION_CONNECTED) {
                return;
            }

            notifyStatus("No REV found. Tap retry or open discovery.", true);
            cancelTapTimer();
            scanLeDevice(false);
            connectionState = CONNECTION_IDLE;
        }
    };

    private final BroadcastReceiver revFinderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            final String action = intent.getAction();
            if (REVRobotFinder.REVRobotFinder_REVFound.equals(action)) {
                pairedFallbackAttempted = false;
            } else if (REVRobotFinder.REVRobotFinder_REVPairedFound.equals(action)) {
                if (intent.getExtras() == null) {
                    return;
                }

                Object pairedObj = intent.getExtras().get("PairedBluetoothDevices");
                if (!(pairedObj instanceof ArrayList)) {
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
            }
        }
    };

    public static synchronized RevScanStateMachine getInstance() {
        if (instance == null) {
            instance = new RevScanStateMachine();
        }
        return instance;
    }

    private RevScanStateMachine() {
    }

    public void start(Activity hostActivity, Listener listener, REVRobot.REVRobotInterface callback) {
        activityRef = new WeakReference<Activity>(hostActivity);
        listenerRef = new WeakReference<Listener>(listener);
        robotCallback = callback;

        notifyStatus("Preparing Bluetooth...", false);
        resetResumeState();
        if (!hasRequiredBluetoothPermissions()) {
            notifyStatus("Bluetooth permission missing", true);
            Listener scanListener = getListener();
            if (scanListener != null) {
                scanListener.onPermissionsRequired();
            }
            return;
        }
        if (!initBluetooth()) {
            Log.e(TAG, "initBluetooth() failed; scan flow aborted.");
            notifyStatus("Bluetooth unavailable", true);
            return;
        }
        registerFinderReceiver();
        if (tryResumeWithConnectedRev()) {
            return;
        }
        if (!ensureBluetoothAdapterAvailableAndEnabled()) {
            notifyStatus("Enable Bluetooth to continue", true);
            return;
        }

        startScanStartupSequence();
    }

    public void stop() {
        notifyStatus("Paused", true);
        cancelScanSessionTimers();
        cancelScanStartupSequence();
        cancelTapTimer();
        scanLeDevice(false);
        pairedFallbackAttempted = false;
        unregisterFinderReceiver();
    }

    public void onRobotReady(REVRobot rev) {
        closestRev = null;
        REVPlayer.getInstance().setPlayerRev(rev);
        notifyStatus("Connected to " + rev.getName(), false);
        setConnectionState(CONNECTION_CONNECTED);
    }

    public void onRobotDisconnected() {
        pairedFallbackAttempted = false;
        notifyStatus("Connection lost, retrying scan", false);
        setConnectionState(CONNECTION_SCANNING);
    }

    public void retry() {
        Activity hostActivity = getHostActivity();
        Listener listener = getListener();
        if (hostActivity == null || listener == null || robotCallback == null) {
            notifyStatus("Cannot retry right now", true);
            return;
        }
        start(hostActivity, listener, robotCallback);
    }

    private void resetResumeState() {
        pairedRevCandidates.clear();
        pairedCandidatesUpdatedAtMs = 0L;
        pairedFallbackAttempted = false;
        hasSeenCandidateInSession = false;
    }

    private boolean hasRequiredBluetoothPermissions() {
        Context context = getHostActivity();
        if (context == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int scanPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN);
            int connectPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT);
            return scanPermission == PackageManager.PERMISSION_GRANTED
                    && connectPermission == PackageManager.PERMISSION_GRANTED;
        }

        int coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        return coarseLocation == PackageManager.PERMISSION_GRANTED;
    }

    private boolean initBluetooth() {
        Activity activity = getHostActivity();
        if (activity == null) {
            return false;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return false;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }

        REVRobotFinder.getInstance().setBluetoothAdapter(bluetoothAdapter);
        REVRobotFinder.getInstance().setApplicationContext(activity);
        return true;
    }

    private void registerFinderReceiver() {
        Activity activity = getHostActivity();
        isRevFinderReceiverRegistered = BroadcastReceiverUtils.registerReceiver(
                activity,
                revFinderBroadcastReceiver,
                REVRobotFinder.getRevRobotFinderIntentFilter(),
                isRevFinderReceiverRegistered,
                false
        );
    }

    private void unregisterFinderReceiver() {
        Activity activity = getHostActivity();
        BroadcastReceiverUtils.unregisterReceiver(
                activity,
                revFinderBroadcastReceiver,
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

        REVPlayer.getInstance().setPlayerRev(connectedRev);
        setConnectionState(CONNECTION_CONNECTED);
        return true;
    }

    private boolean ensureBluetoothAdapterAvailableAndEnabled() {
        Activity activity = getHostActivity();
        if (activity == null) {
            return false;
        }

        BluetoothManager btManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager != null ? btManager.getAdapter() : null;

        if (btAdapter == null) {
            return false;
        }

        if (!btAdapter.isEnabled()) {
            try {
                activity.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } catch (ActivityNotFoundException | AndroidRuntimeException ex) {
                Listener scanListener = getListener();
                if (scanListener != null) {
                    scanListener.onBluetoothEnableRequired();
                }
            }
            return false;
        }

        return true;
    }

    private void startScanStartupSequence() {
        cancelScanStartupSequence();
        handler.postDelayed(scanStartupStepOneRunnable, SCAN_STARTUP_INITIAL_DELAY_MS);
    }

    private void cancelScanStartupSequence() {
        handler.removeCallbacks(scanStartupStepOneRunnable);
        handler.removeCallbacks(scanStartupStepTwoRunnable);
    }

    private void runScanStartupStepOne() {
        if (getHostActivity() == null) {
            return;
        }

        REVRobotFinder.getInstance().clearFoundREVList();
        scanLeDevice(false);
        handler.postDelayed(scanStartupStepTwoRunnable, SCAN_STARTUP_FINAL_DELAY_MS);
    }

    private void runScanStartupStepTwo() {
        if (getHostActivity() == null) {
            return;
        }

        notifyStatus("Scanning for REV...", false);
        scanLeDevice(true);
        startTapTimer();
        startScanSessionTimers();
        setConnectionState(CONNECTION_SCANNING);
    }

    private void startTapTimer() {
        cancelTapTimer();

        tapTimer = new Timer();
        tapTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                tapTimerAction();
            }
        }, 0, 500);
    }

    private void cancelTapTimer() {
        if (tapTimer != null) {
            tapTimer.cancel();
            tapTimer = null;
        }
    }

    private void startScanSessionTimers() {
        cancelScanSessionTimers();
        handler.postDelayed(discoveryRecommendRunnable, DISCOVERY_RECOMMEND_MS);
        handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS);
    }

    private void cancelScanSessionTimers() {
        handler.removeCallbacks(discoveryRecommendRunnable);
        handler.removeCallbacks(scanTimeoutRunnable);
    }

    private void scanLeDevice(boolean enable) {
        if (bluetoothAdapter == null) {
            return;
        }

        if (enable) {
            if (!bluetoothAdapter.isEnabled()) {
                return;
            }
            REVRobotFinder.getInstance().scanForREVContinuous();
        } else {
            REVRobotFinder.getInstance().stopScanForREVContinuous();
        }
    }

    private void tapTimerAction() {
        if (connectionState != CONNECTION_SCANNING && connectionState != CONNECTION_SCAN_HOLD) {
            return;
        }

        List<REVRobot> revFound = REVRobotFinder.getInstance().getRevFoundList();
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
            Log.d(TAG, "tapTimerAction(): foundCount=" + revFound.size() + ", strongest=" + strongestName + "(" + strongestRssi + ")");
        }

        if (closestRev != null) {
            if (closestRev.rssi >= closeRSSI) {
                if (System.currentTimeMillis() - closestTimestamp >= connectWait) {
                    setConnectionState(CONNECTION_CONNECTING);
                    closestRev.setCallbackInterface(robotCallback);
                    closestRev.connect(getHostActivity());
                    scanLeDevice(false);
                }
            } else {
                closestRev = null;
            }
        }

        if (closestRev == null) {
            for (REVRobot rev : revFound) {
                if (rev.rssi >= closeRSSI) {
                    closestRev = rev;
                    closestTimestamp = System.currentTimeMillis();
                    hasSeenCandidateInSession = true;
                    handler.removeCallbacks(discoveryRecommendRunnable);
                    break;
                }
            }
        }

        if (closestRev == null && revFound.isEmpty() && !pairedFallbackAttempted && !pairedRevCandidates.isEmpty()) {
            long ageMs = System.currentTimeMillis() - pairedCandidatesUpdatedAtMs;
            if (ageMs >= PAIRED_FALLBACK_DELAY_MS) {
                BluetoothDevice fallbackDevice = pairedRevCandidates.get(0);
                REVRobot fallbackRev = new REVRobot(fallbackDevice, new ArrayList<AdRecord>(), null);
                fallbackRev.setCallbackInterface(robotCallback);
                pairedFallbackAttempted = true;
                setConnectionState(CONNECTION_CONNECTING);
                fallbackRev.connect(getHostActivity());
                scanLeDevice(false);
                return;
            }
        }

        if (connectionState == CONNECTION_SCANNING && closestRev != null) {
            setConnectionState(CONNECTION_SCAN_HOLD);
        } else if (connectionState == CONNECTION_SCAN_HOLD && closestRev == null) {
            setConnectionState(CONNECTION_SCANNING);
        }
    }

    private void setConnectionState(int state) {
        if (connectionState == state) {
            return;
        }

        connectionState = state;
        switch (connectionState) {
            case CONNECTION_SCANNING:
                notifyStatus("Scanning for REV...", false);
                break;
            case CONNECTION_SCAN_HOLD:
                notifyStatus("REV candidate found, preparing to connect", false);
                connectTimestamp = System.currentTimeMillis();
                break;
            case CONNECTION_CONNECTING:
                notifyStatus("Connecting...", false);
                break;
            case CONNECTION_CONNECTED: {
                cancelScanSessionTimers();
                cancelTapTimer();
                long connectDeltaTime = System.currentTimeMillis() - connectTimestamp;
                long delay = 1200 - connectDeltaTime;
                if (delay < 0) {
                    delay = 0;
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        REVRobot connected = REVPlayer.getInstance().getPlayerRev();
                        Listener scanListener = getListener();
                        if (connected != null && scanListener != null) {
                            scanListener.onNavigateToDriverMode(connected);
                        }
                    }
                }, delay + 1000);
                break;
            }
            default:
                break;
        }
    }

    private Activity getHostActivity() {
        return activityRef.get();
    }

    private Listener getListener() {
        return listenerRef.get();
    }

    private void notifyStatus(final String status, final boolean showRetry) {
        final Listener scanListener = getListener();
        if (scanListener == null) {
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                scanListener.onScanStatusChanged(status, showRetry);
            }
        });
    }
}
