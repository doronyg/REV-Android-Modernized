package com.wowwee.revandroidsampleproject.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundEffects {

    // --- Audio Hardware Config ---
    private const val SAMPLE_RATE = 44100
    private const val MAX_PCM_AMPLITUDE = 32767.0
    private const val RELEASE_PADDING_MS = 30L

    // --- Crisp Minimal Mechanical Impact (120ms) ---
    private const val BUMP_DURATION_MS = 120
    private const val BUMP_START_FREQ_HZ = 800.0   // Bright mechanical impact crack
    private const val BUMP_BASE_FREQ_HZ = 120.0    // Mid-range body
    private const val BUMP_SWEEP_DECAY = 18.0      // Very quick snap
    private const val BUMP_IMPACT_GAIN = 0.70
    private const val BUMP_NOISE_GAIN = 0.40
    private const val BUMP_FILTER_ALPHA = 0.45     // Metallic crunch

    // --- Laser Hit Parameters ---
    private const val LASER_HIT_DURATION_MS = 300
    private const val LASER_HIT_START_FREQ_HZ = 2400.0
    private const val LASER_HIT_BASE_FREQ_HZ = 150.0
    private const val LASER_HIT_SWEEP_DECAY = 6.0
    private const val LASER_HIT_FM_MOD_FREQ_HZ = 120.0
    private const val LASER_HIT_FM_MOD_INDEX = 15.0
    private const val LASER_HIT_SIZZLE_DECAY = 15.0
    private const val LASER_HIT_BODY_DECAY = 4.5
    private const val LASER_HIT_TONE_GAIN = 0.8
    private const val LASER_HIT_SIZZLE_GAIN = 0.2

    // --- Low Sci-Fi Synth Point Scored Parameters (300ms) ---
    private const val POINT_SCORED_DURATION_MS = 300
    private const val C4_FREQ_HZ = 261.63          // C4: Warm mid-low base pitch
    private const val E4_FREQ_HZ = 329.63          // E4
    private const val G4_FREQ_HZ = 392.00          // G4
    private const val POINT_SCORED_GAIN = 0.70

    // --- Laser Shoot & Reload Sequence Parameters (5000ms total) ---
    private const val LASER_SHOOT_SEQUENCE_MS = 5000
    private const val BLAST_DURATION_MS = 500
    private const val PAUSE_DURATION_MS = 200
    private const val RELOAD_HUM_DURATION_MS = 3700 // 700ms to 4400ms
    private const val CHIME_DURATION_MS = 600       // Dramatic 600ms finish

    // Phase 1: Laser Blast
    private const val BLAST_START_FREQ_HZ = 3500.0
    private const val BLAST_END_FREQ_HZ = 400.0
    private const val BLAST_SWEEP_DECAY = 4.5
    private const val BLAST_VIBRATO_FREQ_HZ = 35.0
    private const val BLAST_VIBRATO_DEPTH = 120.0
    private const val BLAST_HARMONIC_GAIN = 0.3
    private const val BLAST_ENVELOPE_DECAY = 5.0

    // Phase 3: Single Monotonous Loading Hum
    private const val RELOAD_SINGLE_TONE_HZ = 440.0 // Single tone (A4)
    private const val RELOAD_HUM_GAIN = 0.02        // Soft background hum

    // Phase 4: High-Register Sci-Fi Power Sweep & Lock
    private const val LOCK_START_FREQ_HZ = 1200.0
    private const val LOCK_TARGET_FREQ_HZ = 4800.0
    private const val LOCK_GAIN = 0.60

    private val random = Random()
    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var wasInit = false

    // Cached PCM buffers
    private val bumpBuffer: ShortArray by lazy { generateBumpData() }
    private val laserHitBuffer: ShortArray by lazy { generateLaserHitData() }
    private val pointScoredBuffer: ShortArray by lazy { generatePointScoredData() }
    private val laserShootBuffer: ShortArray by lazy { generateLaserShootData() }

    fun warmUpCache() {
        if (wasInit) return
        wasInit = true
        audioScope.launch {
            bumpBuffer
            laserHitBuffer
            pointScoredBuffer
            laserShootBuffer
        }
    }

    fun playBump() {
        playPrecalculatedBuffer(bumpBuffer)
    }

    fun playLaserHit() {
        playPrecalculatedBuffer(laserHitBuffer)
    }

    fun playPointScored() {
        playPrecalculatedBuffer(pointScoredBuffer)
    }

    fun playLaserShoot() {
        playPrecalculatedBuffer(laserShootBuffer)
    }

    // --- Playback Engine ---

    private fun playPrecalculatedBuffer(buffer: ShortArray) {
        audioScope.launch {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            val durationMs = (buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong()
            delay(durationMs + RELEASE_PADDING_MS)

            audioTrack.release()
        }
    }

    // --- Math Generators ---

    private fun generateBumpData(): ShortArray {
        val numSamples = SAMPLE_RATE * BUMP_DURATION_MS / 1000
        val buffer = ShortArray(numSamples)
        var lastNoiseSample = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val bodyFreq = BUMP_START_FREQ_HZ * exp(-progress * BUMP_SWEEP_DECAY) + BUMP_BASE_FREQ_HZ
            val bodySignal = sin(2.0 * PI * bodyFreq * t)

            val rawNoise = random.nextDouble() * 2.0 - 1.0
            val filteredNoise = lastNoiseSample + BUMP_FILTER_ALPHA * (rawNoise - lastNoiseSample)
            lastNoiseSample = filteredNoise

            val bodyEnvelope = exp(-progress * 12.0)
            val noiseEnvelope = exp(-progress * 16.0)

            val mixed = (bodySignal * bodyEnvelope * BUMP_IMPACT_GAIN) +
                    (filteredNoise * noiseEnvelope * BUMP_NOISE_GAIN)

            buffer[i] = (mixed.coerceIn(-1.0, 1.0) * MAX_PCM_AMPLITUDE).toInt().toShort()
        }
        return buffer
    }

    private fun generateLaserHitData(): ShortArray {
        val numSamples = SAMPLE_RATE * LASER_HIT_DURATION_MS / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val carrierFreq = LASER_HIT_START_FREQ_HZ * exp(-progress * LASER_HIT_SWEEP_DECAY) + LASER_HIT_BASE_FREQ_HZ
            val modIndex = LASER_HIT_FM_MOD_INDEX * (1.0 - progress)
            val modulation = sin(2.0 * PI * LASER_HIT_FM_MOD_FREQ_HZ * t) * modIndex
            val laserSignal = sin(2.0 * PI * carrierFreq * t + modulation)

            val sizzleNoise = (random.nextDouble() * 2.0 - 1.0) * exp(-progress * LASER_HIT_SIZZLE_DECAY)
            val envelope = exp(-progress * LASER_HIT_BODY_DECAY)

            val mixed = (laserSignal * LASER_HIT_TONE_GAIN + sizzleNoise * LASER_HIT_SIZZLE_GAIN) * envelope
            buffer[i] = (mixed.coerceIn(-1.0, 1.0) * MAX_PCM_AMPLITUDE).toInt().toShort()
        }
        return buffer
    }

    private fun generatePointScoredData(): ShortArray {
        val numSamples = SAMPLE_RATE * POINT_SCORED_DURATION_MS / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val timeMs = (t * 1000).toInt()

            val targetFreq: Double
            val stepProgress: Double
            val isFinal: Boolean

            when {
                // Step 1: C4 (0 - 70ms)
                timeMs < 70 -> {
                    targetFreq = C4_FREQ_HZ
                    stepProgress = timeMs / 70.0
                    isFinal = false
                }
                // Step 2: E4 (70 - 150ms)
                timeMs < 150 -> {
                    targetFreq = E4_FREQ_HZ
                    stepProgress = (timeMs - 70) / 80.0
                    isFinal = false
                }
                // Step 3: G4 (150 - 300ms)
                else -> {
                    targetFreq = G4_FREQ_HZ
                    stepProgress = (timeMs - 150) / 150.0
                    isFinal = true
                }
            }

            // Dual Oscillator Synth Combo: Sine fundamental + Softened Square Wave for retro synth body
            val sineCore = sin(2.0 * PI * targetFreq * t)
            val squareTone = if (sin(2.0 * PI * targetFreq * t) >= 0) 0.3 else -0.3
            val synthSignal = (sineCore * 0.7) + (squareTone * 0.3)

            // Envelope: Fast attack per note, smooth exponential tail on the last note
            val envelope = if (!isFinal) {
                sin(stepProgress * PI)
            } else {
                exp(-stepProgress * 3.0)
            }

            val mixed = synthSignal * envelope * POINT_SCORED_GAIN
            buffer[i] = (mixed.coerceIn(-1.0, 1.0) * MAX_PCM_AMPLITUDE).toInt().toShort()
        }
        return buffer
    }

    private fun generateLaserShootData(): ShortArray {
        val numSamples = SAMPLE_RATE * LASER_SHOOT_SEQUENCE_MS / 1000
        val buffer = ShortArray(numSamples)
        var filteredNoise = 0.0

        val blastEndMs = BLAST_DURATION_MS
        val pauseEndMs = blastEndMs + PAUSE_DURATION_MS
        val reloadEndMs = pauseEndMs + RELOAD_HUM_DURATION_MS

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val timeMs = (t * 1000).toInt()

            var sampleVal = 0.0

            when {
                timeMs < blastEndMs -> {
                    val progress = timeMs.toDouble() / BLAST_DURATION_MS
                    val baseFreq = BLAST_START_FREQ_HZ * exp(-progress * BLAST_SWEEP_DECAY) + BLAST_END_FREQ_HZ
                    val vibrato = sin(2.0 * PI * BLAST_VIBRATO_FREQ_HZ * t) * (BLAST_VIBRATO_DEPTH * (1.0 - progress))
                    val currentFreq = (baseFreq + vibrato).coerceAtLeast(50.0)

                    val primarySignal = sin(2.0 * PI * currentFreq * t)
                    val harmonicSignal = sin(2.0 * PI * (currentFreq * 2.1) * t) * BLAST_HARMONIC_GAIN
                    val envelope = exp(-progress * BLAST_ENVELOPE_DECAY)

                    sampleVal = (primarySignal + harmonicSignal) * envelope
                }

                timeMs < pauseEndMs -> {
                    sampleVal = 0.0
                }

                timeMs < reloadEndMs -> {
                    val progress = (timeMs - pauseEndMs).toDouble() / RELOAD_HUM_DURATION_MS
                    val pureTone = sin(2.0 * PI * RELOAD_SINGLE_TONE_HZ * t)

                    val rawNoise = random.nextDouble() * 2.0 - 1.0
                    filteredNoise += 0.10 * (rawNoise - filteredNoise)

                    val combined = pureTone + filteredNoise
                    val volumeCurve = (0.3 + 0.7 * progress) * (progress.coerceAtMost(0.02) / 0.05)

                    sampleVal = combined * RELOAD_HUM_GAIN * volumeCurve
                }

                else -> {
                    val progress = (timeMs - reloadEndMs).toDouble() / CHIME_DURATION_MS
                    val carrierFreq = LOCK_START_FREQ_HZ * exp(progress * 1.3863)

                    val fmModFreq = 300.0 + (300.0 * progress)
                    val fmModIndex = 10.0 * (1.0 - progress * 0.4)
                    val modulation = sin(2.0 * PI * fmModFreq * t) * fmModIndex

                    val primaryLockSignal = sin(2.0 * PI * carrierFreq * t + modulation)
                    val highShimmer = sin(2.0 * PI * (carrierFreq * 1.5) * t) * 0.25 * progress

                    val envelope = if (progress < 0.75) {
                        (progress / 0.75)
                    } else {
                        exp(-(progress - 0.75) * 10.0)
                    }

                    sampleVal = (primaryLockSignal + highShimmer) * LOCK_GAIN * envelope
                }
            }

            buffer[i] = (sampleVal.coerceIn(-1.0, 1.0) * MAX_PCM_AMPLITUDE).toInt().toShort()
        }
        return buffer
    }
}