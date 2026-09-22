package com.fast.radio;
import android.os.*; import android.widget.TextView; import java.io.*; import java.net.*; import java.util.*; import org.xmlpull.v1.*;
public class WebsiteNewsTicker {
 private final TextView title,text; private final Handler h=new Handler(Looper.getMainLooper()); private final List<String[]> items=new ArrayList<>(); private int index=0; private boolean running=true;
 private final List<String> feeds=Arrays.asList("https://feeds.bbci.co.uk/news/rss.xml","https://rss.dw.com/rdf/rss-en-all","https://www.aljazeera.com/xml/rss/all.xml","https://feeds.skynews.com/feeds/rss/world.xml",
        "https://www.irna.ir/rss",
        "https://www.isna.ir/rss",
        "https://www.mehrnews.com/rss",
        "https://www.farsnews.ir/rss");
 public WebsiteNewsTicker(TextView t,TextView x){title=t;text=x;refresh();} public void stop(){running=false;h.removeCallbacksAndMessages(null);}
 private void refresh(){new Thread(()->{List<String[]> g=new ArrayList<>();for(String f:feeds)try{g.addAll(read(f));}catch(Exception ignored){}synchronized(items){items.clear();items.addAll(g);}h.post(this::showNext);}).start();}
 private List<String[]> read(String f)throws Exception{ArrayList<String[]>o=new ArrayList<>();HttpURLConnection c=(HttpURLConnection)new URL(f).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Fast Radio/4.8");InputStream in=c.getInputStream();XmlPullParserFactory fac=XmlPullParserFactory.newInstance();fac.setNamespaceAware(true);XmlPullParser x=fac.newPullParser();x.setInput(new InputStreamReader(in,"UTF-8"));boolean item=false;String ttl=null,desc=null;int e;while((e=x.next())!=XmlPullParser.END_DOCUMENT){if(e==XmlPullParser.START_TAG){String n=x.getName();if(n.equalsIgnoreCase("item")||n.equalsIgnoreCase("entry")){item=true;ttl=null;desc=null;}else if(item&&n.equalsIgnoreCase("title"))ttl=x.nextText();else if(item&&(n.equalsIgnoreCase("description")||n.equalsIgnoreCase("summary")))desc=x.nextText();}else if(e==XmlPullParser.END_TAG){String n=x.getName();if(item&&(n.equalsIgnoreCase("item")||n.equalsIgnoreCase("entry"))){if(ttl!=null&&!ttl.trim().isEmpty())o.add(new String[]{ttl.trim(),strip(desc)});item=false;}}if(o.size()>=10)break;}in.close();c.disconnect();return o;}
 private String strip(String s){return s==null?"":s.replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim();}
 private void showNext(){if(!running)return;String t="WEB NEWS",x="خبرهای وب‌سایت‌های خبری در حال دریافت است…";synchronized(items){if(!items.isEmpty()){if(index>=items.size())index=0;String[]a=items.get(index++);t="WEB NEWS • HEADLINE";x=a[0]+(a[1].isEmpty()?"":" — "+a[1]);}}title.setText(t);text.setText(x);h.postDelayed(this::showNext,26000);if(index%10==0)h.postDelayed(this::refresh,1000);}
}
