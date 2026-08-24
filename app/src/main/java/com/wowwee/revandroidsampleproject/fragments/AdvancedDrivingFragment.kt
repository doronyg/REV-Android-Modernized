package com.wowwee.revandroidsampleproject.fragments

import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.utils.AppPreferences
import com.wowwee.revandroidsampleproject.utils.DriveCommandSampler
import com.wowwee.revandroidsampleproject.utils.JoystickView
import com.wowwee.revandroidsampleproject.utils.Player
import com.wowwee.revandroidsampleproject.utils.REVPlayer
import android.os.SystemClock
import kotlin.math.sqrt

class AdvancedDrivingFragment : ConnectedRevFragment() {

    companion object {
        private const val ARG_DEVICE_ADDRESS = "arg_device_address"
        private const val DRIVE_LOOP_MS = 80L
        private const val DEFAULT_DRIVE_SPEED = 1.0f
        private const val DEFAULT_TURN_SPEED = 0.5f
        private const val MAX_WHEEL_ROTATION_DEG = 75f
        private const val MAX_WHEEL_ROTATION_SPEED_DEG_PER_SEC = 900f
        private const val CENTER_RETURN_ANIM_MS = 170L

        @JvmStatic
        fun newInstance(deviceAddress: String?): AdvancedDrivingFragment {
            val fragment = AdvancedDrivingFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ADDRESS, deviceAddress)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var touchArea: View
    private lateinit var wheelControl: JoystickView
    private lateinit var leverControl: JoystickView
    private lateinit var wheelBase: ImageView
    private lateinit var wheelThumb: ImageView
    private lateinit var leverThumb: ImageView
    private lateinit var btnFire: Button
    private lateinit var btnMode: Button
    private lateinit var btnDriverHelp: Button
    private lateinit var tvTitle: TextView

    private var wheelPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var leverPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var currentWheelRotationDeg = 0f
    private var lastWheelRotationUpdateMs = 0L
    private var wheelCenterAnimator: ValueAnimator? = null
    private var leverCenterAnimator: ValueAnimator? = null

    private val movementVector = floatArrayOf(0f, 0f)
    private val sendVector = floatArrayOf(0f, 0f)

    private val driveHandler = Handler(Looper.getMainLooper())
    private val driveLoopRunnable = object : Runnable {
        override fun run() {
            sendDriveTick()
            driveHandler.postDelayed(this, DRIVE_LOOP_MS)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val driveTouchListener = View.OnTouchListener { _, event ->
        handleDriveTouch(event)
    }

    override fun layoutId(): Int = R.layout.fragment_advanced_driving

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) ?: return null

        touchArea = view.findViewById(R.id.driver_touch_area)
        wheelControl = view.findViewById(R.id.layoutWheelControl)
        leverControl = view.findViewById(R.id.layoutLeverControl)
        wheelBase = view.findViewById(R.id.joystickBaseR)
        wheelThumb = view.findViewById(R.id.joystickR)
        leverThumb = view.findViewById(R.id.joystickL)
        btnFire = view.findViewById(R.id.btnFire)
        btnMode = view.findViewById(R.id.btnMode)
        btnDriverHelp = view.findViewById(R.id.btnDriverHelp)
        tvTitle = view.findViewById(R.id.tvDriverModeTitle)

        wheelControl.updateRightView()
        wheelControl.visibility = View.VISIBLE
        leverControl.updateLeftView()
        leverControl.visibility = View.VISIBLE

        wheelControl.post { resetWheelThumbPosition() }
        leverControl.post { resetLeverThumbPosition() }
        updateWheelThumbVisual(false)
        rotateWheelBase(0f, force = true)
        wheelControl.post {
            wheelBase.pivotX = wheelControl.width / 2f
            wheelBase.pivotY = wheelControl.height - wheelThumb.height / 2f
            lastWheelRotationUpdateMs = SystemClock.uptimeMillis()
        }

        touchArea.setOnTouchListener(driveTouchListener)
        btnFire.setOnClickListener {
            rev?.let { robot ->
                Player.getInstance().gunFire(robot, 0)
            }
        }
        btnMode.setOnClickListener {
            Toast.makeText(requireContext(), R.string.driver_mode_switch_hint, Toast.LENGTH_SHORT).show()
        }
        btnMode.setOnLongClickListener {
            showModeSelectionDialog()
            true
        }
        btnDriverHelp.setOnClickListener {
            showDriverInstructions()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        rev = resolveTargetRev(ARG_DEVICE_ADDRESS)
        if (rev == null && !isSimulatorMode()) {
            navigateBackToScan()
            return
        }

        rev?.setCallbackInterface(this)
        REVPlayer.getInstance().setPlayerRev(rev)
        switchToDriverMode()
        tvTitle.text = getString(R.string.advanced_mode_title_format, displayRevName())
        maybeShowFirstTimeInstructions()

        driveHandler.removeCallbacks(driveLoopRunnable)
        driveHandler.post(driveLoopRunnable)
    }

    private fun maybeShowFirstTimeInstructions() {
        val context = context ?: return
        if (!AppPreferences.hasSeenAdvancedModeInstructions(context)) {
            showDriverInstructions()
            AppPreferences.markSeenAdvancedModeInstructions(context)
        }
    }

    private fun showDriverInstructions() {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle(R.string.advanced_mode_instructions_title)
            .setMessage(R.string.advanced_mode_instructions_body)
            .setPositiveButton(R.string.driver_mode_instructions_got_it, null)
            .show()
    }

    private fun switchToDriverMode() {
        rev?.revSetTrackingMode(REVRobotConstant.revRobotTrackingMode.REVTrackingUserControl)
        movementVector[0] = 0f
        movementVector[1] = 0f
        sendDriveVector(0f, 0f)
    }

    private fun showModeSelectionDialog() {
        DrivingModeSwitch.showModeSelectionDialog(
            host = this,
            currentMode = DrivingModeOption.ADVANCED,
            deviceAddress = currentDeviceAddress(ARG_DEVICE_ADDRESS)
        )
    }

    override fun onPause() {
        super.onPause()
        driveHandler.removeCallbacks(driveLoopRunnable)
        movementVector[0] = 0f
        movementVector[1] = 0f
        releaseWheelControl()
        releaseLeverControl()
        sendDriveVector(0f, 0f)
    }

    private fun handleDriveTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                val pointerId = event.getPointerId(i)
                val x = event.getX(i)
                val y = event.getY(i)

                if (wheelPointerId == MotionEvent.INVALID_POINTER_ID && isInsideWheelControl(x, y)) {
                    wheelPointerId = pointerId
                    updateWheelFromPoint(x)
                    updateWheelThumbVisual(true)
                }

                if (leverPointerId == MotionEvent.INVALID_POINTER_ID && isInsideLeverControl(x, y)) {
                    leverPointerId = pointerId
                    updateLeverFromPoint(y)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    if (pointerId == wheelPointerId) {
                        updateWheelFromPoint(event.getX(i))
                    }
                    if (pointerId == leverPointerId) {
                        updateLeverFromPoint(event.getY(i))
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    releaseWheelControl()
                    releaseLeverControl()
                    updateWheelThumbVisual(false)
                    return true
                }

                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == wheelPointerId) {
                    releaseWheelControl()
                }
                if (pointerId == leverPointerId) {
                    releaseLeverControl()
                }
            }
        }

        return true
    }

    override fun revDeviceDisconnected(rev: REVRobot?) {
        driveHandler.post { navigateBackToScan() }
    }

    private fun sendDriveTick() {
        val robot = rev ?: return
        if (robot.isDead) {
            return
        }

        if (movementVector[0] == 0f && movementVector[1] == 0f) {
            return
        }

        sendVector[0] = movementVector[0]
        sendVector[1] = movementVector[1]
        if (sendVector[1] < 0f) {
            sendVector[0] *= -1f
        }
        DriveCommandSampler.logDrive(
            source = "advanced.tick",
            x = sendVector[0],
            y = sendVector[1],
            note = "mx=${movementVector[0]} my=${movementVector[1]}"
        )
        robot.revDrive(sendVector, DEFAULT_DRIVE_SPEED, DEFAULT_TURN_SPEED)
    }

    private fun sendDriveVector(x: Float, y: Float) {
        val robot = rev ?: return
        sendVector[0] = x
        sendVector[1] = y
        DriveCommandSampler.logDrive(source = "advanced.direct", x = sendVector[0], y = sendVector[1])
        robot.revDrive(sendVector, DEFAULT_DRIVE_SPEED, DEFAULT_TURN_SPEED)
    }

    private fun isInsideWheelControl(x: Float, y: Float): Boolean {
        val renderedLeft = wheelControl.x
        val renderedTop = wheelControl.y
        val renderedRight = renderedLeft + wheelControl.width
        val renderedBottom = renderedTop + wheelControl.height
        if (x < renderedLeft || x > renderedRight || y < renderedTop || y > renderedBottom) {
            return false
        }

        val centerX = renderedLeft + wheelControl.width / 2f
        val centerY = renderedTop + wheelBase.y + wheelBase.height
        val radius = wheelBase.width / 2f
        val dx = x - centerX
        val dy = y - centerY
        return (dy <= 0f) && (dx * dx + dy * dy <= radius * radius)
    }

    private fun isInsideLeverControl(x: Float, y: Float): Boolean {
        return x >= leverControl.left && x <= leverControl.right && y >= leverControl.top && y <= leverControl.bottom
    }

    private fun updateWheelFromPoint(x: Float) {
        wheelCenterAnimator?.cancel()
        val centerX = wheelControl.x + wheelControl.width / 2f
        val radius = wheelBase.width / 2f
        val inputRadius = (radius * 0.62f).coerceAtLeast(1f)
        val normalizedTurn = ((x - centerX) / inputRadius).coerceIn(-1f, 1f)
        movementVector[0] = normalizedTurn
        rotateWheelBase(normalizedTurn)
        positionWheelThumb(normalizedTurn)
    }

    private fun positionWheelThumb(turn: Float) {
        val radius = wheelControl.width / 2f
        val visualRadius = (radius - wheelThumb.width / 2f).coerceAtLeast(1f)
        val centerX = wheelControl.width / 2f
        val centerY = wheelControl.height - wheelThumb.height / 2f
        val x = turn * visualRadius
        val y = -sqrt((visualRadius * visualRadius - x * x).coerceAtLeast(0f))
        wheelThumb.x = centerX + x - wheelThumb.width / 2f
        wheelThumb.y = centerY + y - wheelThumb.height / 2f
    }

    private fun resetWheelThumbPosition() {
        positionWheelThumb(0f)
    }

    private fun updateLeverFromPoint(y: Float) {
        leverCenterAnimator?.cancel()
        val centerY = (leverControl.top + leverControl.bottom) / 2f
        val range = ((leverControl.height - leverThumb.height) / 2f).coerceAtLeast(1f)
        val normalizedDrive = ((centerY - y) / range).coerceIn(-1f, 1f)
        movementVector[1] = normalizedDrive
        positionLeverThumb(normalizedDrive)
    }

    private fun positionLeverThumb(drive: Float) {
        val centerX = leverControl.width / 2f
        val centerY = leverControl.height / 2f
        val range = ((leverControl.height - leverThumb.height) / 2f).coerceAtLeast(1f)
        leverThumb.x = centerX - leverThumb.width / 2f
        leverThumb.y = centerY - (drive * range) - leverThumb.height / 2f
    }

    private fun resetLeverThumbPosition() {
        positionLeverThumb(0f)
    }

    private fun releaseWheelControl() {
        val startTurn = movementVector[0]
        val startRotation = currentWheelRotationDeg
        wheelPointerId = MotionEvent.INVALID_POINTER_ID
        movementVector[0] = 0f
        animateWheelBackToCenter(startTurn, startRotation)
        updateWheelThumbVisual(false)
    }

    private fun releaseLeverControl() {
        val startDrive = movementVector[1]
        leverPointerId = MotionEvent.INVALID_POINTER_ID
        movementVector[1] = 0f
        animateLeverBackToCenter(startDrive)
    }

    private fun updateWheelThumbVisual(active: Boolean) {
        wheelThumb.setBackgroundResource(
            if (active) R.drawable.drive_thumb_race_car_active else R.drawable.drive_thumb_race_car
        )
        wheelThumb.animate()
            .scaleX(if (active) 1.08f else 1f)
            .scaleY(if (active) 1.08f else 1f)
            .setDuration(110)
            .start()
    }

    private fun rotateWheelBase(turn: Float, force: Boolean = false) {
        val targetRotation = turn.coerceIn(-1f, 1f) * MAX_WHEEL_ROTATION_DEG
        val now = SystemClock.uptimeMillis()

        if (force || lastWheelRotationUpdateMs <= 0L) {
            currentWheelRotationDeg = targetRotation
            wheelBase.rotation = targetRotation
            wheelThumb.rotation = targetRotation
            lastWheelRotationUpdateMs = now
            return
        }

        val dtMs = (now - lastWheelRotationUpdateMs).coerceAtLeast(1L)
        val maxStep = (MAX_WHEEL_ROTATION_SPEED_DEG_PER_SEC * dtMs / 1000f).coerceAtLeast(0.8f)
        val delta = (targetRotation - currentWheelRotationDeg).coerceIn(-maxStep, maxStep)
        currentWheelRotationDeg += delta
        wheelBase.rotation = currentWheelRotationDeg
        wheelThumb.rotation = currentWheelRotationDeg
        lastWheelRotationUpdateMs = now
    }

    private fun animateWheelBackToCenter(startTurn: Float, startRotation: Float) {
        wheelCenterAnimator?.cancel()
        wheelCenterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CENTER_RETURN_ANIM_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                val easedTurn = startTurn * (1f - t)
                currentWheelRotationDeg = startRotation * (1f - t)
                wheelBase.rotation = currentWheelRotationDeg
                wheelThumb.rotation = currentWheelRotationDeg
                positionWheelThumb(easedTurn)
            }
            start()
        }
    }

    private fun animateLeverBackToCenter(startDrive: Float) {
        leverCenterAnimator?.cancel()
        leverCenterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CENTER_RETURN_ANIM_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                positionLeverThumb(startDrive * (1f - t))
            }
            start()
        }
    }
}


