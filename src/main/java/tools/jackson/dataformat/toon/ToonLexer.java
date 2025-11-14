package tools.jackson.dataformat.toon;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Streaming lexer for TOON format.
 * <p>
 * This lexer performs character-level tokenization of TOON input, including:
 * <ul>
 *   <li>Indentation tracking with INDENT/DEDENT token emission</li>
 *   <li>String parsing with escape sequence handling</li>
 *   <li>Number parsing with validation</li>
 *   <li>Identifier and keyword recognition</li>
 *   <li>Position tracking for error reporting</li>
 * </ul>
 * <p>
 * The lexer is streaming - it reads characters one at a time from the input
 * and does not buffer the entire document.
 */
public class ToonLexer {

    // ========================================================================
    // Input and Character State
    // ========================================================================

    private final Reader _input;
    private int _currentChar;      // Current character being examined (-1 for EOF)
    private int _peekChar;         // Next character (1-char lookahead, -1 for EOF)

    // ========================================================================
    // Position Tracking
    // ========================================================================

    private int _line;             // Current line number (1-based)
    private int _column;           // Current column (0-based)
    private int _tokenStartLine;   // Line where current token started
    private int _tokenStartColumn; // Column where current token started

    // ========================================================================
    // Indentation Tracking
    // ========================================================================

    private int _currentIndent;           // Current indentation level (number of spaces)
    private final Deque<Integer> _indentStack;  // Stack of indentation levels
    private final Deque<ToonToken> _pendingTokens; // Buffered tokens (for DEDENT emission)

    // ========================================================================
    // Token State
    // ========================================================================

    private final StringBuilder _tokenText;  // Accumulated text for current token
    private Object _tokenValue;              // Parsed value (for numbers, booleans)
    private String _errorMessage;            // Error message if token is ERROR

    // ========================================================================
    // Configuration
    // ========================================================================

    private final int _indentSize;    // Expected indentation size (default 2)
    private final boolean _strictMode; // Enforce strict validation rules

    // ========================================================================
    // Constructor
    // ========================================================================

    /**
     * Creates a new TOON lexer with default settings.
     *
     * @param input the input reader
     * @throws IOException if reading fails
     */
    public ToonLexer(Reader input) throws IOException {
        this(input, 2, true);
    }

    /**
     * Creates a new TOON lexer with specified settings.
     *
     * @param input the input reader
     * @param indentSize expected indentation size (typically 2 or 4)
     * @param strictMode whether to enforce strict validation
     * @throws IOException if reading fails
     */
    public ToonLexer(Reader input, int indentSize, boolean strictMode) throws IOException {
        this._input = input;
        this._indentSize = indentSize;
        this._strictMode = strictMode;

        this._line = 1;
        this._column = 0;
        this._tokenStartLine = 1;
        this._tokenStartColumn = 0;

        this._currentIndent = 0;
        this._indentStack = new ArrayDeque<>();
        this._indentStack.push(0); // Base indentation level

        this._pendingTokens = new ArrayDeque<>();
        this._tokenText = new StringBuilder();

        // Initialize character lookahead
        this._currentChar = _input.read();
        this._peekChar = _input.read();
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Returns the next token from the input stream.
     *
     * @return the next token
     * @throws IOException if reading fails
     */
    public ToonToken nextToken() throws IOException {
        // First, return any pending tokens (from DEDENT emission)
        if (!_pendingTokens.isEmpty()) {
            return _pendingTokens.poll();
        }

        // Mark token start position
        _tokenStartLine = _line;
        _tokenStartColumn = _column;

        // Reset token state
        _tokenText.setLength(0);
        _tokenValue = null;
        _errorMessage = null;

        // Skip horizontal whitespace (spaces and tabs that aren't significant)
        // Note: We only skip within a line, not across newlines
        while (_currentChar == ' ' || _currentChar == '\t') {
            // If we're at start of line, this is indentation - don't skip!
            if (_column == 0) {
                break;
            }
            advance();
        }

        int ch = _currentChar;

        // End of file
        if (ch == -1) {
            // Emit any pending DEDENTs to close indentation levels
            if (_currentIndent > 0) {
                while (_indentStack.size() > 1) {
                    _indentStack.pop();
                    _pendingTokens.add(ToonToken.DEDENT);
                }
                _currentIndent = 0;
                if (!_pendingTokens.isEmpty()) {
                    return _pendingTokens.poll();
                }
            }
            return ToonToken.EOF;
        }

        // Newline - triggers indentation handling
        if (ch == '\n') {
            return handleNewline();
        }

        // Structural characters
        if (ch == ':') {
            advance();
            return ToonToken.COLON;
        }

        if (ch == ',') {
            advance();
            return ToonToken.COMMA;
        }

        if (ch == '|') {
            advance();
            return ToonToken.PIPE;
        }

        if (ch == '[') {
            advance();
            return ToonToken.LBRACKET;
        }

        if (ch == ']') {
            advance();
            return ToonToken.RBRACKET;
        }

        if (ch == '{') {
            advance();
            return ToonToken.LBRACE;
        }

        if (ch == '}') {
            advance();
            return ToonToken.RBRACE;
        }

        // Hyphen - could be list item prefix or part of identifier/number
        if (ch == '-') {
            advance();
            // Check if followed by space (list item prefix)
            if (_currentChar == ' ') {
                return ToonToken.HYPHEN;
            } else {
                // Could be negative number or unquoted string starting with hyphen
                // Put it back in tokenText and scan as appropriate
                _tokenText.append('-');
                if (isDigit(_currentChar)) {
                    return scanNumber();
                } else {
                    return scanUnquotedString();
                }
            }
        }

        // Quoted string
        if (ch == '"') {
            return scanQuotedString();
        }

        // Numbers
        if (isDigit(ch)) {
            return scanNumber();
        }

        // Identifiers and keywords
        if (isIdentifierStart(ch)) {
            return scanIdentifier();
        }

        // If none of the above, try to scan as unquoted string
        // (may be valid in some contexts)
        return scanUnquotedString();
    }

    /**
     * Returns the text of the current token.
     */
    public String getTokenText() {
        return _tokenText.toString();
    }

    /**
     * Returns the parsed value of the current token (for numbers, booleans).
     */
    public Object getTokenValue() {
        return _tokenValue;
    }

    /**
     * Returns the current indentation level (number of spaces).
     */
    public int getIndentLevel() {
        return _currentIndent;
    }

    /**
     * Returns the line number where the current token started (1-based).
     */
    public int getTokenLine() {
        return _tokenStartLine;
    }

    /**
     * Returns the column where the current token started (0-based).
     */
    public int getTokenColumn() {
        return _tokenStartColumn;
    }

    /**
     * Returns the current line number (1-based).
     */
    public int getLine() {
        return _line;
    }

    /**
     * Returns the current column (0-based).
     */
    public int getColumn() {
        return _column;
    }

    /**
     * Returns the error message if the last token was ERROR.
     */
    public String getErrorMessage() {
        return _errorMessage;
    }

    // ========================================================================
    // Character-Level Operations
    // ========================================================================

    /**
     * Advances to the next character.
     */
    private void advance() throws IOException {
        if (_currentChar == '\n') {
            _line++;
            _column = 0;
        } else if (_currentChar != -1) {
            _column++;
        }

        _currentChar = _peekChar;
        _peekChar = _input.read();
    }

    /**
     * Returns true if the given character is a digit.
     */
    private boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Returns true if the given character can start an identifier.
     */
    private boolean isIdentifierStart(int ch) {
        return (ch >= 'A' && ch <= 'Z')
            || (ch >= 'a' && ch <= 'z')
            || ch == '_';
    }

    /**
     * Returns true if the given character can continue an identifier.
     */
    private boolean isIdentifierContinue(int ch) {
        return isIdentifierStart(ch) || isDigit(ch) || ch == '.';
    }

    // ========================================================================
    // Indentation Handling
    // ========================================================================

    /**
     * Handles newline and indentation tracking.
     */
    private ToonToken handleNewline() throws IOException {
        advance(); // Consume the newline

        // Measure indentation on the next line
        int spaces = 0;
        while (_currentChar == ' ') {
            spaces++;
            advance();
        }

        // Check for blank line (another newline or EOF immediately)
        if (_currentChar == '\n' || _currentChar == -1) {
            // Blank line - emit NEWLINE but don't change indentation
            return ToonToken.NEWLINE;
        }

        // Validate indentation in strict mode
        if (_strictMode && spaces % _indentSize != 0) {
            _errorMessage = String.format(
                "Invalid indentation at line %d: %d spaces is not a multiple of %d",
                _line, spaces, _indentSize
            );
            return ToonToken.ERROR;
        }

        // Check for tabs in indentation (forbidden)
        if (_strictMode && _currentChar == '\t' && _column < spaces) {
            _errorMessage = String.format(
                "Tab character in indentation at line %d (tabs are only allowed as delimiters)",
                _line
            );
            return ToonToken.ERROR;
        }

        int newIndent = spaces;

        // Emit NEWLINE first
        _pendingTokens.add(ToonToken.NEWLINE);

        // Determine indentation change
        if (newIndent > _currentIndent) {
            // Indent
            _indentStack.push(newIndent);
            _currentIndent = newIndent;
            _pendingTokens.add(ToonToken.INDENT);
        } else if (newIndent < _currentIndent) {
            // Dedent (possibly multiple levels)
            while (_indentStack.size() > 1 && newIndent < _indentStack.peek()) {
                _indentStack.pop();
                _pendingTokens.add(ToonToken.DEDENT);
            }

            // Validate that we dedented to a valid level
            if (_indentStack.peek() != newIndent) {
                if (_strictMode) {
                    _errorMessage = String.format(
                        "Invalid dedent at line %d: indentation %d does not match any previous level",
                        _line, newIndent
                    );
                    return ToonToken.ERROR;
                } else {
                    // Lenient: adjust to nearest valid level
                    _indentStack.push(newIndent);
                }
            }

            _currentIndent = newIndent;
        } else {
            // Same indent
            _pendingTokens.add(ToonToken.SAME_INDENT);
        }

        // Return the first pending token
        return _pendingTokens.poll();
    }

    // ========================================================================
    // Token Scanning Methods
    // ========================================================================

    /**
     * Scans a quoted string with escape sequences.
     */
    private ToonToken scanQuotedString() throws IOException {
        advance(); // Consume opening quote

        while (_currentChar != '"') {
            if (_currentChar == -1 || _currentChar == '\n') {
                _errorMessage = String.format(
                    "Unterminated string at line %d, column %d",
                    _tokenStartLine, _tokenStartColumn
                );
                return ToonToken.ERROR;
            }

            if (_currentChar == '\\') {
                advance(); // Consume backslash
                int escapeChar = _currentChar;

                switch (escapeChar) {
                    case '\\':
                        _tokenText.append('\\');
                        break;
                    case '"':
                        _tokenText.append('"');
                        break;
                    case 'n':
                        _tokenText.append('\n');
                        break;
                    case 'r':
                        _tokenText.append('\r');
                        break;
                    case 't':
                        _tokenText.append('\t');
                        break;
                    default:
                        if (_strictMode) {
                            _errorMessage = String.format(
                                "Invalid escape sequence '\\%c' at line %d, column %d",
                                (char) escapeChar, _line, _column
                            );
                            return ToonToken.ERROR;
                        } else {
                            // Lenient: treat as literal character
                            _tokenText.append((char) escapeChar);
                        }
                }
                advance();
            } else {
                _tokenText.append((char) _currentChar);
                advance();
            }
        }

        advance(); // Consume closing quote
        _tokenValue = _tokenText.toString();
        return ToonToken.STRING;
    }

    /**
     * Scans a number (integer or floating point).
     */
    private ToonToken scanNumber() throws IOException {
        // Optional minus (may already be in tokenText from hyphen handling)
        if (_currentChar == '-' && _tokenText.length() == 0) {
            _tokenText.append('-');
            advance();
        }

        // Integer part
        if (_currentChar == '0') {
            _tokenText.append('0');
            advance();

            // Check for forbidden leading zero (like "05")
            if (isDigit(_currentChar)) {
                // This is "0X" where X is a digit - invalid number
                // Scan rest as unquoted string instead
                while (isIdentifierContinue(_currentChar) || isDigit(_currentChar)) {
                    _tokenText.append((char) _currentChar);
                    advance();
                }
                _tokenValue = _tokenText.toString();
                return ToonToken.IDENTIFIER; // Treat as string
            }
        } else {
            if (!isDigit(_currentChar)) {
                _errorMessage = String.format(
                    "Invalid number at line %d, column %d",
                    _tokenStartLine, _tokenStartColumn
                );
                return ToonToken.ERROR;
            }

            while (isDigit(_currentChar)) {
                _tokenText.append((char) _currentChar);
                advance();
            }
        }

        // Optional fraction
        if (_currentChar == '.') {
            _tokenText.append('.');
            advance();

            if (!isDigit(_currentChar)) {
                _errorMessage = String.format(
                    "Invalid number: digit expected after decimal point at line %d, column %d",
                    _line, _column
                );
                return ToonToken.ERROR;
            }

            while (isDigit(_currentChar)) {
                _tokenText.append((char) _currentChar);
                advance();
            }
        }

        // Optional exponent
        if (_currentChar == 'e' || _currentChar == 'E') {
            _tokenText.append((char) _currentChar);
            advance();

            if (_currentChar == '+' || _currentChar == '-') {
                _tokenText.append((char) _currentChar);
                advance();
            }

            if (!isDigit(_currentChar)) {
                _errorMessage = String.format(
                    "Invalid number: digit expected in exponent at line %d, column %d",
                    _line, _column
                );
                return ToonToken.ERROR;
            }

            while (isDigit(_currentChar)) {
                _tokenText.append((char) _currentChar);
                advance();
            }
        }

        // Parse the number
        String numberStr = _tokenText.toString();
        try {
            if (numberStr.contains(".") || numberStr.contains("e") || numberStr.contains("E")) {
                _tokenValue = Double.parseDouble(numberStr);
            } else {
                _tokenValue = Long.parseLong(numberStr);
            }
        } catch (NumberFormatException e) {
            _errorMessage = String.format(
                "Invalid number format '%s' at line %d, column %d",
                numberStr, _tokenStartLine, _tokenStartColumn
            );
            return ToonToken.ERROR;
        }

        return ToonToken.NUMBER;
    }

    /**
     * Scans an identifier or keyword.
     */
    private ToonToken scanIdentifier() throws IOException {
        // First character: letter or underscore
        _tokenText.append((char) _currentChar);
        advance();

        // Remaining: letter, digit, underscore, or dot
        while (isIdentifierContinue(_currentChar)) {
            _tokenText.append((char) _currentChar);
            advance();
        }

        String text = _tokenText.toString();

        // Check for keywords
        switch (text) {
            case "true":
                _tokenValue = Boolean.TRUE;
                return ToonToken.BOOLEAN;
            case "false":
                _tokenValue = Boolean.FALSE;
                return ToonToken.BOOLEAN;
            case "null":
                _tokenValue = null;
                return ToonToken.NULL;
            default:
                _tokenValue = text;
                return ToonToken.IDENTIFIER;
        }
    }

    /**
     * Scans an unquoted string (may contain various characters).
     */
    private ToonToken scanUnquotedString() throws IOException {
        // Scan until we hit a structural character, whitespace, or EOF
        while (_currentChar != -1
                && _currentChar != '\n'
                && _currentChar != ' '
                && _currentChar != '\t'
                && _currentChar != ':'
                && _currentChar != ','
                && _currentChar != '|'
                && _currentChar != '['
                && _currentChar != ']'
                && _currentChar != '{'
                && _currentChar != '}'
                && _currentChar != '"') {
            _tokenText.append((char) _currentChar);
            advance();
        }

        if (_tokenText.length() == 0) {
            _errorMessage = String.format(
                "Unexpected character '%c' at line %d, column %d",
                (char) _currentChar, _line, _column
            );
            return ToonToken.ERROR;
        }

        _tokenValue = _tokenText.toString();
        return ToonToken.IDENTIFIER;
    }
}
