package org.opensurfcast.buoy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generic parser for NDBC whitespace-delimited data files.
 * Handles reading lines, skipping comments (lines starting with #), and
 * delegating row parsing to a provided function.
 *
 * @param <T> the type of object produced for each data row
 */
class NdbcNoaaParser<T> {

    private final int expectedColumns;
    private final Function<ColumnScanner, T> rowMapper;

    /**
     * Creates a parser with the specified column count and row mapper.
     *
     * @param expectedColumns minimum number of columns expected per row
     * @param rowMapper       function to convert each row's ColumnScanner to type T
     */
    NdbcNoaaParser(int expectedColumns, Function<ColumnScanner, T> rowMapper) {
        this.expectedColumns = expectedColumns;
        this.rowMapper = rowMapper;
    }

    /**
     * Parses the given input stream and returns a list of parsed objects.
     *
     * @param inputStream the input stream to parse
     * @return list of parsed objects
     * @throws IOException if an error occurs during parsing
     */
    List<T> parse(InputStream inputStream) throws IOException {
        List<T> results = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");
            if (parts.length < expectedColumns) {
                throw new IOException("Expected " + expectedColumns + " columns but found " + parts.length);
            }

            ColumnScanner scanner = new ColumnScanner(parts);
            T result = rowMapper.apply(scanner);
            results.add(result);
        }

        return results;
    }
}

