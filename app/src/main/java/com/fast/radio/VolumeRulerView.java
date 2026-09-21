package com.fast.radio;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Vertical volume wheel from 0..200 percent. 100% is device maximum; 101..200% uses app gain. */
public class VolumeRulerView extends View {
    public interface Listener { void onValueChanged(int value); }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); private int value=100; private float lastY,scroll; private Listener listener; private float step;
    public VolumeRulerView(Context c, AttributeSet a){super(c,a); setFocusable(true);}
    public void setListener(Listener l){listener=l;} public int getValue(){return value;}
    public void setValue(int v){value=Math.max(0,Math.min(200,v));invalidate();}
    @Override protected void onDraw(Canvas c){ super.onDraw(c); float d=getResources().getDisplayMetrics().density, cy=getHeight()/2f; step=Math.max(5*d,Math.min(12*d,getHeight()/50f));
        paint.setStrokeWidth(2*d); paint.setColor(Color.rgb(70,190,255)); c.drawRoundRect(getWidth()/2f-5*d,8*d,getWidth()/2f+5*d,getHeight()-8*d,5*d,5*d,paint);
        for(int n=0;n<=200;n+=5){float y=cy+(n-value)*step;if(y<-15*d||y>getHeight()+15*d)continue;boolean major=n%20==0;float len=(major?22:11)*d;paint.setColor(major?Color.WHITE:Color.rgb(105,190,235));paint.setStrokeWidth((major?2:1)*d);c.drawLine(getWidth()/2f-len,y,getWidth()/2f-7*d,y,paint);c.drawLine(getWidth()/2f+7*d,y,getWidth()/2f+len,y,paint);paint.setTextSize((major?10:7)*d);paint.setTextAlign(Paint.Align.RIGHT);c.drawText(String.valueOf(n),getWidth()/2f-len-3*d,y+paint.getTextSize()/3,paint);}
        paint.setColor(Color.WHITE);paint.setStrokeWidth(3*d);c.drawLine(2*d,cy,getWidth()-2*d,cy,paint);paint.setColor(Color.rgb(0,170,255));c.drawCircle(getWidth()/2f,cy,8*d,paint);paint.setColor(Color.WHITE);paint.setTextSize(13*d);paint.setTextAlign(Paint.Align.LEFT);c.drawText(value+"%",getWidth()/2f+17*d,cy+5*d,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:lastY=e.getY();return true;case MotionEvent.ACTION_MOVE:float dy=e.getY()-lastY;lastY=e.getY();scroll+=dy;float st=Math.max(1f,step);while(scroll<=-st){value=Math.min(200,value+1);scroll+=st;}while(scroll>=st){value=Math.max(0,value-1);scroll-=st;}if(listener!=null)listener.onValueChanged(value);invalidate();return true;case MotionEvent.ACTION_UP:performClick();return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
