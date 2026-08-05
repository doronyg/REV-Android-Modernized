package com.wowwee.revandroidsampleproject.fragments

import android.annotation.SuppressLint
import android.graphics.Point
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
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.utils.JoystickView
import com.wowwee.revandroidsampleproject.utils.Player
import com.wowwee.revandroidsampleproject.utils.REVPlayer

class DriverModeFragment : BaseViewFragment() {

    companion object {
        private const val ARG_DEVICE_ADDRESS = "arg_device_address"
        private const val DRIVE_LOOP_MS = 80L
        private const val DEFAULT_DRIVE_SPEED = 1.0f
        private const val DEFAULT_TURN_SPEED = 1.0f

        @JvmStatic
        fun newInstance(deviceAddress: String?): DriverModeFragment {
            val fragment = DriverModeFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ADDRESS, deviceAddress)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var touchArea: View
    private lateinit var joystickLeft: JoystickView
    private lateinit var joystickRight: JoystickView
    private lateinit var joystickThumbLeft: ImageView
    private lateinit var joystickThumbRight: ImageView
    private lateinit var btnFire: Button
    private lateinit var tvTitle: TextView

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
    private val driveTouchListener = View.OnTouchListener { v, event ->
        handleDriveTouch(v, event)
    }

    override fun layoutId(): Int = R.layout.fragment_driver_mode

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) ?: return null

        touchArea = view.findViewById(R.id.driver_touch_area)
        joystickLeft = view.findViewById(R.id.layoutleftJoystick)
        joystickRight = view.findViewById(R.id.layoutrightJoystick)
        joystickThumbLeft = view.findViewById(R.id.joystickL)
        joystickThumbRight = view.findViewById(R.id.joystickR)
        btnFire = view.findViewById(R.id.btnFire)
        tvTitle = view.findViewById(R.id.tvDriverModeTitle)

        joystickLeft.updateLeftView()
        joystickLeft.visibility = View.VISIBLE
        joystickRight.updateRightView()
        joystickRight.visibility = View.VISIBLE

        joystickLeft.post { lockJoystickToCurrentCenter(joystickLeft) }
        joystickRight.post { lockJoystickToCurrentCenter(joystickRight) }
        joystickThumbLeft.visibility = View.INVISIBLE
        joystickThumbRight.visibility = View.INVISIBLE

        touchArea.setOnTouchListener(driveTouchListener)
        btnFire.setOnClickListener {
            rev?.let { robot ->
                Player.getInstance().gunFire(robot, 0)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        rev = resolveTargetRev()
        if (rev == null) {
            navigateBackToScan()
            return
        }

        rev?.setCallbackInterface(this)
        REVPlayer.getInstance().setPlayerRev(rev)
        rev?.revSetTrackingMode(REVRobotConstant.revRobotTrackingMode.REVTrackingUserControl)
        tvTitle.text = "Driver Mode: ${rev?.name ?: "REV"}"

        driveHandler.removeCallbacks(driveLoopRunnable)
        driveHandler.post(driveLoopRunnable)
    }

    override fun onPause() {
        super.onPause()
        driveHandler.removeCallbacks(driveLoopRunnable)
        movementVector[0] = 0f
        movementVector[1] = 0f
        sendDriveVector(0f, 0f)
    }

    private fun handleDriveTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (!joystickLeft.isTouchToTrack()) {
                    if (joystickLeft.touchesBegan(event)) {
                        joystickThumbLeft.visibility = View.VISIBLE
                    }
                }
                if (!joystickRight.isTouchToTrack()) {
                    if (joystickRight.touchesBegan(event)) {
                        joystickThumbRight.visibility = View.VISIBLE
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    if (joystickLeft.isTouchToTrack(event, pointerId)) {
                        joystickLeft.touchesMoved(event, i)
                        movementVector[1] = joystickLeft.joystickVectorY
                    }
                    if (joystickRight.isTouchToTrack(event, pointerId)) {
                        joystickRight.touchesMoved(event, i)
                        movementVector[0] = joystickRight.joystickVectorX
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (joystickLeft.isTouchToTrack(event, pointerId)) {
                    joystickLeft.touchesEnded(event)
                    movementVector[1] = 0f
                    lockJoystickToCurrentCenter(joystickLeft)
                    joystickThumbLeft.visibility = View.INVISIBLE
                }
                if (joystickRight.isTouchToTrack(event, pointerId)) {
                    joystickRight.touchesEnded(event)
                    movementVector[0] = 0f
                    lockJoystickToCurrentCenter(joystickRight)
                    joystickThumbRight.visibility = View.INVISIBLE
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
        robot.revDrive(sendVector, DEFAULT_DRIVE_SPEED, DEFAULT_TURN_SPEED)
    }

    private fun sendDriveVector(x: Float, y: Float) {
        val robot = rev ?: return
        sendVector[0] = x
        sendVector[1] = y
        robot.revDrive(sendVector, DEFAULT_DRIVE_SPEED, DEFAULT_TURN_SPEED)
    }

    private fun resolveTargetRev(): REVRobot? {
        val requestedAddress = arguments?.getString(ARG_DEVICE_ADDRESS)
        if (!requestedAddress.isNullOrEmpty()) {
            for (robot in REVRobotFinder.getInstance().getmRevRobotConnectedList()) {
                val address = safeAddress(robot)
                if (requestedAddress.equals(address, ignoreCase = true)) {
                    return robot
                }
            }
        }

        return REVPlayer.getInstance().playerRev ?: REVRobotFinder.getInstance().firstConnectedREV()
    }

    private fun safeAddress(robot: REVRobot): String? {
        return try {
            robot.bluetoothDevice?.address
        } catch (_: SecurityException) {
            null
        }
    }

    private fun navigateBackToScan() {
        val activity = activity ?: return
        FragmentHelper.switchFragment(activity.supportFragmentManager, ScanFragment(), R.id.view_id_content, false)
    }

    private fun lockJoystickToCurrentCenter(joystick: JoystickView) {
        val center = Point(joystick.left + joystick.width / 2, joystick.top + joystick.height / 2)
        joystick.setCenter(center)
    }
}



