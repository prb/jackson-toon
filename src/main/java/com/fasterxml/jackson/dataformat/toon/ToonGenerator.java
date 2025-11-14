package com.fasterxml.jackson.dataformat.toon;

import java.io.*;
import java.util.*;

/**
 * Generator for writing TOON format output.
 * <p>
 * This is a streaming generator that converts Jackson write events
 * into TOON format. It handles indentation, quoting, and format selection.
 */
public class ToonGenerator implements Closeable {

    private final Writer _writer;
    private GeneratorContext _context;
    private boolean _strictMode;
    private boolean _prettyPrint;

    /**
     * Creates a new TOON generator.
     *
     * @param writer the output writer
     */
    public ToonGenerator(Writer writer) {
        this._writer = writer;
        this._context = new GeneratorContext();
        this._strictMode = false;
        this._prettyPrint = true;
    }

    /**
     * Enable or disable strict mode.
     */
    public void setStrictMode(boolean strict) {
        this._strictMode = strict;
    }

    /**
     * Enable or disable pretty printing (currently always enabled).
     */
    public void setPrettyPrint(boolean prettyPrint) {
        this._prettyPrint = prettyPrint;
    }

    // ========================================================================
    // Object Methods
    // ========================================================================

    /**
     * Writes the start of an object.
     */
    public void writeStartObject() throws IOException {
        // At root level or as array element, no explicit start marker
        // Objects are implicit in TOON
        if (_context.getType() == GeneratorContext.Type.ARRAY_INLINE ||
            _context.getType() == GeneratorContext.Type.ARRAY_LIST) {
            // Buffering array - will write later
            Map<String, Object> obj = new LinkedHashMap<>();
            _context.addBufferedElement(obj);
            _context = _context.createChildObject(_context.getIndentLevel() + 1);
        } else if (_context.getType() == GeneratorContext.Type.OBJECT) {
            // Nested object - write field name if pending
            String fieldName = _context.getPendingFieldName();
            if (fieldName != null) {
                writeIndent();
                _writer.write(fieldName);
                _writer.write(":\n");
                _context.setPendingFieldName(null);
                _context.incrementFieldCount();
            }
            _context = _context.createChildObject(_context.getIndentLevel() + 1);
        } else {
            // Root object
            _context = _context.createChildObject(0);
        }
    }

    /**
     * Writes the end of an object.
     */
    public void writeEndObject() throws IOException {
        if (!_context.isInObject()) {
            throw new IOException("Not in an object context");
        }

        // Pop context
        _context = _context.getParent();
    }

    /**
     * Writes a field name.
     */
    public void writeFieldName(String name) throws IOException {
        if (!_context.isInObject()) {
            throw new IOException("Field name can only be written in object context");
        }

        _context.setPendingFieldName(name);
    }

    // ========================================================================
    // Value Methods
    // ========================================================================

    /**
     * Writes a string value.
     */
    public void writeString(String value) throws IOException {
        writeValue(value);
    }

    /**
     * Writes an integer value.
     */
    public void writeNumber(int value) throws IOException {
        writeValue(value);
    }

    /**
     * Writes a long value.
     */
    public void writeNumber(long value) throws IOException {
        writeValue(value);
    }

    /**
     * Writes a double value.
     */
    public void writeNumber(double value) throws IOException {
        writeValue(value);
    }

    /**
     * Writes a boolean value.
     */
    public void writeBoolean(boolean value) throws IOException {
        writeValue(value);
    }

    /**
     * Writes a null value.
     */
    public void writeNull() throws IOException {
        writeValue(null);
    }

    // ========================================================================
    // Array Methods
    // ========================================================================

    /**
     * Writes the start of an array.
     * Array elements are buffered until writeEndArray() to determine format.
     */
    public void writeStartArray() throws IOException {
        // Start buffering array elements
        // We'll decide format (inline vs list) when array ends
        _context = _context.createChildInlineArray(',');
    }

    /**
     * Writes the end of an array.
     * Flushes buffered elements in appropriate format.
     */
    public void writeEndArray() throws IOException {
        if (!_context.isInArray()) {
            throw new IOException("Not in an array context");
        }

        List<Object> elements = _context.getBufferedElements();
        int indentLevel = _context.getIndentLevel();
        char delimiter = _context.getDelimiter();

        // Pop context before writing
        GeneratorContext arrayContext = _context;
        _context = _context.getParent();

        // Determine array format
        boolean allPrimitives = elements.stream()
            .allMatch(e -> !(e instanceof Map));

        if (allPrimitives && elements.size() <= 10) {
            // Write as inline array
            writeInlineArray(elements, delimiter, indentLevel);
        } else {
            // Write as list array
            writeListArray(elements, indentLevel);
        }
    }

    // ========================================================================
    // Internal Write Methods
    // ========================================================================

    /**
     * Writes a value (handles field context).
     */
    private void writeValue(Object value) throws IOException {
        if (_context.isInObject()) {
            // Write as field: value
            String fieldName = _context.getPendingFieldName();
            if (fieldName == null) {
                throw new IOException("No field name set for value");
            }

            writeIndent();
            _writer.write(fieldName);
            _writer.write(": ");

            // Check if value is start of nested structure
            if (value == null) {
                _writer.write("null");
            } else if (value instanceof String) {
                writeStringValue((String) value);
            } else if (value instanceof Number) {
                _writer.write(value.toString());
            } else if (value instanceof Boolean) {
                _writer.write(value.toString());
            } else {
                _writer.write(value.toString());
            }

            _writer.write("\n");
            _context.setPendingFieldName(null);
            _context.incrementFieldCount();

        } else if (_context.isInArray()) {
            // Buffer value for later
            _context.addBufferedElement(value);

        } else {
            // Root level single value (not common in TOON)
            if (value == null) {
                _writer.write("null\n");
            } else if (value instanceof String) {
                writeStringValue((String) value);
                _writer.write("\n");
            } else {
                _writer.write(value.toString());
                _writer.write("\n");
            }
        }
    }

    /**
     * Writes an inline array: [N]: v1,v2,v3
     */
    private void writeInlineArray(List<Object> elements, char delimiter, int indentLevel) throws IOException {
        writeIndent();
        _writer.write("[" + elements.size() + "]");

        // Write delimiter if not comma
        if (delimiter != ',') {
            if (delimiter == '|') {
                _writer.write("{|}");
            } else if (delimiter == '\t') {
                _writer.write("{\\t}");
            }
        }

        _writer.write(": ");

        // Write elements
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                _writer.write(delimiter);
            }
            Object elem = elements.get(i);
            if (elem == null) {
                _writer.write("null");
            } else if (elem instanceof String) {
                writeStringValue((String) elem);
            } else {
                _writer.write(elem.toString());
            }
        }

        _writer.write("\n");
    }

    /**
     * Writes a list array: [N]: - item1 | - item2
     */
    private void writeListArray(List<Object> elements, int indentLevel) throws IOException {
        writeIndent();
        _writer.write("[" + elements.size() + "]:\n");

        // Write each element with - prefix
        for (Object elem : elements) {
            writeIndent();
            _writer.write("  - ");

            if (elem == null) {
                _writer.write("null");
            } else if (elem instanceof String) {
                writeStringValue((String) elem);
            } else if (elem instanceof Map) {
                // Inline object
                @SuppressWarnings("unchecked")
                Map<String, Object> obj = (Map<String, Object>) elem;
                boolean first = true;
                for (Map.Entry<String, Object> entry : obj.entrySet()) {
                    if (!first) {
                        _writer.write("\n");
                        writeIndent();
                        _writer.write("    ");
                    }
                    _writer.write(entry.getKey());
                    _writer.write(": ");
                    Object val = entry.getValue();
                    if (val == null) {
                        _writer.write("null");
                    } else if (val instanceof String) {
                        writeStringValue((String) val);
                    } else {
                        _writer.write(val.toString());
                    }
                    first = false;
                }
            } else {
                _writer.write(elem.toString());
            }

            _writer.write("\n");
        }
    }

    /**
     * Writes a string value with quoting if needed.
     */
    private void writeStringValue(String s) throws IOException {
        if (needsQuoting(s)) {
            _writer.write("\"");
            _writer.write(escapeString(s));
            _writer.write("\"");
        } else {
            _writer.write(s);
        }
    }

    /**
     * Determines if a string needs quoting.
     */
    private boolean needsQuoting(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }

        // Check for leading/trailing whitespace
        if (Character.isWhitespace(s.charAt(0)) ||
            Character.isWhitespace(s.charAt(s.length() - 1))) {
            return true;
        }

        // Check for special characters
        for (char ch : s.toCharArray()) {
            if (ch == ':' || ch == ',' || ch == '|' || ch == '[' || ch == ']' ||
                ch == '{' || ch == '}' || ch == '-' || ch == '\n' || ch == '\r' ||
                ch == '\t' || ch == '"' || ch == '\\') {
                return true;
            }
        }

        // Check if looks like number, boolean, or null
        if (looksLikeNumber(s) || s.equals("true") || s.equals("false") || s.equals("null")) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a string looks like a number.
     */
    private boolean looksLikeNumber(String s) {
        if (s.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Escapes special characters in a string.
     */
    private String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Writes indentation based on current context level.
     */
    private void writeIndent() throws IOException {
        if (!_prettyPrint) {
            return;
        }

        int level = _context.getIndentLevel();
        for (int i = 0; i < level; i++) {
            _writer.write("  ");
        }
    }

    /**
     * Flushes the output writer.
     */
    public void flush() throws IOException {
        _writer.flush();
    }

    /**
     * Closes the generator and underlying writer.
     */
    @Override
    public void close() throws IOException {
        flush();
        _writer.close();
    }
}
