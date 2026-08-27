package com.wowwee.revandroidsampleproject.fragments

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.wowwee.bluetoothrobotcontrollib.rev.REVCommandValues.kRevSendIRCommand
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.network.GameEventType
import com.wowwee.revandroidsampleproject.network.GameStatePacket
import com.wowwee.revandroidsampleproject.pvp.GameSessionCoordinator
import com.wowwee.revandroidsampleproject.pvp.PvpEvent
import com.wowwee.revandroidsampleproject.robot.REVRobotEvent
import com.wowwee.revandroidsampleproject.utils.AppPreferences
import com.wowwee.revandroidsampleproject.utils.DriveCommandSampler
import com.wowwee.revandroidsampleproject.utils.HapticUtils
import com.wowwee.revandroidsampleproject.utils.JoystickView
import com.wowwee.revandroidsampleproject.utils.Player
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlin.math.sin
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
        private const val HIT_STUN_MS = 3000L
        private const val FIRE_RELOAD_MS = 5000L
        private const val HIT_SHAKE_MS = 420L
        private const val HIT_VIBRATION_DURATION = 220L
        private const val DEFAULT_HIT_DAMAGE = 1
        private const val UNKNOWN_ATTACKER_ID = "UNKNOWN"
        private const val DRAWABLE_LEVEL_MAX = 10000

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
    private lateinit var btnStartGame: Button
    private lateinit var btnMode: Button
    private lateinit var btnSimulateHit: Button
    private lateinit var btnSimulateOtherHit: Button
    private lateinit var tvTitle: TextView
    private lateinit var hitSplashOverlay: View

    private var wheelPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var leverPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var currentWheelRotationDeg = 0f
    private var lastWheelRotationUpdateMs = 0L
    private var wheelCenterAnimator: ValueAnimator? = null
    private var leverCenterAnimator: ValueAnimator? = null
    private var fireCooldownAnimator: ValueAnimator? = null
    private var hitStunUntilMs: Long = 0L
    private var fireCooldownUntilMs: Long = 0L
    private var localHitsTakenCount: Int = 0
    private var otherPlayerHitsTakenCount: Int = 0
    private var localPlayerId: String? = null
    private var isWaitingForGameAck: Boolean = false
    private var isSessionActive: Boolean = false
    private var fireButtonLayers: LayerDrawable? = null

    private val movementVector = floatArrayOf(0f, 0f)
    private val sendVector = floatArrayOf(0f, 0f)
    private val pvpDisposables = CompositeDisposable()

    private val driveHandler = Handler(Looper.getMainLooper())
    private val clearHitStunRunnable = Runnable { clearHitStun() }
    private val clearFireCooldownRunnable = Runnable { clearFireCooldown() }
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
        btnStartGame = view.findViewById(R.id.btnStartGame)
        btnMode = view.findViewById(R.id.btnMode)
        btnSimulateHit = view.findViewById(R.id.btnSimulateHit)
        btnSimulateOtherHit = view.findViewById(R.id.btnSimulateOtherHit)
        tvTitle = view.findViewById(R.id.tvDriverModeTitle)
        hitSplashOverlay = view.findViewById(R.id.hitSplashOverlay)

        fireButtonLayers = (btnFire.background as? LayerDrawable)
        updateFireButtonReloadVisual(1f, active = true)

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
            if (isFireCooldownActive()) {
                return@setOnClickListener
            }
            rev?.let { robot ->
                Player.getInstance().gunFire(robot, 0)
            }
            startFireCooldown()
        }
        btnMode.setOnClickListener {
            showModeMenu()
        }
        btnStartGame.setOnClickListener {
            startGameAndWaitForAck()
        }
        btnSimulateHit.setOnClickListener {
            triggerSimulatedHit()
        }
        btnSimulateOtherHit.setOnClickListener {
            triggerSimulatedOtherPlayerHit()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (!prepareConnectedRev(ARG_DEVICE_ADDRESS)) {
            return
        }

        switchToDriverMode()
        updateDriverTitle()
        btnSimulateHit.visibility = if (isSimulatorMode()) View.VISIBLE else View.GONE
        btnSimulateOtherHit.visibility = if (isSimulatorMode()) View.VISIBLE else View.GONE
        updateStartGameButtonState()
        maybeShowFirstTimeInstructions()
        bindPvpEventsIfNeeded()

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

    private fun showModeMenu() {
        val context = context ?: return
        val kioskToggleLabel = getString(kioskModeToggleLabelResId())
        val deviceAddress = currentDeviceAddress(ARG_DEVICE_ADDRESS)
        val modeOptions = DrivingModeSwitch.modeOptionsExcluding(DrivingModeOption.ADVANCED)
        val labels = mutableListOf(kioskToggleLabel, getString(R.string.driver_mode_help))
        labels.addAll(DrivingModeSwitch.modeLabels(context, modeOptions))

        AlertDialog.Builder(context)
            .setTitle(R.string.driver_mode_menu_title)
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == 0) {
                    toggleKioskLockDisabledByUser()
                    return@setItems
                }

                if (which == 1) {
                    showDriverInstructions()
                    return@setItems
                }

                val selectedMode = modeOptions.getOrNull(which - 2) ?: return@setItems
                DrivingModeSwitch.switchToMode(this, selectedMode, deviceAddress)
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        driveHandler.removeCallbacks(clearHitStunRunnable)
        driveHandler.removeCallbacks(clearFireCooldownRunnable)
        driveHandler.removeCallbacks(driveLoopRunnable)
        pvpDisposables.clear()
        fireCooldownAnimator?.cancel()
        fireCooldownAnimator = null
        fireCooldownUntilMs = 0L
        updateFireButtonReloadVisual(1f, active = true)
        movementVector[0] = 0f
        movementVector[1] = 0f
        releaseWheelControl()
        releaseLeverControl()
        updateDriverTitle()
        sendDriveVector(0f, 0f)
    }

    private fun bindPvpEventsIfNeeded() {
        if (pvpDisposables.size() > 0) {
            return
        }

        pvpDisposables.add(
            GameSessionCoordinator.events
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ event ->
                    onPvpEvent(event)
                }, {
                })
        )
    }

    private fun onPvpEvent(event: PvpEvent) {
        when (event) {
            is PvpEvent.GameSessionActive -> {
                localPlayerId = event.localId
                isWaitingForGameAck = false
                isSessionActive = true
                updateStartGameButtonState()
            }

            is PvpEvent.RemotePlayerStateUpdated -> {
                handleRemotePlayerStateUpdate(event.packet)
            }

            is PvpEvent.PvpNetworkError -> {
                if (isWaitingForGameAck) {
                    isWaitingForGameAck = false
                    updateStartGameButtonState()
                }
            }

            else -> Unit
        }
    }

    private fun startGameAndWaitForAck() {
        if (isWaitingForGameAck) {
            return
        }
        isSessionActive = false
        isWaitingForGameAck = true
        updateStartGameButtonState()
        GameSessionCoordinator.startGame()
    }

    private fun updateStartGameButtonState() {
        when {
            isSessionActive -> {
                btnStartGame.isEnabled = true
                btnStartGame.text = getString(R.string.advanced_mode_start_game_active_short)
                btnStartGame.alpha = 1f
            }

            isWaitingForGameAck -> {
                btnStartGame.isEnabled = false
                btnStartGame.text = getString(R.string.advanced_mode_start_game_waiting_short)
                btnStartGame.alpha = 0.8f
            }

            else -> {
                btnStartGame.isEnabled = true
                btnStartGame.text = getString(R.string.advanced_mode_start_game_short)
                btnStartGame.alpha = 1f
            }
        }
    }

    private fun handleRemotePlayerStateUpdate(packet: GameStatePacket) {
        if (packet.eventType != GameEventType.IR_HIT_TAKEN) {
            return
        }
        val localIdSnapshot = localPlayerId
        if (!localIdSnapshot.isNullOrBlank() && packet.senderId == localIdSnapshot) {
            return
        }
        if (localIdSnapshot.isNullOrBlank() && packet.senderId == displayRevName()) {
            return
        }

        otherPlayerHitsTakenCount = packet.totalHitsReceived.coerceAtLeast(0)
        updateDriverTitle()
    }

    private fun updateDriverTitle() {
        if (localHitsTakenCount == 0 && otherPlayerHitsTakenCount == 0) {
            tvTitle.text = getString(R.string.advanced_mode_title_format, displayRevName())
            return
        }
        tvTitle.text = getString(
            R.string.advanced_mode_title_score_format,
            displayRevName(),
            localHitsTakenCount,
            otherPlayerHitsTakenCount
        )
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

    private fun sendDriveTick() {
        if (isHitStunned()) {
            return
        }
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
        if (isHitStunned()) {
            return
        }
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

    override fun onRevEvent(event: REVRobotEvent) {
        when {
            event is REVRobotEvent.RobotCommandProcessed -> {
                val dataArray = event.command.dataArray
                if (event.command.cmdByte == kRevSendIRCommand && dataArray.size >= 2) {
                    handleRevDidReceiveIRCommand(event.robot, dataArray[0], dataArray[1])
                }
            }
            else -> super.onRevEvent(event)
        }
    }

    private fun handleRevDidReceiveIRCommand(robot: REVRobot?, irCommand: Byte, rxSensor: Byte) {
        DriveCommandSampler.logDrive(
            source = "advanced.hit",
            x = 0f,
            y = 0f,
            note = "irCommand=${irCommand.toInt()} rxSensor=${rxSensor.toInt()}"
        )


        Player.getInstance().getShot(robot, irCommand, activity)
        GameSessionCoordinator.registerHitTaken(UNKNOWN_ATTACKER_ID, DEFAULT_HIT_DAMAGE)
        localHitsTakenCount += 1
        updateDriverTitle()

        startHitStun()
    }

    private fun triggerSimulatedHit() {
        // Drive the same local hit handler directly so simulator hit always registers and emits UDP.
        handleRevDidReceiveIRCommand(robot = null, irCommand = 0, rxSensor = 3)
    }

    private fun triggerSimulatedOtherPlayerHit() {
        val simulatedPacket = GameStatePacket(
            senderId = "SIM_REMOTE",
            packetId = SystemClock.uptimeMillis(),
            timestamp = System.currentTimeMillis(),
            health = 100,
            eventType = GameEventType.IR_HIT_TAKEN,
            totalHitsReceived = otherPlayerHitsTakenCount + 1
        )
        onPvpEvent(PvpEvent.RemotePlayerStateUpdated(simulatedPacket))
    }

    private fun startHitStun() {
        resetDrivingControlsForHitStun()
        hitStunUntilMs = SystemClock.uptimeMillis() + HIT_STUN_MS
        driveHandler.removeCallbacks(clearHitStunRunnable)
        driveHandler.postDelayed(clearHitStunRunnable, HIT_STUN_MS)

        rev?.revStop()

        enableDrive(false)

        playHitSplashAnimation()
        vibrateOnHit()
    }

    private fun resetDrivingControlsForHitStun() {
        // Force-reset both controls before disabling touch so pointer IDs never remain latched.
        wheelCenterAnimator?.cancel()
        leverCenterAnimator?.cancel()
        wheelPointerId = MotionEvent.INVALID_POINTER_ID
        leverPointerId = MotionEvent.INVALID_POINTER_ID
        movementVector[0] = 0f
        movementVector[1] = 0f
        rotateWheelBase(0f, force = true)
        resetWheelThumbPosition()
        resetLeverThumbPosition()
        updateWheelThumbVisual(false)
    }

    private fun clearHitStun() {
        hitStunUntilMs = 0L
        enableDrive(true)
        hitSplashOverlay.visibility = View.GONE
        hitSplashOverlay.alpha = 0f
        updateDriverTitle()
    }

    private fun enableDrive(enabled: Boolean) {
        touchArea.isEnabled = enabled
        btnFire.isEnabled = enabled && !isFireCooldownActive()
        btnMode.isEnabled = enabled
    }

    private fun startFireCooldown() {
        fireCooldownUntilMs = SystemClock.uptimeMillis() + FIRE_RELOAD_MS

        driveHandler.removeCallbacks(clearFireCooldownRunnable)
        driveHandler.postDelayed(clearFireCooldownRunnable, FIRE_RELOAD_MS)

        fireCooldownAnimator?.cancel()
        btnFire.isEnabled = false
        updateFireButtonReloadVisual(0f, active = false)

        fireCooldownAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FIRE_RELOAD_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                updateFireButtonReloadVisual(animator.animatedValue as Float, active = false)
            }
            start()
        }
    }

    private fun clearFireCooldown() {
        fireCooldownUntilMs = 0L
        fireCooldownAnimator?.cancel()
        fireCooldownAnimator = null
        btnFire.isEnabled = !isHitStunned()
        updateFireButtonReloadVisual(1f, active = true)
    }

    private fun isFireCooldownActive(): Boolean {
        return SystemClock.uptimeMillis() < fireCooldownUntilMs
    }

    private fun updateFireButtonReloadVisual(progress: Float, active: Boolean) {
        val safeProgress = progress.coerceIn(0f, 1f)
        val level = (safeProgress * DRAWABLE_LEVEL_MAX).toInt().coerceIn(0, DRAWABLE_LEVEL_MAX)
        fireButtonLayers?.findDrawableByLayerId(R.id.fire_fill_layer)?.level = level

        if (active) {
            btnFire.text = getString(R.string.fire)
            btnFire.alpha = 1f
            return
        }

        val remainingMs = (fireCooldownUntilMs - SystemClock.uptimeMillis()).coerceAtLeast(0L)
        val remainingSeconds = ((remainingMs + 999L) / 1000L).toInt()
        btnFire.text = getString(R.string.fire_reloading_seconds, remainingSeconds)
        btnFire.alpha = 0.95f
    }

    private fun isHitStunned(): Boolean {
        return SystemClock.uptimeMillis() < hitStunUntilMs
    }

    private fun playHitSplashAnimation() {
        hitSplashOverlay.visibility = View.VISIBLE
        hitSplashOverlay.alpha = 1f

        val shakeTargets = listOf(wheelControl, leverControl, btnFire, btnStartGame, btnMode)
        val amplitudePx = 18f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = HIT_SHAKE_MS
            addUpdateListener { animator ->
                val phase = (animator.animatedFraction * 8f * Math.PI).toFloat()
                val offset = sin(phase) * amplitudePx
                shakeTargets.forEach { target ->
                    target.translationX = offset
                }
            }
            start()
        }

        hitSplashOverlay.animate()
            .alpha(0.35f)
            .setDuration(HIT_STUN_MS)
            .start()
    }

    private fun vibrateOnHit() {
        HapticUtils.vibrate(context, HIT_VIBRATION_DURATION)
    }
}


