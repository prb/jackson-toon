# Jackson TOON Documentation

This directory contains comprehensive documentation for implementing the TOON data format for Jackson.

## Documentation Files

### 1. [TOON_SPEC.md](TOON_SPEC.md)
Complete TOON 2.0 specification reference including:
- Data model and encoding rules
- Number canonical form
- String quoting rules
- Array formats (inline, tabular, list)
- Object syntax and nesting
- Indentation rules
- Delimiter handling
- Key folding and path expansion
- Strict mode validation rules
- Conformance requirements

**Use this for**: Understanding TOON format requirements and validation rules

### 2. [GRAMMAR.md](GRAMMAR.md)
Formal EBNF grammar definition including:
- Lexical tokens
- Syntactic productions
- Indentation state machine
- Context-sensitive delimiter rules
- Array format selection logic
- Lookahead requirements
- Error productions

**Use this for**: Parser implementation and understanding formal syntax

### 3. [PARSER_DESIGN.md](PARSER_DESIGN.md)
Detailed streaming parser architecture including:
- Two-layer design (Lexer → Parser)
- Lexer state machine with indentation tracking
- Parser state machine for all TOON constructs
- Token flow diagrams
- Pseudocode for all parsing scenarios
- Error handling strategies
- Performance optimization notes

**Use this for**: Implementing ToonLexer and ToonParser classes

### 4. [EDGE_CASES.md](EDGE_CASES.md)
Comprehensive catalog of edge cases and test scenarios:
- Empty structures
- Indentation corner cases
- Number format edge cases
- String quoting scenarios
- Array format combinations
- Delimiter switching
- Unicode and special characters
- Performance edge cases
- Error conditions

**Use this for**: Test case development and validation

## Implementation Phases

### Phase 1: Specification & Design ✅ COMPLETE
- [x] Research TOON 2.0 specification
- [x] Document formal grammar (EBNF)
- [x] Design lexer state machine
- [x] Design parser state machine
- [x] Catalog edge cases

**Deliverables**: All documentation files in this directory

### Phase 2: Lexer Implementation (Next)
- [ ] Implement ToonToken enum
- [ ] Implement ToonLexer class with indentation tracking
- [ ] Implement character-level tokenization
- [ ] Implement string parsing with escapes
- [ ] Implement number parsing
- [ ] Add comprehensive lexer tests

### Phase 3: Parser Implementation
- [ ] Implement ToonParser extending ParserBase
- [ ] Implement parsing context stack
- [ ] Implement all array format parsers
- [ ] Implement object parsing
- [ ] Implement root form detection
- [ ] Add comprehensive parser tests

### Phase 4: Generator Implementation
- [ ] Implement ToonGenerator extending GeneratorBase
- [ ] Implement array format detection
- [ ] Implement number normalization
- [ ] Implement string quoting logic
- [ ] Implement indentation management
- [ ] Add comprehensive generator tests

### Phase 5: Integration
- [ ] Implement ToonFactory and ToonFactoryBuilder
- [ ] Implement ToonMapper with builder
- [ ] Implement feature enums (ToonReadFeature, ToonWriteFeature)
- [ ] Add service provider registration
- [ ] Create module-info.java

### Phase 6: Testing & Polish
- [ ] Comprehensive round-trip tests
- [ ] Performance benchmarks
- [ ] Integration tests with Jackson databind
- [ ] Error message quality tests
- [ ] Documentation and examples

## Key Design Decisions

### 1. Streaming Architecture ✓
- **Decision**: Implement true streaming parser/generator
- **Rationale**: TOON designed for large LLM datasets (100KB-MB range)
- **Approach**: Character-level lexer → token stream → JsonToken emission

### 2. Two-Layer Design ✓
- **Lexer Layer**: Character tokenization, indentation tracking
- **Parser Layer**: Token → JsonToken conversion, structure validation
- **Benefit**: Clear separation of concerns, easier testing

### 3. Indentation Tracking ✓
- **Approach**: Python-style INDENT/DEDENT token emission
- **Implementation**: Integer stack tracking indent levels
- **Validation**: Strict mode enforces multiples of indentSize

### 4. Array Format Detection (Generator)
- **Challenge**: Need to determine tabular vs list format
- **Solution**: Buffer array elements to analyze uniformity
- **Tradeoff**: Arrays buffered, primitives/objects streamed

### 5. Context Stacks ✓
- **Parsing Context**: Stack of current structure type
- **Delimiter Context**: Stack of active delimiters
- **Indent Context**: Stack of indentation levels

## Quick Reference

### TOON Features Summary

| Feature | Syntax | Example |
|---------|--------|---------|
| Simple field | `key: value` | `name: Alice` |
| Nested object | `key:` + indent | `user:\n  id: 1` |
| Inline array | `[N]: v1,v2` | `tags[3]: a,b,c` |
| Tabular array | `[N]{fields}: rows` | `users[2]{id,name}:\n  1,Alice\n  2,Bob` |
| List array | `[N]: - items` | `items[2]:\n  - a\n  - b` |
| Delimiters | `[N<d>]:` | `[3\|]: a\|b\|c` (pipe) |
| Key folding | `a.b.c: value` | `data.metadata.value: 42` |

### Token Types

```
Structural: COLON, COMMA, PIPE, LBRACKET, RBRACKET, LBRACE, RBRACE, HYPHEN
Values: STRING, NUMBER, BOOLEAN, NULL, IDENTIFIER
Whitespace: NEWLINE, INDENT, DEDENT, SAME_INDENT
Special: HTAB (delimiter marker), EOF, ERROR
```

### Jackson Integration Points

```java
ToonParser extends ParserBase implements JsonParser
ToonGenerator extends GeneratorBase implements JsonGenerator
ToonFactory extends TextualTSFactory
ToonMapper extends ObjectMapper
```

## Resources

### External References
- [TOON Specification](https://github.com/toon-format/spec)
- [TOON Examples](https://github.com/toon-format/spec/tree/main/examples)
- [Conformance Tests](https://github.com/toon-format/spec/tree/main/tests)
- [Jackson Documentation](https://github.com/FasterXML/jackson)
- [Jackson YAML Module](https://github.com/FasterXML/jackson-dataformats-text/tree/3.x/yaml)
- [Jackson TOML Module](https://github.com/FasterXML/jackson-dataformats-text/tree/3.x/toml)

### Implementation Examples
See existing Jackson dataformat modules for patterns:
- `YAMLParser` - Wrapper around SnakeYAML Engine
- `TomlParser` - Custom lexer with state machine
- `YAMLGenerator` - Event-based generation
- `TomlGenerator` - Direct character writing

## Next Steps

With Phase 1 complete, we have:
- ✅ Complete TOON 2.0 specification documented
- ✅ Formal grammar in EBNF
- ✅ Detailed parser state machine design
- ✅ Comprehensive edge case catalog

Ready to proceed to **Phase 2: Lexer Implementation**!
