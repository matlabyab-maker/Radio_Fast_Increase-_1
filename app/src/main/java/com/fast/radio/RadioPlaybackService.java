package com.fast.radio;

import androidx.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.audiofx.LoudnessEnhancer;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class RadioPlaybackService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession mediaSession;
    private LoudnessEnhancer loudnessEnhancer;
    private final BroadcastReceiver volumeReceiver=new BroadcastReceiver(){@Override public void onReceive(android.content.Context c,Intent i){if(!"com.fast.radio.SET_VOLUME_GAIN".equals(i.getAction()))return;int p=i.getIntExtra("percent",100);try{if(player!=null&&player.getAudioSessionId()!=android.media.audiofx.AudioEffect.ERROR){if(loudnessEnhancer==null)loudnessEnhancer=new LoudnessEnhancer(player.getAudioSessionId());loudnessEnhancer.setTargetGain(Math.max(0,Math.min(600,(p-100)*6)));loudnessEnhancer.setEnabled(p>100);}}catch(Exception ignored){}}};

    @Override public void onCreate() {
        super.onCreate();
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(5000, 10000, 1000, 2000)
                .build();
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("Fast Radio/3.5")
                .setAllowCrossProtocolRedirects(true);
        player = new ExoPlayer.Builder(this)
                .setRenderersFactory(renderers)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                .build();
        mediaSession = new MediaSession.Builder(this, player).build();
        if(android.os.Build.VERSION.SDK_INT>=33) registerReceiver(volumeReceiver,new IntentFilter("com.fast.radio.SET_VOLUME_GAIN"),android.content.Context.RECEIVER_NOT_EXPORTED); else registerReceiver(volumeReceiver,new IntentFilter("com.fast.radio.SET_VOLUME_GAIN"));
    }
    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return mediaSession; }
    @Override public void onDestroy() {
        if(mediaSession!=null){mediaSession.release();mediaSession=null;}
        try{unregisterReceiver(volumeReceiver);}catch(Exception ignored){}
        if(loudnessEnhancer!=null){try{loudnessEnhancer.release();}catch(Exception ignored){}loudnessEnhancer=null;}
        if(player!=null){player.release();player=null;}
        super.onDestroy();
    }
}
