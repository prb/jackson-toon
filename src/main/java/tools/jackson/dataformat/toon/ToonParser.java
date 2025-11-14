package tools.jackson.dataformat.toon;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Streaming parser for TOON format.
 * <p>
 * This parser converts a stream of ToonTokens from the lexer into
 * a conceptual stream of JSON-equivalent parsing events.
 * <p>
 * The parser is streaming - it processes tokens one at a time and
 * maintains only the context stack in memory (bounded by nesting depth).
 */
public class ToonParser {

    /**
     * Events emitted by the parser (simplified version of JsonToken)
     */
    public enum Event {
        START_OBJECT,
        END_OBJECT,
        START_ARRAY,
        END_ARRAY,
        FIELD_NAME,
        VALUE_STRING,
        VALUE_NUMBER_INT,
        VALUE_NUMBER_FLOAT,
        VALUE_TRUE,
        VALUE_FALSE,
        VALUE_NULL,
        EOF
    }

    // ========================================================================
    // State
    // ========================================================================

    private final ToonLexer _lexer;
    private ToonToken _currentToken;
    private ToonToken _peekToken;
    private String _currentTokenText;      // Text of current token
    private Object _currentTokenValue;     // Value of current token
    private String _peekTokenText;         // Text of peek token
    private Object _peekTokenValue;        // Value of peek token

    private final Deque<ParsingContext> _contextStack;
    private ParsingContext _context;

    private final boolean _strictMode;

    // Current event data
    private Event _currentEvent;
    private String _textValue;
    private Number _numberValue;

    // Parser state
    private enum State {
        NEED_FIELD,           // Expecting to parse a field (in object)
        NEED_VALUE,           // Expecting to parse a value (after field name)
        NEED_CONTENT          // Expecting content (array elements, etc.)
    }
    private State _state;
    private boolean _rootParsed;  // Track if we've parsed the root value

    // ========================================================================
    // Constructor
    // ========================================================================

    public ToonParser(Reader input) throws IOException {
        this(input, 2, true);
    }

    public ToonParser(Reader input, int indentSize, boolean strictMode) throws IOException {
        this._lexer = new ToonLexer(input, indentSize, strictMode);
        this._strictMode = strictMode;
        this._contextStack = new ArrayDeque<>();
        this._context = new ParsingContext(); // Root context
        this._state = State.NEED_CONTENT;
        this._rootParsed = false;

        // Initialize token stream
        _currentToken = _lexer.nextToken();
        _currentTokenText = _lexer.getTokenText();
        _currentTokenValue = _lexer.getTokenValue();
        _peekToken = _lexer.nextToken();
        _peekTokenText = _lexer.getTokenText();
        _peekTokenValue = _lexer.getTokenValue();
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Returns the next parsing event.
     */
    public Event nextEvent() throws IOException {
        if (_context.getType() == ParsingContext.Type.ROOT) {
            // Check if we've already parsed the root
            if (_rootParsed) {
                return Event.EOF;
            }
            // Determine root form and parse
            _rootParsed = true;
            return parseRoot();
        }

        // Continue parsing based on current context
        switch (_context.getType()) {
            case OBJECT:
            case LIST_ITEM_OBJECT:
                return parseObjectContent();

            case ARRAY_INLINE:
                return parseInlineArrayContent();

            case ARRAY_TABULAR:
                return parseTabularArrayContent();

            case ARRAY_LIST:
                return parseListArrayContent();

            case TABULAR_ROW:
                return parseTabularRowContent();

            case LIST_ITEM:
                return parseListItemContent();

            default:
                throw new IOException("Unexpected context: " + _context.getType());
        }
    }

    /**
     * Returns the current event.
     */
    public Event getCurrentEvent() {
        return _currentEvent;
    }

    /**
     * Returns the text value (for FIELD_NAME and VALUE_STRING).
     */
    public String getTextValue() {
        return _textValue;
    }

    /**
     * Returns the number value (for VALUE_NUMBER_*).
     */
    public Number getNumberValue() {
        return _numberValue;
    }

    // ========================================================================
    // Token Management
    // ========================================================================

    private void advance() throws IOException {
        _currentToken = _peekToken;
        _currentTokenText = _peekTokenText;
        _currentTokenValue = _peekTokenValue;
        _peekToken = _lexer.nextToken();
        _peekTokenText = _lexer.getTokenText();
        _peekTokenValue = _lexer.getTokenValue();
    }

    private ToonToken peek() {
        return _peekToken;
    }

    private void expect(ToonToken expected) throws IOException {
        if (_currentToken != expected) {
            throw new IOException(String.format(
                "Expected %s but got %s at line %d, column %d",
                expected, _currentToken, _lexer.getLine(), _lexer.getColumn()
            ));
        }
        advance();
    }

    private void skipWhitespace() throws IOException {
        while (_currentToken == ToonToken.NEWLINE
            || _currentToken == ToonToken.INDENT
            || _currentToken == ToonToken.DEDENT
            || _currentToken == ToonToken.SAME_INDENT) {
            advance();
        }
    }

    // ========================================================================
    // Root Parsing
    // ========================================================================

    private Event parseRoot() throws IOException {
        skipWhitespace();

        if (_currentToken == ToonToken.EOF) {
            // Empty document = empty object
            _currentEvent = Event.START_OBJECT;
            _context = _context.createChildObject(0);
            _state = State.NEED_FIELD;
            return _currentEvent;
        }

        // Check for root array (starts with [N]:)
        if (_currentToken == ToonToken.LBRACKET) {
            _state = State.NEED_CONTENT;
            return parseArray();
        }

        // Check for single primitive
        if (_currentToken.isValue() && peek() == ToonToken.EOF) {
            return parsePrimitiveValue();
        }

        // Otherwise, parse as root object
        _currentEvent = Event.START_OBJECT;
        _context = _context.createChildObject(0);
        _state = State.NEED_FIELD;
        return _currentEvent;
    }

    // ========================================================================
    // Object Parsing
    // ========================================================================

    private Event parseObjectContent() throws IOException {
        skipWhitespace();

        // Check for end of object (dedent or EOF)
        if (_currentToken == ToonToken.EOF
            || _lexer.getIndentLevel() < _context.getExpectedIndentLevel()) {
            _currentEvent = Event.END_OBJECT;
            _context = _context.getParent();
            _state = State.NEED_CONTENT;
            return _currentEvent;
        }

        // State machine for field parsing
        if (_state == State.NEED_FIELD || _state == State.NEED_CONTENT) {
            // Parse and emit field name
            return parseFieldName();
        } else if (_state == State.NEED_VALUE) {
            // Parse and emit value
            return parseFieldValue();
        }

        throw new IOException("Invalid parser state: " + _state);
    }

    private Event parseFieldName() throws IOException {
        // Parse key
        if (!_currentToken.isValue()) {
            throw new IOException("Expected field name at line " + _lexer.getLine());
        }

        _textValue = _currentTokenText;
        _currentEvent = Event.FIELD_NAME;
        _context.setCurrentKey(_textValue);
        advance();

        // Expect colon
        expect(ToonToken.COLON);

        // Transition to NEED_VALUE state
        _state = State.NEED_VALUE;
        return _currentEvent;
    }

    private Event parseFieldValue() throws IOException {
        // Check what follows the colon
        if (_currentToken == ToonToken.NEWLINE) {
            // Nested structure coming
            advance(); // Consume newline

            if (_currentToken == ToonToken.INDENT) {
                advance(); // Consume indent

                // Check if array or nested object
                if (_currentToken == ToonToken.LBRACKET) {
                    // It's an array
                    _state = State.NEED_CONTENT;
                    return parseArray();
                } else {
                    // It's a nested object
                    _currentEvent = Event.START_OBJECT;
                    _context = _context.createChildObject(_lexer.getIndentLevel());
                    _state = State.NEED_FIELD;
                    return _currentEvent;
                }
            } else {
                // Empty value - treat as empty object
                _currentEvent = Event.START_OBJECT;
                _context = _context.createChildObject(_lexer.getIndentLevel());
                _state = State.NEED_FIELD;
                return _currentEvent;
            }
        } else if (_currentToken == ToonToken.LBRACKET) {
            // Array on same line
            _state = State.NEED_CONTENT;
            return parseArray();
        } else {
            // Simple value
            Event event = parsePrimitiveValue();
            _state = State.NEED_FIELD; // Back to parsing fields
            return event;
        }
    }

    // ========================================================================
    // Array Parsing
    // ========================================================================

    private Event parseArray() throws IOException {
        // Parse array header: [N<delim?>]{fields}:
        expect(ToonToken.LBRACKET);

        // Get length
        if (_currentToken != ToonToken.NUMBER) {
            throw new IOException("Expected array length at line " + _lexer.getLine());
        }
        int length = ((Number) _currentTokenValue).intValue();
        advance();

        // Check for delimiter marker
        char delimiter = ','; // Default
        if (_currentToken == ToonToken.HTAB) {
            delimiter = '\t';
            advance();
        } else if (_currentToken == ToonToken.PIPE) {
            delimiter = '|';
            advance();
        }

        expect(ToonToken.RBRACKET);

        // Check for field list
        String[] fields = null;
        if (_currentToken == ToonToken.LBRACE) {
            fields = parseFieldList(delimiter);
        }

        expect(ToonToken.COLON);
        expect(ToonToken.NEWLINE);

        // Determine array format and parse content
        if (fields != null) {
            // Tabular array
            _currentEvent = Event.START_ARRAY;
            _context = _context.createChildTabularArray(length, fields, delimiter);
            return _currentEvent;
        } else if (_currentToken == ToonToken.INDENT) {
            advance(); // Consume indent

            if (_currentToken == ToonToken.HYPHEN) {
                // List array
                _currentEvent = Event.START_ARRAY;
                _context = _context.createChildListArray(length);
                return _currentEvent;
            } else {
                // Inline array
                _currentEvent = Event.START_ARRAY;
                _context = _context.createChildInlineArray(length, delimiter);
                return _currentEvent;
            }
        } else {
            // Empty array
            _currentEvent = Event.START_ARRAY;
            _context = _context.createChildInlineArray(0, delimiter);
            return _currentEvent;
        }
    }

    private String[] parseFieldList(char delimiter) throws IOException {
        List<String> fields = new ArrayList<>();
        expect(ToonToken.LBRACE);

        fields.add(_currentTokenText);
        advance();

        while (_currentToken != ToonToken.RBRACE) {
            // Expect delimiter
            if (delimiter == ',' && _currentToken != ToonToken.COMMA) {
                throw new IOException("Expected comma in field list");
            } else if (delimiter == '|' && _currentToken != ToonToken.PIPE) {
                throw new IOException("Expected pipe in field list");
            } else if (delimiter == '\t' && _currentToken != ToonToken.HTAB) {
                throw new IOException("Expected tab in field list");
            }
            advance();

            // Get field name
            fields.add(_currentTokenText);
            advance();
        }

        expect(ToonToken.RBRACE);
        return fields.toArray(new String[0]);
    }

    // ========================================================================
    // Inline Array Parsing
    // ========================================================================

    private Event parseInlineArrayContent() throws IOException {
        // Parse values separated by delimiter
        if (_context.getCurrentIndex() >= _context.getDeclaredLength()) {
            // End of array
            _currentEvent = Event.END_ARRAY;

            if (_strictMode && _context.getCurrentIndex() != _context.getDeclaredLength()) {
                throw new IOException(String.format(
                    "Array length mismatch: declared %d, got %d",
                    _context.getDeclaredLength(), _context.getCurrentIndex()
                ));
            }

            expect(ToonToken.NEWLINE);
            expect(ToonToken.DEDENT);
            _context = _context.getParent();
            _state = State.NEED_CONTENT;
            return _currentEvent;
        }

        Event event = parsePrimitiveValue();
        _context.incrementIndex();

        // Check for delimiter or end
        char delim = _context.getDelimiter();
        if (_context.getCurrentIndex() < _context.getDeclaredLength()) {
            // Expect delimiter
            if (delim == ',' && _currentToken == ToonToken.COMMA) {
                advance();
            } else if (delim == '|' && _currentToken == ToonToken.PIPE) {
                advance();
            } else if (delim == '\t' && _currentToken == ToonToken.HTAB) {
                advance();
            }
        }

        return event;
    }

    // ========================================================================
    // Tabular Array Parsing
    // ========================================================================

    private Event parseTabularArrayContent() throws IOException {
        if (_context.getCurrentIndex() >= _context.getDeclaredLength()) {
            // End of array
            _currentEvent = Event.END_ARRAY;

            if (_strictMode && _context.getCurrentIndex() != _context.getDeclaredLength()) {
                throw new IOException(String.format(
                    "Array length mismatch: declared %d, got %d",
                    _context.getDeclaredLength(), _context.getCurrentIndex()
                ));
            }

            expect(ToonToken.DEDENT);
            _context = _context.getParent();
            _state = State.NEED_CONTENT;
            return _currentEvent;
        }

        // Start new row (object)
        expect(ToonToken.INDENT);
        _currentEvent = Event.START_OBJECT;
        _context = _context.createTabularRow();
        _state = State.NEED_FIELD; // Start emitting field names
        return _currentEvent;
    }

    private Event parseTabularRowContent() throws IOException {
        if (_context.getCurrentFieldIndex() >= _context.getFieldNames().length) {
            // End of row
            _currentEvent = Event.END_OBJECT;
            expect(ToonToken.NEWLINE);
            _context = _context.getParent();
            _context.incrementIndex();
            _state = State.NEED_CONTENT;
            return _currentEvent;
        }

        // Alternate between field names and values
        if (_state == State.NEED_FIELD || _state == State.NEED_CONTENT) {
            // Emit field name
            _textValue = _context.getCurrentFieldName();
            _currentEvent = Event.FIELD_NAME;
            _state = State.NEED_VALUE;
            return _currentEvent;
        } else if (_state == State.NEED_VALUE) {
            // Parse and emit value
            Event event = parsePrimitiveValue();
            _context.incrementFieldIndex();

            // Check for delimiter or end of row
            char delim = _context.getDelimiter();
            if (_context.getCurrentFieldIndex() < _context.getFieldNames().length) {
                // Expect delimiter
                if (delim == ',' && _currentToken == ToonToken.COMMA) {
                    advance();
                } else if (delim == '|' && _currentToken == ToonToken.PIPE) {
                    advance();
                } else if (delim == '\t' && _currentToken == ToonToken.HTAB) {
                    advance();
                }
            }

            _state = State.NEED_FIELD; // Next field name
            return event;
        }

        throw new IOException("Invalid state in tabular row: " + _state);
    }

    // ========================================================================
    // List Array Parsing
    // ========================================================================

    private Event parseListArrayContent() throws IOException {
        if (_context.getCurrentIndex() >= _context.getDeclaredLength()) {
            // End of array
            _currentEvent = Event.END_ARRAY;

            if (_strictMode && _context.getCurrentIndex() != _context.getDeclaredLength()) {
                throw new IOException(String.format(
                    "Array length mismatch: declared %d, got %d",
                    _context.getDeclaredLength(), _context.getCurrentIndex()
                ));
            }

            expect(ToonToken.DEDENT);
            _context = _context.getParent();
            _state = State.NEED_CONTENT;
            return _currentEvent;
        }

        // Parse list item
        expect(ToonToken.HYPHEN);

        if (_currentToken == ToonToken.LBRACKET) {
            // Nested array
            _context.incrementIndex();
            _state = State.NEED_CONTENT;
            return parseArray();
        } else if (_currentToken.isValue() && peek() == ToonToken.COLON) {
            // Object item
            _currentEvent = Event.START_OBJECT;
            ParsingContext parent = _context;
            _context = _context.createListItemObject(_lexer.getIndentLevel() + 1);
            parent.incrementIndex();
            _state = State.NEED_FIELD;
            return _currentEvent;
        } else {
            // Primitive item
            _context.incrementIndex();
            Event event = parsePrimitiveValue();
            expect(ToonToken.NEWLINE);
            return event;
        }
    }

    private Event parseListItemContent() throws IOException {
        // This is only reached for object items after first field
        return parseObjectContent();
    }

    // ========================================================================
    // Value Parsing
    // ========================================================================

    private Event parsePrimitiveValue() throws IOException {
        if (_currentToken == ToonToken.STRING) {
            _textValue = (String) _currentTokenValue;
            _currentEvent = Event.VALUE_STRING;
            advance();
            return _currentEvent;
        }

        if (_currentToken == ToonToken.NUMBER) {
            _numberValue = (Number) _currentTokenValue;
            if (_numberValue instanceof Long) {
                _currentEvent = Event.VALUE_NUMBER_INT;
            } else {
                _currentEvent = Event.VALUE_NUMBER_FLOAT;
            }
            advance();
            return _currentEvent;
        }

        if (_currentToken == ToonToken.BOOLEAN) {
            boolean value = (Boolean) _currentTokenValue;
            _currentEvent = value ? Event.VALUE_TRUE : Event.VALUE_FALSE;
            advance();
            return _currentEvent;
        }

        if (_currentToken == ToonToken.NULL) {
            _currentEvent = Event.VALUE_NULL;
            advance();
            return _currentEvent;
        }

        if (_currentToken == ToonToken.IDENTIFIER) {
            // Unquoted string
            _textValue = (String) _currentTokenValue;
            _currentEvent = Event.VALUE_STRING;
            advance();
            return _currentEvent;
        }

        throw new IOException("Expected value at line " + _lexer.getLine());
    }
}
