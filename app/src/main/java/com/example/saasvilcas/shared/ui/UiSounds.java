package com.example.saasvilcas.shared.ui;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.provider.Settings;
import android.view.View;
import com.example.saasvilcas.R;

/**
 * Short interface sounds played through a low-latency SoundPool.
 * Silent when the phone is on silent/vibrate or when the system "touch sounds" setting is off.
 */
public final class UiSounds {
    private static SoundPool pool;
    private static AudioManager audio;
    private static Context context;
    private static int tap, open, close, success, error, toggle, swipe, add;
    private static long lastPlayed;
    private UiSounds() { }

    public static void init(Context app) {
        if (pool != null) return;
        context = app.getApplicationContext();
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        pool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        tap = pool.load(context, R.raw.ui_tap, 1);
        open = pool.load(context, R.raw.ui_open, 1);
        close = pool.load(context, R.raw.ui_close, 1);
        success = pool.load(context, R.raw.ui_success, 1);
        error = pool.load(context, R.raw.ui_error, 1);
        toggle = pool.load(context, R.raw.ui_toggle, 1);
        swipe = pool.load(context, R.raw.ui_swipe, 1);
        add = pool.load(context, R.raw.ui_add, 1);
    }

    public static void tap(View source) { play(tap, .55f, false); }
    public static void open() { play(open, .5f, false); }
    public static void close() { play(close, .45f, false); }
    public static void success() { play(success, .7f, true); }
    public static void error() { play(error, .6f, true); }
    public static void toggle() { play(toggle, .55f, true); }
    public static void swipe() { play(swipe, .5f, false); }
    public static void add() { play(add, .6f, true); }

    private static java.lang.ref.WeakReference<Object> lastEvent = new java.lang.ref.WeakReference<>(null);
    /** Plays once per state object, so LiveData re-delivery after rotation stays quiet. */
    public static void forEvent(Object event, Runnable sound) {
        if (event == null || lastEvent.get() == event) return;
        lastEvent = new java.lang.ref.WeakReference<>(event);
        sound.run();
    }

    private static void play(int sound, float volume, boolean important) {
        if (pool == null || sound == 0 || !enabled()) return;
        // Coalesce bursts (e.g. a tap that immediately opens a modal) into one sound.
        long now = android.os.SystemClock.uptimeMillis();
        if (!important && now - lastPlayed < 70) return;
        lastPlayed = now;
        pool.play(sound, volume, volume, important ? 2 : 1, 0, 1f);
    }

    private static boolean enabled() {
        if (audio != null && audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) return false;
        return Settings.System.getInt(context.getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0;
    }
}
