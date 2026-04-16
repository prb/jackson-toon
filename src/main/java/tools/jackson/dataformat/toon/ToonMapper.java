package tools.jackson.dataformat.toon;

import tools.jackson.databind.ObjectMapper;

/**
 * ObjectMapper implementation for TOON format.
 */
public class ToonMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    public ToonMapper() {
        this(new ToonFactory());
    }

    public ToonMapper(ToonFactory factory) {
        super(factory);
    }

    public ToonFactory getFactory() {
        return (ToonFactory) _streamFactory;
    }

    public static class Builder {
        private final ToonFactory _factory;
        private boolean _strictMode = false;

        public Builder() {
            _factory = new ToonFactory();
        }

        public Builder strictMode(boolean strict) {
            _strictMode = strict;
            return this;
        }

        public ToonMapper build() {
            _factory.setStrictMode(_strictMode);
            return new ToonMapper(_factory);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
