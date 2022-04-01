package com.riyazuddin

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.riyazuddin.Constants.MUSIC_NEW
import com.riyazuddin.Constants.MUSIC_PAUSE
import com.riyazuddin.Constants.MUSIC_PLAY
import com.riyazuddin.Constants.MUSIC_PLAYBACK_UPDATE
import com.riyazuddin.Constants.MUSIC_START
import com.riyazuddin.Constants.MUSIC_STOP
import com.riyazuddin.Constants.PLAYBACK
import com.riyazuddin.services.MainActivity
import com.riyazuddin.services.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicService : Service() {

    companion object {
        const val TAG = "MyTag"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val musicBinder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getMusicService() = this@MusicService
    }

    private fun setupMediaPlayer(id: Int) {
        mediaPlayer = MediaPlayer.create(this, id)
        mediaPlayer!!.setOnCompletionListener {
            sendCompleteBroadcast(MUSIC_STOP)
            stopServiceAndNotification()
        }
    }

    private fun sendCompleteBroadcast(action: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.action = MUSIC_PLAYBACK_UPDATE
        intent.putExtra(PLAYBACK, action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun stopServiceAndNotification() {
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ${intent.action}")
        CoroutineScope(Dispatchers.Default).launch {
            when (intent.action) {
                MUSIC_NEW -> {
                    stop()
                    showNotification(false)
                    sendCompleteBroadcast(MUSIC_PLAY)
                    setupMediaPlayer(intent.getIntExtra("song_id", R.raw.music))
                    play()
                }
                MUSIC_START -> {
                    play()
                    showNotification(false)
                    sendCompleteBroadcast(MUSIC_PLAY)
                }
                MUSIC_PLAY -> {
                    play()
                    sendCompleteBroadcast(MUSIC_PLAY)
                    showNotification(false)
                }
                MUSIC_PAUSE -> {
                    pause()
                    sendCompleteBroadcast(MUSIC_PAUSE)
                    showNotification(true)
                }
                MUSIC_STOP -> {
                    stop()
                    sendCompleteBroadcast(MUSIC_STOP)
                    stopServiceAndNotification()
                }
            }
            Log.i(TAG, "onStartCommand in: ${Thread.currentThread().name}")
        }
        return START_STICKY
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification(showPlayButton: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        val playOrPausePI: PendingIntent
        if (showPlayButton) {
            val playIntent = Intent(this, MusicService::class.java)
            playIntent.action = MUSIC_PLAY
            playOrPausePI =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getService(this, 500, playIntent, PendingIntent.FLAG_MUTABLE)
                } else {
                    PendingIntent.getService(
                        this,
                        500,
                        playIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
        } else {
            val pauseIntent = Intent(this, MusicService::class.java)
            pauseIntent.action = MUSIC_PAUSE
            playOrPausePI =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getService(this, 500, pauseIntent, PendingIntent.FLAG_MUTABLE)
                } else {
                    PendingIntent.getService(
                        this,
                        500,
                        pauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
        }

        val stopIntent = Intent(this, MusicService::class.java)
        stopIntent.action = MUSIC_STOP
        val stopPI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getService(this, 500, stopIntent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getService(this, 500, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notificationLayout = RemoteViews(application.packageName, R.layout.music_layout)
        if (showPlayButton) {
            notificationLayout.setImageViewResource(R.id.playOrPause, R.drawable.ic_play)
        } else {
            notificationLayout.setImageViewResource(R.id.playOrPause, R.drawable.ic_pause)
        }
        notificationLayout.setOnClickPendingIntent(R.id.playOrPause, playOrPausePI)
        notificationLayout.setOnClickPendingIntent(R.id.stop, stopPI)

        val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.EXTRA_MEDIA_SESSION)
            .build()
        startForeground(4, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            "CHANNEL_ID",
            "channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel("CHANNEL_ID") == null) {
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(p0: Intent?): IBinder = musicBinder

    fun isPlaying(): Boolean{
        if (mediaPlayer == null)
            return false;
        return mediaPlayer!!.isPlaying
    }

    private fun play() = mediaPlayer!!.start()

    private fun pause() = mediaPlayer!!.pause()

    private fun stop(){
        mediaPlayer?.release()
        mediaPlayer = null
    }

}