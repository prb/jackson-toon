# TOON Edge Cases and Test Scenarios

This document catalogs edge cases, corner conditions, and tricky scenarios for the TOON parser/generator implementation.

## 1. Empty Structures

### Empty Arrays

```toon
# Empty inline array
items[0]:

# Empty array in object
data:
  items[0]:
```

**Expected**: JsonToken sequence should be `START_ARRAY, END_ARRAY`

### Empty Objects

```toon
# Empty root object (empty document)

```

**Expected**: Empty object `{}`

```toon
# Named empty object
user:
```

**Expected**: `{"user": {}}`

### Empty Strings

```toon
# Empty string value (must be quoted)
name: ""

# Empty string in array
items[1]: ""
```

**Expected**: String value with length 0

## 2. Indentation Edge Cases

### Multi-Level Dedent

```toon
root:
  level1:
    level2:
      deep: value
  back_to_level1: value
```

**Test**: Dedent from level 3 directly to level 1 (skip level 2)

**Expected**: Must emit multiple DEDENT tokens

### Invalid Dedent Level

```toon
root:
  level1: value
   invalid: value
```

Note: 3 spaces (not valid dedent to 0 or 2)

**Expected**: Strict mode error, lenient mode may accept

### Tabs in Indentation

```toon
user:
→id: 123
```

(Where → is a tab character used for indentation)

**Expected**: Error in all modes (tabs forbidden for indentation)

### Mixed Spaces

```toon
user:
  id: 123
    name: Alice
```

Note: Second line has 4 spaces (should be 2 for same level or 4 for nested)

**Expected**: Valid if indentSize=2 (nested object)

### Zero Indentation After Dedent

```toon
root:
  nested: value
top_level: value
```

**Expected**: Valid - dedent back to level 0

## 3. Number Edge Cases

### Leading Zeros (Forbidden)

```toon
# These should be treated as strings, not numbers
code: 007
hex: 0xFF
octal: 0777
```

**Expected**: Parsed as strings because of leading zeros after '0'

### Valid Zero

```toon
zero: 0
decimal: 0.5
float: 0.123
```

**Expected**: Valid numbers

### Negative Zero

```toon
value: -0
```

**Expected**: Encoder must normalize to `0`, parser accepts and converts to `0`

### Exponent Notation

```toon
# Parser must accept
scientific: 1e6
negative_exp: 1.5e-3
explicit_plus: 1E+9

# Encoder must NOT produce exponent notation (canonical form)
# 1e6 → 1000000
# 1.5e-3 → 0.0015
```

### Large Numbers

```toon
big_int: 9223372036854775807
big_float: 1.7976931348623157e308
```

**Expected**: Handle Long.MAX_VALUE, Double.MAX_VALUE

### Trailing/Leading Zeros

```toon
# Encoder must normalize
value: 1.5000  # → 1.5
value: 01.5    # → STRING (leading zero)
```

### Integer vs Float Detection

```toon
int: 42
float: 42.0  # Encoder must output as "42" (no .0)
exp: 4.2e1   # Parser accepts, equals 42.0
```

## 4. String Quoting Edge Cases

### Reserved Words

```toon
# Must be quoted to be strings
bool_str: "true"
null_str: "null"
false_str: "false"

# Unquoted are primitives
bool_val: true
null_val: null
```

### Number-Like Strings

```toon
# Must be quoted if they look like numbers
code: "123"
version: "1.0"

# Unquoted are numbers
count: 123
version_num: 1.0
```

### Delimiter in Strings

```toon
# Comma is default delimiter, must quote
text: "hello, world"

# In pipe-delimited array, comma doesn't need quoting
items[2|]: hello, world|another

# But pipe needs quoting in that context
items[2|]: "a|b"|c
```

### Hyphen Edge Cases

```toon
# Exactly "-" must be quoted
dash: "-"

# Starts with "- " must be quoted (looks like list item)
text: "- not a list"

# Hyphen not followed by space is OK unquoted
uuid: 550e8400-e29b-41d4-a716-446655440000

# Negative number
temp: -42
```

### Special Characters

```toon
# Colon requires quoting
key: "value: with colon"

# Brackets/braces require quoting
expr: "{x}"
arr: "[1,2,3]"

# Backslash requires quoting
path: "C:\\Users\\Alice"

# Quote within quoted string
text: "She said \"hello\""
```

### Whitespace

```toon
# Leading/trailing whitespace requires quoting
text: " hello "
leading: " leading"
trailing: "trailing "

# Embedded whitespace without leading/trailing is OK
text: hello world
```

### Empty String

```toon
# Must be quoted
empty: ""

# Not empty: unquoted
space: " "  # (but this needs quotes due to leading/trailing whitespace)
```

## 5. Array Edge Cases

### Length Mismatches

```toon
# Declared 3, actual 2
items[3]: a,b

# Declared 2, actual 3
items[2]: a,b,c
```

**Expected**: Strict mode errors, lenient mode may accept actual count

### Tabular Width Mismatch

```toon
users[2]{id,name}:
  1,Alice
  2,Bob,extra
```

**Expected**: Strict mode error on row 2

### Empty Array Elements

```toon
# Empty string in array
items[3]: a,,c

# Null in array
items[3]: a,null,c

# Empty quoted string
items[3]: a,"",c
```

**Expected**: Middle element is empty string for first case, null for second

### Single-Element Arrays

```toon
# Single primitive
items[1]: only

# Single object (tabular)
users[1]{id,name}:
  1,Alice

# Single object (list)
users[1]:
  - id: 1
    name: Alice
```

### Nested Empty Arrays

```toon
matrix[2]:
  - [0]:
  - [0]:
```

**Expected**: Array of two empty arrays

## 6. Delimiter Edge Cases

### Delimiter Switching

```toon
# Outer uses comma
outer[2]:
  - inner[2|]: a|b
  - inner[2	]: c	d
```

**Expected**: Each array has its own delimiter scope

### Tab Delimiter Syntax

```toon
# HTAB character must appear in header
items[2	]:
  a	b
```

Note: Actual tab character (U+0009) after `[2`

**Expected**: Values split by tab

### Pipe in Non-Delimited Context

```toon
# Pipe is just a character outside arrays
text: hello|world

# But in pipe-delimited array
items[2|]: hello|world
```

**Expected**: First is string "hello|world", second errors (2 items but only 1 value)

### Trailing Delimiter

```toon
# Trailing comma
items[3]: a,b,c,

# Trailing pipe
items[3|]: a|b|c|
```

**Expected**: Strict mode error (4 values instead of 3)

## 7. Object Edge Cases

### Single-Key Chains (Key Folding)

```toon
# Folded form
data.metadata.value: 42

# Expanded form
data:
  metadata:
    value: 42
```

**Expected**: Both represent same structure when path expansion enabled

### Dotted Keys vs Nested Objects

```toon
# With path expansion disabled, this is a literal key
"data.metadata": 42

# With path expansion enabled, this is nested
data.metadata: 42
```

**Expected**: Behavior depends on decoder option

### Key Folding Conflicts

```toon
# Conflict: data.value is both object and primitive
data.value: 42
data.value.nested: 100
```

**Expected**: Strict mode error, lenient mode last-write-wins

### Keys Requiring Quotes

```toon
# Hyphenated
"user-name": Alice

# Numeric
"123": value

# With spaces
"first name": Alice

# With special chars
"key:value": data
```

## 8. List Array Edge Cases

### Object as First Item

```toon
items[2]:
  - id: 1
    name: First
  - id: 2
    name: Second
```

**Expected**: First field on hyphen line, rest indented

### Mixed Item Types

```toon
items[3]:
  - 42
  - text: value
  - simple string
```

**Expected**: Array contains number, object, string

### Nested Arrays in List

```toon
items[2]:
  - [2]: a,b
  - [2]: c,d
```

**Expected**: Array of arrays

### Deeply Nested List Items

```toon
items[1]:
  - id: 1
    data:
      nested:
        deep: value
```

**Expected**: Correct indentation tracking (+1 for sibling fields, +2 for nested objects)

## 9. Root Form Detection

### Root Array

```toon
[3]: a,b,c
```

**Expected**: Root is array, not object with key "[3]"

### Root Primitive

```toon
hello
```

**Expected**: Root is string "hello"

```toon
42
```

**Expected**: Root is number 42

### Ambiguous Single Line

```toon
key: value
```

**Expected**: Root object with one field

### Empty Document

```
(empty file)
```

**Expected**: Empty object `{}`

## 10. Blank Lines

### Allowed Blank Lines

```toon
user:
  id: 123

  name: Alice


  active: true
```

**Expected**: Blank lines between top-level fields are OK

### Forbidden Blank Lines

```toon
# In array
items[3]:
  a

  b
  c
```

**Expected**: Strict mode error

```toon
# In tabular array
users[2]{id,name}:
  1,Alice

  2,Bob
```

**Expected**: Strict mode error

### Multiple Consecutive Blank Lines

```toon
user:
  id: 123



  name: Alice
```

**Expected**: Multiple blanks between fields OK at top level

## 11. Escape Sequences

### Valid Escapes

```toon
text: "backslash: \\ quote: \" newline: \n return: \r tab: \t"
```

**Expected**: String with literal backslash, quote, newline, return, tab

### Invalid Escapes

```toon
# These should error in strict mode
text: "\u0041"  # Unicode escape not supported
text: "\/"      # Solidus escape not needed
text: "\b"      # Backspace not supported
text: "\f"      # Form feed not supported
text: "\x41"    # Hex escape not supported
```

### Escaped Quote

```toon
text: "She said \"hello\" to me"
```

**Expected**: String containing double quotes

### Escaped Backslash

```toon
path: "C:\\Users\\Alice"
```

**Expected**: Windows path with backslashes

## 12. Tabular Array Optimization

### Uniform Arrays

```toon
# All objects have same keys, all primitive values
users[3]{id,name,active}:
  1,Alice,true
  2,Bob,false
  3,Charlie,true
```

**Expected**: Generator should produce tabular format

### Non-Uniform Arrays (Same Keys, Different Order)

```toon
# Objects have same keys but possibly different order in source
# Parser doesn't care about order, but field list defines canonical order
```

**Expected**: Generator should detect uniformity by keys, not order

### Arrays with Nested Values

```toon
# Objects have nested structures - can't use tabular
users[2]:
  - id: 1
    profile:
      name: Alice
  - id: 2
    profile:
      name: Bob
```

**Expected**: Generator must use list format (values aren't primitives)

### Arrays with Different Keys

```toon
# Objects don't have same keys
items[2]:
  - id: 1
    type: user
  - name: Alice
    active: true
```

**Expected**: Generator must use list format

## 13. Large Documents

### Streaming Test

```toon
# 10,000 row tabular array
data[10000]{id,value}:
  1,a
  2,b
  ...
  10000,z
```

**Expected**: Parser should not buffer entire array, process streaming

### Deep Nesting

```toon
# 100 levels deep
level1:
  level2:
    level3:
      ...
        level100: value
```

**Expected**: Parser should handle with stack, not recursion overflow

### Wide Objects

```toon
# 1000 fields in single object
obj:
  field1: value1
  field2: value2
  ...
  field1000: value1000
```

**Expected**: Handle without issue

## 14. Unicode and Special Characters

### UTF-8 Characters

```toon
name: "Alice 😀"
text: "日本語"
emoji: "🎉"
```

**Expected**: Proper UTF-8 handling

### Control Characters

```toon
# Control chars must be escaped or in quoted strings
text: "line1\nline2"
```

### BOM

```
(file starts with UTF-8 BOM)
```

**Expected**: Error or strip BOM

## 15. Conformance Edge Cases

### Trailing Newline

```toon
user:
  id: 123

```

(Note: trailing newline at end)

**Expected**: Encoder must NOT emit trailing newline, parser should accept

### Trailing Spaces

```toon
user:
  id: 123
```

(Note: trailing spaces on lines)

**Expected**: Encoder must NOT emit, parser may accept in lenient mode

### CRLF Line Endings

```
user:\r\n
  id: 123\r\n
```

**Expected**: Encoder must use LF only, parser should accept CRLF in lenient mode

## 16. Performance Edge Cases

### Minimal Document

```toon
x
```

**Expected**: Fast parse, minimal overhead

### Maximal Tabular Efficiency

```toon
# 1000 objects × 10 fields = 10,000 values in compact form
items[1000]{f1,f2,f3,f4,f5,f6,f7,f8,f9,f10}:
  1,2,3,4,5,6,7,8,9,10
  ...
```

**Expected**: Significant token savings vs JSON

### Worst Case for TOON

```toon
# Deep nesting, no tabular opportunities
data:
  items[100]:
    - id: 1
      data:
        nested:
          value: a
    - id: 2
      data:
        nested:
          value: b
    ...
```

**Expected**: Still valid, but minimal token savings vs JSON

## Testing Checklist

- [ ] All empty structure cases
- [ ] All indentation edge cases
- [ ] All number formats and edge cases
- [ ] All string quoting scenarios
- [ ] All array format combinations
- [ ] Delimiter switching and nesting
- [ ] Object nesting and key folding
- [ ] List array variations
- [ ] Root form detection
- [ ] Blank line handling
- [ ] Escape sequences
- [ ] Tabular optimization detection
- [ ] Large document streaming
- [ ] Unicode handling
- [ ] Conformance edge cases
- [ ] Error cases in strict mode
- [ ] Lenient mode behavior
- [ ] Round-trip fidelity

## Error Message Quality

Good error messages should include:
- Line and column number
- Context (show the problematic line)
- Clear explanation
- Suggestion for fix (when possible)

Example:
```
Error at line 5, column 8: Array length mismatch
  items[3]: a,b
          ^
Expected 3 items, but found 2. Add 1 more item or change the declared length.
```

## Summary

These edge cases ensure our implementation is robust and handles the full TOON specification correctly. Each case should have corresponding unit tests.
