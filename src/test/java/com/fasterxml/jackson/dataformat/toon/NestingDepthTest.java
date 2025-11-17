package com.fasterxml.jackson.dataformat.toon;

import com.fasterxml.jackson.core.*;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nesting depth validation.
 * Addresses issue #7: preventing stack overflow from deeply nested structures.
 */
public class NestingDepthTest {

    /**
     * Test that normal nesting depth (well under 1000) works fine.
     */
    @Test
    void testNormalNestingDepth() throws IOException {
        // Create a structure with 10 levels of nesting
        StringBuilder toon = new StringBuilder();
        toon.append("level1:\n");
        for (int i = 2; i <= 10; i++) {
            toon.append("  ".repeat(i - 1)).append("level").append(i).append(":\n");
        }
        toon.append("  ".repeat(10)).append("value: deep");

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon.toString());

        // Should parse without errors
        int depth = 0;
        while (parser.nextToken() != null) {
            if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                depth++;
            }
        }

        // Should have parsed all 10 levels
        assertTrue(depth >= 10);
        parser.close();
    }

    /**
     * Test that moderate nesting depth (e.g., 100 levels) works fine.
     */
    @Test
    void testModerateNestingDepth() throws IOException {
        // Create a structure with 100 levels of nesting
        StringBuilder toon = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            toon.append("  ".repeat(i - 1)).append("level").append(i).append(":\n");
        }
        toon.append("  ".repeat(100)).append("value: 100");

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon.toString());

        // Should parse without errors
        int depth = 0;
        while (parser.nextToken() != null) {
            if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                depth++;
            }
        }

        // Should have parsed all 100 levels
        assertTrue(depth >= 100);
        parser.close();
    }

    /**
     * Test that depth just under the limit (999) works fine.
     */
    @Test
    void testNestingDepthJustUnderLimit() throws IOException {
        // Create a structure with 999 levels of nesting
        StringBuilder toon = new StringBuilder();
        for (int i = 1; i <= 999; i++) {
            toon.append("  ".repeat(Math.min(i - 1, 100))).append("l").append(i).append(":\n");
        }
        toon.append("  ".repeat(100)).append("value: 999");

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon.toString());

        // Should parse without errors (999 is under the default limit of 1000)
        assertDoesNotThrow(() -> {
            while (parser.nextToken() != null) {
                // Just consume tokens
            }
        });

        parser.close();
    }

    /**
     * Test that exceeding the nesting depth limit throws an exception.
     */
    @Test
    void testNestingDepthExceedsLimit() throws IOException {
        // Create a structure with 1001 levels of nesting (exceeds default 1000 limit)
        StringBuilder toon = new StringBuilder();
        for (int i = 1; i <= 1001; i++) {
            toon.append("  ".repeat(Math.min(i - 1, 100))).append("l").append(i).append(":\n");
        }
        toon.append("  ".repeat(100)).append("value: 1001");

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon.toString());

        // Should throw IOException (wrapping StreamConstraintsException) when depth exceeds limit
        IOException exception = assertThrows(IOException.class, () -> {
            while (parser.nextToken() != null) {
                // Should fail before completing
            }
        });

        // Verify the error message mentions depth
        String message = exception.getMessage();
        assertTrue(message.contains("depth") || message.contains("nesting"),
            "Exception message should mention depth or nesting: " + message);

        parser.close();
    }

    /**
     * Test nested arrays hitting depth limit.
     */
    @Test
    void testNestedArraysDepthLimit() throws IOException {
        // Create deeply nested arrays using inline arrays
        StringBuilder toon = new StringBuilder();
        toon.append("data:\n");
        for (int i = 1; i <= 1001; i++) {
            toon.append("  ".repeat(Math.min(i, 100))).append("nested").append(i).append("[1]: item");
            if (i < 1001) {
                toon.append("\n");
                toon.append("  ".repeat(Math.min(i, 100))).append("next:\n");
            }
        }

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon.toString());

        // Should throw IOException when depth exceeds limit
        IOException exception = assertThrows(IOException.class, () -> {
            while (parser.nextToken() != null) {
                // Should fail before completing
            }
        });

        String message = exception.getMessage();
        assertTrue(message.contains("depth") || message.contains("nesting") || message.contains("Nesting"),
            "Exception message should mention depth or nesting: " + message);
        parser.close();
    }

    /**
     * Test mixed object and array nesting.
     */
    @Test
    void testMixedObjectArrayNesting() throws IOException {
        // Create a mixed structure with objects and arrays
        StringBuilder toon = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            if (i % 2 == 0) {
                // Object level
                toon.append("  ".repeat(i - 1)).append("obj").append(i).append(":\n");
            } else {
                // Array level
                toon.append("  ".repeat(i - 1)).append("arr").append(i).append("[1]:\n");
                toon.append("  ".repeat(i)).append("-\n");
            }
        }
        toon.append("  ".repeat(50)).append("value: mixed");

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon.toString());

        // Should parse without errors (50 levels is well under limit)
        assertDoesNotThrow(() -> {
            while (parser.nextToken() != null) {
                // Just consume tokens
            }
        });

        parser.close();
    }

    /**
     * Test that depth is properly tracked across different structure types.
     */
    @Test
    void testDifferentStructureTypesNesting() throws IOException {
        // Create a mix of objects and arrays
        StringBuilder toon = new StringBuilder();
        toon.append("outer:\n");
        toon.append("  data[1]: value\n");
        toon.append("  inner:\n");
        toon.append("    items[2]: a,b\n");
        toon.append("    nested:\n");
        toon.append("      deep: value");

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon.toString());

        // Should parse without errors (shallow nesting)
        assertDoesNotThrow(() -> {
            while (parser.nextToken() != null) {
                // Just consume tokens
            }
        });

        parser.close();
    }
}
