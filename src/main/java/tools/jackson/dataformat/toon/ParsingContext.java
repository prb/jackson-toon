package tools.jackson.dataformat.toon;

/**
 * Represents the current parsing context (state) in the TOON parser.
 * <p>
 * The parser maintains a stack of these contexts to track nested structures
 * (objects within objects, arrays within arrays, etc.).
 */
public class ParsingContext {

    /**
     * Type of context
     */
    public enum Type {
        /** Root level of document */
        ROOT,

        /** Inside an object */
        OBJECT,

        /** Inside an inline array ([N]: v1,v2,v3) */
        ARRAY_INLINE,

        /** Inside a tabular array ([N]{fields}: rows) */
        ARRAY_TABULAR,

        /** Inside a list array ([N]: - items) */
        ARRAY_LIST,

        /** Currently parsing a tabular row */
        TABULAR_ROW,

        /** Inside a list item */
        LIST_ITEM,

        /** Inside an object that's a list item */
        LIST_ITEM_OBJECT
    }

    private final Type _type;
    private final ParsingContext _parent;
    private final int _expectedIndentLevel;

    // For arrays
    private final int _declaredLength;
    private int _currentIndex;

    // For tabular arrays
    private final String[] _fieldNames;
    private int _currentFieldIndex;

    // For objects
    private String _currentKey;

    // For delimiter scope
    private final char _delimiter;

    /**
     * Creates a root context.
     */
    public ParsingContext() {
        this(Type.ROOT, null, 0, 0, null, ',');
    }

    /**
     * Creates a context of the given type.
     */
    private ParsingContext(Type type, ParsingContext parent, int expectedIndent,
                          int declaredLength, String[] fieldNames, char delimiter) {
        this._type = type;
        this._parent = parent;
        this._expectedIndentLevel = expectedIndent;
        this._declaredLength = declaredLength;
        this._fieldNames = fieldNames;
        this._delimiter = delimiter;
        this._currentIndex = 0;
        this._currentFieldIndex = 0;
    }

    /**
     * Creates a child object context.
     */
    public ParsingContext createChildObject(int indentLevel) {
        return new ParsingContext(Type.OBJECT, this, indentLevel, 0, null, _delimiter);
    }

    /**
     * Creates a child inline array context.
     */
    public ParsingContext createChildInlineArray(int length, char delimiter) {
        return new ParsingContext(Type.ARRAY_INLINE, this, _expectedIndentLevel,
                                 length, null, delimiter);
    }

    /**
     * Creates a child tabular array context.
     */
    public ParsingContext createChildTabularArray(int length, String[] fields, char delimiter) {
        return new ParsingContext(Type.ARRAY_TABULAR, this, _expectedIndentLevel,
                                 length, fields, delimiter);
    }

    /**
     * Creates a child list array context.
     */
    public ParsingContext createChildListArray(int length) {
        return new ParsingContext(Type.ARRAY_LIST, this, _expectedIndentLevel,
                                 length, null, _delimiter);
    }

    /**
     * Creates a tabular row context.
     */
    public ParsingContext createTabularRow() {
        return new ParsingContext(Type.TABULAR_ROW, this, _expectedIndentLevel,
                                 _fieldNames.length, _fieldNames, _delimiter);
    }

    /**
     * Creates a list item context.
     */
    public ParsingContext createListItem() {
        return new ParsingContext(Type.LIST_ITEM, this, _expectedIndentLevel,
                                 0, null, _delimiter);
    }

    /**
     * Creates a list item object context.
     */
    public ParsingContext createListItemObject(int indentLevel) {
        return new ParsingContext(Type.LIST_ITEM_OBJECT, this, indentLevel,
                                 0, null, _delimiter);
    }

    // Getters

    public Type getType() {
        return _type;
    }

    public ParsingContext getParent() {
        return _parent;
    }

    public int getExpectedIndentLevel() {
        return _expectedIndentLevel;
    }

    public int getDeclaredLength() {
        return _declaredLength;
    }

    public int getCurrentIndex() {
        return _currentIndex;
    }

    public void incrementIndex() {
        _currentIndex++;
    }

    public String[] getFieldNames() {
        return _fieldNames;
    }

    public int getCurrentFieldIndex() {
        return _currentFieldIndex;
    }

    public void incrementFieldIndex() {
        _currentFieldIndex++;
    }

    public void resetFieldIndex() {
        _currentFieldIndex = 0;
    }

    public String getCurrentFieldName() {
        if (_fieldNames != null && _currentFieldIndex < _fieldNames.length) {
            return _fieldNames[_currentFieldIndex];
        }
        return null;
    }

    public String getCurrentKey() {
        return _currentKey;
    }

    public void setCurrentKey(String key) {
        _currentKey = key;
    }

    public char getDelimiter() {
        return _delimiter;
    }

    public boolean isInArray() {
        return _type == Type.ARRAY_INLINE
            || _type == Type.ARRAY_TABULAR
            || _type == Type.ARRAY_LIST;
    }

    public boolean isInObject() {
        return _type == Type.OBJECT || _type == Type.LIST_ITEM_OBJECT;
    }

    @Override
    public String toString() {
        return String.format("ParsingContext[type=%s, indent=%d, index=%d/%d]",
                           _type, _expectedIndentLevel, _currentIndex, _declaredLength);
    }
}
