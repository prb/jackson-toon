package com.fasterxml.jackson.dataformat.toon;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import java.io.*;

/**
 * Factory for creating TOON format parsers and generators.
 * <p>
 * This is the main entry point for using Jackson with TOON format.
 * It provides methods to create streaming parsers and generators for TOON.
 * <p>
 * Example usage:
 * <pre>
 * ToonFactory factory = new ToonFactory();
 * JsonParser parser = factory.createParser(toonString);
 * JsonGenerator generator = factory.createGenerator(outputStream);
 * </pre>
 *
 * @author Claude Code
 */
public class ToonFactory extends JsonFactory {

    private static final long serialVersionUID = 1L;

    /**
     * Format name identifier for TOON.
     */
    public static final String FORMAT_NAME = "TOON";

    /**
     * Default file extension for TOON files.
     */
    public static final String DEFAULT_TOON_EXTENSION = ".toon";

    // Configuration flags
    protected boolean _strictMode = false;

    /**
     * Default constructor.
     */
    public ToonFactory() {
        super();
    }

    /**
     * Copy constructor.
     */
    protected ToonFactory(ToonFactory src, ObjectCodec codec) {
        super(src, codec);
        this._strictMode = src._strictMode;
    }

    @Override
    public ToonFactory copy() {
        _checkInvalidCopy(ToonFactory.class);
        return new ToonFactory(this, null);
    }

    /**
     * Enable or disable strict mode.
     * In strict mode, the parser validates array lengths and other constraints.
     */
    public ToonFactory setStrictMode(boolean strict) {
        this._strictMode = strict;
        return this;
    }

    public boolean isStrictMode() {
        return _strictMode;
    }

    @Override
    public String getFormatName() {
        return FORMAT_NAME;
    }

    @Override
    public boolean canUseCharArrays() {
        return false;
    }

    // ========================================================================
    // Parser Creation
    // ========================================================================

    @Override
    public JsonParser createParser(String content) throws IOException {
        return new ToonParserAdapter(new ToonParser(new StringReader(content)), _strictMode);
    }

    @Override
    public JsonParser createParser(Reader r) throws IOException {
        return new ToonParserAdapter(new ToonParser(r), _strictMode);
    }

    @Override
    public JsonParser createParser(InputStream in) throws IOException {
        return createParser(new InputStreamReader(in, "UTF-8"));
    }

    @Override
    public JsonParser createParser(byte[] data) throws IOException {
        return createParser(new ByteArrayInputStream(data));
    }

    @Override
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        return createParser(new ByteArrayInputStream(data, offset, len));
    }

    @Override
    public JsonParser createParser(char[] content) throws IOException {
        return createParser(new CharArrayReader(content));
    }

    @Override
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        return createParser(new CharArrayReader(content, offset, len));
    }

    @Override
    public JsonParser createParser(File f) throws IOException {
        return createParser(new FileReader(f));
    }

    // ========================================================================
    // Generator Creation
    // ========================================================================

    @Override
    public JsonGenerator createGenerator(Writer out) throws IOException {
        return new ToonGeneratorAdapter(new ToonGenerator(out), _strictMode);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        return createGenerator(new OutputStreamWriter(out, enc.getJavaName()));
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        return createGenerator(out, JsonEncoding.UTF8);
    }

    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException {
        return createGenerator(new FileOutputStream(f), enc);
    }

    /**
     * Adapter to bridge ToonParser to Jackson's JsonParser interface.
     * This is a minimal implementation - full Jackson integration would require
     * extending ParserBase and implementing all methods.
     */
    private static class ToonParserAdapter extends JsonParser {
        private final ToonParser _toonParser;
        private final boolean _strictMode;
        private JsonToken _currentToken;
        private ToonParser.Event _currentEvent;

        public ToonParserAdapter(ToonParser parser, boolean strictMode) {
            this._toonParser = parser;
            this._strictMode = strictMode;
        }

        @Override
        public JsonToken nextToken() throws IOException {
            _currentEvent = _toonParser.nextEvent();
            _currentToken = convertEvent(_currentEvent);
            return _currentToken;
        }

        private JsonToken convertEvent(ToonParser.Event event) {
            switch (event) {
                case START_OBJECT: return JsonToken.START_OBJECT;
                case END_OBJECT: return JsonToken.END_OBJECT;
                case START_ARRAY: return JsonToken.START_ARRAY;
                case END_ARRAY: return JsonToken.END_ARRAY;
                case FIELD_NAME: return JsonToken.FIELD_NAME;
                case VALUE_STRING: return JsonToken.VALUE_STRING;
                case VALUE_NUMBER_INT: return JsonToken.VALUE_NUMBER_INT;
                case VALUE_NUMBER_FLOAT: return JsonToken.VALUE_NUMBER_FLOAT;
                case VALUE_TRUE: return JsonToken.VALUE_TRUE;
                case VALUE_FALSE: return JsonToken.VALUE_FALSE;
                case VALUE_NULL: return JsonToken.VALUE_NULL;
                case EOF: return null;
                default: return null;
            }
        }

        @Override
        public JsonToken getCurrentToken() {
            return _currentToken;
        }

        @Override
        public String getCurrentName() throws IOException {
            return _toonParser.getTextValue();
        }

        @Override
        public String getText() throws IOException {
            return _toonParser.getTextValue();
        }

        @Override
        public char[] getTextCharacters() throws IOException {
            String text = getText();
            return text != null ? text.toCharArray() : null;
        }

        @Override
        public int getTextLength() throws IOException {
            String text = getText();
            return text != null ? text.length() : 0;
        }

        @Override
        public int getTextOffset() throws IOException {
            return 0;
        }

        @Override
        public Number getNumberValue() throws IOException {
            return _toonParser.getNumberValue();
        }

        @Override
        public NumberType getNumberType() throws IOException {
            if (_currentEvent == ToonParser.Event.VALUE_NUMBER_INT) {
                return NumberType.LONG;
            } else if (_currentEvent == ToonParser.Event.VALUE_NUMBER_FLOAT) {
                return NumberType.DOUBLE;
            }
            return null;
        }

        @Override
        public int getIntValue() throws IOException {
            Number n = getNumberValue();
            return n != null ? n.intValue() : 0;
        }

        @Override
        public long getLongValue() throws IOException {
            Number n = getNumberValue();
            return n != null ? n.longValue() : 0;
        }

        @Override
        public double getDoubleValue() throws IOException {
            Number n = getNumberValue();
            return n != null ? n.doubleValue() : 0.0;
        }

        @Override
        public void close() throws IOException {
            _toonParser.close();
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public ObjectCodec getCodec() {
            return null;
        }

        @Override
        public void setCodec(ObjectCodec c) {
            // Not implemented
        }
    }

    /**
     * Adapter to bridge ToonGenerator to Jackson's JsonGenerator interface.
     */
    private static class ToonGeneratorAdapter extends JsonGenerator {
        private final ToonGenerator _toonGenerator;
        private final boolean _strictMode;

        public ToonGeneratorAdapter(ToonGenerator generator, boolean strictMode) {
            this._toonGenerator = generator;
            this._strictMode = strictMode;
        }

        @Override
        public JsonGenerator enable(Feature f) {
            return this;
        }

        @Override
        public JsonGenerator disable(Feature f) {
            return this;
        }

        @Override
        public boolean isEnabled(Feature f) {
            return false;
        }

        @Override
        public int getFeatureMask() {
            return 0;
        }

        @Override
        public JsonGenerator setFeatureMask(int mask) {
            return this;
        }

        @Override
        public void writeStartArray() throws IOException {
            _toonGenerator.writeStartArray();
        }

        @Override
        public void writeEndArray() throws IOException {
            _toonGenerator.writeEndArray();
        }

        @Override
        public void writeStartObject() throws IOException {
            _toonGenerator.writeStartObject();
        }

        @Override
        public void writeEndObject() throws IOException {
            _toonGenerator.writeEndObject();
        }

        @Override
        public void writeFieldName(String name) throws IOException {
            _toonGenerator.writeFieldName(name);
        }

        @Override
        public void writeString(String text) throws IOException {
            _toonGenerator.writeString(text);
        }

        @Override
        public void writeNumber(int v) throws IOException {
            _toonGenerator.writeNumber(v);
        }

        @Override
        public void writeNumber(long v) throws IOException {
            _toonGenerator.writeNumber(v);
        }

        @Override
        public void writeNumber(double v) throws IOException {
            _toonGenerator.writeNumber(v);
        }

        @Override
        public void writeBoolean(boolean state) throws IOException {
            _toonGenerator.writeBoolean(state);
        }

        @Override
        public void writeNull() throws IOException {
            _toonGenerator.writeNull();
        }

        @Override
        public void flush() throws IOException {
            _toonGenerator.flush();
        }

        @Override
        public void close() throws IOException {
            _toonGenerator.close();
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public ObjectCodec getCodec() {
            return null;
        }

        @Override
        public void setCodec(ObjectCodec oc) {
            // Not implemented
        }
    }
}
