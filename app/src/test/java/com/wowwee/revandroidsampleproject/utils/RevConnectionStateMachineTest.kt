package com.wowwee.revandroidsampleproject.utils

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import io.reactivex.rxjava3.observers.TestObserver
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RevConnectionStateMachineTest {

    @Before
    fun setUp() {
        resetSingletonInstance()
    }

    @After
    fun tearDown() {
        val machine = RevConnectionStateMachine.getInstance()
        machine.stop()
        machine.acknowledgePrimaryDisconnectUiHandled()
        resetSingletonInstance()
    }

    @Test
    fun `initial state is idle and disconnected`() {
        val machine = RevConnectionStateMachine.getInstance()

        assertEquals(RevConnectionStateMachine.ConnectionState.IDLE, machine.getConnectionState())
        assertFalse(machine.isConnected())
        assertNull(machine.getActiveConnectedRev())
    }

    @Test
    fun `start with null context and callback shows retry status and remains idle`() {
        val machine = RevConnectionStateMachine.getInstance()

        machine.start(null, null)

        assertEquals("Cannot start scan right now", machine.getLastScanStatus())
        assertTrue(machine.isLastScanStatusRetryVisible())
        assertEquals(RevConnectionStateMachine.ConnectionState.IDLE, machine.getConnectionState())
    }

    @Test
    fun `retry before successful start shows cannot retry`() {
        val machine = RevConnectionStateMachine.getInstance()

        machine.retry()

        assertEquals("Cannot retry right now", machine.getLastScanStatus())
        assertTrue(machine.isLastScanStatusRetryVisible())
    }

    @Test
    fun `stop updates paused status with retry visible`() {
        val machine = RevConnectionStateMachine.getInstance()

        machine.stop()

        assertEquals("Paused", machine.getLastScanStatus())
        assertTrue(machine.isLastScanStatusRetryVisible())
    }

    @Test
    fun `simulated primary disconnect emits UI event and pending flag is consumable once`() {
        val machine = RevConnectionStateMachine.getInstance()
        machine.acknowledgePrimaryDisconnectUiHandled()
        val observer: TestObserver<RevConnectionStateMachine.UiEvent> = machine.observeUiEvents().test()

        try {
            machine.emitUiEventForSimulator(RevConnectionStateMachine.UiEventType.PRIMARY_REV_DISCONNECTED)

            observer.assertValueCount(1)
            observer.assertValueAt(0) { it.type == RevConnectionStateMachine.UiEventType.PRIMARY_REV_DISCONNECTED }
            assertTrue(machine.isPrimaryDisconnectUiPending())
            assertTrue(machine.consumePrimaryDisconnectUiPending())
            assertFalse(machine.isPrimaryDisconnectUiPending())
            assertFalse(machine.consumePrimaryDisconnectUiPending())
        } finally {
            observer.dispose()
            machine.acknowledgePrimaryDisconnectUiHandled()
        }
    }

    @Test
    fun `simulated non-robot ui event emits type with null robot`() {
        val machine = RevConnectionStateMachine.getInstance()
        val observer: TestObserver<RevConnectionStateMachine.UiEvent> = machine.observeUiEvents().test()

        try {
            machine.emitUiEventForSimulator(RevConnectionStateMachine.UiEventType.REQUEST_PERMISSIONS)

            observer.assertValueCount(1)
            observer.assertValueAt(0) {
                it.type == RevConnectionStateMachine.UiEventType.REQUEST_PERMISSIONS && it.robot == null
            }
        } finally {
            observer.dispose()
        }
    }

    @Test
    fun `discovery recommendation runnable emits recommendation when scanning without candidates`() {
        val machine = RevConnectionStateMachine.getInstance()
        setPrivateField(machine, "connectionState", RevConnectionStateMachine.ConnectionState.SCANNING)
        setPrivateField(machine, "hasSeenCandidateInSession", false)
        val observer: TestObserver<RevConnectionStateMachine.UiEvent> = machine.observeUiEvents().test()

        try {
            val runnable = getPrivateField(machine, "discoveryRecommendRunnable") as Runnable
            runnable.run()

            observer.assertValueCount(1)
            observer.assertValueAt(0) { it.type == RevConnectionStateMachine.UiEventType.DISCOVERY_RECOMMENDED }
            assertEquals("No device yet. You can open discovery mode.", machine.getLastScanStatus())
            assertFalse(machine.isLastScanStatusRetryVisible())
        } finally {
            observer.dispose()
        }
    }

    @Test
    fun `discovery recommendation runnable is suppressed after candidate has been seen`() {
        val machine = RevConnectionStateMachine.getInstance()
        setPrivateField(machine, "connectionState", RevConnectionStateMachine.ConnectionState.SCANNING)
        setPrivateField(machine, "hasSeenCandidateInSession", true)
        val observer: TestObserver<RevConnectionStateMachine.UiEvent> = machine.observeUiEvents().test()

        try {
            val runnable = getPrivateField(machine, "discoveryRecommendRunnable") as Runnable
            runnable.run()

            observer.assertNoValues()
        } finally {
            observer.dispose()
        }
    }

    @Test
    fun `scan timeout runnable sets idle state and retry status when not connected`() {
        val machine = RevConnectionStateMachine.getInstance()
        setPrivateField(machine, "connectionState", RevConnectionStateMachine.ConnectionState.SCANNING)

        val runnable = getPrivateField(machine, "scanTimeoutRunnable") as Runnable
        runnable.run()

        assertEquals("No REV found. Tap retry or open discovery.", machine.getLastScanStatus())
        assertTrue(machine.isLastScanStatusRetryVisible())
        assertEquals(RevConnectionStateMachine.ConnectionState.IDLE, machine.getConnectionState())
    }

    @Test
    fun `setConnectionState updates status and connect timestamp for scan hold`() {
        val machine = RevConnectionStateMachine.getInstance()

        invokePrivateMethod(
            machine,
            "setConnectionState",
            arrayOf(RevConnectionStateMachine.ConnectionState::class.java),
            arrayOf(RevConnectionStateMachine.ConnectionState.SCAN_HOLD)
        )

        assertEquals(RevConnectionStateMachine.ConnectionState.SCAN_HOLD, machine.getConnectionState())
        assertEquals("REV candidate found, preparing to connect", machine.getLastScanStatus())
        assertFalse(machine.isLastScanStatusRetryVisible())
        assertTrue((getPrivateField(machine, "connectTimestamp") as Long) > 0L)
    }

    @Test
    fun `start emits permission request when bluetooth permissions are missing`() {
        val machine = RevConnectionStateMachine.getInstance()
        val appContext = RuntimeEnvironment.getApplication()
        val callback = Proxy.newProxyInstance(
            REVRobot.REVRobotInterface::class.java.classLoader,
            arrayOf(REVRobot.REVRobotInterface::class.java)
        ) { _, _, _ -> null } as REVRobot.REVRobotInterface
        val observer: TestObserver<RevConnectionStateMachine.UiEvent> = machine.observeUiEvents().test()

        try {
            machine.start(appContext, callback)

            observer.assertValue { it.type == RevConnectionStateMachine.UiEventType.REQUEST_PERMISSIONS }
            assertEquals("Bluetooth permission missing", machine.getLastScanStatus())
            assertTrue(machine.isLastScanStatusRetryVisible())
            assertEquals(RevConnectionStateMachine.ConnectionState.IDLE, machine.getConnectionState())
        } finally {
            observer.dispose()
        }
    }

    private fun resetSingletonInstance() {
        val instanceField: Field = RevConnectionStateMachine::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun invokePrivateMethod(target: Any, methodName: String, types: Array<Class<*>>, args: Array<Any?>): Any? {
        val method: Method = target::class.java.getDeclaredMethod(methodName, *types)
        method.isAccessible = true
        return method.invoke(target, *args)
    }
}
