package com.fast.radio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Finds a Fast Radio relay without requiring an address in MainActivity.
 * The app tries the bundled discovery directory, validates /health, and caches
 * the first healthy relay locally. If discovery fails, callers can fall back
 * to direct station playback.
 */
public final class RelayDiscovery {
    private static final String PREFS = "fast_radio_relay";
    private static final String KEY_BASE = "relay_base";
    private static final int TIMEOUT_MS = 4500;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback { void result(String baseUrl); }

    public RelayDiscovery(Context context) { this.context = context.getApplicationContext(); }

    public void discover(Callback callback) {
        executor.execute(() -> {
            String cached = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_BASE, "");
            String found = isHealthy(cached) ? normalizeBase(cached) : "";
            if (found.isEmpty()) {
                for (String candidate : bundledCandidates()) {
                    if (isHealthy(candidate)) { found = normalizeBase(candidate); break; }
                }
            }
            final String result = found;
            if (!result.isEmpty()) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_BASE, result).apply();
            main.post(() -> callback.result(result));
        });
    }

    public void clearCachedRelay() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_BASE).apply();
    }

    private List<String> bundledCandidates() {
        List<String> out = new ArrayList<>();
        try (InputStream in = context.getAssets().open("relay_directory.json")) {
            StringBuilder b = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line; while ((line = r.readLine()) != null) b.append(line);
            JSONArray a = new JSONObject(b.toString()).optJSONArray("relays");
            if (a != null) for (int i=0;i<a.length();i++) {
                String u = a.optString(i, "").trim(); if (!u.isEmpty()) out.add(normalizeBase(u));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private boolean isHealthy(String base) {
        if (base == null || base.trim().isEmpty()) return false;
        HttpURLConnection c = null;
        try {
            URL u = new URL(normalizeBase(base) + "/health");
            c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(TIMEOUT_MS); c.setReadTimeout(TIMEOUT_MS);
            c.setRequestMethod("GET");
            if (c.getResponseCode() != 200) return false;
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder b = new StringBuilder(); String line; while ((line=r.readLine())!=null)b.append(line);
            JSONObject o = new JSONObject(b.toString());
            return o.optBoolean("ok", false) && o.optBoolean("stream_relay", false);
        } catch (Exception ignored) { return false; }
        finally { if (c != null) c.disconnect(); }
    }

    public static String normalizeBase(String value) {
        String s = value == null ? "" : value.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length()-1);
        return s;
    }
}
