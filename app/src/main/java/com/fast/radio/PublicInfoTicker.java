package com.fast.radio;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Three-part low-data public information ticker. */
public class PublicInfoTicker {
    private final TextView iran, world, economy;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private volatile boolean running;
    public PublicInfoTicker(TextView iran, TextView world, TextView economy){this.iran=iran;this.world=world;this.economy=economy;}
    public void start(){if(running)return;running=true;refresh();}
    public void stop(){running=false;main.removeCallbacksAndMessages(null);pool.shutdownNow();}
    private void refresh(){if(!running)return;pool.submit(()->{String a=iranText(),b=worldText(),c=economyText();main.post(()->{if(!running)return;iran.setText(a);world.setText(b);economy.setText(c);main.postDelayed(this::refresh,300000);});});}
    private String iranText(){String q=usgs("25","40","44","64",4);String w=pageHeadlines("https://www.irna.ir/service/weather",new String[]{"هوا","بار","دما","سیل","گرد","باد","برف","هشدار"});StringBuilder b=new StringBuilder();b.append("هوا: ").append(w.isEmpty()?"سازمان هواشناسی کشور (IRIMO)":w);b.append("  |  زلزله: ").append(q.isEmpty()?"مورد بالای ۴ در آخرین ۷ روز یافت نشد":q);return b.toString();}
    private String worldText(){String q=usgs(null,null,null,null,5);return q.isEmpty()?"زلزله بالای ۵ در آخرین ۷ روز یافت نشد":q;}
    private String economyText(){String x=pageHeadlines("https://www.irna.ir/service/economy",new String[]{"یارانه","کالابرگ","معیشت","قیمت","تورم","دستمزد","سوخت","نان","برق","گاز","اقتصاد","بازار"});return x.isEmpty()?"آخرین تیترهای اقتصادی و معیشتی ایرنا در دسترس نیست":"ایرنا: "+x;}
    private String usgs(String minLat,String maxLat,String minLon,String maxLon,double minMag){try{String u="https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime="+daysAgo(7)+"&minmagnitude="+minMag+"&orderby=time";if(minLat!=null)u+="&minlatitude="+minLat+"&maxlatitude="+maxLat+"&minlongitude="+minLon+"&maxlongitude="+maxLon;String s=get(u);JSONArray a=new JSONObject(s).optJSONArray("features");if(a==null)return"";StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(3,a.length());i++){JSONObject p=a.getJSONObject(i).optJSONObject("properties");if(p==null)continue;if(b.length()>0)b.append("  |  ");b.append(String.format(Locale.US,"M%.1f %s",p.optDouble("mag",0),p.optString("place","نامشخص")));}return b.toString();}catch(Exception e){return"";}}
    private String daysAgo(int d){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));c.add(Calendar.DAY_OF_YEAR,-d);return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private String pageHeadlines(String url,String[] keys){try{String s=get(url);String low=s.toLowerCase(Locale.US);ArrayList<String> out=new ArrayList<>();int pos=0;while(out.size()<3){int a=low.indexOf("<a",pos);if(a<0)break;int b=low.indexOf('>',a);int c=low.indexOf("</a>",b);if(b<0||c<0)break;String t=clean(s.substring(b+1,c));pos=c+4;if(t.length()<18||t.length()>180)continue;boolean ok=false;for(String k:keys)if(t.contains(k)){ok=true;break;}if(ok&&!out.contains(t))out.add(t);}return join(out,"  |  ");}catch(Exception e){return"";}}
    private String get(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(9000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","FastRadio/5.2");InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] z=new byte[4096];int n;while((n=in.read(z))!=-1&&o.size()<700000)o.write(z,0,n);in.close();c.disconnect();return new String(o.toByteArray(),StandardCharsets.UTF_8);}
    private String clean(String s){return s.replaceAll("<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&#x27;","'").replace("&quot;","\"").replaceAll("\\s+"," ").trim();}
    private String join(List<String>a,String sep){StringBuilder b=new StringBuilder();for(String x:a){if(b.length()>0)b.append(sep);b.append(x);}return b.toString();}
}
