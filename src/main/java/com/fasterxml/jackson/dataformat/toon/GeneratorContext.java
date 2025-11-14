package com.fasterxml.jackson.dataformat.toon;

import java.util.*;

/**
 * Represents the current generation context (state) in the TOON generator.
 * <p>
 * The generator maintains a stack of these contexts to track nested structures
 * (objects within objects, arrays within arrays, etc.).
 */
public class GeneratorContext {

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

        /** Inside a list item that's an object */
        LIST_ITEM_OBJECT
    }

    private final Type _type;
    private final GeneratorContext _parent;
    private final int _indentLevel;
    private final char _delimiter;

    // For objects
    private String _pendingFieldName;
    private int _fieldCount;

    // For arrays
    private List<Object> _bufferedElements;
    private int _elementCount;

    // For tabular arrays
    private String[] _fieldNames;

    /**
     * Creates a root context.
     */
    public GeneratorContext() {
        this(Type.ROOT, null, 0, ',');
    }

    /**
     * Creates a context of the given type.
     */
    private GeneratorContext(Type type, GeneratorContext parent, int indentLevel, char delimiter) {
        this._type = type;
        this._parent = parent;
        this._indentLevel = indentLevel;
        this._delimiter = delimiter;
        this._fieldCount = 0;
        this._elementCount = 0;
        this._bufferedElements = null;
    }

    /**
     * Creates a child object context.
     */
    public GeneratorContext createChildObject(int indentLevel) {
        return new GeneratorContext(Type.OBJECT, this, indentLevel, _delimiter);
    }

    /**
     * Creates a child inline array context.
     */
    public GeneratorContext createChildInlineArray(char delimiter) {
        GeneratorContext ctx = new GeneratorContext(Type.ARRAY_INLINE, this, _indentLevel, delimiter);
        ctx._bufferedElements = new ArrayList<>();
        return ctx;
    }

    /**
     * Creates a child list array context.
     */
    public GeneratorContext createChildListArray() {
        GeneratorContext ctx = new GeneratorContext(Type.ARRAY_LIST, this, _indentLevel, _delimiter);
        ctx._bufferedElements = new ArrayList<>();
        return ctx;
    }

    /**
     * Creates a child tabular array context.
     */
    public GeneratorContext createChildTabularArray(String[] fieldNames, char delimiter) {
        GeneratorContext ctx = new GeneratorContext(Type.ARRAY_TABULAR, this, _indentLevel, delimiter);
        ctx._fieldNames = fieldNames;
        ctx._bufferedElements = new ArrayList<>();
        return ctx;
    }

    /**
     * Creates a list item object context.
     */
    public GeneratorContext createListItemObject(int indentLevel) {
        return new GeneratorContext(Type.LIST_ITEM_OBJECT, this, indentLevel, _delimiter);
    }

    // Getters

    public Type getType() {
        return _type;
    }

    public GeneratorContext getParent() {
        return _parent;
    }

    public int getIndentLevel() {
        return _indentLevel;
    }

    public char getDelimiter() {
        return _delimiter;
    }

    public String getPendingFieldName() {
        return _pendingFieldName;
    }

    public void setPendingFieldName(String name) {
        this._pendingFieldName = name;
    }

    public int getFieldCount() {
        return _fieldCount;
    }

    public void incrementFieldCount() {
        _fieldCount++;
    }

    public int getElementCount() {
        return _elementCount;
    }

    public void incrementElementCount() {
        _elementCount++;
    }

    public List<Object> getBufferedElements() {
        return _bufferedElements;
    }

    public void addBufferedElement(Object element) {
        if (_bufferedElements == null) {
            _bufferedElements = new ArrayList<>();
        }
        _bufferedElements.add(element);
    }

    public String[] getFieldNames() {
        return _fieldNames;
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
        return String.format("GeneratorContext[type=%s, indent=%d, fields=%d, elements=%d]",
                           _type, _indentLevel, _fieldCount, _elementCount);
    }
}
