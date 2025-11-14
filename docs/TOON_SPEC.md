# TOON 2.0 Specification Reference

**Version**: 2.0 (2025-11-10)
**Source**: https://github.com/toon-format/spec

This document summarizes the TOON (Token-Oriented Object Notation) specification for implementation in jackson-dataformat-toon.

## Overview

TOON is a line-oriented, indentation-based format that encodes JSON data with:
- Explicit structure declarations (array lengths, field lists)
- Minimal quoting (only when necessary)
- Multiple array formats optimized for different data patterns
- Tabular layouts for uniform data

**Design Goal**: Reduce token count for LLM consumption by 30-60% compared to JSON while maintaining readability and validation capabilities.

## Data Model

TOON represents the same data model as JSON:
- **Primitives**: string, number, boolean, null
- **Objects**: Ordered key-value mappings (insertion order preserved)
- **Arrays**: Ordered sequences

## Character Encoding

- **Required**: UTF-8 encoding
- **Line Endings**: LF (`\n`) only (not CRLF)
- **BOM**: Not allowed
- **Trailing Newline**: Must NOT be present at end of document

## Numbers

### Canonical Form (Encoding)

Encoders MUST emit numbers in canonical form:
- No exponent notation (1e6 → 1000000)
- No leading zeros except for "0" itself
- No trailing zeros in fractional part (1.5000 → 1.5)
- Normalize -0 to 0
- Use decimal point with at least one digit after if fractional

**Examples**:
```
Valid:   0, 42, -17, 3.14, -0.5, 1000000
Invalid: 1e6, 1.0, 00, 01, 1.50, -0
```

### Parsing (Decoding)

Decoders MUST accept:
- Decimal integers: `42`, `-17`
- Decimal floats: `3.14`, `-0.5`
- Exponent notation: `1e6`, `1.5e-3`, `-1E+9`

Decoders MUST treat as strings:
- Numbers with forbidden leading zeros: `05`, `0001` (but `0` and `0.5` are valid)

## Strings

### Quoting Rules

Strings MUST be quoted if they:
1. Are empty (`""`)
2. Have leading or trailing whitespace
3. Equal reserved words: `true`, `false`, `null`
4. Match numeric patterns (including those with forbidden leading zeros)
5. Contain any of: `:`, `"`, `\`, `[`, `]`, `{`, `}`, control characters (U+0000–U+001F)
6. Contain the active delimiter (current array's delimiter or document delimiter if outside arrays)
7. Equal exactly `"-"` or start with `"- "`
8. Start with `#` (reserved for future use)

Otherwise, strings MAY be unquoted.

**Examples**:
```
Unquoted: hello, world123, user_name, data.field
Quoted:   "", "true", "42", "hello, world", "key: value", "-", "- item"
```

### Escape Sequences

Only five escapes are valid in quoted strings:
- `\\` → backslash
- `\"` → double quote
- `\n` → newline (LF)
- `\r` → carriage return
- `\t` → tab

Any other escape sequence (e.g., `\u`, `\/`, `\b`) MUST cause an error in strict mode.

### Keys

Unquoted keys must match regex: `^[A-Za-z_][A-Za-z0-9_.]*$`

Otherwise, keys MUST be quoted.

**Examples**:
```
Valid unquoted:   id, userName, data.field, _private, field123
Must be quoted:   "user-name", "123", "", "user name", "data:value"
```

## Objects

### Syntax

Simple field:
```
key: value
```

Nested object:
```
key:
  nested_key: value
  another_key: value2
```

### Rules

1. Nested objects are indicated by `key:` alone on a line (no value)
2. Nested content MUST be indented one level deeper
3. Object keys preserve insertion order
4. No trailing commas
5. Empty objects produce no output (or can be represented as `key:` with no nested content)

### Example

```
user:
  id: 123
  name: Alice
  profile:
    email: alice@example.com
    active: true
  role: admin
```

## Arrays

### Array Header Syntax

General form:
```
key[N<delim?>]{fields}:
```

Where:
- `N` = declared length (required)
- `<delim?>` = optional delimiter marker:
  - (empty) = comma (default)
  - `\t` (HTAB U+0009) = tab delimiter
  - `|` = pipe delimiter
- `{fields}` = optional field list for tabular arrays

**Examples**:
```
tags[3]:              # 3 items, comma-delimited
data[5	]:            # 5 items, tab-delimited (note: actual tab character)
users[2|]:            # 2 items, pipe-delimited
items[2]{id,name}:    # 2 tabular rows with fields id,name
```

### Active Delimiter

The delimiter declared in an array header becomes the "active delimiter" for:
- Splitting inline primitive arrays
- Splitting tabular rows
- Nested arrays within that scope

Delimiter scope is hierarchical - nested arrays can override parent delimiter.

### Array Format 1: Inline Primitive Arrays

For arrays containing only primitives (no objects or arrays):

```
tags[3]: admin,ops,dev
numbers[4]: 1,2,3,4
flags[2]: true,false
```

With pipe delimiter:
```
tags[3|]: admin|ops|dev
```

### Array Format 2: Tabular Arrays

For arrays of objects where:
- All objects have the same keys
- All values are primitives (not nested objects/arrays)

Syntax:
```
key[N]{field1,field2,...}:
  value1,value2,...
  value1,value2,...
```

**Example**:
```
users[3]{id,name,active}:
  1,Alice,true
  2,Bob,false
  3,Charlie,true
```

**Rules**:
1. Field list in header MUST match object keys exactly
2. Each row MUST have same number of values as fields
3. Rows are separated by newlines
4. Each row is indented one level
5. No blank lines within tabular data

**With delimiters**:
```
users[2	]{id	name	active}:
  1	Alice	true
  2	Bob	false
```

### Array Format 3: List Arrays (Mixed/Non-Uniform)

For arrays containing:
- Mixed types (primitives, objects, arrays)
- Objects with different keys
- Nested structures

Syntax uses hyphen prefix:
```
items[3]:
  - value1
  - value2
  - value3
```

**Primitives**:
```
items[3]:
  - 1
  - hello
  - true
```

**Objects**:
```
items[2]:
  - id: 1
    name: First
  - id: 2
    name: Second
```

**Rules for Objects as List Items**:
1. First field appears on the same line as hyphen
2. Remaining fields indented +1 from hyphen
3. Nested objects within items indented +2 from hyphen

**Mixed Types**:
```
items[3]:
  - 1
  - name: Alice
    id: 2
  - simple string
```

### Array Format 4: Arrays of Arrays

```
pairs[2]:
  - [2]: 1,2
  - [2]: 3,4
```

Or with primitives:
```
matrix[2]:
  - [3]: 1,2,3
  - [3]: 4,5,6
```

## Indentation

### Rules

- Default: 2 spaces per level
- Configurable via `indentSize` option (commonly 2 or 4)
- Indentation MUST use spaces only (ASCII space U+0020)
- Tabs MUST NOT be used for indentation (allowed only in quoted strings and as HTAB delimiter)
- In strict mode: leading spaces MUST be exact multiples of indentSize
- No trailing spaces allowed on any line

### Dedentation

When dedenting, you can dedent multiple levels at once:

```
root:
  level1:
    level2:
      deep: value
  back_to_level1: value
```

Dedent from level 3 (4 spaces) to level 1 (2 spaces) is valid.

## Root Form Detection

Parser determines root form by examining document:

1. **Root Array**: First non-empty depth-0 line is valid array header → decode as root array
   ```
   [3]: a,b,c
   ```

2. **Root Primitive**: Exactly one non-empty line, and it's neither header nor key-value → decode as primitive
   ```
   hello
   ```
   or
   ```
   42
   ```

3. **Root Object**: Otherwise → decode as object
   ```
   id: 123
   name: Alice
   ```

4. **Empty Document**: → decode as empty object `{}`

## Key Folding & Path Expansion (v1.5+)

### Key Folding (Encoding)

Collapses chains of single-key objects into dotted notation:

**Before folding**:
```
data:
  metadata:
    items[2]: a,b
```

**After folding**:
```
data.metadata.items[2]: a,b
```

**Safe Mode Rules**:
- Each segment must match `^[A-Za-z_][A-Za-z0-9_]*$`
- No dots within segments
- No quoting required for any segment
- Only collapse chains with single key at each level

### Path Expansion (Decoding)

Splits dotted keys back into nested objects:

**Input**:
```
data.metadata.value: 42
```

**Output**:
```json
{
  "data": {
    "metadata": {
      "value": 42
    }
  }
}
```

**Conflict Handling**:
If paths conflict (object vs primitive at same location):
- Strict mode: ERROR
- Lenient mode: Last write wins

## Delimiters

Three delimiter options:

### 1. Comma (Default)
```
items[3]: a,b,c
users[2]{id,name}:
  1,Alice
  2,Bob
```

### 2. Tab (HTAB)
```
items[3	]: a	b	c
users[2	]{id	name}:
  1	Alice
  2	Bob
```

Note: The character after `[3` is literal tab character (U+0009).

### 3. Pipe
```
items[3|]: a|b|c
users[2|]{id|name}:
  1|Alice
  2|Bob
```

## Blank Lines

- Blank lines (empty or whitespace-only) are allowed between top-level fields
- Blank lines are NOT allowed:
  - Inside arrays (between items)
  - Inside tabular data (between rows)
  - Inside objects (between fields at the same level)
- In strict mode: blank lines in forbidden locations MUST error

## Strict Mode

When `strict: true`, decoders MUST error on:

1. **Array Length Mismatch**: Declared `[N]` doesn't match actual item count
2. **Tabular Width Mismatch**: Row has different number of values than header fields
3. **Missing Colons**: Key without `: ` separator
4. **Invalid Escapes**: Escape sequences other than `\\`, `\"`, `\n`, `\r`, `\t`
5. **Unterminated Strings**: Quoted string without closing quote
6. **Indentation Errors**: Leading spaces not a multiple of indentSize
7. **Tabs in Indentation**: Tab character used for indentation
8. **Blank Lines**: Blank lines in forbidden locations (inside arrays/tables)
9. **Path Expansion Conflicts**: Conflicting paths when expanding dotted keys

When `strict: false` (lenient mode):
- Mismatches and minor errors may be tolerated
- Last-write-wins for conflicts
- Decoder-specific recovery strategies allowed

## Conformance

### Encoders MUST:
1. Produce UTF-8 with LF line endings
2. Use consistent indentation (all same indentSize)
3. Preserve object key order
4. Normalize numbers per canonical form
5. Emit array lengths matching actual item counts
6. Avoid trailing spaces on lines
7. Not emit trailing newline at end of document
8. Quote strings per quoting rules
9. Use only valid escape sequences

### Decoders MUST:
1. Parse array headers correctly
2. Split using active delimiter
3. Unescape quoted strings
4. Accept only valid escape sequences (error on invalid in strict mode)
5. Type primitives correctly (string, number, boolean, null)
6. Enforce strict-mode rules when enabled
7. Preserve object key order
8. Handle root form detection

## Edge Cases

### Empty Structures

```
# Empty array (inline)
items[0]:

# Empty object (no output or just key:)
user:

# Empty string
name: ""
```

### Special Values

```
# Null
value: null

# Boolean
active: true
inactive: false

# Negative zero (normalized to 0)
zero: 0
```

### Quoting Edge Cases

```
# Hyphen needs quoting
hyphen: "-"

# Starts with hyphen-space needs quoting
item: "- not a list"

# Number-like strings
id: "007"
code: "0xFF"

# Contains delimiter (comma is document default)
text: "hello, world"

# Contains delimiter (but pipe is active in this array context)
items[2|]: hello, world|another item
```

### Nested Delimiters

```
# Outer array uses comma, inner uses pipe
outer[2]:
  - inner[2|]: a|b
  - inner[2|]: c|d
```

### Complex Nesting

```
data:
  users[2]{id,name}:
    1,Alice
    2,Bob
  metadata:
    version: 2.0
    items[3]:
      - simple
      - nested:
          value: 42
      - [2]: x,y
```

## Summary of Key Differences from JSON

| Feature | JSON | TOON |
|---------|------|------|
| Braces | Required `{}` | Indentation-based |
| Brackets | Required `[]` | Header with length `[N]:` |
| Commas | Required between elements | Inline arrays only |
| Quotes | Always for strings | Conditional |
| Array lengths | Implicit | Explicit `[N]` |
| Tabular data | Repeated keys | Header once, rows |
| Nesting | Braces | Indentation |
| Delimiters | Comma only | Comma/Tab/Pipe |

## Implementation Notes for Jackson

### Parser Considerations

1. **Indentation Tracking**: Must maintain stack of indentation levels
2. **Delimiter Scope**: Stack of active delimiters for nested arrays
3. **Tabular State**: When in tabular mode, parse rows as CSV with known fields
4. **Lookahead**: Need to distinguish `key:` (object start) vs `key: value` (field)
5. **Root Detection**: Examine first line to determine root form

### Generator Considerations

1. **Format Selection**: Choose array format based on content uniformity
2. **Buffering**: May need to buffer arrays to detect tabular opportunities
3. **Indentation**: Track nesting depth for output
4. **Delimiter Choice**: Configurable, affects quoting rules
5. **Number Normalization**: Must emit canonical form

### Streaming Challenges

1. **Generator buffering**: Need to look ahead for tabular format detection
2. **Large arrays**: Must handle without loading entire array in memory
3. **Nested structures**: Maintain context stack efficiently
4. **Error recovery**: Provide useful error messages with line/column info
