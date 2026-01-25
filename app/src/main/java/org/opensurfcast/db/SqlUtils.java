package org.opensurfcast.db;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Utility helpers for SQLite operations.
 */
public final class SqlUtils {

    private SqlUtils() {
    }

    public static String buildPlaceholders(int count) {
        StringJoiner sj = new StringJoiner(", ");
        IntStream.range(0, count).forEach(n -> sj.add("?"));
        return sj.toString();
    }

    static <T> List<T> map(Cursor cursor, Function<Cursor, T> mapper) {
        List<T> result = new ArrayList<>();
        while (cursor.moveToNext()) {
            result.add(mapper.apply(cursor));
        }
        return result;
    }

}
