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

/** Lightweight world-TV/news ticker. Sources are public RSS feeds; it does not capture TV video. */
public class TvNewsTicker {
    private final TextView original, persian;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<String> feeds=Arrays.asList(
        "https://feeds.bbci.co.uk/news/rss.xml",
        "https://www.aljazeera.com/xml/rss/all.xml",
        "https://rss.dw.com/rdf/rss-en-all"
    );
    private final List<String> headlines=new ArrayList<>();
    private int index=0;
    private boolean running=true;
    public TvNewsTicker(TextView o,TextView p){original=o;persian=p; refresh();}
    public void stop(){running=false;}
    private void refresh(){new Thread(()->{List<String> got=new ArrayList<>();for(String f:feeds){try{got.addAll(readFeed(f));}catch(Exception ignored){}} synchronized(headlines){headlines.clear();headlines.addAll(got);} showNext();}).start();}
    private List<String> readFeed(String feed)throws Exception{
        ArrayList<String> out=new ArrayList<>(); URL u=new URL(feed); HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Fast Radio/3.9");InputStream in=c.getInputStream();
        XmlPullParserFactory fac=XmlPullParserFactory.newInstance();fac.setNamespaceAware(true);XmlPullParser x=fac.newPullParser();x.setInput(new InputStreamReader(in,"UTF-8"));boolean inItem=false;String title=null;int e;
        while((e=x.next())!=XmlPullParser.END_DOCUMENT){if(e==XmlPullParser.START_TAG){String n=x.getName();if("item".equalsIgnoreCase(n)||"entry".equalsIgnoreCase(n)){inItem=true;title=null;}else if(inItem&&"title".equalsIgnoreCase(n)){title=x.nextText();}}else if(e==XmlPullParser.END_TAG){String n=x.getName();if(inItem&&("item".equalsIgnoreCase(n)||"entry".equalsIgnoreCase(n))){if(title!=null&&!title.trim().isEmpty())out.add(title.trim());inItem=false;}}if(out.size()>=12)break;}in.close();c.disconnect();return out;}
    private void showNext(){if(!running)return;String text="WORLD TV NEWS • No feed";synchronized(headlines){if(!headlines.isEmpty()){if(index>=headlines.size())index=0;text=headlines.get(index++);}}final String shown=text;main.post(()->original.setText("WORLD TV NEWS • "+shown));new Thread(()->{String tr=translate(shown);main.post(()->persian.setText(tr.isEmpty()?"ترجمه فارسی در دسترس نیست":"فارسی: "+tr));}).start();main.postDelayed(this::showNext,10000);if(index%12==0)main.postDelayed(this::refresh,1000);}
    private String translate(String text){try{if(text.matches(".*[\\u0600-\\u06FF].*"))return text;String q=URLEncoder.encode(text,"UTF-8");URL u=new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=fa&dt=t&q="+q);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","Fast Radio/3.9");InputStream in=c.getInputStream();StringBuilder b=new StringBuilder();BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));String line;while((line=r.readLine())!=null)b.append(line);r.close();c.disconnect();JSONArray a=new JSONArray(b.toString());JSONArray chunks=a.getJSONArray(0);StringBuilder out=new StringBuilder();for(int i=0;i<chunks.length();i++)out.append(chunks.getJSONArray(i).optString(0));return out.toString();}catch(Exception e){return "";}}
}
