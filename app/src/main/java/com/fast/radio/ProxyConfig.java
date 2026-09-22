package com.fast.radio;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Automatic Fast Radio relay profile and URL builder. */
public final class ProxyConfig {
    private ProxyConfig() {}

    public static final boolean AUTO_RELAY = true;
    public static final boolean AUTO_NETWORK_QUALITY = true;
    public static final int WIFI_KBPS = 48;
    public static final int MOBILE_KBPS = 24;
    public static final int OPUS_SAMPLE_RATE = 24000;

    public static int effectiveKbps(Context context, int requestedKbps) {
        int requested = Math.max(8, Math.min(128, requestedKbps));
        if (!AUTO_NETWORK_QUALITY) return requested;
        if (isMobileNetwork(context)) return Math.min(requested, MOBILE_KBPS);
        return Math.min(requested, WIFI_KBPS);
    }

    private static boolean isMobileNetwork(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            if (network == null) return true;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) return true;
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true;
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return false;
        } catch (Exception ignored) {}
        return true;
    }

    public static String wrap(Context context, String relayBase, String originalUrl, int requestedKbps) {
        String normalized = normalize(originalUrl);
        if (!AUTO_RELAY || relayBase == null || relayBase.trim().isEmpty() || normalized == null || normalized.isEmpty()) return normalized;
        try {
            int kbps = effectiveKbps(context, requestedKbps);
            return RelayDiscovery.normalizeBase(relayBase) + "/stream?url=" +
                    java.net.URLEncoder.encode(normalized, "UTF-8") +
                    "&kbps=" + kbps + "&codec=opus&mono=1&sample_rate=" + OPUS_SAMPLE_RATE;
        } catch (Exception e) { return normalized; }
    }

    public static String normalize(String originalUrl) {
        if (originalUrl == null) return null;
        String u = originalUrl.trim();
        try {
            java.net.URI x = new java.net.URI(u);
            String host = x.getHost(); String path = x.getPath();
            if (host != null && host.toLowerCase(java.util.Locale.US).contains("radiojar.com") && path != null && !path.isEmpty()) {
                String station = path.substring(path.lastIndexOf('/') + 1);
                if (!station.isEmpty()) return "https://stream.radiojar.com/" + station;
            }
        } catch (Exception ignored) {}
        return u;
    }
}
