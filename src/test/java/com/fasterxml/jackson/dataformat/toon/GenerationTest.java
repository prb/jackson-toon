package com.fasterxml.jackson.dataformat.toon;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consolidated JUnit 5 test class for TOON format generation.
 * Tests cover object generation, array generation, and round-trip testing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TOON Generation Tests")
public class GenerationTest {

    // ========== Object Generation Tests ==========

    @Nested
    @DisplayName("Object Generation Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ObjectGenerationTests {

        @Test
        @Order(1)
        @DisplayName("Should generate simple objects correctly")
        void testSimpleObject() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartObject();
            gen.writeFieldName("id");
            gen.writeNumber(123);
            gen.writeFieldName("name");
            gen.writeString("Alice");
            gen.writeEndObject();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("id: 123"), "Should contain id field");
            assertTrue(output.contains("name: Alice"), "Should contain name field");
        }

        @Test
        @Order(2)
        @DisplayName("Should generate nested objects correctly")
        void testNestedObject() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartObject();
            gen.writeFieldName("user");
            gen.writeStartObject();
            gen.writeFieldName("id");
            gen.writeNumber(123);
            gen.writeFieldName("name");
            gen.writeString("Alice");
            gen.writeEndObject();
            gen.writeEndObject();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("user:"), "Should contain user field");
            assertTrue(output.contains("  id: 123"), "Should have indented id");
            assertTrue(output.contains("  name: Alice"), "Should have indented name");
        }

        @Test
        @Order(3)
        @DisplayName("Should generate objects with boolean values")
        void testObjectWithBoolean() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartObject();
            gen.writeFieldName("active");
            gen.writeBoolean(true);
            gen.writeFieldName("deleted");
            gen.writeBoolean(false);
            gen.writeEndObject();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("active:") &&
                      (output.contains("true") || output.contains("TRUE")),
                      "Should contain active: true");
            assertTrue(output.contains("deleted:") &&
                      (output.contains("false") || output.contains("FALSE")),
                      "Should contain deleted: false");
        }

        @Test
        @Order(4)
        @DisplayName("Should generate objects with null values")
        void testObjectWithNull() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeNull();
            gen.writeEndObject();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("name:"), "Should contain name field");
            assertTrue(output.contains("null") || output.contains("NULL"),
                      "Should contain null value");
        }
    }

    // ========== Array Generation Tests ==========

    @Nested
    @DisplayName("Array Generation Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ArrayGenerationTests {

        @Test
        @Order(1)
        @DisplayName("Should generate inline arrays correctly")
        void testInlineArray() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();
            gen.writeString("a");
            gen.writeString("b");
            gen.writeString("c");
            gen.writeEndArray();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("[3]:"), "Should have array header with count");
            assertTrue(output.contains("a") && output.contains("b") && output.contains("c"),
                      "Should contain all array values");
        }

        @Test
        @Order(2)
        @DisplayName("Should generate list arrays correctly")
        void testListArray() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();
            for (int i = 0; i < 15; i++) {
                gen.writeString("item" + i);
            }
            gen.writeEndArray();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("[15]:"), "Should have array header with count 15");
            assertTrue(output.contains("- item"), "Should have list items with dash prefix");
            assertTrue(output.contains("item0"), "Should contain first item");
            assertTrue(output.contains("item14"), "Should contain last item");
        }

        @Test
        @Order(3)
        @DisplayName("Should generate arrays with numbers")
        void testArrayWithNumbers() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();
            gen.writeNumber(1);
            gen.writeNumber(2);
            gen.writeNumber(3);
            gen.writeEndArray();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("[3]:"), "Should have array header");
            assertTrue(output.contains("1") && output.contains("2") && output.contains("3"),
                      "Should contain all numeric values");
        }

        @Test
        @Order(4)
        @DisplayName("Should generate arrays with mixed types")
        void testArrayWithMixedTypes() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();
            gen.writeString("text");
            gen.writeNumber(42);
            gen.writeBoolean(true);
            gen.writeNull();
            gen.writeEndArray();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("[4]:"), "Should have array header with count 4");
            assertTrue(output.contains("text"), "Should contain string value");
            assertTrue(output.contains("42"), "Should contain number value");
        }

        @Test
        @Order(5)
        @DisplayName("Should generate empty arrays")
        void testEmptyArray() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();
            gen.writeEndArray();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("[0]:") || output.contains("[]"),
                      "Should have empty array indicator");
        }

        @Test
        @Order(6)
        @DisplayName("Should generate nested arrays")
        void testNestedArrays() throws IOException {
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();
            gen.writeStartArray();
            gen.writeString("inner1");
            gen.writeString("inner2");
            gen.writeEndArray();
            gen.writeStartArray();
            gen.writeString("inner3");
            gen.writeString("inner4");
            gen.writeEndArray();
            gen.writeEndArray();
            gen.flush();

            String output = sw.toString();

            assertNotNull(output, "Output should not be null");
            assertTrue(output.contains("inner1") && output.contains("inner2") &&
                      output.contains("inner3") && output.contains("inner4"),
                      "Should contain all nested array values");
        }
    }

    // ========== Round Trip Tests ==========

    @Nested
    @DisplayName("Round Trip Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RoundTripTests {

        @Test
        @Order(1)
        @DisplayName("Should round-trip simple objects")
        void testSimpleObjectRoundTrip() throws IOException {
            // Generate TOON
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartObject();
            gen.writeFieldName("id");
            gen.writeNumber(42);
            gen.writeFieldName("name");
            gen.writeString("Bob");
            gen.writeFieldName("active");
            gen.writeBoolean(true);
            gen.writeEndObject();
            gen.flush();

            String toon = sw.toString();
            assertNotNull(toon, "Generated TOON should not be null");
            assertFalse(toon.isEmpty(), "Generated TOON should not be empty");

            // Parse it back
            ToonParser parser = new ToonParser(new StringReader(toon));

            List<ToonParser.Event> events = new ArrayList<>();
            List<String> fieldNames = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            ToonParser.Event event;
            int count = 0;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count++ < 20) {
                events.add(event);

                if (event == ToonParser.Event.FIELD_NAME) {
                    fieldNames.add(parser.getTextValue());
                } else if (event == ToonParser.Event.VALUE_STRING) {
                    values.add(parser.getTextValue());
                } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                    values.add(parser.getNumberValue());
                } else if (event == ToonParser.Event.VALUE_TRUE) {
                    values.add(true);
                } else if (event == ToonParser.Event.VALUE_FALSE) {
                    values.add(false);
                }
            }

            // Verify structure
            assertFalse(events.isEmpty(), "Should have parsed events");
            assertTrue(count < 20, "Should not have infinite loop");
            assertTrue(events.contains(ToonParser.Event.START_OBJECT), "Should have START_OBJECT");
            assertTrue(events.contains(ToonParser.Event.END_OBJECT), "Should have END_OBJECT");
            assertTrue(events.contains(ToonParser.Event.FIELD_NAME), "Should have FIELD_NAME");

            // Verify content
            assertTrue(fieldNames.contains("id") || fieldNames.contains("name") || fieldNames.contains("active"),
                      "Should contain field names");
            assertFalse(values.isEmpty(), "Should have values");
        }

        @Test
        @Order(2)
        @DisplayName("Should round-trip simple arrays")
        void testSimpleArrayRoundTrip() throws IOException {
            // Generate TOON
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();
            gen.writeString("apple");
            gen.writeString("banana");
            gen.writeString("cherry");
            gen.writeEndArray();
            gen.flush();

            String toon = sw.toString();
            assertNotNull(toon, "Generated TOON should not be null");

            // Parse it back
            ToonParser parser = new ToonParser(new StringReader(toon));

            List<ToonParser.Event> events = new ArrayList<>();
            List<String> values = new ArrayList<>();

            ToonParser.Event event;
            int count = 0;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count++ < 20) {
                events.add(event);
                if (event == ToonParser.Event.VALUE_STRING) {
                    values.add(parser.getTextValue());
                }
            }

            // Verify structure
            assertEquals(ToonParser.Event.START_ARRAY, events.get(0), "First event should be START_ARRAY");
            assertEquals(ToonParser.Event.END_ARRAY, events.get(events.size() - 1),
                        "Last event should be END_ARRAY");

            // Verify content
            assertEquals(3, values.size(), "Should have 3 string values");
            assertTrue(values.contains("apple"), "Should contain apple");
            assertTrue(values.contains("banana"), "Should contain banana");
            assertTrue(values.contains("cherry"), "Should contain cherry");
        }

        @Test
        @Order(3)
        @DisplayName("Should round-trip nested structures")
        void testNestedStructureRoundTrip() throws IOException {
            // Generate TOON with nested object
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartObject();
            gen.writeFieldName("user");
            gen.writeStartObject();
            gen.writeFieldName("id");
            gen.writeNumber(100);
            gen.writeFieldName("name");
            gen.writeString("Charlie");
            gen.writeEndObject();
            gen.writeFieldName("tags");
            gen.writeStartArray();
            gen.writeString("admin");
            gen.writeString("user");
            gen.writeEndArray();
            gen.writeEndObject();
            gen.flush();

            String toon = sw.toString();
            assertNotNull(toon, "Generated TOON should not be null");

            // Debug: print generated TOON
            System.out.println("Generated TOON for testNestedStructureRoundTrip:");
            System.out.println(toon);
            System.out.println("---");

            // Parse it back
            ToonParser parser = new ToonParser(new StringReader(toon));

            List<ToonParser.Event> events = new ArrayList<>();
            int count = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count++ < 50) {
                events.add(event);
            }

            // Verify structure
            assertFalse(events.isEmpty(), "Should have events");
            assertTrue(count < 50, "Should not have infinite loop");

            long startObjectCount = events.stream()
                .filter(e -> e == ToonParser.Event.START_OBJECT)
                .count();
            long endObjectCount = events.stream()
                .filter(e -> e == ToonParser.Event.END_OBJECT)
                .count();
            assertEquals(startObjectCount, endObjectCount,
                        "START_OBJECT and END_OBJECT should be balanced");

            long startArrayCount = events.stream()
                .filter(e -> e == ToonParser.Event.START_ARRAY)
                .count();
            long endArrayCount = events.stream()
                .filter(e -> e == ToonParser.Event.END_ARRAY)
                .count();
            assertEquals(startArrayCount, endArrayCount,
                        "START_ARRAY and END_ARRAY should be balanced");
        }

        @Test
        @Order(4)
        @DisplayName("Should round-trip complex nested arrays")
        void testComplexArrayRoundTrip() throws IOException {
            // Generate TOON with array of objects
            StringWriter sw = new StringWriter();
            ToonGenerator gen = new ToonGenerator(sw);

            gen.writeStartArray();

            gen.writeStartObject();
            gen.writeFieldName("id");
            gen.writeNumber(1);
            gen.writeFieldName("name");
            gen.writeString("First");
            gen.writeEndObject();

            gen.writeStartObject();
            gen.writeFieldName("id");
            gen.writeNumber(2);
            gen.writeFieldName("name");
            gen.writeString("Second");
            gen.writeEndObject();

            gen.writeEndArray();
            gen.flush();

            String toon = sw.toString();
            assertNotNull(toon, "Generated TOON should not be null");

            // Debug: print generated TOON
            System.out.println("Generated TOON for testComplexArrayRoundTrip:");
            System.out.println(toon);
            System.out.println("---");

            // Parse it back
            ToonParser parser = new ToonParser(new StringReader(toon));

            List<ToonParser.Event> events = new ArrayList<>();
            int count = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count++ < 50) {
                events.add(event);
            }

            // Verify we got events
            assertFalse(events.isEmpty(), "Should have events");
            assertTrue(count < 50, "Should not have infinite loop");

            // Verify structure balance
            long startObjectCount = events.stream()
                .filter(e -> e == ToonParser.Event.START_OBJECT)
                .count();
            long endObjectCount = events.stream()
                .filter(e -> e == ToonParser.Event.END_OBJECT)
                .count();
            assertEquals(2, startObjectCount, "Should have 2 objects");
            assertEquals(startObjectCount, endObjectCount, "Objects should be balanced");
        }

        @Test
        @Order(5)
        @DisplayName("Should round-trip basic key-value pair")
        void testBasicKeyValueRoundTrip() throws IOException {
            // This test is based on DebugParserTest.java
            String input = "name: Alice";

            // Parse the input
            ToonParser parser = new ToonParser(new StringReader(input));

            List<ToonParser.Event> events = new ArrayList<>();
            List<String> values = new ArrayList<>();

            ToonParser.Event event;
            int count = 0;
            int maxEvents = 20;

            while (count < maxEvents && (event = parser.nextEvent()) != ToonParser.Event.EOF) {
                events.add(event);

                if (event == ToonParser.Event.FIELD_NAME || event == ToonParser.Event.VALUE_STRING) {
                    values.add(parser.getTextValue());
                }
                count++;
            }

            // Verify we didn't hit the limit (no infinite loop)
            assertTrue(count < maxEvents, "Should not hit max event limit");

            // Verify we got events
            assertFalse(events.isEmpty(), "Should have events");

            // Verify we got the expected values
            assertTrue(values.contains("name") || values.contains("Alice"),
                      "Should contain name or Alice");
        }
    }
}
