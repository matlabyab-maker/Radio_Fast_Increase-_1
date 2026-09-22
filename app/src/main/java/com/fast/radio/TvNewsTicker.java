package com.fast.radio;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.io.*;
import java.net.*;
import java.net.URLEncoder;
import java.util.*;
import org.json.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/** Lightweight RSS headline ticker with explicit previous/next/pause/stop controls. */
public class TvNewsTicker {
    private final TextView original, persian;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<String> feeds=Arrays.asList(
        "https://feeds.bbci.co.uk/news/rss.xml",
        "https://www.aljazeera.com/xml/rss/all.xml",
        "https://rss.dw.com/rdf/rss-en-all",
        "https://www.france24.com/en/rss",
        "https://www.euronews.com/rss?level=vertical&name=news"
    );
    private final List<String> headlines=new ArrayList<>();
    private int index=0;
    private volatile boolean running=true, paused=false;
    private volatile HttpURLConnection activeConnection;
    public TvNewsTicker(TextView o,TextView p){original=o;persian=p; refresh();}
    public boolean isPaused(){return paused;}
    public void togglePause(){ if(!running){running=true;refresh();return;} paused=!paused; if(paused) main.removeCallbacksAndMessages(null); else showNext(); }
    public void stop(){running=false;paused=false;main.removeCallbacksAndMessages(null);disconnect();}
    public void next(){if(!running){running=true;} if(headlines.isEmpty()){refresh();return;} index=(index+1)%headlines.size(); showCurrent();}
    public void previous(){if(!running){running=true;} if(headlines.isEmpty()){refresh();return;} index=(index-1+headlines.size())%headlines.size(); showCurrent();}
    private void disconnect(){HttpURLConnection c=activeConnection;if(c!=null){try{c.disconnect();}catch(Exception ignored){}activeConnection=null;}}
    private void refresh(){new Thread(()->{List<String> got=new ArrayList<>();for(String f:feeds){if(!running)break;try{got.addAll(readFeed(f));}catch(Exception ignored){}} synchronized(headlines){headlines.clear();headlines.addAll(got);} if(running)main.post(this::showNext);}).start();}
    private List<String> readFeed(String feed)throws Exception{
        ArrayList<String> out=new ArrayList<>(); URL u=new URL(feed); HttpURLConnection c=(HttpURLConnection)u.openConnection();activeConnection=c;c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Fast Radio/5.1");
        try(InputStream in=c.getInputStream()){
            XmlPullParserFactory fac=XmlPullParserFactory.newInstance();fac.setNamespaceAware(true);XmlPullParser x=fac.newPullParser();x.setInput(new InputStreamReader(in,"UTF-8"));boolean inItem=false;String title=null;int e;
            while((e=x.next())!=XmlPullParser.END_DOCUMENT){if(e==XmlPullParser.START_TAG){String n=x.getName();if("item".equalsIgnoreCase(n)||"entry".equalsIgnoreCase(n)){inItem=true;title=null;}else if(inItem&&"title".equalsIgnoreCase(n)){title=x.nextText();}}else if(e==XmlPullParser.END_TAG){String n=x.getName();if(inItem&&("item".equalsIgnoreCase(n)||"entry".equalsIgnoreCase(n))){if(title!=null&&!title.trim().isEmpty())out.add(title.trim());inItem=false;}}if(out.size()>=12)break;}
        } finally {try{c.disconnect();}catch(Exception ignored){} if(activeConnection==c)activeConnection=null;}
        return out;
    }
    private void showCurrent(){if(!running||paused)return;String text="WORLD TV NEWS • No feed";synchronized(headlines){if(!headlines.isEmpty()){if(index>=headlines.size())index=0;text=headlines.get(index);}}final String shown=text;main.post(()->original.setText("WORLD TV NEWS • "+shown));new Thread(()->{String tr=translate(shown);main.post(()->persian.setText(tr.isEmpty()?"ترجمه فارسی در دسترس نیست":"فارسی: "+tr));}).start();}
    private void showNext(){if(!running||paused)return;showCurrent();index++;main.postDelayed(this::showNext,28000);if(index%12==0)main.postDelayed(this::refresh,1000);}
    private String translate(String text){try{if(text.matches(".*[\\u0600-\\u06FF].*"))return text;String q=URLEncoder.encode(text,"UTF-8");URL u=new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=fa&dt=t&q="+q);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","Fast Radio/5.1");InputStream in=c.getInputStream();StringBuilder b=new StringBuilder();BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));String line;while((line=r.readLine())!=null)b.append(line);r.close();c.disconnect();JSONArray a=new JSONArray(b.toString());JSONArray chunks=a.getJSONArray(0);StringBuilder out=new StringBuilder();for(int i=0;i<chunks.length();i++)out.append(chunks.getJSONArray(i).optString(0));return out.toString();}catch(Exception e){return "";}}
}
