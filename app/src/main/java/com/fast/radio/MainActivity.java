package com.fast.radio;

import android.content.*;
import android.os.*;
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

public class MainActivity extends AppCompatActivity {
    private ListView customList, iranList, worldList, newsList;
    private TextView status, nowPlaying, qualityValue, usagePerMinute;
    private final List<RadioStation> custom=new ArrayList<>(), iran=new ArrayList<>(), world=new ArrayList<>(), favorites=new ArrayList<>();
    private StationAdapter customAdapter, iranAdapter, worldAdapter;
    private MediaController controller; private ListenableFuture<MediaController> controllerFuture;
    private Spinner regionSpinner, countrySpinner; private EditText search;
    private final List<RadioBrowserClient.CountryItem> countries=new ArrayList<>();
    private final String[] regions=RegionCatalog.REGIONS;
    private VerticalRulerView qualityRuler; private RadioStation selected;
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
        qualityRuler=findViewById(R.id.qualityRuler); regionSpinner=findViewById(R.id.regionSpinner); countrySpinner=findViewById(R.id.countrySpinner); search=findViewById(R.id.search);
        qualityRuler.setListener(v->{qualityValue.setText(v+" kbps"); updateUsage(v); status.setText("Quality target: "+v+" kbps");});
        findViewById(R.id.play).setOnClickListener(v->playSelected()); findViewById(R.id.stop).setOnClickListener(v->{if(controller!=null)controller.stop();status.setText("Stopped");});
        findViewById(R.id.fav).setOnClickListener(v->{if(selected!=null)toggleFavorite(selected);});
        findViewById(R.id.searchButton).setOnClickListener(v->doSearch()); findViewById(R.id.saveList).setOnClickListener(v->saveWorldList()); findViewById(R.id.favorites).setOnClickListener(v->showFavorites());
        setupScroll(R.id.customUp,customList,true); setupScroll(R.id.customDown,customList,false); setupScroll(R.id.iranUp,iranList,true); setupScroll(R.id.iranDown,iranList,false);
        updateUsage(15);
    }
    private void updateUsage(int kbps){ double mb=(kbps*60.0)/(8.0*1024.0); usagePerMinute.setText(String.format(Locale.US,"%.3f MB / min",mb)); }
    private void setupScroll(int id,ListView l,boolean up){findViewById(id).setOnClickListener(v->{int p=l.getFirstVisiblePosition();l.setSelection(Math.max(0,p+(up?-8:8)));});}
    private void loadAssets(){try{JSONArray a=new JSONArray(readAsset("stations.json"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);custom.add(new RadioStation(o.optString("name"),o.optString("url")));}}catch(Exception ignored){} loadFavorites();}
    private String readAsset(String n)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open(n),"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
    private void setupLists(){StationAdapter.Listener l=new StationAdapter.Listener(){public void select(RadioStation s){showSelected(s);}public void favorite(RadioStation s){toggleFavorite(s);}};customAdapter=new StationAdapter(this,custom,l);iranAdapter=new StationAdapter(this,iran,l);worldAdapter=new StationAdapter(this,world,l);customList.setAdapter(customAdapter);iranList.setAdapter(iranAdapter);worldList.setAdapter(worldAdapter);}
    private void setupNews(){
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,newsNames){@Override public View getView(int p,View c,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,c,parent);t.setText(newsNames[p]+"   ★");t.setTextColor(android.graphics.Color.rgb(255,225,45));t.setTextSize(14);t.setPadding(10,6,4,6);t.setBackgroundColor(android.graphics.Color.TRANSPARENT);return t;}};
        newsList.setAdapter(a); newsList.setOnItemClickListener((p,v,pos,id)->{String q=newsNames[pos].replace(" فارسی",""); RadioBrowserClient.search(q,"",new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{world.clear();world.addAll(x);worldAdapter.notifyDataSetChanged();status.setText(newsNames[pos]+" • "+x.size()+" results");});}public void error(Exception e){runOnUiThread(()->status.setText("No live result for "+newsNames[pos]));}});});
    }
    private void showSelected(RadioStation s){selected=s;nowPlaying.setText(s.name+"  •  "+(s.country.isEmpty()?"":s.country)+"  •  "+(s.bitrate>0?s.bitrate+" kbps":"Auto"));status.setText("Selected • target "+qualityRuler.getValue()+" kbps");}
    private void connectController(){SessionToken token=new SessionToken(this,new ComponentName(this,RadioPlaybackService.class));controllerFuture=new MediaController.Builder(this,token).buildAsync();controllerFuture.addListener(()->{try{controller=controllerFuture.get();}catch(Exception e){status.setText("Controller error");}},getMainExecutor());}
    private void playSelected(){if(selected==null){status.setText("Select a station first");return;}if(controller==null){status.setText("Player not ready");return;}String finalUrl = ProxyConfig.wrap(selected.url); controller.setMediaItem(MediaItem.fromUri(finalUrl));controller.prepare();controller.play();nowPlaying.setText("▶ "+selected.name);status.setText("Media3 ExoPlayer • buffer 5–10 s • target "+qualityRuler.getValue()+" kbps");}
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
    private void showFavorites(){world.clear();world.addAll(favorites);worldAdapter.notifyDataSetChanged();status.setText("Favorites: "+favorites.size());}
    private String readFile(File f)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
    private void writeFile(File f,String s)throws Exception{FileOutputStream o=new FileOutputStream(f);o.write(s.getBytes("UTF-8"));o.close();}
    @Override protected void onDestroy(){if(controllerFuture!=null)MediaController.releaseFuture(controllerFuture);super.onDestroy();}
}
