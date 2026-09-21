package com.fast.radio;

import androidx.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Equalizer;
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
    private Equalizer equalizer;
    private boolean userStopped = false;
    private android.os.Handler retryHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final BroadcastReceiver eqReceiver=new BroadcastReceiver(){@Override public void onReceive(android.content.Context c,Intent i){if(!"com.fast.radio.SET_EQ_BAND".equals(i.getAction()))return; try{int band=i.getIntExtra("band",0); short level=(short)i.getIntExtra("level",0); ensureEqualizer(); if(equalizer!=null && band>=0 && band<equalizer.getNumberOfBands()){short min=equalizer.getBandLevelRange()[0], max=equalizer.getBandLevelRange()[1]; equalizer.setBandLevel((short)band,(short)Math.max(min,Math.min(max,level)));}}catch(Exception ignored){}}};
    private final BroadcastReceiver volumeReceiver=new BroadcastReceiver(){@Override public void onReceive(android.content.Context c,Intent i){if(!"com.fast.radio.SET_VOLUME_GAIN".equals(i.getAction()))return;int p=i.getIntExtra("percent",100);try{if(player!=null&&player.getAudioSessionId()!=android.media.audiofx.AudioEffect.ERROR){if(loudnessEnhancer==null)loudnessEnhancer=new LoudnessEnhancer(player.getAudioSessionId());loudnessEnhancer.setTargetGain(Math.max(0,Math.min(600,(p-100)*6)));loudnessEnhancer.setEnabled(p>100);}}catch(Exception ignored){}}};

    @Override public void onCreate() {
        super.onCreate();
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(10000, 30000, 3000, 5000)
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
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
                .build();
        mediaSession = new MediaSession.Builder(this, player).build();
        player.addListener(new androidx.media3.common.Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){ if(state==androidx.media3.common.Player.STATE_READY) ensureEqualizer(); }
            @Override public void onPlayerError(androidx.media3.common.PlaybackException error){ if(!userStopped && player.getMediaItemCount()>0){ retryHandler.removeCallbacksAndMessages(null); retryHandler.postDelayed(()->{try{player.prepare();player.play();}catch(Exception ignored){}},1500); } }
        });
        if(android.os.Build.VERSION.SDK_INT>=33) registerReceiver(eqReceiver,new IntentFilter("com.fast.radio.SET_EQ_BAND"),android.content.Context.RECEIVER_NOT_EXPORTED); else registerReceiver(eqReceiver,new IntentFilter("com.fast.radio.SET_EQ_BAND"));
        if(android.os.Build.VERSION.SDK_INT>=33) registerReceiver(volumeReceiver,new IntentFilter("com.fast.radio.SET_VOLUME_GAIN"),android.content.Context.RECEIVER_NOT_EXPORTED); else registerReceiver(volumeReceiver,new IntentFilter("com.fast.radio.SET_VOLUME_GAIN"));
    }
    private void ensureEqualizer(){ try{ if(player==null)return; int sid=player.getAudioSessionId(); if(sid==android.media.audiofx.AudioEffect.ERROR)return; if(equalizer==null){ equalizer=new Equalizer(0,sid); equalizer.setEnabled(true); } }catch(Exception ignored){} }
    @Override public int onStartCommand(Intent intent,int flags,int startId){ userStopped=false; return START_STICKY; }
    public void markStopped(){ userStopped=true; if(retryHandler!=null)retryHandler.removeCallbacksAndMessages(null); }
    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return mediaSession; }
    @Override public void onDestroy() {
        if(mediaSession!=null){mediaSession.release();mediaSession=null;}
        try{unregisterReceiver(eqReceiver);}catch(Exception ignored){}
        try{unregisterReceiver(volumeReceiver);}catch(Exception ignored){}
        if(equalizer!=null){try{equalizer.release();}catch(Exception ignored){}equalizer=null;}
        if(loudnessEnhancer!=null){try{loudnessEnhancer.release();}catch(Exception ignored){}loudnessEnhancer=null;}
        if(player!=null){player.release();player=null;}
        super.onDestroy();
    }
}
