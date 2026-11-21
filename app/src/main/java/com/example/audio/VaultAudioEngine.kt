package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow

enum class WaveForm {
  SINE,
  TRIANGLE,
  SQUARE,
  SAW,
}

data class TrackBlueprint(
  val id: String,
  val label: String,
  val bpm: Int,
  val wave: WaveForm,
  val melodyNotes: IntArray, // MIDI note numbers, 0 = rest
  val bassNotes: IntArray, // MIDI note numbers matching melody length
)

object VaultTrackCatalog {
  val LOBBY =
    TrackBlueprint(
      id = "lobby",
      label = "Vault Lounge Synth",
      bpm = 92,
      wave = WaveForm.TRIANGLE,
      melodyNotes = intArrayOf(60, 64, 67, 71, 67, 64, 69, 72, 57, 60, 64, 69, 64, 60, 62, 67),
      bassNotes = intArrayOf(48, 0, 48, 0, 48, 0, 48, 0, 45, 0, 45, 0, 43, 0, 43, 0),
    )

  private val GAME_TRACKS: Map<String, TrackBlueprint> =
    listOf(
      TrackBlueprint(
        id = "neon_hop",
        label = "Neon Hop · Cyber Synth",
        bpm = 132,
        wave = WaveForm.SQUARE,
        melodyNotes = intArrayOf(69, 72, 76, 72, 79, 76, 72, 0, 67, 71, 74, 71, 76, 74, 71, 67),
        bassNotes = intArrayOf(45, 45, 57, 45, 45, 45, 57, 45, 43, 43, 55, 43, 41, 41, 53, 43),
      ),
      TrackBlueprint(
        id = "skyline_dash",
        label = "Skyline Dash · Rooftop Rush",
        bpm = 148,
        wave = WaveForm.SAW,
        melodyNotes = intArrayOf(64, 67, 71, 76, 74, 71, 67, 71, 64, 67, 71, 79, 76, 74, 71, 67),
        bassNotes = intArrayOf(40, 40, 52, 40, 43, 43, 55, 43, 45, 45, 57, 45, 47, 47, 59, 47),
      ),
      TrackBlueprint(
        id = "street_fighter_2d",
        label = "Street Fighter 2D · Dojo Clash",
        bpm = 144,
        wave = WaveForm.SQUARE,
        melodyNotes = intArrayOf(62, 63, 66, 69, 66, 63, 62, 0, 62, 66, 69, 74, 72, 69, 66, 63),
        bassNotes = intArrayOf(38, 38, 50, 38, 39, 39, 51, 39, 38, 38, 50, 38, 43, 41, 39, 38),
      ),
      TrackBlueprint(
        id = "flappy_bird",
        label = "Flappy Bird · Sky Bounce",
        bpm = 126,
        wave = WaveForm.SQUARE,
        melodyNotes = intArrayOf(67, 71, 74, 79, 74, 71, 67, 0, 69, 72, 76, 79, 76, 72, 74, 0),
        bassNotes = intArrayOf(43, 0, 55, 0, 43, 0, 55, 0, 48, 0, 52, 0, 50, 0, 43, 0),
      ),
      TrackBlueprint(
        id = "hill_climb",
        label = "Hill Climb · Canyon Groove",
        bpm = 122,
        wave = WaveForm.SAW,
        melodyNotes = intArrayOf(64, 68, 71, 74, 71, 68, 64, 62, 64, 68, 71, 76, 74, 71, 69, 64),
        bassNotes = intArrayOf(40, 47, 40, 47, 45, 52, 45, 52, 40, 47, 40, 47, 47, 54, 45, 40),
      ),
      TrackBlueprint(
        id = "vault_chess",
        label = "Vault Chess · Gambit Baroque",
        bpm = 88,
        wave = WaveForm.TRIANGLE,
        melodyNotes = intArrayOf(62, 69, 65, 64, 62, 65, 64, 61, 62, 69, 70, 69, 67, 65, 64, 61),
        bassNotes = intArrayOf(50, 0, 46, 0, 45, 0, 44, 0, 50, 0, 46, 0, 43, 0, 45, 0),
      ),
      TrackBlueprint(
        id = "prism_2048",
        label = "Prism 2048 · Glass Pulse",
        bpm = 108,
        wave = WaveForm.SINE,
        melodyNotes = intArrayOf(65, 69, 72, 76, 72, 69, 67, 72, 65, 69, 74, 76, 74, 72, 69, 67),
        bassNotes = intArrayOf(41, 0, 48, 0, 43, 0, 50, 0, 41, 0, 48, 0, 45, 0, 43, 0),
      ),
      TrackBlueprint(
        id = "flux_circuit",
        label = "Flux Circuit · Voltage Grid",
        bpm = 118,
        wave = WaveForm.SQUARE,
        melodyNotes = intArrayOf(61, 64, 68, 73, 68, 64, 61, 68, 63, 66, 71, 75, 71, 66, 63, 68),
        bassNotes = intArrayOf(37, 49, 37, 49, 37, 49, 37, 49, 39, 51, 39, 51, 44, 56, 44, 56),
      ),
      TrackBlueprint(
        id = "vault_sweeper",
        label = "Vault Sweeper · Radar Stealth",
        bpm = 100,
        wave = WaveForm.TRIANGLE,
        melodyNotes = intArrayOf(55, 0, 58, 62, 0, 63, 62, 58, 55, 0, 60, 63, 62, 0, 58, 54),
        bassNotes = intArrayOf(43, 0, 43, 0, 43, 0, 46, 0, 48, 0, 48, 0, 46, 0, 42, 0),
      ),
      TrackBlueprint(
        id = "lumen_toggle",
        label = "Lumen Toggle · Matrix Chimes",
        bpm = 104,
        wave = WaveForm.SINE,
        melodyNotes = intArrayOf(63, 67, 70, 75, 70, 67, 63, 70, 65, 68, 72, 77, 72, 68, 65, 70),
        bassNotes = intArrayOf(39, 0, 51, 0, 46, 0, 51, 0, 41, 0, 53, 0, 46, 0, 53, 0),
      ),
      TrackBlueprint(
        id = "chroma_flood",
        label = "Chroma Flood · Pastel Drift",
        bpm = 112,
        wave = WaveForm.TRIANGLE,
        melodyNotes = intArrayOf(70, 74, 77, 74, 72, 75, 79, 75, 69, 72, 77, 72, 70, 74, 77, 82),
        bassNotes = intArrayOf(46, 0, 46, 0, 48, 0, 48, 0, 45, 0, 45, 0, 46, 0, 46, 0),
      ),
      TrackBlueprint(
        id = "nonogrid",
        label = "NonoGrid · Pixel Sketch",
        bpm = 98,
        wave = WaveForm.TRIANGLE,
        melodyNotes = intArrayOf(65, 69, 72, 69, 67, 70, 74, 70, 65, 69, 72, 77, 76, 72, 70, 67),
        bassNotes = intArrayOf(41, 0, 53, 0, 43, 0, 55, 0, 41, 0, 53, 0, 48, 0, 43, 0),
      ),
      TrackBlueprint(
        id = "slide_15",
        label = "Slide-15 · Clockwork Gears",
        bpm = 114,
        wave = WaveForm.SQUARE,
        melodyNotes = intArrayOf(60, 63, 67, 63, 68, 67, 63, 62, 60, 63, 67, 72, 71, 67, 65, 62),
        bassNotes = intArrayOf(48, 36, 48, 36, 44, 32, 43, 31, 48, 36, 48, 36, 43, 31, 43, 31),
      ),
      TrackBlueprint(
        id = "hanoi_vault",
        label = "Tower of Hanoi · Zen Bells",
        bpm = 84,
        wave = WaveForm.SINE,
        melodyNotes = intArrayOf(62, 64, 69, 71, 74, 71, 69, 0, 64, 69, 71, 76, 74, 71, 69, 64),
        bassNotes = intArrayOf(50, 0, 0, 50, 45, 0, 0, 45, 47, 0, 0, 47, 50, 0, 0, 45),
      ),
      TrackBlueprint(
        id = "cipher_break",
        label = "Cipher Mastermind · Spy Protocol",
        bpm = 116,
        wave = WaveForm.SAW,
        melodyNotes = intArrayOf(59, 62, 66, 65, 64, 62, 59, 0, 59, 62, 66, 71, 69, 66, 64, 61),
        bassNotes = intArrayOf(47, 47, 47, 46, 45, 45, 47, 0, 47, 47, 47, 43, 42, 42, 47, 42),
      ),
      TrackBlueprint(
        id = "sudoku_six",
        label = "Sudoku Six · Lo-Fi Logic",
        bpm = 86,
        wave = WaveForm.SINE,
        melodyNotes = intArrayOf(68, 72, 75, 79, 75, 72, 70, 75, 68, 72, 77, 79, 77, 75, 72, 70),
        bassNotes = intArrayOf(44, 0, 51, 0, 46, 0, 53, 0, 44, 0, 51, 0, 49, 0, 46, 0),
      ),
      TrackBlueprint(
        id = "crate_sokoban",
        label = "Crate Pusher · Heavy Forge",
        bpm = 96,
        wave = WaveForm.SQUARE,
        melodyNotes = intArrayOf(52, 55, 57, 55, 52, 55, 59, 57, 52, 55, 60, 59, 57, 55, 54, 52),
        bassNotes = intArrayOf(40, 0, 40, 40, 43, 0, 43, 43, 45, 0, 45, 45, 47, 0, 42, 40),
      ),
      TrackBlueprint(
        id = "queens_grid",
        label = "Queens N-Grid · Royal Endgame",
        bpm = 94,
        wave = WaveForm.TRIANGLE,
        melodyNotes = intArrayOf(67, 70, 74, 78, 79, 74, 70, 74, 67, 72, 75, 79, 78, 74, 70, 66),
        bassNotes = intArrayOf(43, 0, 50, 0, 43, 0, 50, 0, 48, 0, 51, 0, 50, 0, 42, 0),
      ),
    ).associateBy { it.id }

  fun forKey(trackKey: String): TrackBlueprint =
    if (trackKey == "lobby") LOBBY else GAME_TRACKS[trackKey] ?: GAME_TRACKS.getValue("neon_hop")
}

object VaultAudioEngine {
  private const val SAMPLE_RATE = 22050

  private val currentTrackKey = MutableStateFlow("lobby")
  @Volatile private var musicEnabled = true
  @Volatile private var lobbySfxEnabled = true
  @Volatile private var inLobby = true
  @Volatile private var isForeground = true

  @Volatile private var running = false
  private var workerThread: Thread? = null

  private val clickBuffer: ShortArray by lazy { synthesizeClick(freqStart = 760.0, freqEnd = 1140.0, ms = 36) }
  private val confirmBuffer: ShortArray by lazy { synthesizeClick(freqStart = 880.0, freqEnd = 1320.0, ms = 48) }

  fun syncSettings(musicOn: Boolean, lobbySfxOn: Boolean, isLobbyScreen: Boolean) {
    musicEnabled = musicOn
    lobbySfxEnabled = lobbySfxOn
    inLobby = isLobbyScreen
    if (musicOn && isForeground) {
      ensureMusicRunning()
    } else if (!musicOn) {
      stopMusicThread()
    }
  }

  fun setTrack(trackKey: String) {
    if (currentTrackKey.value != trackKey) {
      currentTrackKey.value = trackKey
    }
    inLobby = (trackKey == "lobby")
    if (musicEnabled && isForeground) {
      ensureMusicRunning()
    }
  }

  fun onAppForegroundChanged(foreground: Boolean) {
    isForeground = foreground
    if (foreground && musicEnabled) {
      ensureMusicRunning()
    } else {
      stopMusicThread()
    }
  }

  /** Plays a synthesized UI button sound strictly when inside the lobby and toggled ON. */
  fun playLobbyButtonSound(accent: Boolean = false) {
    if (!lobbySfxEnabled || !inLobby || !isForeground) return
    val pcm = if (accent) confirmBuffer else clickBuffer
    thread(isDaemon = true, name = "VaultLobbySfx") {
      runCatching {
        val track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        Thread.sleep(70L)
        track.stop()
        track.release()
      }
    }
  }

  @Synchronized
  private fun ensureMusicRunning() {
    if (running) return
    running = true
    workerThread =
      thread(isDaemon = true, name = "VaultMusicSynth") {
        var audioTrack: AudioTrack? = null
        try {
          val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            .coerceAtLeast(2048)
          audioTrack =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(SAMPLE_RATE)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setTransferMode(AudioTrack.MODE_STREAM)
              .setBufferSizeInBytes(minBuf * 2)
              .build()
          audioTrack.play()

          var step = 0
          var activeId = ""
          var melPhase = 0.0
          var bassPhase = 0.0

          while (running && musicEnabled && isForeground) {
            val bp = VaultTrackCatalog.forKey(currentTrackKey.value)
            if (bp.id != activeId) {
              activeId = bp.id
              step = 0
            }

            // 16th-note step duration
            val stepSeconds = 60.0 / (bp.bpm * 2.0)
            val samplesPerStep = (SAMPLE_RATE * stepSeconds).toInt().coerceIn(800, 16000)
            val pcm = ShortArray(samplesPerStep)

            val melMidi = bp.melodyNotes[step % bp.melodyNotes.size]
            val bassMidi = bp.bassNotes[step % bp.bassNotes.size]
            val melFreq = if (melMidi > 0) midiToFreq(melMidi) else 0.0
            val bassFreq = if (bassMidi > 0) midiToFreq(bassMidi) else 0.0

            val melInc = 2.0 * PI * melFreq / SAMPLE_RATE
            val bassInc = 2.0 * PI * bassFreq / SAMPLE_RATE

            for (i in 0 until samplesPerStep) {
              val t = i.toDouble() / samplesPerStep
              val env =
                when {
                  t < 0.08 -> t / 0.08
                  t > 0.72 -> ((1.0 - t) / 0.28).coerceAtLeast(0.0)
                  else -> 1.0 - (t - 0.08) * 0.25
                }

              var sample = 0.0
              if (melFreq > 0.0) {
                sample += sampleWave(bp.wave, melPhase) * env * 0.14
                melPhase = (melPhase + melInc) % (2.0 * PI)
              }
              if (bassFreq > 0.0) {
                sample += sampleWave(WaveForm.TRIANGLE, bassPhase) * env * 0.11
                bassPhase = (bassPhase + bassInc) % (2.0 * PI)
              }

              val scaled = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
              pcm[i] = scaled.toShort()
            }

            audioTrack.write(pcm, 0, pcm.size)
            step = (step + 1) % 16
          }
        } catch (_: Throwable) {
          // Graceful no-op on environments without audio hardware (e.g., unit tests)
        } finally {
          runCatching {
            audioTrack?.stop()
            audioTrack?.release()
          }
          running = false
        }
      }
  }

  @Synchronized
  private fun stopMusicThread() {
    running = false
    workerThread = null
  }

  private fun midiToFreq(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)

  private fun sampleWave(wave: WaveForm, phase: Double): Double {
    val norm = phase / (2.0 * PI)
    return when (wave) {
      WaveForm.SINE -> sin(phase)
      WaveForm.TRIANGLE -> 1.0 - 4.0 * kotlin.math.abs(norm - 0.5)
      WaveForm.SQUARE -> if (norm < 0.5) 0.7 else -0.7
      WaveForm.SAW -> 2.0 * norm - 1.0
    }
  }

  private fun synthesizeClick(freqStart: Double, freqEnd: Double, ms: Int): ShortArray {
    val count = (SAMPLE_RATE * ms) / 1000
    val out = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val p = i.toDouble() / count
      val freq = freqStart + (freqEnd - freqStart) * p
      val env = (1.0 - p) * (1.0 - p)
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val s = sin(phase) * env * 0.35
      out[i] = (s * Short.MAX_VALUE).toInt().toShort()
    }
    return out
  }
}
