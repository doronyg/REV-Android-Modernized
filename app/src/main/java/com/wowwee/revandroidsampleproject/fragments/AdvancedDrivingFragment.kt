package com.wowwee.revandroidsampleproject.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlin.math.sqrt

class AdvancedDrivingFragment : ConnectedRevFragment() {

    companion object {
        private const val ARG_DEVICE_ADDRESS = "arg_device_address"
        private const val DRIVE_LOOP_MS = 80L
        private const val DEFAULT_DRIVE_SPEED = 1.0f
        private const val DEFAULT_TURN_SPEED = 0.5f

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
    private lateinit var wheelThumb: ImageView
    private lateinit var leverThumb: ImageView
    private lateinit var btnFire: Button
    private lateinit var btnMode: Button
    private lateinit var btnDriverHelp: Button
    private lateinit var tvTitle: TextView

    private var wheelPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var leverPointerId: Int = MotionEvent.INVALID_POINTER_ID

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
        if (x < wheelControl.left || x > wheelControl.right || y < wheelControl.top || y > wheelControl.bottom) {
            return false
        }

        val centerX = (wheelControl.left + wheelControl.right) / 2f
        val centerY = wheelControl.bottom.toFloat()
        val radius = wheelControl.width / 2f
        val dx = x - centerX
        val dy = y - centerY
        return (dy <= 0f) && (dx * dx + dy * dy <= radius * radius)
    }

    private fun isInsideLeverControl(x: Float, y: Float): Boolean {
        return x >= leverControl.left && x <= leverControl.right && y >= leverControl.top && y <= leverControl.bottom
    }

    private fun updateWheelFromPoint(x: Float) {
        val centerX = (wheelControl.left + wheelControl.right) / 2f
        val radius = wheelControl.width / 2f
        val normalizedTurn = ((x - centerX) / radius).coerceIn(-1f, 1f)
        movementVector[0] = normalizedTurn
        positionWheelThumb(normalizedTurn)
    }

    private fun positionWheelThumb(turn: Float) {
        val radius = wheelControl.width / 2f
        val visualRadius = radius * 0.72f
        val centerX = wheelControl.width / 2f
        val centerY = wheelControl.height.toFloat()
        val x = turn * visualRadius
        val y = -sqrt((visualRadius * visualRadius - x * x).coerceAtLeast(0f))
        wheelThumb.x = centerX + x - wheelThumb.width / 2f
        wheelThumb.y = centerY + y - wheelThumb.height / 2f
    }

    private fun resetWheelThumbPosition() {
        positionWheelThumb(0f)
    }

    private fun updateLeverFromPoint(y: Float) {
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
        wheelPointerId = MotionEvent.INVALID_POINTER_ID
        movementVector[0] = 0f
        resetWheelThumbPosition()
    }

    private fun releaseLeverControl() {
        leverPointerId = MotionEvent.INVALID_POINTER_ID
        movementVector[1] = 0f
        resetLeverThumbPosition()
    }
}


