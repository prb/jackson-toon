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
            // Set the buffered object reference so writeValue() knows to add to this Map
            _context.setBufferedObject(obj);
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
     * Writes the start of an array without size hint.
     * Array elements are buffered until writeEndArray() to determine format.
     */
    public void writeStartArray() throws IOException {
        writeStartArray(-1);
    }

    /**
     * Writes the start of an array with size hint.
     * If size is provided (>= 0), array will be streamed directly.
     * If size is unknown (-1), array elements will be buffered.
     */
    public void writeStartArray(int sizeHint) throws IOException {
        // Capture pending field name from parent context before creating child
        String pendingFieldName = _context.getPendingFieldName();

        // Create array context with size hint
        _context = _context.createChildInlineArray(',', sizeHint);

        // Store the field name in the array context for later use
        if (pendingFieldName != null) {
            _context.setPendingFieldName(pendingFieldName);
            // Clear it from parent (will be handled when array is written)
            if (_context.getParent() != null) {
                _context.getParent().setPendingFieldName(null);
                _context.getParent().incrementFieldCount();
            }
        }
    }

    /**
     * Writes the end of an array.
     * For buffered arrays: flushes buffered elements in appropriate format.
     * For streaming arrays: just finalizes the array.
     */
    public void writeEndArray() throws IOException {
        if (!_context.isInArray()) {
            throw new IOException("Not in an array context");
        }

        // Check if streaming mode
        if (_context.isStreamingMode()) {
            // Streaming mode - just finish the array line if inline
            if (_context.getType() == GeneratorContext.Type.ARRAY_INLINE) {
                _writer.write("\n");
            }
            // Pop context
            _context = _context.getParent();
            return;
        }

        // Buffering mode - write all elements
        List<Object> elements = _context.getBufferedElements();
        int indentLevel = _context.getIndentLevel();
        char delimiter = _context.getDelimiter();
        String fieldName = _context.getPendingFieldName();

        // Pop context before writing
        GeneratorContext arrayContext = _context;
        _context = _context.getParent();

        // Determine array format
        boolean allPrimitives = elements.stream()
            .allMatch(e -> !(e instanceof Map));

        if (allPrimitives && elements.size() <= 10) {
            // Write as inline array
            writeInlineArray(fieldName, elements, delimiter, indentLevel);
        } else {
            // Write as list array
            writeListArray(fieldName, elements, indentLevel);
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

            // Check if this object is being buffered in an array
            Map<String, Object> bufferedObject = _context.getBufferedObject();
            if (bufferedObject != null) {
                // Add to buffered object Map instead of writing
                bufferedObject.put(fieldName, value);
                _context.setPendingFieldName(null);
                _context.incrementFieldCount();
            } else {
                // Write directly to output
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
            }

        } else if (_context.isInArray()) {
            // Check if streaming mode
            if (_context.isStreamingMode()) {
                // Streaming mode - write directly
                if (!_context.isHeaderWritten()) {
                    // First element - determine format and write header
                    writeStreamingArrayHeader(value);
                    _context.setHeaderWritten(true);
                }
                // Write element (header already written, or just written above)
                writeStreamingArrayElement(value);
                _context.incrementElementCount();
            } else {
                // Buffering mode - buffer value for later
                _context.addBufferedElement(value);
            }

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
     * Writes an inline array: fieldname[N]: v1,v2,v3 or [N]: v1,v2,v3
     */
    private void writeInlineArray(String fieldName, List<Object> elements, char delimiter, int indentLevel) throws IOException {
        writeIndent();

        // Write field name if present
        if (fieldName != null) {
            _writer.write(fieldName);
        }

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
     * Writes a list array: fieldname[N]: - item1 | - item2 or [N]: - item1 | - item2
     */
    private void writeListArray(String fieldName, List<Object> elements, int indentLevel) throws IOException {
        writeIndent();

        // Write field name if present
        if (fieldName != null) {
            _writer.write(fieldName);
        }

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
     * Writes the array header for streaming arrays.
     * Determines format based on first element type.
     */
    private void writeStreamingArrayHeader(Object firstValue) throws IOException {
        String fieldName = _context.getPendingFieldName();
        int size = _context.getDeclaredSize();
        char delimiter = _context.getDelimiter();

        writeIndent();

        // Write field name if present
        if (fieldName != null) {
            _writer.write(fieldName);
        }

        _writer.write("[" + size + "]");

        // Determine format based on first value type
        boolean isObject = firstValue instanceof Map;

        if (isObject) {
            // List format for objects
            _writer.write(":\n");
            // Update context type to list
            // (This is a simplification - we keep it as ARRAY_INLINE but treat it as list)
        } else {
            // Inline format for primitives
            // Write delimiter if not comma
            if (delimiter != ',') {
                if (delimiter == '|') {
                    _writer.write("{|}");
                } else if (delimiter == '\t') {
                    _writer.write("{\\t}");
                }
            }
            _writer.write(": ");
        }
    }

    /**
     * Writes a single element for streaming arrays.
     */
    private void writeStreamingArrayElement(Object value) throws IOException {
        int elementIndex = _context.getElementCount();
        boolean isObject = value instanceof Map;

        if (isObject) {
            // List format - write with - prefix
            writeIndent();
            _writer.write("  - ");

            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) value;
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
            _writer.write("\n");
        } else {
            // Inline format - write delimiter before element (except first)
            if (elementIndex > 0) {
                _writer.write(_context.getDelimiter());
            }

            if (value == null) {
                _writer.write("null");
            } else if (value instanceof String) {
                writeStringValue((String) value);
            } else {
                _writer.write(value.toString());
            }
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
