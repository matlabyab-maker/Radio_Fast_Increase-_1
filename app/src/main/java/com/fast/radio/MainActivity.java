package com.fast.radio;

import android.content.*;
import android.app.AlertDialog;
import android.os.*;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.*;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private ListView customList, iranList, worldList, newsList;
    private TextView status, nowPlaying, qualityValue, usagePerMinute, persianCalendar;
    private TvNewsTicker tvNewsTicker; private WebsiteNewsTicker webNewsTicker;
    private final List<RadioStation> custom=new ArrayList<>(), iran=new ArrayList<>(), world=new ArrayList<>(), favorites=new ArrayList<>(), builtinFavorites=new ArrayList<>();
    private StationAdapter customAdapter, iranAdapter, worldAdapter;
    private MediaController controller; private ListenableFuture<MediaController> controllerFuture;
    private Spinner regionSpinner, countrySpinner; private EditText search;
    private final List<RadioBrowserClient.CountryItem> countries=new ArrayList<>();
    private final String[] regions=RegionCatalog.REGIONS;
    private VerticalRulerView qualityRuler; private VolumeRulerView volumeRuler; private RadioStation selected;
    private MediaRecorder recorder; private boolean recording=false;
    private final Handler usageHandler=new Handler(Looper.getMainLooper()); private long playStartedMs=0; private int activeKbps=15; private TextView recordLight; private static final int REQ_RECORD_AUDIO=401;
    private Button eqButton;
    private final String[] newsNames={"Sputnik فارسی","BBC Persian","Iran International","VOA Persian","BBC News","NHK Japan"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_main); bind(); loadAssets(); setupSpinners(); setupLists(); setupNews(); connectController();
        RadioBrowserClient.countries(new RadioBrowserClient.CountryCallback(){
            public void result(List<RadioBrowserClient.CountryItem> x){runOnUiThread(()->{countries.clear();countries.addAll(x);refreshCountries();loadIran();});}
            public void error(Exception e){runOnUiThread(()->{status.setText("World search offline");loadIran();});}
        });
    }
    private void bind(){
        customList=findViewById(R.id.customList); iranList=findViewById(R.id.iranList); worldList=findViewById(R.id.worldList); newsList=findViewById(R.id.newsList);
        status=findViewById(R.id.status); nowPlaying=findViewById(R.id.nowPlaying); qualityValue=findViewById(R.id.qualityValue); usagePerMinute=findViewById(R.id.usagePerMinute); persianCalendar=findViewById(R.id.persianCalendar); updatePersianCalendar();
        qualityRuler=findViewById(R.id.qualityRuler); volumeRuler=findViewById(R.id.volumeRuler); recordLight=findViewById(R.id.recordLight); eqButton=findViewById(R.id.equalizer);
        tvNewsTicker=new TvNewsTicker(findViewById(R.id.tvTickerOriginal),findViewById(R.id.tvTickerPersian)); webNewsTicker=new WebsiteNewsTicker(findViewById(R.id.webTickerTitle),findViewById(R.id.webTickerText)); regionSpinner=findViewById(R.id.regionSpinner); countrySpinner=findViewById(R.id.countrySpinner); search=findViewById(R.id.search);
        qualityRuler.setListener(v->{activeKbps=v; qualityValue.setText(v+" kbps"); updateUsage(v); status.setText("Quality target: "+v+" kbps");});
        volumeRuler.setListener(v->setOutputVolume(v));
        findViewById(R.id.record).setOnClickListener(v->toggleRecording());
        eqButton.setOnClickListener(v->showEqualizer());
        findViewById(R.id.play).setOnClickListener(v->playSelected()); findViewById(R.id.stop).setOnClickListener(v->{if(controller!=null){controller.stop();} status.setText("Stopped");});
        findViewById(R.id.fav).setOnClickListener(v->{if(selected!=null)toggleFavorite(selected);});
        findViewById(R.id.searchButton).setOnClickListener(v->doSearch()); findViewById(R.id.saveList).setOnClickListener(v->saveWorldList()); findViewById(R.id.favorites).setOnClickListener(v->showFavorites());
        setupScroll(R.id.customUp,customList,true); setupScroll(R.id.customDown,customList,false); setupScroll(R.id.iranUp,iranList,true); setupScroll(R.id.iranDown,iranList,false);
        updateUsage(15);
    }
    private void updatePersianCalendar(){
        Calendar c=Calendar.getInstance();
        int[] j=gregorianToJalali(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));
        String[] months={"فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند"};
        String[] days={"یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه","شنبه"};
        String text=days[c.get(Calendar.DAY_OF_WEEK)-1]+"  "+toPersianDigits(j[2])+" "+months[j[1]-1]+" "+toPersianDigits(j[0]);
        persianCalendar.setText(text);
        persianCalendar.postDelayed(this::updatePersianCalendar,60000);
    }
    private String toPersianDigits(int n){return String.valueOf(n).replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷').replace('8','۸').replace('9','۹');}
    private int[] gregorianToJalali(int gy,int gm,int gd){
        int[] gdm={31,28,31,30,31,30,31,31,30,31,30,31};
        int gy2=gy-1600, gm2=gm-1, gd2=gd-1;
        int gDay=365*gy2+(gy2+3)/4-(gy2+99)/100+(gy2+399)/400;
        for(int i=0;i<gm2;i++) gDay+=gdm[i];
        if(gm2>1 && ((gy%4==0 && gy%100!=0)||gy%400==0)) gDay++;
        gDay+=gd2;
        int jDay=gDay-79;
        int jNp=jDay/12053; jDay%=12053;
        int jy=979+33*jNp+4*(jDay/1461); jDay%=1461;
        if(jDay>=366){jy+=(jDay-1)/365; jDay=(jDay-1)%365;}
        int jm, jd;
        if(jDay<186){jm=1+jDay/31; jd=1+jDay%31;} else {jm=7+(jDay-186)/30; jd=1+(jDay-186)%30;}
        return new int[]{jy,jm,jd};
    }

    private void setOutputVolume(int percent){
        AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE); int max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); int system=Math.round(max*Math.min(percent,100)/100f); am.setStreamVolume(AudioManager.STREAM_MUSIC,system,0);
        Intent i=new Intent("com.fast.radio.SET_VOLUME_GAIN").setPackage(getPackageName()); i.putExtra("percent",percent); sendBroadcast(i);
        status.setText("Volume: "+percent+"%"+(percent>100?" • boost":""));
    }
    private void toggleRecording(){
        if(recording){ stopRecording(); return; }
        if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_RECORD_AUDIO);return;}
        startRecording();
    }
    private void startRecording(){
        try{ File dir=new File(getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),"FastRadio"); if(!dir.exists())dir.mkdirs(); String name="FastRadio_"+new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".amr";
            recorder=new MediaRecorder(); recorder.setAudioSource(MediaRecorder.AudioSource.MIC); recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB); recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); recorder.setAudioSamplingRate(8000); recorder.setAudioEncodingBitRate(12200); recorder.setOutputFile(new File(dir,name).getAbsolutePath()); recorder.prepare(); recorder.start(); recording=true; recordLight.setVisibility(View.VISIBLE); ((Button)findViewById(R.id.record)).setText("STOP REC"); status.setText("Recording AMR • "+name);
        }catch(Exception e){recording=false;if(recorder!=null){try{recorder.release();}catch(Exception ignored){}}recorder=null;status.setText("Record could not start");}
    }
    private void stopRecording(){try{if(recorder!=null){recorder.stop();recorder.release();}}catch(Exception ignored){}recorder=null;recording=false;recordLight.setVisibility(View.GONE);((Button)findViewById(R.id.record)).setText("REC");status.setText("Recording saved as .amr");}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_RECORD_AUDIO && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED)startRecording();else if(r==REQ_RECORD_AUDIO)status.setText("Microphone permission required for AMR recording");}
    private void updateUsage(int kbps){ double mb=(kbps*60.0)/(8.0*1024.0); long elapsed=playStartedMs>0?(System.currentTimeMillis()-playStartedMs)/1000:0; long min=elapsed/60, sec=elapsed%60; usagePerMinute.setText(String.format(Locale.US,"%d:%02d • %.3f MB/min",min,sec,mb)); usageHandler.removeCallbacksAndMessages(null); usageHandler.postDelayed(()->{if(controller!=null&&controller.isPlaying())updateUsage(activeKbps);},1000); }
    private void setupScroll(int id,ListView l,boolean up){findViewById(id).setOnClickListener(v->{int p=l.getFirstVisiblePosition();l.setSelection(Math.max(0,p+(up?-8:8)));});}
    private void loadAssets(){try{JSONArray a=new JSONArray(readAsset("stations.json"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);custom.add(new RadioStation(o.optString("name"),o.optString("url")));}}catch(Exception ignored){} loadBuiltInFavorites(); loadFavorites();}
    private String readAsset(String n)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open(n),"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
    private void loadBuiltInFavorites(){try{JSONArray a=new JSONArray(readAsset("builtin_favorites.json"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String url=o.optString("url");if(url==null||url.trim().isEmpty())continue;builtinFavorites.add(new RadioStation(o.optString("name"),url,o.optString("country"),o.optString("countryCode",o.optString("countrycode")),o.optString("codec"),o.optInt("bitrate",0),o.optString("homepage"),o.optString("favicon")));}}catch(Exception ignored){}}
    private void setupLists(){StationAdapter.Listener l=new StationAdapter.Listener(){public void select(RadioStation s){showSelected(s); playSelected();}public void favorite(RadioStation s){toggleFavorite(s);}};customAdapter=new StationAdapter(this,custom,l);iranAdapter=new StationAdapter(this,iran,l);worldAdapter=new StationAdapter(this,world,l);customList.setAdapter(customAdapter);iranList.setAdapter(iranAdapter);worldList.setAdapter(worldAdapter);}
    private void setupNews(){
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,newsNames){@Override public View getView(int p,View c,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,c,parent);t.setText(newsNames[p]+"   ★");t.setTextColor(android.graphics.Color.rgb(255,225,45));t.setTextSize(14);t.setPadding(10,6,4,6);t.setBackgroundColor(android.graphics.Color.TRANSPARENT);return t;}};
        newsList.setAdapter(a); newsList.setOnItemClickListener((p,v,pos,id)->{String q=newsNames[pos].replace(" فارسی",""); RadioBrowserClient.search(q,"",new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{world.clear();world.addAll(x);worldAdapter.notifyDataSetChanged();status.setText(newsNames[pos]+" • "+x.size()+" results");});}public void error(Exception e){runOnUiThread(()->status.setText("No live result for "+newsNames[pos]));}});});
    }
    private void showSelected(RadioStation s){selected=s;nowPlaying.setText(s.name+"  •  "+(s.country.isEmpty()?"":s.country)+"  •  "+(s.bitrate>0?s.bitrate+" kbps":"Auto"));status.setText("Selected • target "+qualityRuler.getValue()+" kbps");}
    private void connectController(){SessionToken token=new SessionToken(this,new ComponentName(this,RadioPlaybackService.class));controllerFuture=new MediaController.Builder(this,token).buildAsync();controllerFuture.addListener(()->{try{controller=controllerFuture.get(); controller.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int state){runOnUiThread(()->{if(state==Player.STATE_BUFFERING)status.setText("Buffering…"); else if(state==Player.STATE_READY && controller.isPlaying())status.setText("Playing • buffer active");});}});}catch(Exception e){status.setText("Controller error");}},getMainExecutor());}
    private void showEqualizer(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(18,8,18,8);
        String[] labels={"60 Hz","230 Hz","910 Hz","3.6 kHz","14 kHz"};
        int[] bands={0,1,2,3,4};
        for(int i=0;i<labels.length;i++){ final int band=bands[i]; TextView l=new TextView(this); l.setText(labels[i]+"  0 dB"); l.setTextColor(android.graphics.Color.WHITE); box.addView(l); SeekBar bar=new SeekBar(this); bar.setMax(30); bar.setProgress(15); box.addView(bar,new LinearLayout.LayoutParams(-1,48)); bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){int db=(p-15)*100;l.setText(labels[band]+"  "+(db>=0?"+":"")+(db/100)+" dB"); Intent in=new Intent("com.fast.radio.SET_EQ_BAND").setPackage(getPackageName());in.putExtra("band",band);in.putExtra("level",db);sendBroadcast(in);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}}); }
        new AlertDialog.Builder(this).setTitle("Equalizer").setView(box).setPositiveButton("OK",null).show();
    }

    private void playSelected(){
        if(selected==null){status.setText("Select a station first");return;}
        String finalUrl = ProxyConfig.wrap(selected.url, qualityRuler.getValue());
        if(finalUrl==null || finalUrl.isEmpty()){status.setText("Invalid stream URL");return;}
        String lower=finalUrl.toLowerCase(Locale.US);
        if(lower.endsWith(".html") || lower.endsWith(".htm") || lower.contains("gurutv.online/")){
            try { Intent i=new Intent(this,WebStreamActivity.class); i.putExtra("url",finalUrl); startActivity(i); status.setText("Opening live web stream…"); }
            catch(Exception e){status.setText("Web stream could not be opened");}
            return;
        }
        if(controller==null){status.setText("Player not ready");return;}
        controller.setMediaItem(MediaItem.fromUri(finalUrl));
        controller.prepare();controller.play();
        activeKbps=qualityRuler.getValue(); playStartedMs=System.currentTimeMillis(); updateUsage(activeKbps);
        nowPlaying.setText("▶ "+selected.name);
        status.setText("Playing • Media3 HLS/HTTP • buffer 5–10 s • target "+qualityRuler.getValue()+" kbps");
    }
    private void loadIran(){RadioBrowserClient.byCountry("IR",new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{iran.clear();iran.addAll(x);iranAdapter.notifyDataSetChanged();});}public void error(Exception e){runOnUiThread(()->status.setText("Iran Radio list unavailable"));}});}
    private void setupSpinners(){regionSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,regions));regionSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?>p){}public void onItemSelected(android.widget.AdapterView<?>p,View v,int pos,long id){refreshCountries();}});countrySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?>p){}public void onItemSelected(android.widget.AdapterView<?>p,View v,int pos,long id){if(pos>0)doCountrySearch();}});}
    private void refreshCountries(){String r=regions[regionSpinner.getSelectedItemPosition()];List<String> names=new ArrayList<>();names.add("All countries");for(RadioBrowserClient.CountryItem c:countries)if(RegionCatalog.region(c.code).equals(r))names.add(c.toString());ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);countrySpinner.setAdapter(a);}
    private String selectedCountryCode(){if(countrySpinner.getSelectedItemPosition()<=0)return "";String wanted=String.valueOf(countrySpinner.getSelectedItem());for(RadioBrowserClient.CountryItem c:countries)if(c.toString().equals(wanted))return c.code;return "";}
    private void doCountrySearch(){String code=selectedCountryCode();if(code.isEmpty())return;RadioBrowserClient.byCountry(code,new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{world.clear();world.addAll(x);worldAdapter.notifyDataSetChanged();status.setText("Loaded "+x.size()+" stations");});}public void error(Exception e){runOnUiThread(()->status.setText("World search error"));}});}
    private void doSearch(){String q=search.getText().toString().trim();if(q.isEmpty()){doCountrySearch();return;}String code=selectedCountryCode();RadioBrowserClient.search(q,code,new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{world.clear();world.addAll(x);worldAdapter.notifyDataSetChanged();status.setText("Search: "+x.size()+" stations");});}public void error(Exception e){runOnUiThread(()->status.setText("Search error"));}});}
    private void toggleFavorite(RadioStation s){s.favorite=!s.favorite;if(s.favorite&&!favorites.contains(s))favorites.add(s);if(!s.favorite)favorites.remove(s);saveFavorites();customAdapter.notifyDataSetChanged();iranAdapter.notifyDataSetChanged();worldAdapter.notifyDataSetChanged();}
    private void loadFavorites(){try{File f=new File(getFilesDir(),"favorites.json");if(!f.exists())return;JSONArray a=new JSONArray(readFile(f));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);favorites.add(new RadioStation(o.optString("name"),o.optString("url"),o.optString("country"),o.optString("countryCode"),o.optString("codec"),o.optInt("bitrate"),o.optString("homepage"),o.optString("favicon")));}}catch(Exception ignored){}}
    private void saveFavorites(){try{JSONArray a=new JSONArray();for(RadioStation s:favorites)a.put(toJson(s));writeFile(new File(getFilesDir(),"favorites.json"),a.toString());}catch(Exception ignored){}}
    private JSONObject toJson(RadioStation s)throws Exception{return new JSONObject().put("name",s.name).put("url",s.url).put("country",s.country).put("countryCode",s.countryCode).put("codec",s.codec).put("bitrate",s.bitrate).put("homepage",s.homepage).put("favicon",s.favicon);}
    private void saveWorldList(){try{JSONArray a=new JSONArray();for(RadioStation s:world)a.put(toJson(s));File f=new File(getFilesDir(),"saved_world_list_"+System.currentTimeMillis()+".json");writeFile(f,a.toString());status.setText("Saved: "+f.getName());}catch(Exception e){status.setText("Save failed");}}
    private void showFavorites(){world.clear();for(RadioStation s:builtinFavorites){if(!containsUrl(favorites,s.url))world.add(s);}world.addAll(favorites);worldAdapter.notifyDataSetChanged();status.setText("Favorites: "+world.size()+" • built-in "+builtinFavorites.size());}
    private boolean containsUrl(List<RadioStation> list,String url){for(RadioStation s:list)if(s.url!=null&&s.url.equals(url))return true;return false;}
    private String readFile(File f)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
    private void writeFile(File f,String s)throws Exception{FileOutputStream o=new FileOutputStream(f);o.write(s.getBytes("UTF-8"));o.close();}
    @Override protected void onDestroy(){if(recording)stopRecording();if(tvNewsTicker!=null)tvNewsTicker.stop(); if(webNewsTicker!=null)webNewsTicker.stop(); usageHandler.removeCallbacksAndMessages(null);if(controllerFuture!=null)MediaController.releaseFuture(controllerFuture);super.onDestroy();}
}
