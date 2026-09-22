package com.fast.radio;
import android.os.*; import android.widget.TextView; import java.io.*; import java.net.*; import java.util.*; import org.xmlpull.v1.*;
/** Website headline/summary ticker with explicit previous/next/pause/stop controls. */
public class WebsiteNewsTicker {
 private final TextView title,text; private final Handler h=new Handler(Looper.getMainLooper()); private final List<String[]> items=new ArrayList<>(); private int index=0; private volatile boolean running=true,paused=false; private volatile HttpURLConnection activeConnection;
 private final List<String> feeds=Arrays.asList("https://feeds.bbci.co.uk/news/rss.xml","https://rss.dw.com/rdf/rss-en-all","https://www.aljazeera.com/xml/rss/all.xml","https://feeds.skynews.com/feeds/rss/world.xml","https://www.irna.ir/rss","https://www.isna.ir/rss","https://www.mehrnews.com/rss","https://www.farsnews.ir/rss");
 public WebsiteNewsTicker(TextView t,TextView x){title=t;text=x;refresh();}
 public boolean isPaused(){return paused;}
 public void togglePause(){if(!running){running=true;refresh();return;}paused=!paused;if(paused)h.removeCallbacksAndMessages(null);else showNext();}
 public void stop(){running=false;paused=false;h.removeCallbacksAndMessages(null);disconnect();}
 public void next(){if(!running)running=true;if(items.isEmpty()){refresh();return;}index=(index+1)%items.size();showCurrent();}
 public void previous(){if(!running)running=true;if(items.isEmpty()){refresh();return;}index=(index-1+items.size())%items.size();showCurrent();}
 private void disconnect(){HttpURLConnection c=activeConnection;if(c!=null){try{c.disconnect();}catch(Exception ignored){}activeConnection=null;}}
 private void refresh(){new Thread(()->{List<String[]> g=new ArrayList<>();for(String f:feeds){if(!running)break;try{g.addAll(read(f));}catch(Exception ignored){}}synchronized(items){items.clear();items.addAll(g);}if(running)h.post(this::showNext);}).start();}
 private List<String[]> read(String f)throws Exception{ArrayList<String[]>o=new ArrayList<>();HttpURLConnection c=(HttpURLConnection)new URL(f).openConnection();activeConnection=c;c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Fast Radio/5.1");try(InputStream in=c.getInputStream()){XmlPullParserFactory fac=XmlPullParserFactory.newInstance();fac.setNamespaceAware(true);XmlPullParser x=fac.newPullParser();x.setInput(new InputStreamReader(in,"UTF-8"));boolean item=false;String ttl=null,desc=null;int e;while((e=x.next())!=XmlPullParser.END_DOCUMENT){if(e==XmlPullParser.START_TAG){String n=x.getName();if(n.equalsIgnoreCase("item")||n.equalsIgnoreCase("entry")){item=true;ttl=null;desc=null;}else if(item&&n.equalsIgnoreCase("title"))ttl=x.nextText();else if(item&&(n.equalsIgnoreCase("description")||n.equalsIgnoreCase("summary")))desc=x.nextText();}else if(e==XmlPullParser.END_TAG){String n=x.getName();if(item&&(n.equalsIgnoreCase("item")||n.equalsIgnoreCase("entry"))){if(ttl!=null&&!ttl.trim().isEmpty())o.add(new String[]{ttl.trim(),strip(desc)});item=false;}}if(o.size()>=10)break;}}finally{try{c.disconnect();}catch(Exception ignored){}if(activeConnection==c)activeConnection=null;}return o;}
 private String strip(String s){return s==null?"":s.replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim();}
 private void showCurrent(){if(!running||paused)return;String t="WEB NEWS",x="خبرهای وب‌سایت‌های خبری در حال دریافت است…";synchronized(items){if(!items.isEmpty()){if(index>=items.size())index=0;String[]a=items.get(index);t="WEB NEWS • HEADLINE";x=a[0]+(a[1].isEmpty()?"":" — "+a[1]);}}h.post(()->{title.setText(t);text.setText(x);});}
 private void showNext(){if(!running||paused)return;showCurrent();index++;h.postDelayed(this::showNext,30000);if(index%10==0)h.postDelayed(this::refresh,1000);}
}
