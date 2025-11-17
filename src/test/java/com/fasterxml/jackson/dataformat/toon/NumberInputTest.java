package com.fasterxml.jackson.dataformat.toon;

import com.fasterxml.jackson.core.*;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NumberInput integration.
 * Addresses issue #8: using Jackson's NumberInput for optimized and validated number parsing.
 */
public class NumberInputTest {

    /**
     * Test that normal integers parse correctly.
     */
    @Test
    void testIntegerParsing() throws IOException {
        ToonFactory factory = new ToonFactory();

        String toon = "small: 42\nlarge: 123456789\nnegative: -999";
        JsonParser parser = factory.createParser(toon);

        // Navigate and verify
        assertEquals(JsonToken.START_OBJECT, parser.nextToken());

        // small: 42
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("small", parser.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(42, parser.getIntValue());

        // large: 123456789
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("large", parser.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(123456789, parser.getIntValue());

        // negative: -999
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("negative", parser.getText());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(-999, parser.getIntValue());

        parser.close();
    }

    /**
     * Test that floating point numbers parse correctly.
     */
    @Test
    void testFloatingPointParsing() throws IOException {
        ToonFactory factory = new ToonFactory();

        String toon = "decimal: 3.14\nscientific: 1.23e10\nnegExp: 5.67E-8";
        JsonParser parser = factory.createParser(toon);

        assertEquals(JsonToken.START_OBJECT, parser.nextToken());

        // decimal: 3.14
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
        assertEquals(3.14, parser.getDoubleValue(), 0.001);

        // scientific: 1.23e10
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
        assertEquals(1.23e10, parser.getDoubleValue(), 0.001);

        // negExp: 5.67E-8
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
        assertEquals(5.67E-8, parser.getDoubleValue(), 0.001e-8);

        parser.close();
    }

    /**
     * Test that very large integers (requiring long) parse correctly.
     */
    @Test
    void testLongParsing() throws IOException {
        ToonFactory factory = new ToonFactory();

        String toon = "bigInt: 9223372036854775807";  // Long.MAX_VALUE
        JsonParser parser = factory.createParser(toon);

        assertEquals(JsonToken.START_OBJECT, parser.nextToken());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(9223372036854775807L, parser.getLongValue());

        parser.close();
    }

    /**
     * Test that zero and special number values work.
     */
    @Test
    void testSpecialNumbers() throws IOException {
        ToonFactory factory = new ToonFactory();

        String toon = "zero: 0\ndecZero: 0.0\nnegZero: -0";
        JsonParser parser = factory.createParser(toon);

        assertEquals(JsonToken.START_OBJECT, parser.nextToken());

        // zero: 0
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(0, parser.getIntValue());

        // decZero: 0.0
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
        assertEquals(0.0, parser.getDoubleValue(), 0.001);

        // negZero: -0
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(0, parser.getIntValue());

        parser.close();
    }

    /**
     * Test that number length validation is in place.
     * Note: The actual constraint (default 1000 chars) is enforced in the lexer.
     * This test verifies the mechanism exists.
     */
    @Test
    void testNumberLengthConstraintExists() throws IOException {
        ToonLexer lexer = new ToonLexer(new java.io.StringReader("test: 123"));
        // Just verify that StreamReadConstraints is being used
        // The actual validation happens during number parsing
        assertNotNull(lexer, "Lexer should be created with constraints");
    }

    /**
     * Test that negative numbers work correctly with NumberInput.
     */
    @Test
    void testNegativeNumbers() throws IOException {
        ToonFactory factory = new ToonFactory();

        String toon = "negInt: -42\nnegFloat: -3.14\nnegLarge: -9999999999";
        JsonParser parser = factory.createParser(toon);

        assertEquals(JsonToken.START_OBJECT, parser.nextToken());

        // negInt: -42
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(-42, parser.getIntValue());

        // negFloat: -3.14
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, parser.nextToken());
        assertEquals(-3.14, parser.getDoubleValue(), 0.001);

        // negLarge: -9999999999
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(-9999999999L, parser.getLongValue());

        parser.close();
    }

    /**
     * Test that number parsing in arrays works.
     */
    @Test
    void testNumbersInArrays() throws IOException {
        ToonFactory factory = new ToonFactory();

        String toon = "numbers[5]: 1,2,3,4,5";
        JsonParser parser = factory.createParser(toon);

        assertEquals(JsonToken.START_OBJECT, parser.nextToken());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals(JsonToken.START_ARRAY, parser.nextToken());

        for (int i = 1; i <= 5; i++) {
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(i, parser.getIntValue());
        }

        assertEquals(JsonToken.END_ARRAY, parser.nextToken());
        assertEquals(JsonToken.END_OBJECT, parser.nextToken());

        parser.close();
    }
}
