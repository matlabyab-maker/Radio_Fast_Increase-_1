package com.fast.radio;

import android.net.Uri;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RadioBrowserClient {
    public interface StationCallback { void result(List<RadioStation> stations); void error(Exception e); }
    public interface CountryCallback { void result(List<CountryItem> countries); void error(Exception e); }
    public static class CountryItem { public String name,code; public int count; public CountryItem(String n,String c,int x){name=n;code=c;count=x;} public String toString(){return name+(count>0?" ("+count+")":"");} }
    private static final String[] HOSTS={"https://de1.api.radio-browser.info","https://at1.api.radio-browser.info","https://nl1.api.radio-browser.info"};
    private static String get(String url)throws Exception{ HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(12000); c.setRequestProperty("User-Agent","FastRadio/2.0"); InputStream in=new BufferedInputStream(c.getInputStream()); BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8")); StringBuilder b=new StringBuilder(); String l; while((l=r.readLine())!=null)b.append(l); r.close(); return b.toString(); }
    private static List<RadioStation> parseStations(String json)throws Exception{ JSONArray a=new JSONArray(json); List<RadioStation> out=new ArrayList<>(); for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i); String url=o.optString("url_resolved",o.optString("url")); if(url.isEmpty())continue; out.add(new RadioStation(o.optString("name","Unnamed"),url,o.optString("country"),o.optString("countrycode"),o.optString("codec"),o.optInt("bitrate",0),o.optString("homepage"),o.optString("favicon")));} return out; }
    public static void countries(CountryCallback cb){ Executors.newSingleThreadExecutor().execute(()->{Exception last=null; for(String h:HOSTS)try{JSONArray a=new JSONArray(get(h+"/json/countries?order=stationcount&reverse=true")); List<CountryItem> out=new ArrayList<>(); for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);out.add(new CountryItem(o.optString("name"),o.optString("iso_3166_1"),o.optInt("stationcount")));} cb.result(out);return;}catch(Exception e){last=e;} cb.error(last);}); }
    public static void byCountry(String code,StationCallback cb){ request("/json/stations/bycountrycode/"+Uri.encode(code)+"?hidebroken=true&order=votes&reverse=true&limit=200",cb); }
    public static void search(String text,String code,StationCallback cb){ String q=Uri.encode(text); String path="/json/stations/search?name="+q+(code==null||code.isEmpty()?"":"&countrycode="+Uri.encode(code))+"&hidebroken=true&order=votes&reverse=true&limit=200"; request(path,cb); }
    private static void request(String path,StationCallback cb){ Executors.newSingleThreadExecutor().execute(()->{Exception last=null; for(String h:HOSTS){try{cb.result(parseStations(get(h+path)));return;}catch(Exception e){last=e;}} cb.error(last==null?new IOException("No RadioBrowser server"):last);}); }
}
