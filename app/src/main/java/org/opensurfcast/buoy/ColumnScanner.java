package org.opensurfcast.buoy;

/**
 * A scanner for parsing whitespace-delimited columns from NDBC data files.
 * Handles the NDBC convention of using "MM" to represent missing values.
 */
class ColumnScanner {

    private static final String MISSING_VALUE = "MM";

    private final String[] parts;
    private int index = 0;

    /**
     * Creates a scanner from pre-split column parts.
     *
     * @param parts the whitespace-split column values
     */
    ColumnScanner(String[] parts) {
        this.parts = parts;
    }

    /**
     * Returns the next column value as an int.
     * @throws NumberFormatException if the value cannot be parsed as an integer
     */
    int nextInt() {
        return Integer.parseInt(nextValue());
    }

    /**
     * Returns the next column value as a Double, or null if the value is "MM" (missing).
     * @throws NumberFormatException if the value is not "MM" and cannot be parsed as a double
     */
    Double nextOptionalDouble() {
        String value = nextValue();
        if (MISSING_VALUE.equals(value)) {
            return null;
        }
        return Double.parseDouble(value);
    }

    /**
     * Returns the next column value as an Integer, or null if the value is "MM" (missing).
     * @throws NumberFormatException if the value is not "MM" and cannot be parsed as an integer
     */
    Integer nextOptionalInteger() {
        String value = nextValue();
        if (MISSING_VALUE.equals(value)) {
            return null;
        }
        return Integer.parseInt(value);
    }

    /**
     * Returns the next column value as a String, or null if the value is "MM" (missing).
     */
    String nextOptionalString() {
        String value = nextValue();
        if (MISSING_VALUE.equals(value)) {
            return null;
        }
        return value;
    }

    private String nextValue() {
        return parts[index++];
    }
}

