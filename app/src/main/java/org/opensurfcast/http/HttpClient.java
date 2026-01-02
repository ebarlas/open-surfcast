package org.opensurfcast.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {

    public static <T> T send(String url, Parser<T> parser) throws IOException {
        return send(url, null, parser).value();
    }

    public static <T> Modified<T> send(String url, String lastModified, Parser<T> parser) throws IOException {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setRequestMethod("GET");
        if (lastModified != null) {
            conn.setRequestProperty("If-Modified-Since", lastModified);
        }
        int statusCode = conn.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return null;
        }
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP status code: " + statusCode);
        }
        String lastMod = conn.getHeaderField("Last-Modified");
        try (InputStream is = conn.getInputStream()) {
            T body = parser.parse(is);
            return new Modified<>(body, lastMod);
        }
    }

}
