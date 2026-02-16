package org.opensurfcast.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * I/O utility helpers.
 * <p>
 * Provides API-level-compatible alternatives to Java APIs that require
 * higher SDK versions (e.g. {@code InputStream#readAllBytes} requires API 33).
 */
public final class IoUtils {

    private static final int BUFFER_SIZE = 8192;

    private IoUtils() {
    }

    /**
     * Reads the entire input stream into a String using UTF-8 encoding.
     * <p>
     * Compatible with API 24+. Use instead of
     * {@code new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)}
     * which requires API 33.
     *
     * @param inputStream the stream to read
     * @return the stream contents as a UTF-8 string
     * @throws IOException if an I/O error occurs
     */
    public static String readStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
