# TOON Parser Design for Streaming Implementation

This document details the state machine design for implementing a streaming TOON parser in Jackson.

## Architecture Overview

```
Input Stream (bytes/chars)
      ↓
ToonLexer (character-level tokenization)
      ↓ (produces)
ToonToken stream
      ↓ (consumed by)
ToonParser (state machine)
      ↓ (emits)
JsonToken stream
      ↓ (to)
Jackson ObjectMapper
```

## Layer 1: Lexer State Machine

### Lexer Responsibilities

1. Character-level tokenization
2. Indentation tracking (emit INDENT/DEDENT/SAME_INDENT)
3. String parsing with escape handling
4. Number parsing and validation
5. Identifier vs keyword distinction
6. Position tracking (line, column)

### Lexer State

```java
class LexerState {
    Reader input;
    int currentChar;           // Current character being examined
    int peekChar;              // Next character (1-char lookahead)

    int line;                  // Current line number (1-based)
    int column;                // Current column (0-based)
    int lineStartPos;          // Position where current line starts

    // Indentation tracking
    int currentIndent;         // Current indentation level
    Deque<Integer> indentStack; // Stack of indentation levels
    Queue<ToonToken> pendingTokens; // Buffered DEDENT tokens

    // Token data
    StringBuilder tokenText;   // Current token text
    Object tokenValue;         // Parsed value (for numbers, etc.)
}
```

### Lexer States

```
STATE_LINE_START          → Just after newline, measuring indentation
STATE_LINE_CONTENT        → Normal token scanning
STATE_IN_STRING           → Inside quoted string
STATE_IN_ESCAPE           → After backslash in string
STATE_IN_NUMBER           → Parsing number
STATE_IN_IDENTIFIER       → Parsing identifier/unquoted string
STATE_IN_COMMENT          → (Future: comments)
```

### Indentation State Machine

```
                             ┌─────────────┐
                     ┌───────│ LINE_START  │←──────┐
                     │       └─────────────┘       │
                     │              │              │
                     │         measure spaces      │
                     │              │              │
                     │              ▼              │
                     │    ┌──────────────────┐    │
         emit DEDENT │    │ Compare to stack │    │ NEWLINE
         (multiple)  │    └──────────────────┘    │
                     │         /    |    \         │
                     │        /     |     \        │
                     │       /      |      \       │
                     │      /       |       \      │
                     │ greater   equal   less      │
                     │    /         |        \     │
                     │   ▼          ▼         ▼    │
                     │ INDENT   SAME_INDENT  DEDENT│
                     │   │          │         │    │
                     └───┴──────────┴─────────┴────┘
                              │
                              ▼
                         LINE_CONTENT
```

**Pseudocode**:

```python
def handle_newline():
    emit(NEWLINE)

    # Measure indentation
    spaces = 0
    while peek() == SPACE:
        spaces += 1
        advance()

    # Check for blank line
    if peek() == NEWLINE or peek() == EOF:
        return  # Ignore blank line indentation

    # Validate indentation (strict mode)
    if strict_mode and spaces % indent_size != 0:
        error("Invalid indentation")

    new_indent = spaces

    if new_indent > current_indent:
        # Indent
        indent_stack.push(new_indent)
        current_indent = new_indent
        emit(INDENT)

    elif new_indent < current_indent:
        # Dedent (possibly multiple levels)
        while new_indent < indent_stack.top():
            pending_tokens.enqueue(DEDENT)
            indent_stack.pop()
            current_indent = indent_stack.top()

        if new_indent != current_indent:
            error("Invalid dedent level")

    else:
        # Same indent
        emit(SAME_INDENT)
```

### Token Scanning

```python
def next_token():
    # First, return any pending tokens (DEDENTs)
    if not pending_tokens.is_empty():
        return pending_tokens.dequeue()

    skip_whitespace()  # Skip spaces/tabs (not newlines)

    ch = peek()

    if ch == EOF:
        return EOF

    elif ch == NEWLINE:
        return handle_newline()

    elif ch == ':':
        advance()
        return COLON

    elif ch == ',':
        advance()
        return COMMA

    elif ch == '|':
        advance()
        return PIPE

    elif ch == '[':
        advance()
        return LBRACKET

    elif ch == ']':
        advance()
        return RBRACKET

    elif ch == '{':
        advance()
        return LBRACE

    elif ch == '}':
        advance()
        return RBRACE

    elif ch == '-':
        advance()
        if peek() == SPACE:
            return HYPHEN
        else:
            # Could be negative number or unquoted string
            return scan_unquoted_or_number()

    elif ch == '"':
        return scan_quoted_string()

    elif ch == HTAB:
        advance()
        return HTAB_TOKEN

    elif is_digit(ch) or (ch == '-'):
        return scan_number()

    elif is_id_start(ch):
        return scan_identifier()

    else:
        error(f"Unexpected character: {ch}")
```

### String Scanning

```python
def scan_quoted_string():
    token_text.clear()
    consume('"')  # Opening quote

    while peek() != '"':
        ch = peek()

        if ch == EOF or ch == NEWLINE:
            error("Unterminated string")

        if ch == '\\':
            advance()
            escape_ch = peek()

            if escape_ch == '\\':
                token_text.append('\\')
            elif escape_ch == '"':
                token_text.append('"')
            elif escape_ch == 'n':
                token_text.append('\n')
            elif escape_ch == 'r':
                token_text.append('\r')
            elif escape_ch == 't':
                token_text.append('\t')
            else:
                if strict_mode:
                    error(f"Invalid escape: \\{escape_ch}")
                else:
                    # Lenient: treat as literal
                    token_text.append(escape_ch)

            advance()
        else:
            token_text.append(ch)
            advance()

    consume('"')  # Closing quote

    return STRING(token_text.toString())
```

### Number Scanning

```python
def scan_number():
    token_text.clear()

    # Optional minus
    if peek() == '-':
        token_text.append('-')
        advance()

    # Integer part
    if peek() == '0':
        token_text.append('0')
        advance()

        # Check for forbidden leading zero (like "05")
        if is_digit(peek()):
            # This is "0X" where X is digit - invalid number
            # Treat as unquoted string instead
            return scan_rest_as_unquoted()
    else:
        if not is_digit(peek()):
            error("Invalid number")

        while is_digit(peek()):
            token_text.append(peek())
            advance()

    # Optional fraction
    if peek() == '.':
        token_text.append('.')
        advance()

        if not is_digit(peek()):
            error("Invalid number: digit expected after decimal point")

        while is_digit(peek()):
            token_text.append(peek())
            advance()

    # Optional exponent
    if peek() in ['e', 'E']:
        token_text.append(peek())
        advance()

        if peek() in ['+', '-']:
            token_text.append(peek())
            advance()

        if not is_digit(peek()):
            error("Invalid number: digit expected in exponent")

        while is_digit(peek()):
            token_text.append(peek())
            advance()

    # Parse the number
    number_str = token_text.toString()

    try:
        if '.' in number_str or 'e' in number_str or 'E' in number_str:
            value = parse_double(number_str)
        else:
            value = parse_long(number_str)
    except:
        error("Invalid number format")

    return NUMBER(value)
```

### Identifier/Keyword Scanning

```python
def scan_identifier():
    token_text.clear()

    # First character: letter or underscore
    if is_id_start(peek()):
        token_text.append(peek())
        advance()

    # Remaining: letter, digit, underscore, or dot
    while is_id_continue(peek()):
        token_text.append(peek())
        advance()

    text = token_text.toString()

    # Check for keywords
    if text == "true":
        return BOOLEAN(true)
    elif text == "false":
        return BOOLEAN(false)
    elif text == "null":
        return NULL()
    else:
        return IDENTIFIER(text)
```

## Layer 2: Parser State Machine

### Parser Responsibilities

1. Convert ToonToken stream to JsonToken stream
2. Maintain parsing context stack (objects, arrays, modes)
3. Handle array format detection
4. Enforce structural rules (length validation, etc.)
5. Manage delimiter scope

### Parser State

```java
class ParserState {
    ToonLexer lexer;
    ToonToken currentToken;
    ToonToken peekToken;       // 1-token lookahead

    // Context stack
    Deque<ParsingContext> contextStack;

    // Delimiter stack
    Deque<Delimiter> delimiterStack;  // Current active delimiter

    // Current token being emitted to Jackson
    JsonToken currentJsonToken;
    String textValue;
    Number numberValue;

    // Configuration
    int indentSize;
    boolean strictMode;
}

class ParsingContext {
    enum Type {
        ROOT,
        OBJECT,
        ARRAY_INLINE,
        ARRAY_TABULAR,
        ARRAY_LIST,
        TABULAR_ROW,
        LIST_ITEM,
        LIST_ITEM_OBJECT
    }

    Type type;
    int expectedIndentLevel;

    // For arrays
    int declaredLength;
    int currentIndex;

    // For tabular arrays
    String[] fieldNames;
    int currentFieldIndex;

    // For objects
    String currentKey;
}
```

### Top-Level Parsing State Machine

```
┌─────────┐
│  START  │
└────┬────┘
     │
     ▼
┌──────────────┐
│ Peek first   │
│ line         │
└────┬─────────┘
     │
     ├─── Array header? ──→ ROOT_ARRAY
     │
     ├─── Single line?  ──→ ROOT_PRIMITIVE
     │
     └─── Otherwise     ──→ ROOT_OBJECT
```

### Object Parsing State Machine

```
         ┌──────────────┐
    ┌───→│ EXPECT_FIELD │←──────┐
    │    └──────┬───────┘       │
    │           │                │
    │      Read key              │
    │           │                │
    │           ▼                │
    │    ┌─────────────┐        │
    │    │ EXPECT_COLON│        │
    │    └──────┬──────┘        │
    │           │                │
    │      Consume ':'           │
    │           │                │
    │           ▼                │
    │    ┌──────────────┐       │
    │    │ Check next   │       │
    │    └──────┬───────┘       │
    │           │                │
    │     ┌─────┴─────┐         │
    │     │           │         │
    │  NEWLINE     VALUE        │
    │     │           │         │
    │     ▼           ▼         │
    │  NESTED     SIMPLE        │
    │  OBJECT      FIELD        │
    │     │           │         │
    └─────┴───────────┴─────────┘
         (on DEDENT or end)
```

**Pseudocode**:

```python
def parse_object():
    emit(START_OBJECT)

    base_indent = lexer.current_indent

    while true:
        # Check for end of object (dedent or EOF)
        if current_indent < base_indent or current_token == EOF:
            break

        # Expect key
        key = parse_key()
        emit(FIELD_NAME, key)

        expect(COLON)

        # Check what follows
        if current_token == NEWLINE:
            # Nested structure
            advance()  # Consume newline

            if current_token == INDENT:
                advance()  # Consume indent

                if peek() == LBRACKET or (peek() == IDENTIFIER and peek_ahead_for_array_header()):
                    parse_array()
                else:
                    parse_object()
            else:
                # Empty object or error
                emit(START_OBJECT)
                emit(END_OBJECT)

        elif current_token == LBRACKET:
            # Array on same line as key
            parse_array()

        else:
            # Simple value
            value = parse_value()
            emit_value_token(value)
            expect(NEWLINE)

    emit(END_OBJECT)
```

### Array Parsing State Machine

```
┌──────────────┐
│ ARRAY_HEADER │
└──────┬───────┘
       │
  Parse header:
  [N<delim?>]{fields}:
       │
       ▼
┌────────────────┐
│ Peek first     │
│ content line   │
└────┬───────────┘
     │
     ├─── INDENT VALUE<delim> ──→ Has field_list? ──┬─ Yes ─→ TABULAR
     │                                               │
     │                                               └─ No ──→ INLINE
     │
     ├─── INDENT HYPHEN ──→ LIST
     │
     └─── Empty ──→ EMPTY_ARRAY
```

**Array Header Parsing**:

```python
def parse_array_header():
    # Optional key (for non-root arrays)
    key = None
    if current_token == IDENTIFIER or current_token == STRING:
        key = current_token.value
        advance()

    expect(LBRACKET)

    # Length
    if current_token != NUMBER:
        error("Array length expected")
    length = current_token.value
    advance()

    # Optional delimiter marker
    delimiter = COMMA  # Default
    if current_token == HTAB:
        delimiter = TAB
        advance()
    elif current_token == PIPE:
        delimiter = PIPE
        advance()

    expect(RBRACKET)

    # Optional field list
    fields = None
    if current_token == LBRACE:
        fields = parse_field_list(delimiter)

    expect(COLON)
    expect(NEWLINE)

    return ArrayHeader(key, length, delimiter, fields)
```

**Inline Array Parsing**:

```python
def parse_inline_array(header):
    emit(START_ARRAY)

    expect(INDENT)

    delimiter = header.delimiter
    values = parse_delimited_values(delimiter)

    if len(values) != header.declared_length:
        if strict_mode:
            error("Array length mismatch")

    for value in values:
        emit_value_token(value)

    expect(NEWLINE)
    expect(DEDENT)

    emit(END_ARRAY)
```

**Tabular Array Parsing**:

```python
def parse_tabular_array(header):
    emit(START_ARRAY)

    fields = header.field_names
    delimiter = header.delimiter

    row_count = 0
    base_indent = lexer.current_indent

    while current_indent >= base_indent and current_token != EOF:
        expect(INDENT)

        values = parse_delimited_values(delimiter)

        if len(values) != len(fields):
            if strict_mode:
                error(f"Row width mismatch: expected {len(fields)}, got {len(values)}")

        # Emit object for this row
        emit(START_OBJECT)
        for i, field in enumerate(fields):
            emit(FIELD_NAME, field)
            if i < len(values):
                emit_value_token(values[i])
            else:
                emit(VALUE_NULL)
        emit(END_OBJECT)

        row_count += 1
        expect(NEWLINE)

    if row_count != header.declared_length:
        if strict_mode:
            error("Array length mismatch")

    expect(DEDENT)
    emit(END_ARRAY)
```

**List Array Parsing**:

```python
def parse_list_array(header):
    emit(START_ARRAY)

    item_count = 0
    base_indent = lexer.current_indent

    while current_indent >= base_indent and current_token == HYPHEN:
        expect(HYPHEN)

        # What follows the hyphen?
        if current_token == LBRACKET:
            # Nested array
            parse_array()

        elif current_token == IDENTIFIER and peek() == COLON:
            # Object item
            parse_list_item_object()

        else:
            # Primitive item
            value = parse_value()
            emit_value_token(value)
            expect(NEWLINE)

        item_count += 1

    if item_count != header.declared_length:
        if strict_mode:
            error("Array length mismatch")

    expect(DEDENT)
    emit(END_ARRAY)
```

**List Item Object Parsing**:

```python
def parse_list_item_object():
    emit(START_OBJECT)

    # First field on hyphen line
    key = parse_key()
    emit(FIELD_NAME, key)
    expect(COLON)
    value = parse_value()
    emit_value_token(value)
    expect(NEWLINE)

    # Remaining fields indented +1 from hyphen
    item_base_indent = lexer.current_indent

    while current_indent > item_base_indent:
        expect(INDENT)

        key = parse_key()
        emit(FIELD_NAME, key)
        expect(COLON)

        if current_token == NEWLINE:
            # Nested object/array
            advance()
            if current_token == INDENT:
                advance()
                if peek() == LBRACKET:
                    parse_array()
                else:
                    parse_object()
        else:
            value = parse_value()
            emit_value_token(value)
            expect(NEWLINE)

    emit(END_OBJECT)
```

### Value Parsing

```python
def parse_value():
    token = current_token

    if token.type == STRING:
        return token.value

    elif token.type == NUMBER:
        return token.value

    elif token.type == BOOLEAN:
        return token.value

    elif token.type == NULL:
        return None

    elif token.type == IDENTIFIER:
        # Unquoted string (already validated by lexer)
        return token.value

    else:
        error("Value expected")

def emit_value_token(value):
    if value is None:
        emit(VALUE_NULL)
    elif isinstance(value, bool):
        emit(VALUE_TRUE if value else VALUE_FALSE)
    elif isinstance(value, str):
        emit(VALUE_STRING, value)
    elif isinstance(value, int):
        emit(VALUE_NUMBER_INT, value)
    elif isinstance(value, float):
        emit(VALUE_NUMBER_FLOAT, value)
```

### Delimiter Context Management

```python
delimiter_stack = [COMMA]  # Document default

def enter_array(delimiter):
    delimiter_stack.push(delimiter)

def exit_array():
    delimiter_stack.pop()

def current_delimiter():
    return delimiter_stack.top()

def parse_delimited_values(delimiter):
    values = []

    values.append(parse_value())
    advance()

    while current_token.type == delimiter:
        advance()  # Consume delimiter
        values.append(parse_value())
        advance()

    return values
```

## Error Handling

### Error Context

```java
class ParseError {
    String message;
    int line;
    int column;
    String context;  // Surrounding text

    public String format() {
        return $"Error at line {line}, column {column}: {message}\n" +
               $"  {context}\n" +
               $"  {' ' * column}^";
    }
}
```

### Common Errors

```python
errors = {
    "LENGTH_MISMATCH": "Array length mismatch: declared {declared}, got {actual}",
    "WIDTH_MISMATCH": "Tabular row width mismatch: expected {expected} fields, got {actual}",
    "MISSING_COLON": "Expected ':' after key",
    "INVALID_ESCAPE": "Invalid escape sequence: \\{char}",
    "UNTERMINATED_STRING": "Unterminated string",
    "INVALID_INDENT": "Indentation must be multiple of {indent_size}",
    "INVALID_DEDENT": "Invalid dedent level",
    "UNEXPECTED_TOKEN": "Unexpected token: {token}",
    "BLANK_LINE": "Blank lines not allowed here",
}
```

## Performance Considerations

### Streaming Guarantees

1. **Constant memory per nesting level**: Context stack bounded by nesting depth
2. **No document buffering**: Parse character-by-character
3. **Lazy token emission**: Only read ahead as needed
4. **Efficient indentation**: Track levels with int stack, not string comparison

### Optimization Opportunities

1. **Token reuse**: Pool ToonToken objects
2. **StringBuilder pooling**: Reuse for token text
3. **Lookahead buffering**: Small fixed-size buffer (2-3 tokens)
4. **Fast path for common patterns**: Optimize hot paths (simple fields, inline arrays)

## Testing Strategy

### Lexer Tests

- Indentation handling (indent, dedent, multi-level dedent)
- String escaping (all valid escapes, invalid escapes)
- Number parsing (integers, floats, exponents, edge cases)
- Identifier vs keyword
- Position tracking

### Parser Tests

- All array formats
- Nested structures
- Delimiter switching
- Error conditions
- Length/width validation
- Edge cases (empty arrays, empty objects)

### Integration Tests

- Round-trip: TOON → Jackson → Java objects → Jackson → TOON
- Large documents (streaming verification)
- Real-world examples from benchmark suite

## Next Steps

With this design in hand, we can now:
1. Implement ToonLexer with indentation tracking ✓
2. Implement ToonParser state machine ✓
3. Integrate with Jackson's ParserBase
4. Build comprehensive test suite

Phase 1 (Design) complete! Ready for Phase 2 (Implementation).
