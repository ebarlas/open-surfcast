package org.opensurfcast.http;

public record Modified<T>(T value, String lastModified) {
}
