package com.fasterxml.jackson.dataformat.toon;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consolidated JUnit 5 test class for TOON format parsing.
 * Tests cover lexer functionality, basic parsing, array formats, and nested structures.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TOON Core Parsing Tests")
public class CoreParsingTest {

    // ========== Lexer Tests ==========

    @Nested
    @DisplayName("Lexer Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class LexerTests {

        @Test
        @Order(1)
        @DisplayName("Should parse basic tokens correctly")
        void testBasicTokens() throws IOException {
            String input = "name: Alice";
            ToonLexer lexer = new ToonLexer(new StringReader(input));

            assertEquals(ToonToken.IDENTIFIER, lexer.nextToken(), "First token should be IDENTIFIER");
            assertEquals("name", lexer.getTokenText(), "First token text");

            assertEquals(ToonToken.COLON, lexer.nextToken(), "Second token should be COLON");

            assertEquals(ToonToken.IDENTIFIER, lexer.nextToken(), "Third token should be IDENTIFIER");
            assertEquals("Alice", lexer.getTokenText(), "Third token text");

            assertEquals(ToonToken.EOF, lexer.nextToken(), "Should reach EOF");
        }

        @Test
        @Order(2)
        @DisplayName("Should track indentation correctly")
        void testIndentation() throws IOException {
            String input = "root:\n  nested: value";
            ToonLexer lexer = new ToonLexer(new StringReader(input));

            lexer.nextToken(); // "root"
            lexer.nextToken(); // ":"

            assertEquals(ToonToken.NEWLINE, lexer.nextToken(), "Should get NEWLINE");
            assertEquals(ToonToken.INDENT, lexer.nextToken(), "Should get INDENT");

            lexer.nextToken(); // "nested"
            lexer.nextToken(); // ":"
            lexer.nextToken(); // "value"
        }

        @Test
        @Order(3)
        @DisplayName("Should parse numbers correctly")
        void testNumbers() throws IOException {
            ToonLexer lexer = new ToonLexer(new StringReader("42 3.14 -17 1e6"));

            assertEquals(ToonToken.NUMBER, lexer.nextToken(), "First number");
            assertEquals(42L, lexer.getTokenValue(), "First value");

            assertEquals(ToonToken.NUMBER, lexer.nextToken(), "Second number");
            assertEquals(3.14, (Double) lexer.getTokenValue(), 0.001, "Second value");

            assertEquals(ToonToken.NUMBER, lexer.nextToken(), "Third number");
            assertEquals(-17L, lexer.getTokenValue(), "Third value");

            assertEquals(ToonToken.NUMBER, lexer.nextToken(), "Fourth number");
            assertEquals(1e6, (Double) lexer.getTokenValue(), 0.001, "Fourth value");
        }

        @Test
        @Order(4)
        @DisplayName("Should parse strings correctly")
        void testStrings() throws IOException {
            ToonLexer lexer = new ToonLexer(new StringReader("\"hello\" \"world\\ntest\""));

            assertEquals(ToonToken.STRING, lexer.nextToken(), "First string");
            assertEquals("hello", lexer.getTokenValue(), "First value");

            assertEquals(ToonToken.STRING, lexer.nextToken(), "Second string");
            assertEquals("world\ntest", lexer.getTokenValue(), "Second value with escape");
        }

        @Test
        @Order(5)
        @DisplayName("Should parse array headers correctly")
        void testArrayHeader() throws IOException {
            String input = "items[3]{id,name}:";
            ToonLexer lexer = new ToonLexer(new StringReader(input));

            assertEquals(ToonToken.IDENTIFIER, lexer.nextToken(), "items");
            assertEquals(ToonToken.LBRACKET, lexer.nextToken(), "[");
            assertEquals(ToonToken.NUMBER, lexer.nextToken(), "3");
            assertEquals(3L, lexer.getTokenValue(), "value is 3");
            assertEquals(ToonToken.RBRACKET, lexer.nextToken(), "]");
            assertEquals(ToonToken.LBRACE, lexer.nextToken(), "{");
            assertEquals(ToonToken.IDENTIFIER, lexer.nextToken(), "id");
            assertEquals(ToonToken.COMMA, lexer.nextToken(), ",");
            assertEquals(ToonToken.IDENTIFIER, lexer.nextToken(), "name");
            assertEquals(ToonToken.RBRACE, lexer.nextToken(), "}");
            assertEquals(ToonToken.COLON, lexer.nextToken(), ":");
        }
    }

    // ========== Basic Parsing Tests ==========

    @Nested
    @DisplayName("Basic Parsing Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BasicParsingTests {

        @Test
        @Order(1)
        @DisplayName("Should parse simple key-value pairs")
        void testSimpleKeyValue() throws IOException {
            String input = "name: Alice";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<String> events = new ArrayList<>();
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event.toString());
                if (event == ToonParser.Event.FIELD_NAME) {
                    events.add("  \"" + parser.getTextValue() + "\"");
                } else if (event == ToonParser.Event.VALUE_STRING) {
                    events.add("  \"" + parser.getTextValue() + "\"");
                }
            }

            assertNotNull(events, "Events should not be null");
            assertFalse(events.isEmpty(), "Events should not be empty");
        }

        @Test
        @Order(2)
        @DisplayName("Should parse simple objects")
        void testSimpleObject() throws IOException {
            String input = "id: 123\nname: Alice";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<ToonParser.Event> events = new ArrayList<>();
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event);
            }

            assertFalse(events.isEmpty(), "Should have events");
            assertEquals(ToonParser.Event.START_OBJECT, events.get(0), "First event");
            assertEquals(ToonParser.Event.FIELD_NAME, events.get(1), "Second event");
        }

        @Test
        @Order(3)
        @DisplayName("Should parse nested objects")
        void testNestedObject() throws IOException {
            String input = "user:\n  id: 123\n  name: Alice";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<ToonParser.Event> events = new ArrayList<>();
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event);
            }

            assertFalse(events.isEmpty(), "Should have events");
            assertTrue(events.contains(ToonParser.Event.START_OBJECT), "Should contain START_OBJECT");
            assertTrue(events.contains(ToonParser.Event.FIELD_NAME), "Should contain FIELD_NAME");
        }

        @Test
        @Order(4)
        @DisplayName("Should parse inline arrays")
        void testInlineArray() throws IOException {
            String input = "[3]: a,b,c";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<ToonParser.Event> events = new ArrayList<>();
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event);
            }

            assertEquals(ToonParser.Event.START_ARRAY, events.get(0), "First event");
            assertEquals(ToonParser.Event.VALUE_STRING, events.get(1), "Second event");
            assertEquals(ToonParser.Event.VALUE_STRING, events.get(2), "Third event");
            assertEquals(ToonParser.Event.VALUE_STRING, events.get(3), "Fourth event");
        }
    }

    // ========== Array Tests ==========

    @Nested
    @DisplayName("Array Format Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ArrayTests {

        @Test
        @Order(1)
        @DisplayName("Should parse inline array on same line")
        void testInlineArraySameLine() throws IOException {
            String input = "[3]: a,b,c";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<String> events = new ArrayList<>();
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event.toString());
            }

            assertEquals(5, events.size(), "Event count");
            assertEquals("START_ARRAY", events.get(0), "First event");
            assertEquals("VALUE_STRING", events.get(1), "Second event");
            assertEquals("END_ARRAY", events.get(4), "Last event");
        }

        @Test
        @Order(2)
        @DisplayName("Should parse inline array on multiple lines")
        void testInlineArrayMultiLine() throws IOException {
            String input = "[3]:\n  a,b,c";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<String> events = new ArrayList<>();
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event.toString());
            }

            assertEquals(5, events.size(), "Event count");
            assertEquals("START_ARRAY", events.get(0), "First event");
            assertEquals("END_ARRAY", events.get(4), "Last event");
        }

        @Test
        @Order(3)
        @DisplayName("Should parse tabular arrays")
        void testTabularArray() throws IOException {
            String input = "[2]{id,name}:\n  1,Alice\n  2,Bob";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<String> eventLog = new ArrayList<>();
            ToonParser.Event event;
            int count = 0;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count < 30) {
                String log = event.toString();
                if (event == ToonParser.Event.FIELD_NAME || event == ToonParser.Event.VALUE_STRING) {
                    log += " (" + parser.getTextValue() + ")";
                } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                    log += " (" + parser.getNumberValue() + ")";
                }
                eventLog.add(log);
                count++;
            }

            assertFalse(eventLog.isEmpty(), "Should have events");
            assertEquals("START_ARRAY", eventLog.get(0), "First event");
        }

        @Test
        @Order(4)
        @DisplayName("Should parse list arrays")
        void testListArray() throws IOException {
            String input = "[2]:\n  - apple\n  - banana";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<String> eventLog = new ArrayList<>();
            ToonParser.Event event;
            int count = 0;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count < 20) {
                String log = event.toString();
                if (event == ToonParser.Event.VALUE_STRING) {
                    log += " (" + parser.getTextValue() + ")";
                }
                eventLog.add(log);
                count++;
            }

            assertFalse(eventLog.isEmpty(), "Should have events");
            assertEquals("START_ARRAY", eventLog.get(0), "First event");
            assertEquals("END_ARRAY", eventLog.get(eventLog.size() - 1), "Last event");
        }
    }

    // ========== Nested Structure Tests ==========

    @Nested
    @DisplayName("Nested Structure Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class NestedStructureTests {

        @Test
        @Order(1)
        @DisplayName("Should handle nested structures with lexer")
        void testNestedStructureLexer() throws IOException {
            String input = "user:\n  id: 123\n  name: Alice";
            ToonLexer lexer = new ToonLexer(new StringReader(input));

            List<ToonToken> tokens = new ArrayList<>();
            ToonToken token;
            int count = 0;
            while (count < 20 && (token = lexer.nextToken()) != ToonToken.EOF) {
                tokens.add(token);
                count++;
            }

            assertFalse(tokens.isEmpty(), "Should have tokens");
            assertTrue(count < 20, "Should not have too many tokens");
            assertTrue(tokens.contains(ToonToken.IDENTIFIER), "Should contain identifiers");
            assertTrue(tokens.contains(ToonToken.COLON), "Should contain colons");
        }

        @Test
        @Order(2)
        @DisplayName("Should handle nested structures with parser")
        void testNestedStructureParser() throws IOException {
            String input = "user:\n  id: 123\n  name: Alice";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<ToonParser.Event> events = new ArrayList<>();
            ToonParser.Event event;
            int count = 0;
            while (count < 20 && (event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event);
                count++;
            }

            assertFalse(events.isEmpty(), "Should have events");
            assertTrue(count < 20, "Should not have infinite loop - too many events");

            // Verify we have the expected structure
            assertTrue(events.contains(ToonParser.Event.START_OBJECT), "Should have START_OBJECT");
            assertTrue(events.contains(ToonParser.Event.FIELD_NAME), "Should have FIELD_NAME");

            // Count specific event types
            long startObjectCount = events.stream()
                .filter(e -> e == ToonParser.Event.START_OBJECT)
                .count();
            long endObjectCount = events.stream()
                .filter(e -> e == ToonParser.Event.END_OBJECT)
                .count();

            assertEquals(startObjectCount, endObjectCount,
                "START_OBJECT and END_OBJECT should be balanced");
        }

        @Test
        @Order(3)
        @DisplayName("Should parse nested object values correctly")
        void testNestedObjectValues() throws IOException {
            String input = "user:\n  id: 123\n  name: Alice";
            ToonParser parser = new ToonParser(new StringReader(input));

            List<String> fieldNames = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
                if (event == ToonParser.Event.FIELD_NAME) {
                    fieldNames.add(parser.getTextValue());
                } else if (event == ToonParser.Event.VALUE_STRING) {
                    values.add(parser.getTextValue());
                } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                    values.add(parser.getNumberValue());
                }
            }

            assertFalse(fieldNames.isEmpty(), "Should have field names");
            assertFalse(values.isEmpty(), "Should have values");
            assertTrue(fieldNames.contains("user") || fieldNames.contains("id") || fieldNames.contains("name"),
                "Should contain expected field names");
        }
    }
}
