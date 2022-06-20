package com.shong.workmanager_service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.*
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoundVibeService : Service() {
    private val TAG = this::class.java.simpleName + "_sHong"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        initSoundPool()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                Log.d(TAG, "알림음/진동 사용 ok / 휴대폰 모드 : 일반 모드")
                soundStart()
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                Log.d(TAG, "알림음/진동 사용 ok / 휴대폰 모드 : 진동 모드")
                vibeStart()
            }
            AudioManager.RINGER_MODE_SILENT -> {
                Log.d(TAG, "알림음/진동 사용 ok / 휴대폰 모드 : 무음 모드")
                null
            }
            else -> Log.d(TAG, "알림음/진동 사용 ok / 휴대폰 모드 : ${audioManager.ringerMode}")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        soundDestroy()
        super.onDestroy()
    }

    lateinit var soundPool: SoundPool
    var soundId: Int = 0
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder().apply {
            setMaxStreams(6)
            setAudioAttributes(audioAttributes)
        }.build()
    }

    private fun soundStart() {
        try {
            soundId = soundPool.load(this, R.raw.sound, 1)
            soundPool.setOnLoadCompleteListener { soundPool, i, i2 ->
                soundPool.play(soundId, 1F, 1F, 1, 0, 1f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "알림음 재생 에러")
        }
    }

    private fun soundDestroy() {
        soundPool.release()
    }

    private val VIBE_TIME = 500L
    private fun vibeStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val workRequest = OneTimeWorkRequestBuilder<VibrateWorker>().apply {
                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }.build()
                val workManager = WorkManager.getInstance(this)
                workManager.enqueue(workRequest)
            } catch (e: Exception) {
                Log.e(TAG, "진동 재생 에러")
            }
        } else {
            startForegroundService()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val effect = VibrationEffect.createOneShot(VIBE_TIME, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VIBE_TIME)
            }
            stopForeground(true)
        }
    }

    class VibrateWorker(val context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        private val VIBE_TIME = 500L
        private val TAG = this::class.java.simpleName + "_sHong"

        override suspend fun doWork(): Result {
            Log.d(TAG, "Vibrate doWork 실행")
            return try {
                try {
                    setForeground(gettingForegroundInfo())
                } catch (e: Exception) {
                    Log.e(TAG, "corountine worker setForeground ERROR! : $e")
                }
                withContext(Dispatchers.Main) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager =
                                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            val vibrationEffect = VibrationEffect.createOneShot(
                                VIBE_TIME,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                            val combinedVibration =
                                CombinedVibration.createParallel(vibrationEffect)
                            vibratorManager.vibrate(combinedVibration)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                            val effect = VibrationEffect.createOneShot(
                                VIBE_TIME,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                            vibrator.vibrate(effect)
                        } else {
                            val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                            vibrator.vibrate(VIBE_TIME)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "진동 발생 오류 $e")
                    }
                }
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Worker Exception $e")
                Result.failure()
            }
        }

        private suspend fun gettingForegroundInfo(): ForegroundInfo {
            val notiMaker = NotiMaker()
            val builder = notiMaker.getVibeNotiBuilder(context)

            return ForegroundInfo(5555, builder.build())
        }

    }

    private fun startForegroundService() {
        val notiMaker = NotiMaker()
        val builder = notiMaker.getVibeNotiBuilder(this)
        startForeground(5555, builder.build())
    }
}