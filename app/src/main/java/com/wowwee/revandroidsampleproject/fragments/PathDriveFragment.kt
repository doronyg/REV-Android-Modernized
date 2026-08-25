package com.wowwee.revandroidsampleproject.fragments

import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.robot.REVRobotEvent
import com.wowwee.revandroidsampleproject.utils.AppPreferences
import com.wowwee.revandroidsampleproject.utils.DriveCommandSampler
import com.wowwee.revandroidsampleproject.utils.PathDrawingView
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class PathDriveFragment : ConnectedRevFragment() {

    companion object {
        private const val ARG_DEVICE_ADDRESS = "arg_device_address"
        // Tune total replay time without changing movement power output.
        private const val PATH_DURATION_MULTIPLIER = 1.0f
        private const val BASE_TARGET_DURATION_MS = 5000L
        private const val MIN_TARGET_DURATION_MS = 2000L
        private const val MAX_TARGET_DURATION_MS = 20000L
        private const val OP_INTERVAL_MS = 40L
        private const val MIN_STEP_COUNT = 40
        private const val MAX_STEP_COUNT = 220
        private const val MIN_SAMPLE_DISTANCE_PX = 4f
        private const val MAX_SAMPLE_DISTANCE_PX = 12f
        private const val SEGMENT_MIN_PX = 6f
        private const val TURN_ACCUM_THRESHOLD_DEGREES = 30f
        private const val TURN_CHUNK_DEGREES = 30f
        private const val HEADING_JITTER_DEGREES = 1f
        private const val HEADING_LOOKAHEAD_POINTS = 3
        private const val MAX_DELTA_DEGREES_PER_STEP = 20f
        private const val MIN_TURN_EVIDENCE_COUNT = 1
        private const val TURN_POWER = 0.7f
        private const val DRIVE_POWER = 1f
        // Calibrate from experiments: canvas distance converted to forward runtime.
        private const val DRIVE_MS_PER_PX = 1.0f
        private const val DEFAULT_TURN_SPEED = 1.0f

        @JvmStatic
        fun newInstance(deviceAddress: String?): PathDriveFragment {
            val fragment = PathDriveFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ADDRESS, deviceAddress)
            fragment.arguments = args
            return fragment
        }
    }

    private enum class ActionType {
        FORWARD,
        TURN_LEFT,
        TURN_RIGHT
    }

    private data class DriveStep(
        val action: ActionType,
        val durationMs: Long
    )
    private data class ComputedRunData(
        val steps: List<DriveStep>,
        val rawPathLength: Float,
        val sampleDistancePx: Float
    )

    private lateinit var drawingView: PathDrawingView
    private lateinit var btnClear: Button
    private lateinit var btnRun: Button
    private lateinit var btnStop: Button
    private lateinit var btnBackToManual: Button
    private lateinit var tvDebugOverlay: TextView

    private val runHandler = Handler(Looper.getMainLooper())
    private var runSteps: List<DriveStep> = emptyList()
    private var currentStepIndex = 0
    private var isRunning = false

    override fun layoutId(): Int = R.layout.fragment_path_drive

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) ?: return null

        drawingView = view.findViewById(R.id.pathDrawingView)
        btnClear = view.findViewById(R.id.btnPathClear)
        btnRun = view.findViewById(R.id.btnPathRun)
        btnStop = view.findViewById(R.id.btnPathStop)
        btnBackToManual = view.findViewById(R.id.btnBackToManual)
        tvDebugOverlay = view.findViewById(R.id.tvPathDebugOverlay)

        btnClear.setOnClickListener {
            if (!isRunning) {
                drawingView.clearPath()
                showIdleDebugOverlay()
            }
        }
        btnRun.setOnClickListener { startRun() }
        btnStop.setOnClickListener { stopRun() }
        btnBackToManual.setOnClickListener { navigateBackToManual() }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (!prepareConnectedRev(ARG_DEVICE_ADDRESS)) {
            return
        }

        rev?.revSetTrackingMode(REVRobotConstant.revRobotTrackingMode.REVTrackingUserControl)

        maybeShowFirstTimeInstructions()
        updateRunningState(false)
        showIdleDebugOverlay()
    }

    override fun onPause() {
        super.onPause()
        stopRun()
    }

    override fun onRevEvent(event: REVRobotEvent) {
        if (isCurrentRevDisconnected(event)) {
            runHandler.post { stopRun() }
        }
        super.onRevEvent(event)
    }

    private fun maybeShowFirstTimeInstructions() {
        val context = context ?: return
        if (!AppPreferences.hasSeenPathModeInstructions(context)) {
            AlertDialog.Builder(context)
                .setTitle(R.string.path_mode_instructions_title)
                .setMessage(R.string.path_mode_instructions_body)
                .setPositiveButton(R.string.driver_mode_instructions_got_it, null)
                .show()
            AppPreferences.markSeenPathModeInstructions(context)
        }
    }

    private fun startRun() {
        if (isRunning) {
            return
        }
        val runData = computeRunData() ?: return
        val steps = runData.steps
        val rawPathLength = runData.rawPathLength
        val sampleDistancePx = runData.sampleDistancePx

        val actualDurationMs = steps.sumOf { it.durationMs }
        val vectors = steps.map { actionVector(it.action) }
        val forwardCount = steps.count { it.action == ActionType.FORWARD }
        val leftCount = steps.count { it.action == ActionType.TURN_LEFT }
        val rightCount = steps.count { it.action == ActionType.TURN_RIGHT }
        updateDebugOverlay(
            pathLengthPx = rawPathLength,
            durationMs = actualDurationMs,
            stepCount = steps.size,
            sampleDistancePx = sampleDistancePx,
            avgAbsDrive = vectors.map { abs(it.second) }.average().toFloat(),
            maxAbsDrive = vectors.maxOf { abs(it.second) }
        )
        DriveCommandSampler.logDrive(
            source = "path.summary",
            x = 0f,
            y = 0f,
            note = "len=${rawPathLength.toInt()} durationMs=$actualDurationMs steps=${steps.size} fwd=$forwardCount left=$leftCount right=$rightCount"
        )

        runSteps = steps
        currentStepIndex = 0
        updateRunningState(true)
        runCurrentStep()
    }

    private fun computeRunData(): ComputedRunData? {
        val rawPoints = drawingView.getPathPoints()
        val areaWidth = drawingView.width.toFloat()
        val areaHeight = drawingView.height.toFloat()
        if (!validateStartInputs(rawPoints, areaWidth, areaHeight)) {
            return null
        }

        val rawPathLength = calculatePathLength(rawPoints)
        if (!validatePathLength(rawPathLength)) {
            return null
        }

        val targetDurationMs = computeTargetDurationMs(rawPathLength, areaWidth, areaHeight)
        val desiredStepCount = ceil(targetDurationMs.toDouble() / OP_INTERVAL_MS.toDouble())
            .toInt()
            .coerceIn(MIN_STEP_COUNT, MAX_STEP_COUNT)
        val sampleDistancePx = (rawPathLength / desiredStepCount.toFloat())
            .coerceIn(MIN_SAMPLE_DISTANCE_PX, MAX_SAMPLE_DISTANCE_PX)

        val points = drawingView.getPlaybackPoints(sampleDistancePx)
        if (!validatePlaybackPoints(points)) {
            return null
        }

        val steps = buildSteps(points)
        if (!validateGeneratedSteps(steps)) {
            return null
        }

        return ComputedRunData(
            steps = steps,
            rawPathLength = rawPathLength,
            sampleDistancePx = sampleDistancePx
        )
    }

    private fun runCurrentStep() {
        val robot = rev
        if (!isRunning || currentStepIndex >= runSteps.size || robot == null || robot.isDead) {
            stopRun()
            return
        }

        val step = runSteps[currentStepIndex]
        val (x, y) = actionVector(step.action)
        val seconds = step.durationMs / 1000f
        val firmwareTimeField = (seconds * 100f).roundToInt().coerceIn(0, 255)
        val apiCall = when (step.action) {
            ActionType.FORWARD -> "revDriveForwardWithTime"
            ActionType.TURN_LEFT -> "revTurnLeftByTime"
            ActionType.TURN_RIGHT -> "revTurnRightByTime"
        }
        DriveCommandSampler.logDrive(
            source = "path.step",
            x = x,
            y = y,
            note = "idx=${currentStepIndex + 1}/${runSteps.size} action=${step.action} api=$apiCall durationMs=${step.durationMs} durationSec=$seconds timeField=$firmwareTimeField"
        )

        when (step.action) {
            ActionType.FORWARD -> robot.revDriveForwardWithTime(seconds)
            ActionType.TURN_LEFT -> robot.revTurnLeftByTime(seconds, DEFAULT_TURN_SPEED)
            ActionType.TURN_RIGHT -> robot.revTurnRightByTime(seconds, DEFAULT_TURN_SPEED)
        }

        currentStepIndex += 1
        runHandler.postDelayed({ runCurrentStep() }, step.durationMs)
    }

    private fun validateStartInputs(rawPoints: List<PointF>, areaWidth: Float, areaHeight: Float): Boolean {
        if (rawPoints.size < 2 || areaWidth <= 0f || areaHeight <= 0f) {
            showDrawFirstError()
            return false
        }
        return true
    }

    private fun validatePathLength(rawPathLength: Float): Boolean {
        if (rawPathLength < 1f) {
            showDrawFirstError()
            return false
        }
        return true
    }

    private fun validatePlaybackPoints(points: List<PointF>): Boolean {
        if (points.size < 2) {
            showDrawFirstError()
            return false
        }
        return true
    }

    private fun validateGeneratedSteps(steps: List<DriveStep>): Boolean {
        if (steps.isEmpty()) {
            showDrawFirstError()
            return false
        }
        return true
    }

    private fun showDrawFirstError() {
        Toast.makeText(requireContext(), R.string.path_mode_draw_first, Toast.LENGTH_SHORT).show()
    }

    private fun stopRun() {
        runHandler.removeCallbacksAndMessages(null)
        rev?.revStop()
        runSteps = emptyList()
        currentStepIndex = 0
        updateRunningState(false)
    }

    private fun updateRunningState(running: Boolean) {
        isRunning = running
        drawingView.setDrawingEnabled(!running)
        btnRun.isEnabled = !running
        btnClear.isEnabled = !running
        btnStop.isEnabled = running
    }

    private fun buildSteps(points: List<PointF>): List<DriveStep> {
        // Path-to-step algorithm (open-loop, discrete actions):
        // 1) Walk resampled segments and estimate heading from local lookahead points.
        // 2) Accumulate linear travel and heading change; ignore tiny heading jitter.
        // 3) When accumulated turn exceeds threshold, flush linear travel into
        //    FORWARD/BACKWARD timed steps, then emit quantized TURN steps.
        // 4) Repeat until the end and flush remaining linear travel.
        // This yields a simple one-action-at-a-time command stream suitable for
        // legacy hardware without wheel feedback.
        val ops = mutableListOf<DriveStep>()
        var previousHeadingDeg: Float? = null
        var pendingTurnDeg = 0f
        var turnEvidenceCount = 0
        var pendingForwardPx = 0f

        for (i in 0 until points.lastIndex) {
            val from = points[i]
            val to = points[i + 1]
            val dx = to.x - from.x
            val dy = to.y - from.y
            val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (distance < SEGMENT_MIN_PX) {
                continue
            }

            val headingDeg = segmentHeadingDegrees(points, i)
            val deltaDeg = if (previousHeadingDeg == null) {
                0f
            } else {
                normalizeDegrees(headingDeg - previousHeadingDeg)
                    .coerceIn(-MAX_DELTA_DEGREES_PER_STEP, MAX_DELTA_DEGREES_PER_STEP)
            }
            previousHeadingDeg = headingDeg
            pendingForwardPx += distance
            if (abs(deltaDeg) >= HEADING_JITTER_DEGREES) {
                pendingTurnDeg += deltaDeg
                turnEvidenceCount += 1
            } else {
                turnEvidenceCount = max(0, turnEvidenceCount - 1)
            }

            if (abs(pendingTurnDeg) >= TURN_ACCUM_THRESHOLD_DEGREES && turnEvidenceCount >= MIN_TURN_EVIDENCE_COUNT) {
                appendForwardSteps(ops, pendingForwardPx)
                pendingForwardPx = 0f

                pendingTurnDeg = flushTurnDebt(ops, pendingTurnDeg)
                turnEvidenceCount = 0
            }
        }

        flushTurnDebt(ops, pendingTurnDeg)

        appendForwardSteps(ops, pendingForwardPx)

        return ops
    }

    private fun flushTurnDebt(ops: MutableList<DriveStep>, pendingTurnDeg: Float): Float {
        var remaining = pendingTurnDeg

        while (abs(remaining) >= TURN_CHUNK_DEGREES) {
            val chunkSign = if (remaining > 0f) 1f else -1f
            appendTurnSteps(ops, chunkSign * TURN_CHUNK_DEGREES)
            remaining -= chunkSign * TURN_CHUNK_DEGREES
        }

        if (abs(remaining) >= HEADING_JITTER_DEGREES) {
            appendTurnSteps(ops, remaining)
            remaining = 0f
        }

        return remaining
    }

    private fun appendForwardSteps(ops: MutableList<DriveStep>, distancePx: Float) {
        if (distancePx <= 0f) {
            return
        }

        val requestedMs = distancePx * DRIVE_MS_PER_PX
        val repeatCount = quantizedPulseCount(requestedMs)
        repeat(repeatCount) {
            ops.add(DriveStep(ActionType.FORWARD, OP_INTERVAL_MS))
        }
    }

    private fun segmentHeadingDegrees(points: List<PointF>, segmentStart: Int): Float {
        val from = points[segmentStart]
        val to = points[min(points.lastIndex, segmentStart + HEADING_LOOKAHEAD_POINTS)]
        return Math.toDegrees(atan2((-(to.y - from.y)).toDouble(), (to.x - from.x).toDouble())).toFloat()
    }

    private fun appendTurnSteps(ops: MutableList<DriveStep>, turnDegrees: Float) {
        // Screen-space heading delta sign is opposite of robot turn command sign.
        val turnAction = if (turnDegrees > 0f) ActionType.TURN_LEFT else ActionType.TURN_RIGHT
        ops.add(DriveStep(turnAction, OP_INTERVAL_MS))
    }

    private fun actionVector(action: ActionType): Pair<Float, Float> {
        return when (action) {
            ActionType.FORWARD -> 0f to DRIVE_POWER
            ActionType.TURN_LEFT -> -TURN_POWER to 0f
            ActionType.TURN_RIGHT -> TURN_POWER to 0f
        }
    }

    private fun quantizedPulseCount(requestedMs: Float): Int {
        val quantizedMs = (requestedMs / OP_INTERVAL_MS.toFloat()).roundToLong() * OP_INTERVAL_MS
        return max(1L, quantizedMs / OP_INTERVAL_MS).toInt()
    }

    private fun normalizeDegrees(value: Float): Float {
        var out = value
        while (out > 180f) {
            out -= 360f
        }
        while (out < -180f) {
            out += 360f
        }
        return out
    }

    private fun computeTargetDurationMs(pathLengthPx: Float, areaWidth: Float, areaHeight: Float): Long {
        val diagonal = hypot(areaWidth.toDouble(), areaHeight.toDouble()).toFloat().coerceAtLeast(1f)
        val lengthRatio = pathLengthPx / diagonal
        val scaled = (BASE_TARGET_DURATION_MS * lengthRatio * PATH_DURATION_MULTIPLIER).roundToLong()
        return scaled.coerceIn(MIN_TARGET_DURATION_MS, MAX_TARGET_DURATION_MS)
    }

    private fun calculatePathLength(points: List<PointF>): Float {
        var total = 0f
        for (i in 0 until points.lastIndex) {
            val dx = points[i + 1].x - points[i].x
            val dy = points[i + 1].y - points[i].y
            total += hypot(dx.toDouble(), dy.toDouble()).toFloat()
        }
        return total
    }

    private fun showIdleDebugOverlay() {
        tvDebugOverlay.text = getString(R.string.path_mode_debug_idle)
    }

    private fun updateDebugOverlay(
        pathLengthPx: Float,
        durationMs: Long,
        stepCount: Int,
        sampleDistancePx: Float,
        avgAbsDrive: Float,
        maxAbsDrive: Float
    ) {
        val rateHz = if (durationMs > 0L) stepCount * 1000f / durationMs.toFloat() else 0f
        tvDebugOverlay.text = getString(
            R.string.path_mode_debug_template,
            pathLengthPx,
            durationMs / 1000f,
            stepCount,
            rateHz,
            sampleDistancePx,
            avgAbsDrive,
            maxAbsDrive
        )
    }

    private fun navigateBackToManual() {
        val activity = activity ?: return
        stopRun()
        FragmentHelper.switchFragment(
            activity.supportFragmentManager,
            AdvancedDrivingFragment.newInstance(currentDeviceAddress(ARG_DEVICE_ADDRESS)),
            R.id.view_id_content,
            false
        )
    }
}


