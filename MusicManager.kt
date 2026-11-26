package com.example.mobilegame

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

class MusicManager(private val context: Context) {
    private val tracks = listOf(
        R.raw.music_bg1, R.raw.music_bg2, R.raw.music_bg3, R.raw.music_bg4, R.raw.music_bg5
    )

    private var player: MediaPlayer? = null
    private var musicLockoutUntil: Long = 0L
    private var triggeredMilestones: Set<Int> = emptySet()

    var currentTrackIndex by mutableIntStateOf(-1)
        private set

    val currentTrackNumber: Int
        get() = if (currentTrackIndex == -1) 0 else currentTrackIndex + 1

    fun stop() {
        player?.release()
        player = null
    }

    fun pause() {
        player?.pause()
    }

    fun resume(noSound: Boolean) {
        if (!noSound && player != null && player?.isPlaying == false) {
            player?.start()
        }
    }

    fun playNextTrack(noSound: Boolean) {
        if (noSound) return
        stop()
        currentTrackIndex = (currentTrackIndex + 1) % tracks.size
        try {
            player = MediaPlayer.create(context, tracks[currentTrackIndex])?.apply {
                isLooping = true
                start()
            }
            musicLockoutUntil = System.currentTimeMillis() + 5 * 60 * 1000
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkAndPlayForScore(currentScore: Int, noSound: Boolean) {
        if (noSound) return
        val milestones = listOf(50, 100, 200, 300, 400)
        val newlyReached = milestones.filter { currentScore >= it && it !in triggeredMilestones }
        if (newlyReached.isNotEmpty()) {
            triggeredMilestones = triggeredMilestones + newlyReached
            if (player?.isPlaying != true &&
                Random.nextFloat() <= 0.8f &&
                System.currentTimeMillis() > musicLockoutUntil
            ) {
                playNextTrack(noSound = false)
            }
        }
    }

    fun resetMilestones() {
        triggeredMilestones = emptySet()
    }
}