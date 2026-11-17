package com.nativebirds.game;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** SoundPool-backed effects. Silently degrades to nothing if a file cannot be opened. */
public class Audio implements Sfx {
    private final SoundPool pool;
    private final Map<String, Integer> ids = new HashMap<String, Integer>();
    private final List<Integer> loaded = new ArrayList<Integer>();
    private boolean ready;

    private static final String[] NAMES = {
            "bird_select", "bird_launch", "bird_fly", "bird_hit", "yellow_dash",
            "black_bomb", "blue_split", "white_egg", "wood_hit", "wood_break",
            "stone_hit", "stone_break", "ice_hit", "ice_break", "pig_hit",
            "pig_damage", "pig_pop", "pig_talk", "win", "lose", "star", "score", "start"
    };

    public Audio(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (status == 0) {
                    loaded.add(sampleId);
                    ready = true;
                }
            }
        });
        for (String n : NAMES) {
            try {
                AssetFileDescriptor fd = context.getAssets().openFd("sfx/" + n + ".mp3");
                int id = pool.load(fd, 1);
                fd.close();
                ids.put(n, id);
            } catch (Exception e) {
                // uncompressed asset not available - skip this sound
            }
        }
    }

    @Override
    public void play(String name, float volume, float rate) {
        if (!ready) {
            return;
        }
        Integer id = ids.get(name);
        if (id == null) {
            return;
        }
        try {
            pool.play(id, volume, volume, 1, 0, Math.max(0.5f, Math.min(2f, rate)));
        } catch (Exception ignored) {
        }
    }

    public void release() {
        pool.release();
    }
}
