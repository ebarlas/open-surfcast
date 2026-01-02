package org.opensurfcast.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {

    public static <T> T send(String url, Parser<T> parser) throws IOException {
        return send(new HttpRequest(url, null, null), parser).body();
    }

    public static <T> HttpResponse<T> send(HttpRequest request, Parser<T> parser) throws IOException {
        URL url = new URL(request.url());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setRequestMethod("GET");
        if (request.lastModified() != null) {
            conn.setRequestProperty("If-Modified-Since", request.lastModified());
        } else if (request.eTag() != null) {
            conn.setRequestProperty("If-None-Match", request.eTag());
        }
        int statusCode = conn.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return new HttpResponse<>(false, null, null, null);
        }
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP status code: " + statusCode);
        }
        String lastModified = conn.getHeaderField("Last-Modified");
        String eTag = conn.getHeaderField("ETag");
        try (InputStream is = conn.getInputStream()) {
            T body = parser.parse(is);
            return new HttpResponse<>(true, lastModified, eTag, body);
        }
    }

}
