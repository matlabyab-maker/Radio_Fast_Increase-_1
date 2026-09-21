package com.fast.radio;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.content.Context;

/** Compatibility playback path for simple direct HTTP/HTTPS audio streams. */
public class LegacyMediaPlayerEngine {
    private final Context context; private MediaPlayer player;
    public LegacyMediaPlayerEngine(Context c){context=c.getApplicationContext();}
    public void play(String url) throws Exception {
        release(); player=new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        player.setDataSource(url); player.prepareAsync(); player.setOnPreparedListener(MediaPlayer::start);
    }
    public void stop(){if(player!=null&&player.isPlaying())player.stop();release();}
    public void release(){if(player!=null){try{player.reset();}catch(Exception ignored){}player.release();player=null;}}
}
