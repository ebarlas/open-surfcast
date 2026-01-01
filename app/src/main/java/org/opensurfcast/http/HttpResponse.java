package org.opensurfcast.http;

public record HttpResponse<T>(
        boolean present,
        String lastModified,
        String eTag,
        T body) {}
