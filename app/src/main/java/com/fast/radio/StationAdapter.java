package com.fast.radio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class StationAdapter extends BaseAdapter {
    public interface Listener { void select(RadioStation s); void favorite(RadioStation s); }
    private final Context context; private final List<RadioStation> data; private final Listener listener;
    private final ExecutorService imagePool=Executors.newFixedThreadPool(3);
    private final Map<String,Bitmap> cache=new ConcurrentHashMap<>();
    public StationAdapter(Context c,List<RadioStation> d,Listener l){context=c;data=d;listener=l;}
    @Override public int getCount(){return data.size();}
    @Override public Object getItem(int p){return data.get(p);}
    @Override public long getItemId(int p){return p;}
    @Override public View getView(int p, View convert, ViewGroup parent){
        RadioStation s=data.get(p);
        LinearLayout row=new LinearLayout(context); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(8,4,4,4);
        ImageView icon=new ImageView(context); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); icon.setImageResource(com.fast.radio.R.drawable.ic_fast_radio);
        row.addView(icon,new LinearLayout.LayoutParams(52,52));
        TextView t=new TextView(context); t.setText(s.toString()); t.setTextColor(Color.WHITE); t.setTextSize(14); t.setGravity(Gravity.CENTER_VERTICAL); row.addView(t,new LinearLayout.LayoutParams(0,58,1));
        Button star=new Button(context); star.setText(s.favorite?"★":"☆"); star.setTextSize(18); star.setTextColor(Color.rgb(80,190,255)); row.addView(star,new LinearLayout.LayoutParams(54,54));
        String key=s.favicon==null?"":s.favicon.trim();
        if(!key.isEmpty()){
            Bitmap b=cache.get(key); if(b!=null) icon.setImageBitmap(b); else { final int pos=p; imagePool.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(key).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(7000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Fast Radio/3.9");InputStream in=c.getInputStream();Bitmap bm=BitmapFactory.decodeStream(in);in.close();c.disconnect();if(bm!=null){cache.put(key,bm);((android.app.Activity)context).runOnUiThread(this::notifyDataSetChanged);}}catch(Exception ignored){}}); }
        }
        row.setOnClickListener(v->listener.select(s)); star.setOnClickListener(v->{listener.favorite(s); notifyDataSetChanged();});
        return row;
    }
}
