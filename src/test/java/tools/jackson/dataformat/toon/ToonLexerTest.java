package tools.jackson.dataformat.toon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ToonLexer.
 */
public class ToonLexerTest {

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private ToonLexer createLexer(String input) throws IOException {
        return new ToonLexer(new StringReader(input));
    }

    private ToonLexer createLexer(String input, int indentSize, boolean strictMode) throws IOException {
        return new ToonLexer(new StringReader(input), indentSize, strictMode);
    }

    private List<ToonToken> tokenize(String input) throws IOException {
        ToonLexer lexer = createLexer(input);
        List<ToonToken> tokens = new ArrayList<>();
        ToonToken token;
        while ((token = lexer.nextToken()) != ToonToken.EOF) {
            tokens.add(token);
        }
        tokens.add(ToonToken.EOF);
        return tokens;
    }

    private void assertTokenSequence(String input, ToonToken... expectedTokens) throws IOException {
        List<ToonToken> actual = tokenize(input);
        assertEquals(expectedTokens.length, actual.size(),
            "Token count mismatch. Expected: " + java.util.Arrays.toString(expectedTokens)
            + ", Actual: " + actual);

        for (int i = 0; i < expectedTokens.length; i++) {
            assertEquals(expectedTokens[i], actual.get(i),
                "Token mismatch at position " + i);
        }
    }

    // ========================================================================
    // Basic Token Tests
    // ========================================================================

    @Test
    public void testStructuralTokens() throws IOException {
        assertTokenSequence(":", ToonToken.COLON, ToonToken.EOF);
        assertTokenSequence(",", ToonToken.COMMA, ToonToken.EOF);
        assertTokenSequence("|", ToonToken.PIPE, ToonToken.EOF);
        assertTokenSequence("[", ToonToken.LBRACKET, ToonToken.EOF);
        assertTokenSequence("]", ToonToken.RBRACKET, ToonToken.EOF);
        assertTokenSequence("{", ToonToken.LBRACE, ToonToken.EOF);
        assertTokenSequence("}", ToonToken.RBRACE, ToonToken.EOF);
        assertTokenSequence("- ", ToonToken.HYPHEN, ToonToken.EOF);
    }

    @Test
    public void testKeywords() throws IOException {
        ToonLexer lexer = createLexer("true false null");

        assertEquals(ToonToken.BOOLEAN, lexer.nextToken());
        assertEquals(Boolean.TRUE, lexer.getTokenValue());

        assertEquals(ToonToken.BOOLEAN, lexer.nextToken());
        assertEquals(Boolean.FALSE, lexer.getTokenValue());

        assertEquals(ToonToken.NULL, lexer.nextToken());
        assertNull(lexer.getTokenValue());
    }

    @Test
    public void testIdentifiers() throws IOException {
        ToonLexer lexer = createLexer("name user_id data.field _private");

        assertEquals(ToonToken.IDENTIFIER, lexer.nextToken());
        assertEquals("name", lexer.getTokenText());

        assertEquals(ToonToken.IDENTIFIER, lexer.nextToken());
        assertEquals("user_id", lexer.getTokenText());

        assertEquals(ToonToken.IDENTIFIER, lexer.nextToken());
        assertEquals("data.field", lexer.getTokenText());

        assertEquals(ToonToken.IDENTIFIER, lexer.nextToken());
        assertEquals("_private", lexer.getTokenText());
    }

    @Test
    public void testIntegers() throws IOException {
        ToonLexer lexer = createLexer("0 42 -17 999");

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(0L, lexer.getTokenValue());

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(42L, lexer.getTokenValue());

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(-17L, lexer.getTokenValue());

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(999L, lexer.getTokenValue());
    }

    @Test
    public void testFloats() throws IOException {
        ToonLexer lexer = createLexer("3.14 -0.5 0.123");

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(3.14, (Double) lexer.getTokenValue(), 0.001);

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(-0.5, (Double) lexer.getTokenValue(), 0.001);

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(0.123, (Double) lexer.getTokenValue(), 0.001);
    }

    @Test
    public void testExponentNumbers() throws IOException {
        ToonLexer lexer = createLexer("1e6 1.5e-3 -1E+9");

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(1e6, (Double) lexer.getTokenValue(), 0.001);

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(1.5e-3, (Double) lexer.getTokenValue(), 0.00001);

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(-1E+9, (Double) lexer.getTokenValue(), 0.001);
    }

    @Test
    public void testLeadingZerosAreStrings() throws IOException {
        ToonLexer lexer = createLexer("007 0123");

        // Leading zeros should be treated as identifiers/strings
        assertEquals(ToonToken.IDENTIFIER, lexer.nextToken());
        assertEquals("007", lexer.getTokenText());

        assertEquals(ToonToken.IDENTIFIER, lexer.nextToken());
        assertEquals("0123", lexer.getTokenText());
    }

    @Test
    public void testQuotedStrings() throws IOException {
        ToonLexer lexer = createLexer("\"hello\" \"world\" \"\"");

        assertEquals(ToonToken.STRING, lexer.nextToken());
        assertEquals("hello", lexer.getTokenValue());

        assertEquals(ToonToken.STRING, lexer.nextToken());
        assertEquals("world", lexer.getTokenValue());

        assertEquals(ToonToken.STRING, lexer.nextToken());
        assertEquals("", lexer.getTokenValue());
    }

    @Test
    public void testEscapeSequences() throws IOException {
        ToonLexer lexer = createLexer("\"line1\\nline2\" \"tab\\there\" \"quote\\\"test\" \"backslash\\\\\"");

        assertEquals(ToonToken.STRING, lexer.nextToken());
        assertEquals("line1\nline2", lexer.getTokenValue());

        assertEquals(ToonToken.STRING, lexer.nextToken());
        assertEquals("tab\there", lexer.getTokenValue());

        assertEquals(ToonToken.STRING, lexer.nextToken());
        assertEquals("quote\"test", lexer.getTokenValue());

        assertEquals(ToonToken.STRING, lexer.nextToken());
        assertEquals("backslash\\", lexer.getTokenValue());
    }

    @Test
    public void testInvalidEscapeInStrictMode() throws IOException {
        ToonLexer lexer = createLexer("\"test\\uABCD\"", 2, true);

        assertEquals(ToonToken.ERROR, lexer.nextToken());
        assertNotNull(lexer.getErrorMessage());
        assertTrue(lexer.getErrorMessage().contains("Invalid escape"));
    }

    @Test
    public void testUnterminatedString() throws IOException {
        ToonLexer lexer = createLexer("\"unterminated");

        assertEquals(ToonToken.ERROR, lexer.nextToken());
        assertNotNull(lexer.getErrorMessage());
        assertTrue(lexer.getErrorMessage().contains("Unterminated"));
    }

    // ========================================================================
    // Indentation Tests
    // ========================================================================

    @Test
    public void testSimpleIndent() throws IOException {
        String input = "key:\n  value";
        assertTokenSequence(input,
            ToonToken.IDENTIFIER,  // "key"
            ToonToken.COLON,
            ToonToken.NEWLINE,
            ToonToken.INDENT,      // Indentation increased
            ToonToken.IDENTIFIER,  // "value"
            ToonToken.EOF
        );
    }

    @Test
    public void testSimpleDedent() throws IOException {
        String input = "root:\n  nested: value\nback: here";
        assertTokenSequence(input,
            ToonToken.IDENTIFIER,  // "root"
            ToonToken.COLON,
            ToonToken.NEWLINE,
            ToonToken.INDENT,      // Indent to level 1
            ToonToken.IDENTIFIER,  // "nested"
            ToonToken.COLON,
            ToonToken.IDENTIFIER,  // "value"
            ToonToken.NEWLINE,
            ToonToken.DEDENT,      // Dedent back to level 0
            ToonToken.IDENTIFIER,  // "back"
            ToonToken.COLON,
            ToonToken.IDENTIFIER,  // "here"
            ToonToken.EOF
        );
    }

    @Test
    public void testMultiLevelIndent() throws IOException {
        String input = "a:\n  b:\n    c: value";
        List<ToonToken> tokens = tokenize(input);

        // Should have INDENT tokens for each level
        long indentCount = tokens.stream()
            .filter(t -> t == ToonToken.INDENT)
            .count();
        assertEquals(2, indentCount, "Should have 2 INDENT tokens");
    }

    @Test
    public void testMultiLevelDedent() throws IOException {
        String input = "a:\n  b:\n    c: value\nback: here";
        List<ToonToken> tokens = tokenize(input);

        // Should have DEDENT tokens to unwind from level 2 to level 0
        long dedentCount = tokens.stream()
            .filter(t -> t == ToonToken.DEDENT)
            .count();
        assertEquals(2, dedentCount, "Should have 2 DEDENT tokens");
    }

    @Test
    public void testSameIndent() throws IOException {
        String input = "a: 1\nb: 2\nc: 3";
        List<ToonToken> tokens = tokenize(input);

        // After each newline at same level, should have SAME_INDENT
        long sameIndentCount = tokens.stream()
            .filter(t -> t == ToonToken.SAME_INDENT)
            .count();
        assertEquals(2, sameIndentCount);
    }

    @Test
    public void testInvalidIndentation() throws IOException {
        // 3 spaces is not a multiple of 2 (default indentSize)
        String input = "root:\n   invalid";
        ToonLexer lexer = createLexer(input, 2, true);

        // Should eventually get ERROR token
        ToonToken token;
        boolean foundError = false;
        while ((token = lexer.nextToken()) != ToonToken.EOF) {
            if (token == ToonToken.ERROR) {
                foundError = true;
                assertTrue(lexer.getErrorMessage().contains("Invalid indentation"));
                break;
            }
        }
        assertTrue(foundError, "Should find indentation error");
    }

    @Test
    public void testBlankLinesIgnored() throws IOException {
        String input = "a: 1\n\n\nb: 2";
        assertTokenSequence(input,
            ToonToken.IDENTIFIER,  // "a"
            ToonToken.COLON,
            ToonToken.IDENTIFIER,  // "1"
            ToonToken.NEWLINE,
            ToonToken.NEWLINE,     // Blank line
            ToonToken.NEWLINE,     // Blank line
            ToonToken.SAME_INDENT,
            ToonToken.IDENTIFIER,  // "b"
            ToonToken.COLON,
            ToonToken.IDENTIFIER,  // "2"
            ToonToken.EOF
        );
    }

    // ========================================================================
    // Complex Scenarios
    // ========================================================================

    @Test
    public void testSimpleKeyValue() throws IOException {
        String input = "name: Alice";
        assertTokenSequence(input,
            ToonToken.IDENTIFIER,  // "name"
            ToonToken.COLON,
            ToonToken.IDENTIFIER,  // "Alice"
            ToonToken.EOF
        );
    }

    @Test
    public void testArrayHeader() throws IOException {
        String input = "items[3]:";
        assertTokenSequence(input,
            ToonToken.IDENTIFIER,  // "items"
            ToonToken.LBRACKET,
            ToonToken.NUMBER,      // 3
            ToonToken.RBRACKET,
            ToonToken.COLON,
            ToonToken.EOF
        );
    }

    @Test
    public void testArrayHeaderWithFields() throws IOException {
        String input = "users[2]{id,name}:";
        assertTokenSequence(input,
            ToonToken.IDENTIFIER,  // "users"
            ToonToken.LBRACKET,
            ToonToken.NUMBER,      // 2
            ToonToken.RBRACKET,
            ToonToken.LBRACE,
            ToonToken.IDENTIFIER,  // "id"
            ToonToken.COMMA,
            ToonToken.IDENTIFIER,  // "name"
            ToonToken.RBRACE,
            ToonToken.COLON,
            ToonToken.EOF
        );
    }

    @Test
    public void testInlineArray() throws IOException {
        String input = "[3]: a,b,c";
        assertTokenSequence(input,
            ToonToken.LBRACKET,
            ToonToken.NUMBER,      // 3
            ToonToken.RBRACKET,
            ToonToken.COLON,
            ToonToken.IDENTIFIER,  // "a"
            ToonToken.COMMA,
            ToonToken.IDENTIFIER,  // "b"
            ToonToken.COMMA,
            ToonToken.IDENTIFIER,  // "c"
            ToonToken.EOF
        );
    }

    @Test
    public void testListArray() throws IOException {
        String input = "items[2]:\n- first\n- second";
        List<ToonToken> tokens = tokenize(input);

        // Should contain HYPHEN tokens
        long hyphenCount = tokens.stream()
            .filter(t -> t == ToonToken.HYPHEN)
            .count();
        assertEquals(2, hyphenCount, "Should have 2 HYPHEN tokens");
    }

    @Test
    public void testPositionTracking() throws IOException {
        String input = "a: 1\nb: 2";
        ToonLexer lexer = createLexer(input);

        lexer.nextToken(); // "a"
        assertEquals(1, lexer.getTokenLine());
        assertEquals(0, lexer.getTokenColumn());

        lexer.nextToken(); // ":"
        lexer.nextToken(); // "1"
        lexer.nextToken(); // NEWLINE
        lexer.nextToken(); // SAME_INDENT

        lexer.nextToken(); // "b"
        assertEquals(2, lexer.getTokenLine());
        assertEquals(0, lexer.getTokenColumn());
    }

    @Test
    public void testHyphenInIdentifier() throws IOException {
        // Hyphen not followed by space should be part of identifier
        String input = "user-id";
        assertTokenSequence(input,
            ToonToken.IDENTIFIER,  // "user" (up to hyphen)
            ToonToken.IDENTIFIER,  // "id" (after hyphen)
            ToonToken.EOF
        );
    }

    @Test
    public void testNegativeNumber() throws IOException {
        String input = "temp: -42";
        ToonLexer lexer = createLexer(input);

        lexer.nextToken(); // "temp"
        lexer.nextToken(); // ":"

        assertEquals(ToonToken.NUMBER, lexer.nextToken());
        assertEquals(-42L, lexer.getTokenValue());
    }

    @Test
    public void testDottedKey() throws IOException {
        String input = "data.metadata.value";
        ToonLexer lexer = createLexer(input);

        assertEquals(ToonToken.IDENTIFIER, lexer.nextToken());
        assertEquals("data.metadata.value", lexer.getTokenText());
    }
}
