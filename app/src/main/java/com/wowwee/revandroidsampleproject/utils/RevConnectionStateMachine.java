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
import android.util.AndroidRuntimeException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;
import com.wowwee.revandroidsampleproject.robot.REVRobotEvent;
import com.wowwee.revandroidsampleproject.robot.REVRobotEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class RevConnectionStateMachine {

    public enum ConnectionState {
        IDLE,
        SCANNING,
        SCAN_HOLD,
        CONNECTING,
        CONNECTED
    }

    public enum UiEventType {
        REQUEST_PERMISSIONS,
        REQUEST_ENABLE_BLUETOOTH,
        DISCOVERY_RECOMMENDED,
        NAVIGATE_TO_DRIVER_MODE,
        PRIMARY_REV_DISCONNECTED
    }

    public static final class UiEvent {
        public final UiEventType type;
        @Nullable
        public final REVRobot robot;

        public UiEvent(UiEventType type) {
            this(type, null);
        }

        public UiEvent(UiEventType type, @Nullable REVRobot robot) {
            this.type = type;
            this.robot = robot;
        }
    }

    public static final class ScanUiState {
        public final String status;
        public final boolean showRetry;

        public ScanUiState(String status, boolean showRetry) {
            this.status = status;
            this.showRetry = showRetry;
        }
    }

    private static final String TAG = "REV-ScanStateMachine";

    private static final long PAIRED_FALLBACK_DELAY_MS = 2500L;
    private static final long SCAN_STARTUP_INITIAL_DELAY_MS = 500L;
    private static final long SCAN_STARTUP_FINAL_DELAY_MS = 1000L;
    private static final long DISCOVERY_RECOMMEND_MS = 10000L;
    private static final long SCAN_TIMEOUT_MS = 30000L;

    private static RevConnectionStateMachine instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompositeDisposable appRevEventDisposables = new CompositeDisposable();
    private final Subject<ScanUiState> scanUiStateSubject = BehaviorSubject.<ScanUiState>createDefault(new ScanUiState("Idle", false)).toSerialized();
    private final Subject<UiEvent> uiEventSubject = PublishSubject.<UiEvent>create().toSerialized();
    private final ArrayList<BluetoothDevice> pairedRevCandidates = new ArrayList<>();
    @Nullable
    private Context appContext = null;
    private REVRobot.REVRobotInterface robotCallback;

    private BluetoothAdapter bluetoothAdapter;
    private Timer tapTimer;
    private ConnectionState connectionState = ConnectionState.IDLE;
    private long connectTimestamp;
    private long closestTimestamp;
    private REVRobot closestRev;
    private REVRobot activeConnectedRev;
    private int scanStatusTick;
    private long pairedCandidatesUpdatedAtMs;
    private boolean pairedFallbackAttempted;
    private boolean isRevFinderReceiverRegistered;
    private boolean hasSeenCandidateInSession;
    private boolean pendingPrimaryDisconnectUi;
    private String lastScanStatus = "";
    private boolean lastScanStatusShowRetry;

    private final Runnable scanStartupStepOneRunnable = this::runScanStartupStepOne;
    private final Runnable scanStartupStepTwoRunnable = this::runScanStartupStepTwo;
    private final Runnable discoveryRecommendRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectionState != ConnectionState.SCANNING || hasSeenCandidateInSession) {
                return;
            }

            notifyStatus("No device yet. You can open discovery mode.", false);
            uiEventSubject.onNext(new UiEvent(UiEventType.DISCOVERY_RECOMMENDED));
         }
    };

    private final Runnable scanTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectionState == ConnectionState.CONNECTED) {
                return;
            }

            notifyStatus("No REV found. Tap retry or open discovery.", true);
            cancelTapTimer();
            scanLeDevice(false);
            connectionState = ConnectionState.IDLE;
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

    public static synchronized RevConnectionStateMachine getInstance() {
        if (instance == null) {
            instance = new RevConnectionStateMachine();
        }
        return instance;
    }

    private RevConnectionStateMachine() {
    }

    public Observable<ScanUiState> observeScanUiState() {
        return scanUiStateSubject.hide();
    }

    public Observable<UiEvent> observeUiEvents() {
        return uiEventSubject.hide();
    }

    public synchronized void start(@Nullable Context context, REVRobot.REVRobotInterface callback) {
        if (context == null || context.getApplicationContext() == null || callback == null) {
            notifyStatus("Cannot start scan right now", true);
            return;
        }

        appContext = context.getApplicationContext();
        robotCallback = callback;

        notifyStatus("Preparing Bluetooth...", false);
        resetResumeState();
        if (!hasRequiredBluetoothPermissions()) {
            notifyStatus("Bluetooth permission missing", true);
            uiEventSubject.onNext(new UiEvent(UiEventType.REQUEST_PERMISSIONS));
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
            uiEventSubject.onNext(new UiEvent(UiEventType.REQUEST_ENABLE_BLUETOOTH));
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
        activeConnectedRev = rev;
        pendingPrimaryDisconnectUi = false;
        REVPlayer.getInstance().setPlayerRev(rev);
        notifyStatus("Connected to " + rev.getName(), false);
        setConnectionState(ConnectionState.CONNECTED);
    }

    public void onRobotDisconnected(REVRobot disconnectedRev) {
        if (!isDisconnectForActiveRev(disconnectedRev)) {
            return;
        }

        pairedFallbackAttempted = false;
        activeConnectedRev = null;

        REVRobot playerRev = REVPlayer.getInstance().getPlayerRev();
        if (isSameRobot(playerRev, disconnectedRev)) {
            REVPlayer.getInstance().setPlayerRev(null);
        }

        notifyStatus("Connection lost, returning to scan", false);
        setConnectionState(ConnectionState.IDLE);
        requestPrimaryDisconnectUi();
    }

    public synchronized void retry() {
        if (appContext == null || robotCallback == null) {
            notifyStatus("Cannot retry right now", true);
            return;
        }
        start(appContext, robotCallback);
    }

    public synchronized ConnectionState getConnectionState() {
        return connectionState;
    }

    public synchronized boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED && activeConnectedRev != null;
    }

    @Nullable
    public synchronized REVRobot getActiveConnectedRev() {
        return activeConnectedRev;
    }

    @Nullable
    public synchronized String getActiveConnectedRevAddress() {
        return safeAddress(activeConnectedRev);
    }

    public synchronized String getLastScanStatus() {
        return lastScanStatus;
    }

    public synchronized boolean isLastScanStatusRetryVisible() {
        return lastScanStatusShowRetry;
    }

    public synchronized boolean isPrimaryDisconnectUiPending() {
        return pendingPrimaryDisconnectUi;
    }

    public synchronized boolean consumePrimaryDisconnectUiPending() {
        if (!pendingPrimaryDisconnectUi) {
            return false;
        }
        pendingPrimaryDisconnectUi = false;
        return true;
    }

    public synchronized void acknowledgePrimaryDisconnectUiHandled() {
        pendingPrimaryDisconnectUi = false;
    }

    private void resetResumeState() {
        pairedRevCandidates.clear();
        pairedCandidatesUpdatedAtMs = 0L;
        pairedFallbackAttempted = false;
        hasSeenCandidateInSession = false;
        activeConnectedRev = null;
    }

    private boolean hasRequiredBluetoothPermissions() {
        Context context = appContext;
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
        Context context = appContext;
        if (context == null) {
            return false;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return false;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }

        REVRobotFinder.getInstance().setBluetoothAdapter(bluetoothAdapter);
        REVRobotFinder.getInstance().setApplicationContext(context);
        return true;
    }

    private void registerFinderReceiver() {
        isRevFinderReceiverRegistered = BroadcastReceiverUtils.registerReceiver(
                appContext,
                revFinderBroadcastReceiver,
                REVRobotFinder.getRevRobotFinderIntentFilter(),
                isRevFinderReceiverRegistered,
                false
        );
    }

    private void unregisterFinderReceiver() {
        BroadcastReceiverUtils.unregisterReceiver(
                appContext,
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

        activeConnectedRev = connectedRev;
        REVPlayer.getInstance().setPlayerRev(connectedRev);
        setConnectionState(ConnectionState.CONNECTED);
        return true;
    }

    private boolean ensureBluetoothAdapterAvailableAndEnabled() {
        Context context = appContext;
        if (context == null) {
            return false;
        }

        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager != null ? btManager.getAdapter() : null;

        if (btAdapter == null) {
            return false;
        }

        if (!btAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                uiEventSubject.onNext(new UiEvent(UiEventType.REQUEST_PERMISSIONS));
                return false;
            }

            if (context instanceof Activity) {
                try {
                    context.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                } catch (ActivityNotFoundException | AndroidRuntimeException | SecurityException ex) {
                    uiEventSubject.onNext(new UiEvent(UiEventType.REQUEST_ENABLE_BLUETOOTH));
                }
            } else {
                uiEventSubject.onNext(new UiEvent(UiEventType.REQUEST_ENABLE_BLUETOOTH));
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
        if (appContext == null) {
            return;
        }

        REVRobotFinder.getInstance().clearFoundREVList();
        scanLeDevice(false);
        handler.postDelayed(scanStartupStepTwoRunnable, SCAN_STARTUP_FINAL_DELAY_MS);
    }

    private void runScanStartupStepTwo() {
        if (appContext == null) {
            return;
        }

        notifyStatus("Scanning for REV...", false);
        scanLeDevice(true);
        startTapTimer();
        startScanSessionTimers();
        setConnectionState(ConnectionState.SCANNING);
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
        if (connectionState != ConnectionState.SCANNING && connectionState != ConnectionState.SCAN_HOLD) {
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
                    setConnectionState(ConnectionState.CONNECTING);
                    closestRev.setCallbackInterface(robotCallback);
                    closestRev.connect(appContext);
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
                setConnectionState(ConnectionState.CONNECTING);
                fallbackRev.connect(appContext);
                scanLeDevice(false);
                return;
            }
        }

        if (connectionState == ConnectionState.SCANNING && closestRev != null) {
            setConnectionState(ConnectionState.SCAN_HOLD);
        } else if (connectionState == ConnectionState.SCAN_HOLD && closestRev == null) {
            setConnectionState(ConnectionState.SCANNING);
        }
    }

    private void setConnectionState(ConnectionState state) {
        if (connectionState == state) {
            return;
        }

        connectionState = state;
        switch (connectionState) {
            case SCANNING:
                notifyStatus("Scanning for REV...", false);
                break;
            case SCAN_HOLD:
                notifyStatus("REV candidate found, preparing to connect", false);
                connectTimestamp = System.currentTimeMillis();
                break;
            case CONNECTING:
                notifyStatus("Connecting...", false);
                break;
            case CONNECTED: {
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
                        if (connected != null) {
                            uiEventSubject.onNext(new UiEvent(UiEventType.NAVIGATE_TO_DRIVER_MODE, connected));
                        }
                    }
                }, delay + 1000);
                break;
            }
            default:
                break;
        }
    }

    private void notifyStatus(final String status, final boolean showRetry) {
        lastScanStatus = status;
        lastScanStatusShowRetry = showRetry;
        scanUiStateSubject.onNext(new ScanUiState(status, showRetry));
    }

    private boolean isDisconnectForActiveRev(REVRobot disconnectedRev) {
        if (disconnectedRev == null) {
            return false;
        }

        if (activeConnectedRev != null) {
            return isSameRobot(activeConnectedRev, disconnectedRev);
        }

        REVRobot playerRev = REVPlayer.getInstance().getPlayerRev();
        return isSameRobot(playerRev, disconnectedRev);
    }

    private boolean isSameRobot(REVRobot left, REVRobot right) {
        if (left == null || right == null) {
            return false;
        }

        if (left == right) {
            return true;
        }

        String leftAddress = safeAddress(left);
        String rightAddress = safeAddress(right);
        return leftAddress != null && leftAddress.equalsIgnoreCase(rightAddress);
    }

    private String safeAddress(REVRobot robot) {
        try {
            return robot.getBluetoothDevice() != null ? robot.getBluetoothDevice().getAddress() : null;
        } catch (SecurityException ex) {
            return null;
        }
    }

    public synchronized void bindAppLevelRevEvents() {
        if (!appRevEventDisposables.isDisposed() && appRevEventDisposables.size() > 0) {
            return;
        }

        appRevEventDisposables.add(
                REVRobotEventBus.getEvents()
                        .observeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                event -> {
                                    if (event instanceof REVRobotEvent.DeviceDisconnected) {
                                        REVRobot disconnectedRobot = ((REVRobotEvent.DeviceDisconnected) event).getRobot();
                                        Log.d(TAG, "REV event: device disconnected");
                                        onRobotDisconnected(disconnectedRobot);
                                    }
                                },
                                error -> Log.e(TAG, "REV event stream error", error)
                        )
        );
    }

    public synchronized void unbindAppLevelRevEvents() {
        appRevEventDisposables.clear();
    }

    public synchronized void emitUiEventForSimulator(UiEventType type) {
        if (type == UiEventType.PRIMARY_REV_DISCONNECTED) {
            pendingPrimaryDisconnectUi = true;
            uiEventSubject.onNext(new UiEvent(type));
            return;
        }

        if (type == UiEventType.NAVIGATE_TO_DRIVER_MODE) {
            REVRobot connected = activeConnectedRev != null ? activeConnectedRev : REVPlayer.getInstance().getPlayerRev();
            uiEventSubject.onNext(new UiEvent(type, connected));
            return;
        }

        uiEventSubject.onNext(new UiEvent(type));
    }

    private void requestPrimaryDisconnectUi() {
        pendingPrimaryDisconnectUi = true;
        uiEventSubject.onNext(new UiEvent(UiEventType.PRIMARY_REV_DISCONNECTED));
    }

}

