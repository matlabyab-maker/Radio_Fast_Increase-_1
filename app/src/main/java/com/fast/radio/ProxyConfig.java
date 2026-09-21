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
        if (!ENABLED || originalUrl == null || originalUrl.trim().isEmpty()) {
            return originalUrl;
        }
        try {
            return SERVER_BASE_URL + "/stream?url=" +
                    java.net.URLEncoder.encode(originalUrl, "UTF-8");
        } catch (Exception e) {
            return originalUrl;
        }
    }
}
