package com.fast.radio;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Full-height touch ruler for 1..400 kbps. Every integer has a tick/label; the selected value is centered. */
public class VerticalRulerView extends View {
    public interface Listener { void onValueChanged(int value); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float centerY;
    private int value = 15;
    private float lastY;
    private float scrollOffset;
    private Listener listener;
    private final float density;
    private float step;

    public VerticalRulerView(Context c, AttributeSet a) { super(c, a); density=getResources().getDisplayMetrics().density; paint.setTypeface(Typeface.create("sans", Typeface.NORMAL)); setFocusable(true); }
    public void setListener(Listener l){listener=l;}
    public int getValue(){return value;}
    public void setValue(int v){value=Math.max(1,Math.min(400,v)); invalidate();}
    private float pxPerDegree(){ return Math.max(5f*density, Math.min(14f*density, getHeight()/60f)); }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); centerY=getHeight()/2f; step=pxPerDegree();
        paint.setStrokeWidth(2*density); paint.setColor(Color.rgb(70,190,255));
        c.drawRoundRect(getWidth()/2f-5*density, 8*density, getWidth()/2f+5*density, getHeight()-8*density, 5*density,5*density,paint);
        // Draw every integer tick/label in the virtual ruler. The viewport shows the region around the selected value.
        for(int n=1;n<=400;n++){
            float y=centerY + (n-value)*step;
            if(y < -12*density || y > getHeight()+12*density) continue;
            boolean major = (n%10==0 || n==1 || n==400);
            float len=(major?24:12)*density;
            paint.setColor(major?Color.WHITE:Color.rgb(105,190,235)); paint.setStrokeWidth((major?2:1)*density);
            c.drawLine(getWidth()/2f-len, y, getWidth()/2f-7*density, y, paint);
            c.drawLine(getWidth()/2f+7*density, y, getWidth()/2f+len, y, paint);
            paint.setTextSize((major?11:7)*density); paint.setColor(major?Color.WHITE:Color.rgb(165,190,210));
            paint.setTextAlign(Paint.Align.RIGHT); c.drawText(String.valueOf(n), getWidth()/2f-len-4*density, y+paint.getTextSize()/3, paint);
        }
        // selected marker
        paint.setColor(Color.rgb(255,255,255)); paint.setStrokeWidth(3*density);
        c.drawLine(2*density, centerY, getWidth()-2*density, centerY, paint);
        paint.setColor(Color.rgb(0,170,255)); c.drawCircle(getWidth()/2f, centerY, 8*density, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(15*density); paint.setTextAlign(Paint.Align.LEFT); c.drawText(value+" kbps", getWidth()/2f+18*density, centerY+5*density, paint);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN: lastY=e.getY(); return true;
            case MotionEvent.ACTION_MOVE:
                float dy=e.getY()-lastY; lastY=e.getY();
                float s=Math.max(1f,step); scrollOffset += dy;
                while(scrollOffset <= -s){ value=Math.min(400,value+1); scrollOffset += s; }
                while(scrollOffset >= s){ value=Math.max(1,value-1); scrollOffset -= s; }
                if(listener!=null)listener.onValueChanged(value); invalidate(); return true;
            case MotionEvent.ACTION_UP: performClick(); return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
