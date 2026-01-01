package org.opensurfcast.http;

public record HttpRequest(
        String url,
        String lastModified,
        String eTag) {
}
