package com.fasterxml.jackson.dataformat.toon;

import org.junit.jupiter.api.*;
import tools.jackson.dataformat.toon.ToonParser;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consolidated JUnit 5 test class for TOON advanced features.
 * Tests cover quoted field names, blank line tolerance, delimiter support,
 * root form detection, and strict mode validation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TOON Advanced Features Tests")
public class AdvancedFeaturesTest {

    // ========== Quoted Field Tests ==========

    @Nested
    @DisplayName("Quoted Field Names")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class QuotedFieldTests {

        @Test
        @Order(1)
        @DisplayName("Should parse quoted field with colon")
        void testQuotedFieldWithColon() throws IOException {
            String input = "\"order:id\": 7";
            ToonParser parser = new ToonParser(new StringReader(input));

            assertEquals(ToonParser.Event.START_OBJECT, parser.nextEvent(),
                "Should start with START_OBJECT");
            assertEquals(ToonParser.Event.FIELD_NAME, parser.nextEvent(),
                "Should have FIELD_NAME event");
            assertEquals("order:id", parser.getTextValue(),
                "Field name should preserve colon");
        }

        @Test
        @Order(2)
        @DisplayName("Should parse quoted field with brackets")
        void testQuotedFieldWithBrackets() throws IOException {
            String input = "\"[index]\": 5";
            ToonParser parser = new ToonParser(new StringReader(input));

            assertEquals(ToonParser.Event.START_OBJECT, parser.nextEvent(),
                "Should start with START_OBJECT");
            assertEquals(ToonParser.Event.FIELD_NAME, parser.nextEvent(),
                "Should have FIELD_NAME event");
            assertEquals("[index]", parser.getTextValue(),
                "Field name should preserve brackets");
        }

        @Test
        @Order(3)
        @DisplayName("Should parse quoted field with spaces")
        void testQuotedFieldWithSpaces() throws IOException {
            String input = "\"full name\": Ada";
            ToonParser parser = new ToonParser(new StringReader(input));

            assertEquals(ToonParser.Event.START_OBJECT, parser.nextEvent(),
                "Should start with START_OBJECT");
            assertEquals(ToonParser.Event.FIELD_NAME, parser.nextEvent(),
                "Should have FIELD_NAME event");
            assertEquals("full name", parser.getTextValue(),
                "Field name should preserve spaces");
        }

        @Test
        @Order(4)
        @DisplayName("Should parse quoted field with comma")
        void testQuotedFieldWithComma() throws IOException {
            String input = "\"a,b\": 1";
            ToonParser parser = new ToonParser(new StringReader(input));

            assertEquals(ToonParser.Event.START_OBJECT, parser.nextEvent(),
                "Should start with START_OBJECT");
            assertEquals(ToonParser.Event.FIELD_NAME, parser.nextEvent(),
                "Should have FIELD_NAME event");
            assertEquals("a,b", parser.getTextValue(),
                "Field name should preserve comma");
        }

        @Test
        @Order(5)
        @DisplayName("Should parse empty string as field name")
        void testQuotedEmptyStringField() throws IOException {
            String input = "\"\": 1";
            ToonParser parser = new ToonParser(new StringReader(input));

            assertEquals(ToonParser.Event.START_OBJECT, parser.nextEvent(),
                "Should start with START_OBJECT");
            assertEquals(ToonParser.Event.FIELD_NAME, parser.nextEvent(),
                "Should have FIELD_NAME event");
            assertEquals("", parser.getTextValue(),
                "Field name should be empty string");
        }

        @Test
        @Order(6)
        @DisplayName("Should parse quoted numeric field name")
        void testQuotedNumericField() throws IOException {
            String input = "\"123\": x";
            ToonParser parser = new ToonParser(new StringReader(input));

            assertEquals(ToonParser.Event.START_OBJECT, parser.nextEvent(),
                "Should start with START_OBJECT");
            assertEquals(ToonParser.Event.FIELD_NAME, parser.nextEvent(),
                "Should have FIELD_NAME event");
            assertEquals("123", parser.getTextValue(),
                "Field name should be numeric string");
        }
    }

    // ========== Blank Line Tests ==========

    @Nested
    @DisplayName("Blank Line Tolerance")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BlankLineTests {

        @Test
        @Order(1)
        @DisplayName("Should tolerate blank lines in objects")
        void testBlankLinesInObject() throws IOException {
            String input = "id: 123\n\nname: Ada\n\nactive: true";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }

        @Test
        @Order(2)
        @DisplayName("Should tolerate blank lines in list arrays")
        void testBlankLinesInListArray() throws IOException {
            String input = "[3]:\n  - item1\n\n  - item2\n\n  - item3";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }

        @Test
        @Order(3)
        @DisplayName("Should tolerate multiple consecutive blank lines")
        void testMultipleBlankLines() throws IOException {
            String input = "user:\n\n\n  id: 123";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }

        @Test
        @Order(4)
        @DisplayName("Should tolerate blank line before end")
        void testBlankLineBeforeEnd() throws IOException {
            String input = "user:\n  id: 123\n\n";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }
    }

    // ========== Delimiter Tests ==========

    @Nested
    @DisplayName("Delimiter Support")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DelimiterTests {

        @Test
        @Order(1)
        @DisplayName("Should parse default comma delimiter")
        void testDefaultCommaDelimiter() throws IOException {
            String input = "[3]: a,b,c";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }

        @Test
        @Order(2)
        @DisplayName("Should parse tabular array with comma delimiter")
        void testTabularArrayCommaDelimiter() throws IOException {
            String input = "[2]{id,name}:\n  1,Alice\n  2,Bob";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }

        @Test
        @Order(3)
        @DisplayName("Should parse inline array with quoted comma values")
        void testQuotedStringsContainingCommas() throws IOException {
            String input = "[3]: \"a,x\",\"b,y\",\"c,z\"";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }

        @Test
        @Order(4)
        @DisplayName("Should parse mixed primitive array")
        void testMixedPrimitives() throws IOException {
            String input = "[4]: 1,true,null,text";
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }
    }

    // ========== Root Form Tests ==========

    @Nested
    @DisplayName("Root Form Detection")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RootFormTests {

        @Test
        @Order(1)
        @DisplayName("Should detect single string at root")
        void testRootString() throws IOException {
            String input = "hello";
            ToonParser parser = new ToonParser(new StringReader(input));

            ToonParser.Event event = parser.nextEvent();
            assertEquals(ToonParser.Event.VALUE_STRING, event,
                "Root should be a string value");
        }

        @Test
        @Order(2)
        @DisplayName("Should detect single number at root")
        void testRootNumber() throws IOException {
            String input = "42";
            ToonParser parser = new ToonParser(new StringReader(input));

            ToonParser.Event event = parser.nextEvent();
            assertEquals(ToonParser.Event.VALUE_NUMBER_INT, event,
                "Root should be a number value");
        }

        @Test
        @Order(3)
        @DisplayName("Should detect single boolean at root")
        void testRootBoolean() throws IOException {
            String input = "true";
            ToonParser parser = new ToonParser(new StringReader(input));

            ToonParser.Event event = parser.nextEvent();
            assertEquals(ToonParser.Event.VALUE_TRUE, event,
                "Root should be a boolean true value");
        }

        @Test
        @Order(4)
        @DisplayName("Should detect single null at root")
        void testRootNull() throws IOException {
            String input = "null";
            ToonParser parser = new ToonParser(new StringReader(input));

            ToonParser.Event event = parser.nextEvent();
            assertEquals(ToonParser.Event.VALUE_NULL, event,
                "Root should be a null value");
        }

        @Test
        @Order(5)
        @DisplayName("Should detect array at root")
        void testRootArray() throws IOException {
            String input = "[3]: a,b,c";
            ToonParser parser = new ToonParser(new StringReader(input));

            ToonParser.Event event = parser.nextEvent();
            assertEquals(ToonParser.Event.START_ARRAY, event,
                "Root should be an array");
        }
    }

    // ========== Strict Mode Tests ==========

    @Nested
    @DisplayName("Strict Mode Validation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class StrictModeTests {

        @Test
        @Order(1)
        @DisplayName("Should error on array length mismatch in strict mode")
        void testArrayLengthMismatch() {
            String input = "[3]: a,b"; // Declares 3, provides 2

            assertThrows(IOException.class, () -> {
                ToonParser parser = new ToonParser(new StringReader(input), 2, true);

                int eventCount = 0;
                ToonParser.Event event;
                while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                    // Parse all events - should error
                }
            }, "Should throw IOException for array length mismatch");
        }

        @Test
        @Order(2)
        @DisplayName("Should error on invalid indentation in strict mode")
        void testInvalidIndentation() {
            String input = "user:\n   id: 123"; // 3 spaces - not multiple of 2

            assertThrows(IOException.class, () -> {
                ToonParser parser = new ToonParser(new StringReader(input), 2, true);

                int eventCount = 0;
                ToonParser.Event event;
                while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                    // Parse all events - should error
                }
            }, "Should throw IOException for invalid indentation");
        }

        @Test
        @Order(3)
        @DisplayName("Should accept valid indentation in strict mode")
        void testValidIndentation() throws IOException {
            String input = "user:\n  id: 123"; // 2 spaces - valid
            ToonParser parser = new ToonParser(new StringReader(input), 2, true);

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }

        @Test
        @Order(4)
        @DisplayName("Should accept array with correct length in strict mode")
        void testArrayCorrectLength() throws IOException {
            String input = "[3]: a,b,c";
            ToonParser parser = new ToonParser(new StringReader(input), 2, true);

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Parse all events without error
            }

            assertTrue(eventCount > 0, "Should have parsed events");
            assertTrue(eventCount < 50, "Should not have infinite loop");
        }
    }
}
