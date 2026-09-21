package com.fast.radio;

/**
 * Settings for the optional bandwidth-reduction middleware.
 * Put the public server address here, for example:
 * http://YOUR-VPS-IP:8000
 *
 * Leave empty to use the station's original URL directly.
 */
public final class ProxyConfig {
    private ProxyConfig() {}

    public static final String SERVER_BASE_URL = "";
    public static final boolean ENABLED = !SERVER_BASE_URL.trim().isEmpty();

    public static String wrap(String originalUrl) {
        String normalized = normalize(originalUrl);
        if (!ENABLED || normalized == null || normalized.trim().isEmpty()) {
            return normalized;
        }
        try {
            return wrap(normalized, 48);
        } catch (Exception e) {
            return normalized;
        }
    }

    public static String wrap(String originalUrl, int kbps) {
        String normalized = normalize(originalUrl);
        if (!ENABLED || normalized == null || normalized.trim().isEmpty()) return normalized;
        try {
            int safe = Math.max(1, Math.min(400, kbps));
            return SERVER_BASE_URL + "/stream?url=" +
                    java.net.URLEncoder.encode(normalized, "UTF-8") +
                    "&kbps=" + safe;
        } catch (Exception e) { return normalized; }
    }

    /** RadioJar signed URLs contain very short-lived rj-ttl/rj-tok query parameters.
     *  RadioJar also exposes the same station through its stable stream endpoint.
     */
    public static String normalize(String originalUrl) {
        if (originalUrl == null) return null;
        String u = originalUrl.trim();
        try {
            java.net.URI x = new java.net.URI(u);
            String host = x.getHost();
            String path = x.getPath();
            if (host != null && host.toLowerCase(java.util.Locale.US).contains("radiojar.com")
                    && path != null && !path.isEmpty()) {
                String station = path.substring(path.lastIndexOf('/') + 1);
                if (!station.isEmpty()) return "https://stream.radiojar.com/" + station;
            }
        } catch (Exception ignored) {}
        return u;
    }
}
