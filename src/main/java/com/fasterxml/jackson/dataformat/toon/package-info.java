/**
 * Jackson dataformat module for reading and writing
 * <a href="https://github.com/toon-format/spec">TOON (Token-Oriented Object Notation)</a> encoded data.
 * <p>
 * TOON is a compact, human-readable, line-oriented data format designed for AI/LLM applications,
 * achieving 30-60% token reduction compared to JSON while maintaining readability.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Streaming parser and generator for memory-efficient processing</li>
 *   <li>Python-style indentation for structure (no curly braces)</li>
 *   <li>Three array formats: inline, tabular, and list</li>
 *   <li>Smart string quoting (only when necessary)</li>
 *   <li>Strict mode validation for production use</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple Usage with ObjectMapper</h3>
 * <pre>
 * ToonMapper mapper = new ToonMapper();
 *
 * // Write TOON
 * User user = new User(123, "Alice");
 * String toon = mapper.writeValueAsString(user);
 * // Output: id: 123\nname: Alice
 *
 * // Read TOON
 * User parsed = mapper.readValue(toon, User.class);
 * </pre>
 *
 * <h3>Streaming API</h3>
 * <pre>
 * ToonFactory factory = new ToonFactory();
 *
 * // Parse TOON
 * try (JsonParser parser = factory.createParser(toonString)) {
 *     while (parser.nextToken() != null) {
 *         // Process tokens
 *     }
 * }
 *
 * // Generate TOON
 * try (JsonGenerator gen = factory.createGenerator(outputStream)) {
 *     gen.writeStartObject();
 *     gen.writeNumberField("id", 123);
 *     gen.writeStringField("name", "Alice");
 *     gen.writeEndObject();
 * }
 * </pre>
 *
 * <h3>TOON Format Examples</h3>
 *
 * <h4>Simple Object</h4>
 * <pre>
 * id: 123
 * name: Alice
 * active: true
 * </pre>
 *
 * <h4>Nested Object</h4>
 * <pre>
 * user:
 *   id: 123
 *   address:
 *     city: NYC
 *     zip: 10001
 * </pre>
 *
 * <h4>Inline Array</h4>
 * <pre>
 * tags[3]: reading,gaming,coding
 * </pre>
 *
 * <h4>Tabular Array</h4>
 * <pre>
 * users[2]{id,name,role}:
 *   1,Alice,admin
 *   2,Bob,user
 * </pre>
 *
 * <h4>List Array</h4>
 * <pre>
 * items[3]:
 *   - apple
 *   - banana
 *   - cherry
 * </pre>
 *
 * @see com.fasterxml.jackson.dataformat.toon.ToonFactory
 * @see com.fasterxml.jackson.dataformat.toon.ToonMapper
 */
package com.fasterxml.jackson.dataformat.toon;
