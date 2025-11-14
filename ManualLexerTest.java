import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Manual test for ToonLexer - runs without JUnit
 */
public class ManualLexerTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== TOON Lexer Manual Tests ===\n");

            testBasicTokens();
            testIndentation();
            testNumbers();
            testStrings();
            testArrayHeader();

            System.out.println("\n=== All tests passed! ===");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testBasicTokens() throws IOException {
        System.out.println("Test: Basic Tokens");
        String input = "name: Alice";
        ToonLexer lexer = new ToonLexer(new StringReader(input));

        assertEqual(ToonToken.IDENTIFIER, lexer.nextToken(), "First token should be IDENTIFIER");
        assertEqual("name", lexer.getTokenText(), "First token text");

        assertEqual(ToonToken.COLON, lexer.nextToken(), "Second token should be COLON");

        assertEqual(ToonToken.IDENTIFIER, lexer.nextToken(), "Third token should be IDENTIFIER");
        assertEqual("Alice", lexer.getTokenText(), "Third token text");

        assertEqual(ToonToken.EOF, lexer.nextToken(), "Should reach EOF");

        System.out.println("  ✓ Basic tokens work\n");
    }

    static void testIndentation() throws IOException {
        System.out.println("Test: Indentation");
        String input = "root:\n  nested: value";
        ToonLexer lexer = new ToonLexer(new StringReader(input));

        lexer.nextToken(); // "root"
        lexer.nextToken(); // ":"

        assertEqual(ToonToken.NEWLINE, lexer.nextToken(), "Should get NEWLINE");
        assertEqual(ToonToken.INDENT, lexer.nextToken(), "Should get INDENT");

        lexer.nextToken(); // "nested"
        lexer.nextToken(); // ":"
        lexer.nextToken(); // "value"

        System.out.println("  ✓ Indentation tracking works\n");
    }

    static void testNumbers() throws IOException {
        System.out.println("Test: Numbers");
        ToonLexer lexer = new ToonLexer(new StringReader("42 3.14 -17 1e6"));

        assertEqual(ToonToken.NUMBER, lexer.nextToken(), "First number");
        assertEqual(42L, lexer.getTokenValue(), "First value");

        assertEqual(ToonToken.NUMBER, lexer.nextToken(), "Second number");
        assertDoubleEqual(3.14, (Double) lexer.getTokenValue(), "Second value");

        assertEqual(ToonToken.NUMBER, lexer.nextToken(), "Third number");
        assertEqual(-17L, lexer.getTokenValue(), "Third value");

        assertEqual(ToonToken.NUMBER, lexer.nextToken(), "Fourth number");
        assertDoubleEqual(1e6, (Double) lexer.getTokenValue(), "Fourth value");

        System.out.println("  ✓ Number parsing works\n");
    }

    static void testStrings() throws IOException {
        System.out.println("Test: Strings");
        ToonLexer lexer = new ToonLexer(new StringReader("\"hello\" \"world\\ntest\""));

        assertEqual(ToonToken.STRING, lexer.nextToken(), "First string");
        assertEqual("hello", lexer.getTokenValue(), "First value");

        assertEqual(ToonToken.STRING, lexer.nextToken(), "Second string");
        assertEqual("world\ntest", lexer.getTokenValue(), "Second value with escape");

        System.out.println("  ✓ String parsing works\n");
    }

    static void testArrayHeader() throws IOException {
        System.out.println("Test: Array Header");
        String input = "items[3]{id,name}:";
        ToonLexer lexer = new ToonLexer(new StringReader(input));

        assertEqual(ToonToken.IDENTIFIER, lexer.nextToken(), "items");
        assertEqual(ToonToken.LBRACKET, lexer.nextToken(), "[");
        assertEqual(ToonToken.NUMBER, lexer.nextToken(), "3");
        assertEqual(3L, lexer.getTokenValue(), "value is 3");
        assertEqual(ToonToken.RBRACKET, lexer.nextToken(), "]");
        assertEqual(ToonToken.LBRACE, lexer.nextToken(), "{");
        assertEqual(ToonToken.IDENTIFIER, lexer.nextToken(), "id");
        assertEqual(ToonToken.COMMA, lexer.nextToken(), ",");
        assertEqual(ToonToken.IDENTIFIER, lexer.nextToken(), "name");
        assertEqual(ToonToken.RBRACE, lexer.nextToken(), "}");
        assertEqual(ToonToken.COLON, lexer.nextToken(), ":");

        System.out.println("  ✓ Array header parsing works\n");
    }

    static void assertEqual(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format(
                "%s: expected <%s> but got <%s>",
                message, expected, actual
            ));
        }
    }

    static void assertDoubleEqual(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(String.format(
                "%s: expected <%f> but got <%f>",
                message, expected, actual
            ));
        }
    }
}
