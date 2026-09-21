package com.fast.radio;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

public class StationAdapter extends BaseAdapter {
    public interface Listener { void select(RadioStation s); void favorite(RadioStation s); }
    private final Context context; private final List<RadioStation> data; private final Listener listener;
    public StationAdapter(Context c,List<RadioStation> d,Listener l){context=c;data=d;listener=l;}
    @Override public int getCount(){return data.size();}
    @Override public Object getItem(int p){return data.get(p);}
    @Override public long getItemId(int p){return p;}
    @Override public View getView(int p, View convert, ViewGroup parent){
        RadioStation s=data.get(p); LinearLayout row=new LinearLayout(context); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(8,4,4,4);
        TextView t=new TextView(context); t.setText(s.toString()); t.setTextColor(Color.WHITE); t.setTextSize(14); t.setGravity(Gravity.CENTER_VERTICAL); row.addView(t,new LinearLayout.LayoutParams(0,58,1));
        Button star=new Button(context); star.setText(s.favorite?"★":"☆"); star.setTextSize(18); star.setTextColor(Color.rgb(80,190,255)); row.addView(star,new LinearLayout.LayoutParams(54,54));
        row.setOnClickListener(v->listener.select(s)); star.setOnClickListener(v->{listener.favorite(s); notifyDataSetChanged();});
        return row;
    }
}
