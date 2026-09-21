package com.fast.radio;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class RadioPlaybackService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession mediaSession;

    @Override public void onCreate() {
        super.onCreate();
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(5000, 10000, 1000, 2000)
                .build();
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        player = new ExoPlayer.Builder(this).setRenderersFactory(renderers).setLoadControl(loadControl)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                .build();
        mediaSession = new MediaSession.Builder(this, player).build();
    }
    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return mediaSession; }
    @Override public void onDestroy() {
        if(mediaSession!=null){mediaSession.release();mediaSession=null;}
        if(player!=null){player.release();player=null;}
        super.onDestroy();
    }
}
