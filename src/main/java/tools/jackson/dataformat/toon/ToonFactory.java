package tools.jackson.dataformat.toon;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.type.ResolvedType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.VersionUtil;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;

/**
 * Factory for creating TOON format parsers and generators.
 */
public class ToonFactory extends JsonFactory {

    private static final long serialVersionUID = 1L;

    public static final String FORMAT_NAME = "TOON";

    protected boolean _strictMode = false;

    public ToonFactory() {
        super();
    }

    protected ToonFactory(ToonFactory src) {
        super(src);
        this._strictMode = src._strictMode;
    }

    @Override
    public ToonFactory copy() {
        return new ToonFactory(this);
    }

    @Override
    public JsonFactoryBuilder rebuild() {
        throw new UnsupportedOperationException("ToonFactory does not support rebuild()");
    }

    @Override
    public ToonFactory snapshot() {
        return this;
    }

    @Override
    public String getFormatName() {
        return FORMAT_NAME;
    }

    @Override
    public Version version() {
        return VersionUtil.versionFor(getClass());
    }

    public ToonFactory setStrictMode(boolean strict) {
        this._strictMode = strict;
        return this;
    }

    public boolean isStrictMode() {
        return _strictMode;
    }

    // ========================================================================
    // Protected factory methods - called by TokenStreamFactory convenience API
    // ========================================================================

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt, InputStream in)
            throws JacksonException {
        try {
            ToonParser tp = new ToonParser(
                    new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8),
                    2, _strictMode, readCtxt.streamReadConstraints());
            return new ToonParserAdapter(tp, readCtxt);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt, Reader r)
            throws JacksonException {
        try {
            ToonParser tp = new ToonParser(r, 2, _strictMode, readCtxt.streamReadConstraints());
            return new ToonParserAdapter(tp, readCtxt);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len) throws JacksonException {
        try {
            String content = new String(data, offset, len, java.nio.charset.StandardCharsets.UTF_8);
            ToonParser tp = new ToonParser(new StringReader(content), 2, _strictMode,
                    readCtxt.streamReadConstraints());
            return new ToonParserAdapter(tp, readCtxt);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len, boolean recyclable) throws JacksonException {
        try {
            ToonParser tp = new ToonParser(new CharArrayReader(data, offset, len), 2, _strictMode,
                    readCtxt.streamReadConstraints());
            return new ToonParserAdapter(tp, readCtxt);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt, DataInput input)
            throws JacksonException {
        throw new UnsupportedOperationException("ToonFactory does not support DataInput");
    }

    @Override
    protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt, Writer w)
            throws JacksonException {
        return new ToonGeneratorAdapter(new ToonGenerator(w), writeCtxt);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt, IOContext ioCtxt, OutputStream out)
            throws JacksonException {
        return new ToonGeneratorAdapter(
                new ToonGenerator(new OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8)),
                writeCtxt);
    }

    // ========================================================================
    // Inner class: ToonParserAdapter
    // ========================================================================

    static class ToonParserAdapter extends JsonParser {

        private final ToonParser _toonParser;
        private final ObjectReadContext _readCtxt;
        private JsonToken _currentToken;
        private ToonParser.Event _currentEvent;
        private Object _currentValue;
        private boolean _closed = false;

        ToonParserAdapter(ToonParser toonParser, ObjectReadContext readCtxt) {
            _toonParser = toonParser;
            _readCtxt = readCtxt;
        }

        @Override
        public ObjectReadContext objectReadContext() {
            return _readCtxt != null ? _readCtxt : ObjectReadContext.empty();
        }

        @Override
        public TokenStreamContext streamReadContext() {
            return null;
        }

        @Override
        public TokenStreamLocation currentTokenLocation() {
            return TokenStreamLocation.NA;
        }

        @Override
        public TokenStreamLocation currentLocation() {
            return TokenStreamLocation.NA;
        }

        @Override
        public long currentTokenCount() {
            return 0L;
        }

        @Override
        public Object streamReadInputSource() {
            return null;
        }

        @Override
        public Object currentValue() {
            return _currentValue;
        }

        @Override
        public void assignCurrentValue(Object v) {
            _currentValue = v;
        }

        @Override
        public boolean isEnabled(StreamReadFeature f) {
            return false;
        }

        @Override
        public int streamReadFeatures() {
            return 0;
        }

        @Override
        public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
            return DEFAULT_READ_CAPABILITIES;
        }

        @Override
        public StreamReadConstraints streamReadConstraints() {
            return StreamReadConstraints.defaults();
        }

        @Override
        public void finishToken() throws JacksonException {
            // no-op
        }

        @Override
        public Version version() {
            return VersionUtil.versionFor(getClass());
        }

        @Override
        public JsonToken nextToken() throws JacksonException {
            try {
                _currentEvent = _toonParser.nextEvent();
                _currentToken = convertEvent(_currentEvent);
                return _currentToken;
            } catch (IOException e) {
                throw new StreamReadException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonToken nextValue() throws JacksonException {
            JsonToken t = nextToken();
            if (t == JsonToken.PROPERTY_NAME) {
                t = nextToken();
            }
            return t;
        }

        private static JsonToken convertEvent(ToonParser.Event event) {
            switch (event) {
                case START_OBJECT: return JsonToken.START_OBJECT;
                case END_OBJECT: return JsonToken.END_OBJECT;
                case START_ARRAY: return JsonToken.START_ARRAY;
                case END_ARRAY: return JsonToken.END_ARRAY;
                case FIELD_NAME: return JsonToken.PROPERTY_NAME;
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
        public JsonParser skipChildren() throws JacksonException {
            if (_currentToken == JsonToken.START_OBJECT || _currentToken == JsonToken.START_ARRAY) {
                int depth = 1;
                while (depth > 0) {
                    JsonToken t = nextToken();
                    if (t == null) break;
                    if (t.isStructStart()) depth++;
                    else if (t.isStructEnd()) depth--;
                }
            }
            return this;
        }

        @Override
        public String nextName() throws JacksonException {
            JsonToken t = nextToken();
            if (t == JsonToken.PROPERTY_NAME) {
                return currentName();
            }
            return null;
        }

        @Override
        public boolean nextName(SerializableString str) throws JacksonException {
            String name = nextName();
            return name != null && name.equals(str.getValue());
        }

        @Override
        public int nextNameMatch(PropertyNameMatcher matcher) throws JacksonException {
            String name = nextName();
            if (name == null) return PropertyNameMatcher.MATCH_ODD_TOKEN;
            int index = matcher.matchName(name);
            return index >= 0 ? index : PropertyNameMatcher.MATCH_UNKNOWN_NAME;
        }

        @Override
        public int currentNameMatch(PropertyNameMatcher matcher) {
            String name = currentName();
            if (name == null) return PropertyNameMatcher.MATCH_ODD_TOKEN;
            int index = matcher.matchName(name);
            return index >= 0 ? index : PropertyNameMatcher.MATCH_UNKNOWN_NAME;
        }

        @Override
        public JsonToken currentToken() {
            return _currentToken;
        }

        @Override
        public int currentTokenId() {
            return _currentToken == null ? JsonTokenId.ID_NO_TOKEN : _currentToken.id();
        }

        @Override
        public boolean hasCurrentToken() {
            return _currentToken != null;
        }

        @Override
        public boolean hasTokenId(int id) {
            return _currentToken != null && _currentToken.id() == id;
        }

        @Override
        public boolean hasToken(JsonToken t) {
            return _currentToken == t;
        }

        @Override
        public void clearCurrentToken() {
            _currentToken = null;
        }

        @Override
        public JsonToken getLastClearedToken() {
            return null;
        }

        @Override
        public String currentName() {
            return _toonParser.getTextValue();
        }

        @Override
        public String getString() throws JacksonException {
            return _toonParser.getTextValue();
        }

        @Override
        public int getString(Writer w) throws JacksonException {
            String s = _toonParser.getTextValue();
            if (s == null) return 0;
            try {
                w.write(s);
            } catch (IOException e) {
                throw new StreamReadException(this, e.getMessage(), e);
            }
            return s.length();
        }

        @Override
        public boolean hasStringCharacters() {
            return false;
        }

        @Override
        public char[] getStringCharacters() throws JacksonException {
            String s = getString();
            return s == null ? null : s.toCharArray();
        }

        @Override
        public int getStringLength() throws JacksonException {
            String s = getString();
            return s == null ? 0 : s.length();
        }

        @Override
        public int getStringOffset() throws JacksonException {
            return 0;
        }

        @Override
        public Number getNumberValue() throws JacksonException {
            return _toonParser.getNumberValue();
        }

        @Override
        public Number getNumberValueExact() throws JacksonException {
            return getNumberValue();
        }

        @Override
        public Object getNumberValueDeferred() throws JacksonException {
            return getNumberValue();
        }

        @Override
        public NumberType getNumberType() {
            if (_currentEvent == ToonParser.Event.VALUE_NUMBER_INT) {
                return NumberType.LONG;
            } else if (_currentEvent == ToonParser.Event.VALUE_NUMBER_FLOAT) {
                return NumberType.DOUBLE;
            }
            return null;
        }

        @Override
        public NumberTypeFP getNumberTypeFP() {
            if (_currentEvent == ToonParser.Event.VALUE_NUMBER_FLOAT) {
                return NumberTypeFP.DOUBLE64;
            }
            return NumberTypeFP.UNKNOWN;
        }

        @Override
        public byte getByteValue() throws JacksonException {
            return (byte) getIntValue();
        }

        @Override
        public short getShortValue() throws JacksonException {
            return (short) getIntValue();
        }

        @Override
        public int getIntValue() throws JacksonException {
            Number n = getNumberValue();
            return n != null ? n.intValue() : 0;
        }

        @Override
        public long getLongValue() throws JacksonException {
            Number n = getNumberValue();
            return n != null ? n.longValue() : 0L;
        }

        @Override
        public BigInteger getBigIntegerValue() throws JacksonException {
            Number n = getNumberValue();
            if (n == null) return null;
            if (n instanceof BigInteger) return (BigInteger) n;
            if (n instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) n;
                streamReadConstraints().validateBigIntegerScale(bd.scale());
                return bd.toBigInteger();
            }
            return BigInteger.valueOf(n.longValue());
        }

        @Override
        public float getFloatValue() throws JacksonException {
            Number n = getNumberValue();
            return n != null ? n.floatValue() : 0.0f;
        }

        @Override
        public double getDoubleValue() throws JacksonException {
            Number n = getNumberValue();
            return n != null ? n.doubleValue() : 0.0;
        }

        @Override
        public BigDecimal getDecimalValue() throws JacksonException {
            Number n = getNumberValue();
            if (n == null) return null;
            if (n instanceof BigDecimal) return (BigDecimal) n;
            if (n instanceof Double || n instanceof Float) return BigDecimal.valueOf(n.doubleValue());
            return BigDecimal.valueOf(n.longValue());
        }

        @Override
        public boolean getBooleanValue() throws JacksonException {
            return _currentToken == JsonToken.VALUE_TRUE;
        }

        @Override
        public boolean isExpectedStartArrayToken() {
            return _currentToken == JsonToken.START_ARRAY;
        }

        @Override
        public boolean isExpectedStartObjectToken() {
            return _currentToken == JsonToken.START_OBJECT;
        }

        @Override
        public boolean isExpectedNumberIntToken() {
            return _currentToken == JsonToken.VALUE_NUMBER_INT;
        }

        @Override
        public boolean isNaN() {
            return false;
        }

        @Override
        public boolean getValueAsBoolean(boolean defaultValue) {
            if (_currentToken == null) return defaultValue;
            switch (_currentToken) {
                case VALUE_TRUE: return true;
                case VALUE_FALSE: return false;
                case VALUE_NUMBER_INT:
                    Number n = _toonParser.getNumberValue();
                    return n != null && n.intValue() != 0;
                case VALUE_STRING:
                    String s = _toonParser.getTextValue();
                    return "true".equalsIgnoreCase(s) || "1".equals(s);
                default: return defaultValue;
            }
        }

        @Override
        public String getValueAsString(String defaultValue) {
            if (_currentToken == null) return defaultValue;
            String s = _toonParser.getTextValue();
            return s != null ? s : defaultValue;
        }

        @Override
        public byte[] getBinaryValue(Base64Variant variant) throws JacksonException {
            String s = getString();
            if (s == null) return null;
            return variant.decode(s);
        }

        @Override
        public Object getEmbeddedObject() {
            return null;
        }

        @Override
        public <T> T readValueAs(Class<T> cls) throws JacksonException {
            return objectReadContext().readValue(this, cls);
        }

        @Override
        public <T> T readValueAs(TypeReference<T> valueTypeRef) throws JacksonException {
            return objectReadContext().readValue(this, valueTypeRef);
        }

        @Override
        public <T> T readValueAs(ResolvedType type) throws JacksonException {
            return objectReadContext().readValue(this, type);
        }

        @Override
        public <T extends TreeNode> T readValueAsTree() throws JacksonException {
            return objectReadContext().readTree(this);
        }

        @Override
        public void close() {
            _closed = true;
            try {
                _toonParser.close();
            } catch (IOException e) {
                // best-effort on close
            }
        }

        @Override
        public boolean isClosed() {
            return _closed;
        }
    }

    // ========================================================================
    // Inner class: ToonGeneratorAdapter
    // ========================================================================

    static class ToonGeneratorAdapter extends JsonGenerator {

        private static final JacksonFeatureSet<StreamWriteCapability> TOON_WRITE_CAPABILITIES =
                JacksonFeatureSet.fromDefaults(StreamWriteCapability.values());

        private final ToonGenerator _toonGenerator;
        private final ObjectWriteContext _writeCtxt;
        private Object _currentValue;

        ToonGeneratorAdapter(ToonGenerator toonGenerator, ObjectWriteContext writeCtxt) {
            _toonGenerator = toonGenerator;
            _writeCtxt = writeCtxt;
        }

        @Override
        public ObjectWriteContext objectWriteContext() {
            return _writeCtxt != null ? _writeCtxt : ObjectWriteContext.empty();
        }

        @Override
        public TokenStreamContext streamWriteContext() {
            return null;
        }

        @Override
        public Object streamWriteOutputTarget() {
            return null;
        }

        @Override
        public int streamWriteOutputBuffered() {
            return -1;
        }

        @Override
        public Object currentValue() {
            return _currentValue;
        }

        @Override
        public void assignCurrentValue(Object v) {
            _currentValue = v;
        }

        @Override
        public JsonGenerator configure(StreamWriteFeature f, boolean state) {
            return this;
        }

        @Override
        public boolean isEnabled(StreamWriteFeature f) {
            return false;
        }

        @Override
        public int streamWriteFeatures() {
            return 0;
        }

        @Override
        public boolean has(StreamWriteCapability cap) {
            return TOON_WRITE_CAPABILITIES.isEnabled(cap);
        }

        @Override
        public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
            return TOON_WRITE_CAPABILITIES;
        }

        @Override
        public Version version() {
            return VersionUtil.versionFor(getClass());
        }

        @Override
        public JsonGenerator writeStartArray() throws JacksonException {
            try {
                _toonGenerator.writeStartArray();
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeStartArray(Object forValue) throws JacksonException {
            return writeStartArray();
        }

        @Override
        public JsonGenerator writeStartArray(Object forValue, int size) throws JacksonException {
            try {
                _toonGenerator.writeStartArray(size);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeEndArray() throws JacksonException {
            try {
                _toonGenerator.writeEndArray();
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeStartObject() throws JacksonException {
            try {
                _toonGenerator.writeStartObject();
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeStartObject(Object forValue) throws JacksonException {
            return writeStartObject();
        }

        @Override
        public JsonGenerator writeStartObject(Object forValue, int size) throws JacksonException {
            return writeStartObject();
        }

        @Override
        public JsonGenerator writeEndObject() throws JacksonException {
            try {
                _toonGenerator.writeEndObject();
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeName(String name) throws JacksonException {
            try {
                _toonGenerator.writeFieldName(name);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeName(SerializableString name) throws JacksonException {
            return writeName(name.getValue());
        }

        @Override
        public JsonGenerator writePropertyId(long id) throws JacksonException {
            return writeName(Long.toString(id));
        }

        @Override
        public JsonGenerator writeString(String text) throws JacksonException {
            try {
                _toonGenerator.writeString(text);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeString(SerializableString text) throws JacksonException {
            return writeString(text.getValue());
        }

        @Override
        public JsonGenerator writeString(Reader r, int len) throws JacksonException {
            try {
                char[] buf = new char[Math.max(len > 0 ? len : 1024, 64)];
                StringBuilder sb = new StringBuilder();
                int n;
                while ((n = r.read(buf)) >= 0) sb.append(buf, 0, n);
                return writeString(sb.toString());
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeString(char[] text, int offset, int len) throws JacksonException {
            return writeString(new String(text, offset, len));
        }

        @Override
        public JsonGenerator writeRawUTF8String(byte[] text, int offset, int len) throws JacksonException {
            return writeString(new String(text, offset, len, java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public JsonGenerator writeUTF8String(byte[] text, int offset, int len) throws JacksonException {
            return writeString(new String(text, offset, len, java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public JsonGenerator writeRaw(String text) throws JacksonException {
            try {
                _toonGenerator.writeRaw(text);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeRaw(String text, int offset, int len) throws JacksonException {
            return writeRaw(text.substring(offset, offset + len));
        }

        @Override
        public JsonGenerator writeRaw(SerializableString text) throws JacksonException {
            return writeRaw(text.getValue());
        }

        @Override
        public JsonGenerator writeRaw(char[] text, int offset, int len) throws JacksonException {
            return writeRaw(new String(text, offset, len));
        }

        @Override
        public JsonGenerator writeRaw(char c) throws JacksonException {
            return writeRaw(String.valueOf(c));
        }

        @Override
        public JsonGenerator writeRawValue(String text) throws JacksonException {
            return writeRaw(text);
        }

        @Override
        public JsonGenerator writeRawValue(String text, int offset, int len) throws JacksonException {
            return writeRaw(text.substring(offset, offset + len));
        }

        @Override
        public JsonGenerator writeRawValue(char[] text, int offset, int len) throws JacksonException {
            return writeRaw(new String(text, offset, len));
        }

        @Override
        public JsonGenerator writeBinary(Base64Variant variant, byte[] data, int offset, int len)
                throws JacksonException {
            byte[] chunk = (offset == 0 && len == data.length) ? data
                    : java.util.Arrays.copyOfRange(data, offset, offset + len);
            return writeString(variant.encode(chunk));
        }

        @Override
        public int writeBinary(Base64Variant variant, InputStream data, int dataLength) throws JacksonException {
            try {
                byte[] buf = data.readAllBytes();
                writeBinary(variant, buf, 0, buf.length);
                return buf.length;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeNumber(short v) throws JacksonException {
            return writeNumber((int) v);
        }

        @Override
        public JsonGenerator writeNumber(int v) throws JacksonException {
            try {
                _toonGenerator.writeNumber(v);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeNumber(long v) throws JacksonException {
            try {
                _toonGenerator.writeNumber(v);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeNumber(BigInteger v) throws JacksonException {
            if (v == null) return writeNull();
            try {
                _toonGenerator.writeNumber(v.longValue());
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeNumber(double v) throws JacksonException {
            try {
                _toonGenerator.writeNumber(v);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeNumber(float v) throws JacksonException {
            return writeNumber((double) v);
        }

        @Override
        public JsonGenerator writeNumber(BigDecimal v) throws JacksonException {
            if (v == null) return writeNull();
            return writeNumber(v.doubleValue());
        }

        @Override
        public JsonGenerator writeNumber(String encodedValue) throws JacksonException {
            try {
                if (encodedValue.contains(".") || encodedValue.contains("e") || encodedValue.contains("E")) {
                    _toonGenerator.writeNumber(Double.parseDouble(encodedValue));
                } else {
                    _toonGenerator.writeNumber(Long.parseLong(encodedValue));
                }
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            } catch (NumberFormatException e) {
                throw new StreamWriteException(this, "Invalid number: " + encodedValue, e);
            }
        }

        @Override
        public JsonGenerator writeBoolean(boolean state) throws JacksonException {
            try {
                _toonGenerator.writeBoolean(state);
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writeNull() throws JacksonException {
            try {
                _toonGenerator.writeNull();
                return this;
            } catch (IOException e) {
                throw new StreamWriteException(this, e.getMessage(), e);
            }
        }

        @Override
        public JsonGenerator writePOJO(Object value) throws JacksonException {
            if (value == null) return writeNull();
            objectWriteContext().writeValue(this, value);
            return this;
        }

        @Override
        public JsonGenerator writeTree(TreeNode rootNode) throws JacksonException {
            if (rootNode == null) return writeNull();
            objectWriteContext().writeTree(this, rootNode);
            return this;
        }

        @Override
        public void flush() {
            try {
                _toonGenerator.flush();
            } catch (IOException e) {
                // best-effort
            }
        }

        @Override
        public void close() {
            try {
                _toonGenerator.close();
            } catch (IOException e) {
                // best-effort on close
            }
        }

        @Override
        public boolean isClosed() {
            return _toonGenerator.isClosed();
        }
    }
}
