package org.opensurfcast.buoy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class NdbcNoaaParserTest {

    private static final NdbcNoaaParser<String> PARSER =
            new NdbcNoaaParser<>(3, scanner -> {
                String a = scanner.nextOptionalString();
                String b = scanner.nextOptionalString();
                String c = scanner.nextOptionalString();
                return a + "-" + b + "-" + c;
            });

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<String> parse(String text) throws IOException {
        return PARSER.parse(toInputStream(text));
    }

    @Test
    public void testParseLines() throws IOException {
        String input = """
                A B C
                D E F
                """;
        List<String> results = parse(input);
        assertEquals(2, results.size());
        assertEquals("A-B-C", results.get(0));
        assertEquals("D-E-F", results.get(1));
    }

    @Test
    public void testSkipsCommentLines() throws IOException {
        String input = """
                #header
                #units
                A B C
                """;
        List<String> results = parse(input);
        assertEquals(1, results.size());
        assertEquals("A-B-C", results.get(0));
    }

    @Test
    public void testSkipsEmptyLines() throws IOException {
        String input = """
                A B C
                
                D E F
                """;
        List<String> results = parse(input);
        assertEquals(2, results.size());
    }

    @Test
    public void testTrimsWhitespace() throws IOException {
        List<String> results = parse("  A   B   C  ");
        assertEquals("A-B-C", results.get(0));
    }

    @Test
    public void testEmptyInput() throws IOException {
        List<String> results = parse("");
        assertEquals(0, results.size());
    }

    @Test
    public void testInsufficientColumnsThrows() {
        IOException ex = assertThrows(IOException.class, () -> parse("A B"));
        assertTrue(ex.getMessage().contains("Expected 3 columns but found 2"));
    }
}
