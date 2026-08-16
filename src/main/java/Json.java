import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(String.valueOf(entry.getKey()))).append(':').append(write(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) out.append(',');
                first = false;
                out.append(write(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Cannot encode " + value.getClass().getName());
    }

    static Object read(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (parser.position != source.length()) throw new IllegalArgumentException("Trailing JSON content");
        return value;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) { this.source = source; }

        private Object value() {
            space();
            if (position >= source.length()) throw new IllegalArgumentException("Empty JSON");
            return switch (source.charAt(position)) {
                case '{' -> object();
                case '[' -> array();
                case '\"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            position++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) return result;
            do {
                space();
                String key = string();
                space();
                expect(':');
                result.put(key, value());
                space();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            position++;
            List<Object> result = new ArrayList<>();
            space();
            if (take(']')) return result;
            do {
                result.add(value());
                space();
            } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('\"');
            StringBuilder out = new StringBuilder();
            while (position < source.length()) {
                char c = source.charAt(position++);
                if (c == '\"') return out.toString();
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (position >= source.length()) throw new IllegalArgumentException("Incomplete escape");
                char escaped = source.charAt(position++);
                switch (escaped) {
                    case '\"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (position + 4 > source.length()) throw new IllegalArgumentException("Incomplete unicode escape");
                        out.append((char) Integer.parseInt(source.substring(position, position + 4), 16));
                        position += 4;
                    }
                    default -> throw new IllegalArgumentException("Unknown escape");
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Object number() {
            int start = position;
            while (position < source.length() && "-+0123456789.eE".indexOf(source.charAt(position)) >= 0) position++;
            String token = source.substring(start, position);
            try {
                return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.parseDouble(token) : Long.parseLong(token);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("Invalid JSON value", error);
            }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, position)) throw new IllegalArgumentException("Invalid JSON literal");
            position += token.length();
            return value;
        }

        private void space() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private boolean take(char expected) {
            if (position < source.length() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected " + expected + " at " + position);
        }
    }
}
