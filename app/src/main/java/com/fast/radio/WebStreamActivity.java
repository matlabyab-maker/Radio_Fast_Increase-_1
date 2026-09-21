package com.fast.radio;

import android.app.AlertDialog;

import android.content.*;
import android.os.*;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.*;
import java.io.*;
import java.util.*;
import java.net.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.view.animation.DecelerateInterpolator;
import android.provider.Settings;

public class WebStreamActivity extends AppCompatActivity {
    private ListView customList, iranList, worldList, newsList;
    private TextView status, nowPlaying, qualityValue, usagePerMinute;
    private final List<RadioStation> custom=new ArrayList<>(), iran=new ArrayList<>(), world=new ArrayList<>(), favorites=new ArrayList<>(), builtinFavorites=new ArrayList<>();
    private StationAdapter customAdapter, iranAdapter, worldAdapter;
    private MediaController controller; private ListenableFuture<MediaController> controllerFuture;
    private Spinner regionSpinner, countrySpinner; private EditText search;
    private final List<RadioBrowserClient.CountryItem> countries=new ArrayList<>();
    private final String[] regions=RegionCatalog.REGIONS;
    private VerticalRulerView qualityRuler; private VolumeRulerView volumeRuler; private RadioStation selected;
    private View yellowLight;
    private Handler yellowHandler = new Handler(Looper.getMainLooper());
    private Runnable yellowRunnable;
    private static final int REQ_IMPORT_FILE=711;
    private MediaRecorder recorder; private boolean recording=false; private TextView recordLight; private static final int REQ_RECORD_AUDIO=401;
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
        status=findViewById(R.id.status); nowPlaying=findViewById(R.id.nowPlaying); qualityValue=findViewById(R.id.qualityValue); usagePerMinute=findViewById(R.id.usagePerMinute);
        qualityRuler=findViewById(R.id.qualityRuler); volumeRuler=findViewById(R.id.volumeRuler); recordLight=findViewById(R.id.recordLight); regionSpinner=findViewById(R.id.regionSpinner); countrySpinner=findViewById(R.id.countrySpinner); search=findViewById(R.id.search);
        qualityRuler.setListener(v->{qualityValue.setText(v+" kbps"); updateUsage(v); status.setText("Quality target: "+v+" kbps");});
        volumeRuler.setListener(v->setOutputVolume(v));
        findViewById(R.id.record).setOnClickListener(v->toggleRecording());
        findViewById(R.id.play).setOnClickListener(v->playSelected()); findViewById(R.id.stop).setOnClickListener(v->{if(controller!=null)controller.stop();status.setText("Stopped");});
        findViewById(R.id.fav).setOnClickListener(v->{if(selected!=null)toggleFavorite(selected);});
        findViewById(R.id.searchButton).setOnClickListener(v->doSearch()); findViewById(R.id.saveList).setOnClickListener(v->saveWorldList()); findViewById(R.id.favorites).setOnClickListener(v->showFavorites());
        findViewById(R.id.importButton).setOnClickListener(v->showImportDialog());
        findViewById(R.id.savedListsButton).setOnClickListener(v->showSavedLists());
        setupYellowMinuteLight();
        setupScroll(R.id.customUp,customList,true); setupScroll(R.id.customDown,customList,false); setupScroll(R.id.iranUp,iranList,true); setupScroll(R.id.iranDown,iranList,false);
        updateUsage(15);
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

    private void updateUsage(int kbps){ double mb=(kbps*60.0)/(8.0*1024.0); usagePerMinute.setText(String.format(Locale.US,"%.3f MB / min",mb)); }
    private void setupScroll(int id,ListView l,boolean up){findViewById(id).setOnClickListener(v->{int p=l.getFirstVisiblePosition();l.setSelection(Math.max(0,p+(up?-8:8)));});}
    private void loadAssets(){try{JSONArray a=new JSONArray(readAsset("stations.json"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);custom.add(new RadioStation(o.optString("name"),o.optString("url")));}}catch(Exception ignored){} loadBuiltInFavorites(); loadFavorites();}
    private String readAsset(String n)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open(n),"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
    private void loadBuiltInFavorites(){try{JSONArray a=new JSONArray(readAsset("builtin_favorites.json"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String url=o.optString("url");if(url==null||url.trim().isEmpty())continue;builtinFavorites.add(new RadioStation(o.optString("name"),url,o.optString("country"),o.optString("countryCode",o.optString("countrycode")),o.optString("codec"),o.optInt("bitrate",0),o.optString("homepage"),o.optString("favicon")));}}catch(Exception ignored){}}
    private void setupLists(){StationAdapter.Listener l=new StationAdapter.Listener(){public void select(RadioStation s){showSelected(s);}public void favorite(RadioStation s){toggleFavorite(s);}};customAdapter=new StationAdapter(this,custom,l);iranAdapter=new StationAdapter(this,iran,l);worldAdapter=new StationAdapter(this,world,l);customList.setAdapter(customAdapter);iranList.setAdapter(iranAdapter);worldList.setAdapter(worldAdapter);}
    private void setupNews(){
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,newsNames){@Override public View getView(int p,View c,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,c,parent);t.setText(newsNames[p]+"   ★");t.setTextColor(android.graphics.Color.rgb(255,225,45));t.setTextSize(14);t.setPadding(10,6,4,6);t.setBackgroundColor(android.graphics.Color.TRANSPARENT);return t;}};
        newsList.setAdapter(a); newsList.setOnItemClickListener((p,v,pos,id)->{String q=newsNames[pos].replace(" فارسی",""); RadioBrowserClient.search(q,"",new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{world.clear();world.addAll(x);worldAdapter.notifyDataSetChanged();status.setText(newsNames[pos]+" • "+x.size()+" results");});}public void error(Exception e){runOnUiThread(()->status.setText("No live result for "+newsNames[pos]));}});});
    }
    private void showSelected(RadioStation s){selected=s;nowPlaying.setText(s.name+"  •  "+(s.country.isEmpty()?"":s.country)+"  •  "+(s.bitrate>0?s.bitrate+" kbps":"Auto"));status.setText("Selected • target "+qualityRuler.getValue()+" kbps");}
    private void connectController(){SessionToken token=new SessionToken(this,new ComponentName(this,RadioPlaybackService.class));controllerFuture=new MediaController.Builder(this,token).buildAsync();controllerFuture.addListener(()->{try{controller=controllerFuture.get();}catch(Exception e){status.setText("Controller error");}},getMainExecutor());}
    private void playSelected(){
        if(selected==null){status.setText("Select a station first");return;}
        String finalUrl = ProxyConfig.wrap(selected.url);
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
    private void saveWorldList(){
        try{
            JSONArray a=new JSONArray(); for(RadioStation s:world)a.put(toJson(s));
            File f=new File(getFilesDir(),"saved_search_"+System.currentTimeMillis()+".json");
            writeFile(f,a.toString()); status.setText("Search list saved: "+f.getName());
        }catch(Exception e){status.setText("Save failed");}
    }
    private void showSavedLists(){
        File[] fs=getFilesDir().listFiles((d,n)->n.startsWith("saved_search_")&&n.endsWith(".json"));
        if(fs==null||fs.length==0){status.setText("No saved search lists");return;}
        Arrays.sort(fs,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
        String[] names=new String[fs.length]; for(int i=0;i<fs.length;i++)names[i]=fs[i].getName();
        new AlertDialog.Builder(this).setTitle("Saved search lists").setItems(names,(d,w)->loadSavedList(fs[w])).setNegativeButton("CLOSE",null).show();
    }
    private void loadSavedList(File f){
        try{
            JSONArray a=new JSONArray(readFile(f)); world.clear();
            for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);world.add(new RadioStation(o.optString("name"),o.optString("url"),o.optString("country"),o.optString("countryCode"),o.optString("codec"),o.optInt("bitrate"),o.optString("homepage"),o.optString("favicon")));}
            worldAdapter.notifyDataSetChanged(); status.setText("Loaded "+world.size()+" stations");
        }catch(Exception e){status.setText("Could not load saved list");}
    }
    private void showImportDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(24,8,24,0);
        TextView hint=new TextView(this); hint.setText("Paste one or many URLs, M3U/PLS/TXT/JSON content, or a webpage address. One item per line."); hint.setTextColor(Color.DKGRAY);
        box.addView(hint);
        EditText input=new EditText(this); input.setHint("http://...\\nhttps://..."); input.setGravity(Gravity.TOP); input.setMinLines(7); input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_VARIATION_URI); box.addView(input,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Import radio / web list").setView(box).setPositiveButton("IMPORT",null).setNeutralButton("OPEN FILE",null).setNegativeButton("CANCEL",null).create();
        dlg.setOnShowListener(x->{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{importText(input.getText().toString());dlg.dismiss();});dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{dlg.dismiss();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_IMPORT_FILE);});}); dlg.show();
    }
    private void importText(String raw){
        String t=raw==null?"":raw.trim(); if(t.isEmpty())return;
        if((t.startsWith("http://")||t.startsWith("https://")) && (t.endsWith(".m3u")||t.endsWith(".m3u8")||t.endsWith(".pls")||t.endsWith(".txt")||t.contains("m3u8?"))){
            importRemoteList(t); return;
        }
        List<RadioStation> parsed=parseListText(t); addImported(parsed);
    }
    private List<RadioStation> parseListText(String raw){
        List<RadioStation> out=new ArrayList<>(); String pendingName=null;
        String[] lines=raw.replace("\r","").split("\n");
        for(String line:lines){
            String x=line.trim(); if(x.isEmpty())continue;
            if(x.startsWith("#EXTINF")){int comma=x.indexOf(',');pendingName=comma>=0?x.substring(comma+1).trim():"Imported";continue;}
            if(x.startsWith("[playlist]"))continue;
            if(x.matches("(?i)^Title\\d+=.*")){pendingName=x.substring(x.indexOf('=')+1).trim();continue;}
            if(x.matches("(?i)^File\\d+=.*")){String u=x.substring(x.indexOf('=')+1).trim();if(u.matches("(?i)https?://\\S+")){out.add(new RadioStation(pendingName==null?"Imported Stream":pendingName,u));pendingName=null;}continue;}
            if(x.startsWith("Length")||x.startsWith("#"))continue;
            if(x.matches("(?i)https?://\\S+")){
                String name=pendingName!=null?pendingName:(x.contains(".m3u8")?"HLS Stream":"Imported Stream");
                out.add(new RadioStation(name,x)); pendingName=null;
            }
        }
        return out;
    }
    private void importRemoteList(String url){
        status.setText("Reading remote list…");
        new Thread(()->{try{
            String data=downloadText(url); List<RadioStation> list=parseListText(data);
            if(list.isEmpty()) list.add(new RadioStation("Web page / stream",url));
            runOnUiThread(()->addImported(list));
        }catch(Exception e){runOnUiThread(()->{addImported(Collections.singletonList(new RadioStation("Web page / stream",url)));status.setText("Web page imported");});}}).start();
    }
    private String downloadText(String u)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","FastRadio/3.7");
        BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l).append('\n');r.close();return b.toString();
    }
    private void addImported(List<RadioStation> list){
        if(list==null||list.isEmpty()){status.setText("No playable URL found");return;}
        custom.addAll(list);customAdapter.notifyDataSetChanged(); status.setText("Imported "+list.size()+" item(s)");
    }
    @Override protected void onActivityResult(int r,int c,Intent data){
        super.onActivityResult(r,c,data);
        if(r==REQ_IMPORT_FILE&&c==RESULT_OK&&data!=null&&data.getData()!=null)new Thread(()->{try{
            InputStream in=getContentResolver().openInputStream(data.getData());BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=br.readLine())!=null)b.append(l).append('\n');br.close();
            List<RadioStation> list=parseListText(b.toString());runOnUiThread(()->addImported(list));
        }catch(Exception e){runOnUiThread(()->status.setText("Could not import file"));}}).start();
    }
    private void setupYellowMinuteLight(){
        if(yellowLight==null){
            FrameLayout decor=new FrameLayout(this);
            ViewGroup oldParent=(ViewGroup)findViewById(android.R.id.content);
            View content=oldParent.getChildAt(0);
            oldParent.removeView(content);
            decor.addView(content,new FrameLayout.LayoutParams(-1,-1));
            yellowLight=new View(this);
            GradientDrawable glow=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00FFFF00,0x44FFFF00,0x00FFFF00});
            yellowLight.setBackground(glow); yellowLight.setVisibility(View.GONE); yellowLight.setClickable(false);
            decor.addView(yellowLight,new FrameLayout.LayoutParams(-1,-1));
            oldParent.addView(decor,new ViewGroup.LayoutParams(-1,-1));
        }
        yellowRunnable=()->{
            if(yellowLight==null)return;
            yellowLight.setAlpha(0f); yellowLight.setVisibility(View.VISIBLE);
            ObjectAnimator fade=ObjectAnimator.ofFloat(yellowLight,"alpha",0f,0.30f,0f); fade.setDuration(5000); fade.setInterpolator(new DecelerateInterpolator()); fade.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){yellowLight.setVisibility(View.GONE);}});
            fade.start(); yellowHandler.postDelayed(yellowRunnable,60000);
        };
        yellowHandler.postDelayed(yellowRunnable,60000);
    }
    private void showFavorites(){world.clear();for(RadioStation s:builtinFavorites){if(!containsUrl(favorites,s.url))world.add(s);}world.addAll(favorites);worldAdapter.notifyDataSetChanged();status.setText("Favorites: "+world.size()+" • built-in "+builtinFavorites.size());}
    private boolean containsUrl(List<RadioStation> list,String url){for(RadioStation s:list)if(s.url!=null&&s.url.equals(url))return true;return false;}
    private String readFile(File f)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
    private void writeFile(File f,String s)throws Exception{FileOutputStream o=new FileOutputStream(f);o.write(s.getBytes("UTF-8"));o.close();}
    @Override protected void onDestroy(){if(recording)stopRecording(); if(controllerFuture!=null)MediaController.releaseFuture(controllerFuture); if(yellowHandler!=null&&yellowRunnable!=null)yellowHandler.removeCallbacks(yellowRunnable); super.onDestroy();}
}
