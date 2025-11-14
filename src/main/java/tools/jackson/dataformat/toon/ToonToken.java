package tools.jackson.dataformat.toon;

/**
 * Enumeration of all token types in the TOON format.
 * <p>
 * TOON is a line-oriented, indentation-based format, so tokens include
 * both structural elements (like JSON) and whitespace-related tokens
 * (NEWLINE, INDENT, DEDENT) that are significant for parsing.
 */
public enum ToonToken {

    // ========================================================================
    // Structural Tokens
    // ========================================================================

    /**
     * Colon character ':' - separates keys from values
     */
    COLON,

    /**
     * Comma character ',' - default delimiter for arrays and field lists
     */
    COMMA,

    /**
     * Pipe character '|' - alternative delimiter for arrays and field lists
     */
    PIPE,

    /**
     * Left bracket '[' - starts array header or field index
     */
    LBRACKET,

    /**
     * Right bracket ']' - ends array header or field index
     */
    RBRACKET,

    /**
     * Left brace '{' - starts field list in array header
     */
    LBRACE,

    /**
     * Right brace '}' - ends field list in array header
     */
    RBRACE,

    /**
     * Hyphen character '-' followed by space - list array item prefix
     */
    HYPHEN,

    /**
     * Horizontal tab character (U+0009) - delimiter marker in array headers
     * Note: Tabs are ONLY allowed as delimiter markers, not for indentation
     */
    HTAB,

    // ========================================================================
    // Value Tokens
    // ========================================================================

    /**
     * Identifier - unquoted key or string value matching [A-Za-z_][A-Za-z0-9_.]*
     */
    IDENTIFIER,

    /**
     * Quoted string - "..." with escape sequences
     */
    STRING,

    /**
     * Numeric value - integer or floating point
     */
    NUMBER,

    /**
     * Boolean value - true or false
     */
    BOOLEAN,

    /**
     * Null value
     */
    NULL,

    // ========================================================================
    // Whitespace Tokens (Significant in TOON)
    // ========================================================================

    /**
     * Newline character (LF) - line terminator
     */
    NEWLINE,

    /**
     * Increased indentation level - emitted when indentation increases
     */
    INDENT,

    /**
     * Decreased indentation level - emitted when indentation decreases
     * May be emitted multiple times for multi-level dedents
     */
    DEDENT,

    /**
     * Same indentation level - emitted when indentation stays the same
     */
    SAME_INDENT,

    // ========================================================================
    // Special Tokens
    // ========================================================================

    /**
     * End of file
     */
    EOF,

    /**
     * Lexer error token
     */
    ERROR;

    /**
     * Returns true if this token represents a value (not structural or whitespace)
     */
    public boolean isValue() {
        return this == IDENTIFIER || this == STRING || this == NUMBER
            || this == BOOLEAN || this == NULL;
    }

    /**
     * Returns true if this token represents a delimiter
     */
    public boolean isDelimiter() {
        return this == COMMA || this == PIPE || this == HTAB;
    }

    /**
     * Returns true if this token represents indentation change
     */
    public boolean isIndentation() {
        return this == INDENT || this == DEDENT || this == SAME_INDENT;
    }

    /**
     * Returns true if this token represents a structural character
     */
    public boolean isStructural() {
        return this == COLON || this == LBRACKET || this == RBRACKET
            || this == LBRACE || this == RBRACE || this == HYPHEN;
    }
}
