package tools.jackson.dataformat.toon;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ObjectMapper implementation for TOON format.
 * <p>
 * This class provides a convenient API for serializing and deserializing
 * Java objects to and from TOON format.
 * <p>
 * Example usage:
 * <pre>
 * ToonMapper mapper = new ToonMapper();
 *
 * // Serialize object to TOON
 * User user = new User(123, "Alice");
 * String toon = mapper.writeValueAsString(user);
 *
 * // Deserialize TOON to object
 * User parsed = mapper.readValue(toon, User.class);
 * </pre>
 *
 * @author Claude Code
 */
public class ToonMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor that creates a ToonMapper with default settings.
     */
    public ToonMapper() {
        this(new ToonFactory());
    }

    /**
     * Constructor that accepts a custom ToonFactory.
     *
     * @param factory the ToonFactory to use
     */
    public ToonMapper(ToonFactory factory) {
        super(factory);
    }

    /**
     * Copy constructor.
     */
    protected ToonMapper(ToonMapper src) {
        super(src);
    }

    @Override
    public ToonMapper copy() {
        _checkInvalidCopy(ToonMapper.class);
        return new ToonMapper(this);
    }

    /**
     * Returns the ToonFactory used by this mapper.
     */
    @Override
    public ToonFactory getFactory() {
        return (ToonFactory) _jsonFactory;
    }

    /**
     * Builder for configuring ToonMapper instances.
     */
    public static class Builder {
        private ToonFactory _factory;
        private boolean _strictMode = false;

        public Builder() {
            this._factory = new ToonFactory();
        }

        /**
         * Enable or disable strict mode.
         */
        public Builder strictMode(boolean strict) {
            this._strictMode = strict;
            return this;
        }

        /**
         * Build the ToonMapper with configured settings.
         */
        public ToonMapper build() {
            _factory.setStrictMode(_strictMode);
            return new ToonMapper(_factory);
        }
    }

    /**
     * Creates a new builder for configuring a ToonMapper.
     */
    public static Builder builder() {
        return new Builder();
    }
}
