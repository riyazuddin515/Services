package com.riyazuddin.services

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.riyazuddin.Constants.MUSIC_NEW
import com.riyazuddin.Constants.MUSIC_PAUSE
import com.riyazuddin.Constants.MUSIC_PLAY
import com.riyazuddin.Constants.MUSIC_PLAYBACK_UPDATE
import com.riyazuddin.Constants.MUSIC_START
import com.riyazuddin.Constants.MUSIC_STOP
import com.riyazuddin.Constants.PLAYBACK
import com.riyazuddin.Constants.SONG_1
import com.riyazuddin.Constants.SONG_2
import com.riyazuddin.MusicService
import com.riyazuddin.services.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MyTag"
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var musicService: MusicService
    private var isBounded = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            musicService = (binder as MusicService.MusicBinder).getMusicService()
            isBounded = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBounded = false
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent) {
            Log.i(TAG, "onReceive: ${intent.action}")
            when (intent.getStringExtra(PLAYBACK)) {
                MUSIC_STOP -> {
                    binding.btnPlayOrPause.text = "Play"
                    binding.btnPlayOrPause.isVisible = false
                }
                MUSIC_PLAY -> {
                    binding.btnPlayOrPause.text = "Pause"
                    binding.btnPlayOrPause.isVisible = true
                }
                MUSIC_PAUSE -> {
                    binding.btnPlayOrPause.text = "Play"
                    binding.btnPlayOrPause.isVisible = true
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.song1.setOnClickListener {
            triggerEvent(SONG_1)
//            binding.progressBar.isVisible = true
        }
        binding.song2.setOnClickListener {
            triggerEvent(SONG_2)
        }

        binding.btnPlayOrPause.setOnClickListener {
            triggerEvent("")
        }
    }

    private fun triggerEvent(song: String) {
        if (isBounded) {
            val intent = Intent(this, MusicService::class.java)

            when (song) {
                SONG_1 -> {
                    intent.putExtra("song_id", R.raw.agar_tum_sath_ho)
                }
                SONG_2 -> {
                    intent.putExtra("song_id", R.raw.chaap_tilak)
                }
            }
            if (song.isEmpty()) {
                intent.action = MUSIC_START
                if (musicService.isPlaying()) {
                    intent.action = MUSIC_PAUSE
                }
            } else {
                intent.action = MUSIC_NEW
            }

            if (!binding.btnPlayOrPause.isVisible) {
                binding.btnPlayOrPause.isVisible = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(MUSIC_PLAYBACK_UPDATE))
    }

    override fun onStop() {
        if (isBounded) {
            unbindService(serviceConnection)
            LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(receiver)
        }
        super.onStop()
    }

}