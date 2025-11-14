# TOON Generator Design

## Overview

The ToonGenerator is responsible for converting Jackson write events into TOON format output. It implements Jackson's streaming generator interface to enable efficient serialization of Java objects to TOON format.

## Architecture

### Core Components

1. **ToonGenerator**: Main generator class extending Jackson's `GeneratorBase`
2. **GeneratorContext**: Tracks current output state (in object, array, etc.)
3. **IndentationManager**: Manages indentation levels and whitespace output
4. **DelimiterScope**: Tracks which delimiter to use in current context

### State Management

The generator maintains a context stack to track:
- Current container type (ROOT, OBJECT, ARRAY_INLINE, ARRAY_TABULAR, ARRAY_LIST)
- Current indentation level
- Active delimiter (comma, pipe, or tab)
- Whether we're expecting a field name or value
- Array metadata (declared length, field names for tabular)

## Generator Methods

### Object Methods
```java
void writeStartObject()           // Start a new object
void writeEndObject()             // End current object
void writeFieldName(String name)  // Write a field name (followed by ':')
```

### Array Methods
```java
void writeStartArray()            // Start inline array (defer format decision)
void writeEndArray()              // End current array
```

### Value Methods
```java
void writeString(String text)     // Write string value (quote if needed)
void writeNumber(int v)           // Write integer
void writeNumber(long v)          // Write long
void writeNumber(double v)        // Write double
void writeBoolean(boolean v)      // Write boolean (true/false)
void writeNull()                  // Write null
```

## Output Strategy

### Indentation Rules

1. **Objects**: Child fields indented by 2 spaces
   ```
   user:
     id: 123
     name: Alice
   ```

2. **Nested Objects**: Each level adds 2 spaces
   ```
   user:
     address:
       city: NYC
       zip: 10001
   ```

3. **Arrays**: Elements indented by 2 spaces
   ```
   [2]:
     - item1
     - item2
   ```

### String Quoting

Quote strings when they:
- Contain delimiter characters (`,`, `|`, `\t`)
- Contain special characters (`:`, `[`, `]`, `{`, `}`, `-`)
- Start/end with whitespace
- Contain newlines
- Could be confused with numbers, booleans, or null

### Array Format Selection

The generator needs to decide which array format to use:

1. **Inline Array** (default for primitives):
   - Use when all elements are primitives
   - Use comma delimiter by default
   ```
   [3]: a,b,c
   ```

2. **List Array** (for mixed/complex types):
   - Use when elements are objects or mixed types
   - Each element on own line with `-` prefix
   ```
   [2]:
     - apple
     - banana
   ```

3. **Tabular Array** (optimization):
   - Use when all elements are objects with same fields
   - Requires analyzing all elements first (not streaming-friendly)
   - For now, not used in streaming mode
   ```
   [2]{id,name}:
     1,Alice
     2,Bob
   ```

### Delimiter Selection

1. **Comma (default)**: Most common, used unless values contain commas
2. **Pipe**: Used when values contain commas but not pipes
3. **Tab**: Used when values contain both commas and pipes

The generator analyzes values to select the best delimiter.

## Context Stack

```java
class GeneratorContext {
    enum Type {
        ROOT,
        OBJECT,
        ARRAY_INLINE,
        ARRAY_LIST,
        ARRAY_TABULAR
    }

    Type type;
    int indentLevel;
    char delimiter;
    int elementCount;
    boolean expectingField;  // For objects
    String[] fieldNames;     // For tabular arrays
}
```

## Output Flow Examples

### Simple Object
```
Input:  writeStartObject()
        writeFieldName("id")
        writeNumber(123)
        writeFieldName("name")
        writeString("Alice")
        writeEndObject()

Output: id: 123
        name: Alice
```

### Nested Object
```
Input:  writeStartObject()
        writeFieldName("user")
        writeStartObject()
        writeFieldName("id")
        writeNumber(123)
        writeEndObject()
        writeEndObject()

Output: user:
          id: 123
```

### Inline Array
```
Input:  writeStartArray()
        writeString("a")
        writeString("b")
        writeString("c")
        writeEndArray()

Output: [3]: a,b,c
```

### List Array
```
Input:  writeStartArray()
        writeStartObject()
        writeFieldName("id")
        writeNumber(1)
        writeEndObject()
        writeStartObject()
        writeFieldName("id")
        writeNumber(2)
        writeEndObject()
        writeEndArray()

Output: [2]:
          - id: 1
          - id: 2
```

## Implementation Challenges

### 1. Array Format Decision

**Problem**: Can't decide array format until we see elements, but streaming requires immediate output.

**Solutions**:
- **Buffered Array Mode**: Buffer array elements until writeEndArray(), then decide format
- **Heuristic Mode**: Use simple heuristics (primitives = inline, objects = list)
- **Explicit API**: Add custom methods like `writeStartArrayInline()`, `writeStartArrayList()`

**Recommendation**: Start with heuristic mode for simplicity.

### 2. Array Length Declaration

**Problem**: TOON requires `[N]:` syntax with known length upfront.

**Solutions**:
- **Buffered Mode**: Buffer all elements to count them
- **JSON-LD Style**: Allow `[]:` syntax for unknown length (extension to spec)
- **Two-Pass**: Require counting pass before writing

**Recommendation**: Use buffered mode - accumulate elements in memory before writing.

### 3. Delimiter Selection

**Problem**: Need to analyze values to choose optimal delimiter.

**Solution**:
- Default to comma
- When writing array, scan values for comma presence
- Switch to pipe if commas found
- Switch to tab if both comma and pipe found

### 4. String Quoting

**Problem**: Deciding when to quote strings.

**Solution**: Implement quoting rules:
```java
boolean needsQuoting(String s) {
    if (s.isEmpty()) return true;
    if (Character.isWhitespace(s.charAt(0)) ||
        Character.isWhitespace(s.charAt(s.length()-1))) return true;
    if (s.contains(":") || s.contains(",") || s.contains("|")) return true;
    if (looksLikeNumber(s) || looksLikeBoolean(s) || s.equals("null")) return true;
    return false;
}
```

## Generator State Machine

```
ROOT
├─ writeStartObject() → OBJECT
├─ writeStartArray() → buffer for format decision
│
OBJECT
├─ writeFieldName() → set pendingFieldName
├─ writeValue() → output "field: value\n"
├─ writeStartObject() → NESTED_OBJECT (indent++)
├─ writeStartArray() → buffer for format decision
├─ writeEndObject() → pop context
│
ARRAY_INLINE
├─ writeValue() → output "value,"
├─ writeEndArray() → output final value, close array
│
ARRAY_LIST
├─ writeValue() → output "- value\n"
├─ writeStartObject() → output "- ", start inline object
├─ writeEndArray() → close array
```

## Buffered Array Strategy

Since TOON requires knowing the array length and format upfront, implement buffering:

```java
class BufferedArray {
    List<Object> elements = new ArrayList<>();

    void addPrimitive(Object value) {
        elements.add(value);
    }

    void addObject(Map<String, Object> obj) {
        elements.add(obj);
    }

    void flush(Writer out) {
        // Analyze elements
        boolean allPrimitives = elements.stream()
            .allMatch(e -> !(e instanceof Map));

        if (allPrimitives) {
            writeInlineArray(out);
        } else {
            writeListArray(out);
        }
    }
}
```

## Error Handling

1. **Invalid Structure**: Detect invalid event sequences
   - `writeEndObject()` without `writeStartObject()`
   - `writeFieldName()` outside of object
   - Missing field value after field name

2. **Encoding Errors**: Handle special characters
   - Invalid UTF-8 sequences
   - Control characters in strings

3. **Resource Errors**: Handle I/O exceptions
   - Writer failures
   - Out of memory (large buffered arrays)

## Performance Considerations

1. **Buffer Size**: Limit array buffering to prevent OOM
2. **String Building**: Use StringBuilder for efficient concatenation
3. **Context Stack**: Pre-allocate reasonable depth (e.g., 32 levels)
4. **Write Batching**: Buffer small writes, flush periodically

## Testing Strategy

1. **Unit Tests**: Test each write method independently
2. **Round-Trip Tests**: Parse → Generate → Parse again
3. **Edge Cases**: Empty objects, empty arrays, deep nesting
4. **Performance Tests**: Large documents, deep nesting
5. **Compatibility Tests**: Ensure output is valid TOON

## Next Steps

1. Implement GeneratorContext class
2. Implement IndentationManager utility
3. Implement ToonGenerator with basic object/value support
4. Add array buffering logic
5. Add delimiter selection logic
6. Add string quoting logic
7. Create comprehensive tests
8. Optimize performance

## Open Questions

1. Should we support strict mode (validate lengths, reject extensions)?
2. Should we add pretty-print vs compact mode?
3. Should we support comments in output?
4. How to handle circular references?
5. Should we support custom array format hints via annotations?
