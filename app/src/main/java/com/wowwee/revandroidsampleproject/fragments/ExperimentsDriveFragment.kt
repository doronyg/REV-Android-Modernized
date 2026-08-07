package com.wowwee.revandroidsampleproject.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.wowwee.bluetoothrobotcontrollib.RobotCommand
import com.wowwee.bluetoothrobotcontrollib.rev.REVCommandValues
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.utils.DriveCommandSampler
import com.wowwee.revandroidsampleproject.utils.REVPlayer
import kotlin.math.abs
import kotlin.math.max

class ExperimentsDriveFragment : ConnectedRevFragment() {

    companion object {
        private const val ARG_DEVICE_ADDRESS = "arg_device_address"
        private const val DEFAULT_DURATION_MS = 240L
        private const val DEFAULT_INTERVAL_MS = 40L
        private const val MAX_SIGNED_SPEED = 32
        private const val SEEK_CENTER = 32
        private const val DURATION_MIN_MS = 100
        private const val DURATION_MAX_MS = 5000
        private const val INTERVAL_MIN_MS = 20
        private const val INTERVAL_MAX_MS = 500

        @JvmStatic
        fun newInstance(deviceAddress: String?): ExperimentsDriveFragment {
            val fragment = ExperimentsDriveFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ADDRESS, deviceAddress)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var sbDrive: SeekBar
    private lateinit var sbTurn: SeekBar
    private lateinit var sbDuration: SeekBar
    private lateinit var sbInterval: SeekBar
    private lateinit var tvDriveValue: TextView
    private lateinit var tvTurnValue: TextView
    private lateinit var tvDurationValue: TextView
    private lateinit var tvIntervalValue: TextView
    private lateinit var btnExperimentStart: Button
    private lateinit var btnExperimentStop: Button
    private lateinit var btnExperimentBack: Button
    private lateinit var tvExperimentSummary: TextView

    private val commandHandler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var startAtMs = 0L
    private var durationMs = DEFAULT_DURATION_MS
    private var intervalMs = DEFAULT_INTERVAL_MS
    private var commandCount = 0
    private var commandByteDrive: Byte = 0
    private var commandByteTurn: Byte = 0
    private var driveSignedSpeed = 0
    private var turnSignedSpeed = 0

    override fun layoutId(): Int = R.layout.fragment_experiments_drive

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) ?: return null

        sbDrive = view.findViewById(R.id.sbExperimentDrive)
        sbTurn = view.findViewById(R.id.sbExperimentTurn)
        sbDuration = view.findViewById(R.id.sbExperimentDuration)
        sbInterval = view.findViewById(R.id.sbExperimentInterval)
        tvDriveValue = view.findViewById(R.id.tvExperimentDriveValue)
        tvTurnValue = view.findViewById(R.id.tvExperimentTurnValue)
        tvDurationValue = view.findViewById(R.id.tvExperimentDurationValue)
        tvIntervalValue = view.findViewById(R.id.tvExperimentIntervalValue)
        btnExperimentStart = view.findViewById(R.id.btnExperimentStart)
        btnExperimentStop = view.findViewById(R.id.btnExperimentStop)
        btnExperimentBack = view.findViewById(R.id.btnExperimentBack)
        tvExperimentSummary = view.findViewById(R.id.tvExperimentSummary)

        setupSliders()

        btnExperimentStart.setOnClickListener { startExperiment() }
        btnExperimentStop.setOnClickListener { stopExperiment(true) }
        btnExperimentBack.setOnClickListener { navigateBackToManual() }

        updateSummary(getString(R.string.experiment_summary_idle))
        updateRunningState(false)
        return view
    }

    override fun onResume() {
        super.onResume()
        rev = resolveTargetRev(ARG_DEVICE_ADDRESS)
        if (rev == null) {
            navigateBackToScan()
            return
        }

        rev?.setCallbackInterface(this)
        REVPlayer.getInstance().setPlayerRev(rev)
        rev?.revSetTrackingMode(REVRobotConstant.revRobotTrackingMode.REVTrackingUserControl)
    }

    override fun onPause() {
        super.onPause()
        stopExperiment(true)
    }

    override fun revDeviceDisconnected(rev: REVRobot?) {
        commandHandler.post {
            stopExperiment(false)
            navigateBackToScan()
        }
    }

    private fun setupSliders() {
        sbDrive.max = MAX_SIGNED_SPEED * 2
        sbTurn.max = MAX_SIGNED_SPEED * 2
        sbDuration.max = DURATION_MAX_MS - DURATION_MIN_MS
        sbInterval.max = INTERVAL_MAX_MS - INTERVAL_MIN_MS

        sbDrive.progress = SEEK_CENTER
        sbTurn.progress = SEEK_CENTER
        sbDuration.progress = (DEFAULT_DURATION_MS.toInt() - DURATION_MIN_MS).coerceAtLeast(0)
        sbInterval.progress = (DEFAULT_INTERVAL_MS.toInt() - INTERVAL_MIN_MS).coerceAtLeast(0)

        val valueListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSliderLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }

        sbDrive.setOnSeekBarChangeListener(valueListener)
        sbTurn.setOnSeekBarChangeListener(valueListener)
        sbDuration.setOnSeekBarChangeListener(valueListener)
        sbInterval.setOnSeekBarChangeListener(valueListener)

        updateSliderLabels()
    }

    private fun startExperiment() {
        val robot = rev ?: return
        if (isRunning) {
            stopExperiment(true)
        }

        driveSignedSpeed = sliderToSignedSpeed(sbDrive.progress)
        turnSignedSpeed = sliderToSignedSpeed(sbTurn.progress)
        durationMs = (DURATION_MIN_MS + sbDuration.progress).toLong()
        intervalMs = (INTERVAL_MIN_MS + sbInterval.progress).toLong()

        commandByteDrive = buildDriveByte(driveSignedSpeed)
        commandByteTurn = buildTurnByte(turnSignedSpeed)

        startAtMs = System.currentTimeMillis()
        commandCount = 0
        isRunning = true
        updateRunningState(true)

        DriveCommandSampler.logDrive(
            source = "experiment.summary",
            x = 0f,
            y = 0f,
            note = "durationMs=$durationMs intervalMs=$intervalMs driveByte=${commandByteDrive.toInt()} turnByte=${commandByteTurn.toInt()}"
        )

        val commandRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) {
                    return
                }
                val elapsed = System.currentTimeMillis() - startAtMs
                if (elapsed >= durationMs) {
                    stopExperiment(true)
                    return
                }

                sendContinuousDriveCommand(robot, commandByteDrive, commandByteTurn)
                commandCount += 1
                commandHandler.postDelayed(this, intervalMs)
            }
        }

        commandRunnable.run()
        updateSummary(
            getString(
                R.string.experiment_summary_running,
                durationMs,
                intervalMs,
                commandByteDrive.toInt(),
                commandByteTurn.toInt()
            )
        )
    }

    private fun stopExperiment(sendStop: Boolean) {
        commandHandler.removeCallbacksAndMessages(null)
        if (sendStop) {
            rev?.revStop()
            DriveCommandSampler.logDrive(source = "experiment.stop", x = 0f, y = 0f, note = "commands=$commandCount")
        }
        isRunning = false
        updateRunningState(false)
        updateSummary(
            getString(
                R.string.experiment_summary_stopped,
                commandCount
            )
        )
    }

    private fun sendContinuousDriveCommand(robot: REVRobot, driveByte: Byte, turnByte: Byte) {
        val command = RobotCommand.create(REVCommandValues.kRevDrive_Continuous, driveByte, turnByte)
        robot.sendRobotCommand(command)

        val driveNormalized = normalizedDriveForLog()
        val turnNormalized = normalizedTurnForLog()
        DriveCommandSampler.logDrive(
            source = "experiment.step",
            x = turnNormalized,
            y = driveNormalized,
            note = "driveByte=${driveByte.toInt()} turnByte=${turnByte.toInt()} idx=${commandCount + 1}"
        )
    }

    private fun buildDriveByte(signedSpeed: Int): Byte {
        if (signedSpeed == 0) {
            return 0
        }
        val magnitude = abs(signedSpeed).coerceIn(1, MAX_SIGNED_SPEED)
        val base = if (signedSpeed > 0) {
            REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_FW_Speed1.getValue().toInt()
        } else {
            REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_BW_Speed1.getValue().toInt()
        }
        return (base + (magnitude - 1)).toByte()
    }

    private fun buildTurnByte(signedSpeed: Int): Byte {
        if (signedSpeed == 0) {
            return 0
        }
        val magnitude = abs(signedSpeed).coerceIn(1, MAX_SIGNED_SPEED)
        val base = if (signedSpeed < 0) {
            REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_Left_Speed1.getValue().toInt()
        } else {
            REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_Right_Speed1.getValue().toInt()
        }
        return (base + (magnitude - 1)).toByte()
    }

    private fun updateSummary(text: String) {
        tvExperimentSummary.text = text
    }

    private fun updateRunningState(running: Boolean) {
        btnExperimentStart.isEnabled = !running
        btnExperimentStop.isEnabled = running
    }

    private fun normalizedDriveForLog(): Float {
        return driveSignedSpeed.coerceIn(-MAX_SIGNED_SPEED, MAX_SIGNED_SPEED) / MAX_SIGNED_SPEED.toFloat()
    }

    private fun normalizedTurnForLog(): Float {
        return turnSignedSpeed.coerceIn(-MAX_SIGNED_SPEED, MAX_SIGNED_SPEED) / MAX_SIGNED_SPEED.toFloat()
    }

    private fun sliderToSignedSpeed(progress: Int): Int {
        return (progress - SEEK_CENTER).coerceIn(-MAX_SIGNED_SPEED, MAX_SIGNED_SPEED)
    }

    private fun updateSliderLabels() {
        val drive = sliderToSignedSpeed(sbDrive.progress)
        val turn = sliderToSignedSpeed(sbTurn.progress)
        val duration = DURATION_MIN_MS + sbDuration.progress
        val interval = INTERVAL_MIN_MS + sbInterval.progress

        val fwd = max(drive, 0)
        val bwd = max(-drive, 0)
        val right = max(turn, 0)
        val left = max(-turn, 0)

        tvDriveValue.text = getString(R.string.experiment_drive_slider_value, bwd, fwd)
        tvTurnValue.text = getString(R.string.experiment_turn_slider_value, left, right)
        tvDurationValue.text = getString(R.string.experiment_duration_value, duration)
        tvIntervalValue.text = getString(R.string.experiment_interval_value, interval)
    }

    private fun navigateBackToManual() {
        val activity = activity ?: return
        stopExperiment(true)
        FragmentHelper.switchFragment(
            activity.supportFragmentManager,
            DriverModeFragment.newInstance(currentDeviceAddress(ARG_DEVICE_ADDRESS)),
            R.id.view_id_content,
            false
        )
    }
}





