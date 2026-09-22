package com.fast.radio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

/** Small animated audio dance display for the currently playing radio. */
public class AudioDanceView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final float[] levels = new float[12];
    private boolean running = false;
    private long phase = 0;

    public AudioDanceView(Context c){super(c); init();}
    public AudioDanceView(Context c, AttributeSet a){super(c,a); init();}
    public AudioDanceView(Context c, AttributeSet a, int s){super(c,a,s); init();}
    private void init(){
        paint.setStrokeCap(Paint.Cap.ROUND);
        setBackgroundColor(0xFF0B1522);
        for(int i=0;i<levels.length;i++) levels[i]=0.25f;
    }
    public void start(){ running=true; invalidate(); }
    public void stop(){ running=false; for(int i=0;i<levels.length;i++) levels[i]=0.08f; invalidate(); }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        paint.setColor(0xFF39B9FF);
        paint.setStrokeWidth(Math.max(3f, w/45f));
        float gap=w/(levels.length+1f);
        long now=System.currentTimeMillis();
        phase=now/70L;
        for(int i=0;i<levels.length;i++){
            if(running){
                double wave=(Math.sin((phase+i*2.1)*0.45)+1.0)/2.0;
                float jitter=0.15f+random.nextFloat()*0.35f;
                levels[i]=Math.min(1f, (float)(0.18+wave*0.45)+jitter);
            }
            float x=gap*(i+1);
            float barH=Math.max(3f, levels[i]*(h-8));
            c.drawLine(x,h-4,x,h-4-barH,paint);
        }
        if(running) postInvalidateDelayed(70);
    }
}
