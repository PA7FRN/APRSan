package nl.pa7frn.aprsan;

/**
 * Created by earts001 on 2/19/2017.
 * manages notification audiofor APRS events
 */

import android.content.Context;
import android.media.ToneGenerator;
import android.media.AudioManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class AprsAudioManager {
    private boolean controlVolume = false;
    private boolean doDelaySound = false;
    private int volumeBackup;
    private int maxVol;
    private int soundToPlay = -1;
    private AudioManager audioManager;
    private ToneGenerator toneOnOff;
    private ScheduledExecutorService scheduleControlVolExecutor;
    private ScheduledExecutorService scheduleSoundDelayExecutor;
    private ScheduledFuture<?> playSoundScheduleHandle;

    AprsAudioManager(Context context) {
        toneOnOff = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        scheduleSoundDelayExecutor = Executors.newScheduledThreadPool(5);
    }

    private void setVolumeMax() {
        if (controlVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);
        }
    }

    void startControlVolume() {
        controlVolume = true;
        volumeBackup = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        scheduleControlVolExecutor = Executors.newScheduledThreadPool(5);
        scheduleControlVolExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() { setVolumeMax(); }
        }, 0, 10, TimeUnit.MINUTES);
    }

    void stopControlVolume() {
        if (controlVolume) {
            controlVolume = false;
            if (scheduleControlVolExecutor != null) {
                scheduleControlVolExecutor.shutdown();
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBackup, 0);
        }
    }

    void playSound(int toneType) {
        if (doDelaySound) {
            soundToPlay = toneType;
        }
        else {
            toneOnOff.startTone(toneType, 200);
            soundToPlay = -1;
        }
    }

    void delaySounds() {
        doDelaySound = true;

        if (playSoundScheduleHandle != null) {
            if (!playSoundScheduleHandle.isDone()) {
                playSoundScheduleHandle.cancel(true);
            }
        }
        playSoundScheduleHandle = scheduleSoundDelayExecutor.schedule(new Runnable() {
            public void run() {
                doDelaySound = false;
                if (soundToPlay > -1) {
                    playSound(soundToPlay);
                }
            }
        }, 7, TimeUnit.SECONDS); // 6<x<9

    }

}
