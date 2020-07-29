package it.mscuttari.kaoldb.processor;

public class StringUtils {

    private StringUtils() {

    }

    /**
     * Escape the string representation of an object.
     *
     * @param obj   object whose string representation should be escaped
     * @return escaped string (<code>null</code> if <code>obj</code> is <code>null</code>)
     */
    public static String escape(Object obj) {
        if (obj == null) {
            return null;
        }

        return escape(String.valueOf(obj));
    }

    /**
     * Escape a string.
     * <p>Every <code>"</code> character is replaced with <code>""</code> and the whole string is
     * wrapped in double quotes.</p>
     *
     * @param str   string to be escaped
     * @return escaped string (<code>null</code> if <code>str</code> is <code>null</code>)
     */
    public static String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append('"');

        int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            if (c == '"') {
                sb.append('"');
            }

            sb.append(c);
        }

        sb.append('"');

        return sb.toString();
    }


    /**
     * Convert a collection to a string representation where each element is separated by a given string.
     * <p>A custom object-to-string converter, implementing {@link StringConverter}, can be specified
     * in order to get a define a temporary {@link Object#toString()} method. If not specified, the
     * default {@link Object#toString()} implementation of each object is used.<br>
     * If the separator is set to <code>null</code>, a comma is placed between the elements.</p>
     *
     * @param objs      objects
     * @param converter converter to be used to get the string representation of each object
     * @param separator separator to be used between the elements
     * @param <T>       objects type
     *
     * @return string representation
     */
    public static <T> String implode(Iterable<T> objs,
                                     StringConverter<T> converter,
                                     String separator) {

        if (objs == null)
            return "";

        if (converter == null)
            converter = Object::toString;

        if (separator == null)
            separator = ",";

        StringBuilder sb = new StringBuilder();
        String sep = "";

        for (T obj : objs) {
            sb.append(sep).append(converter.convert(obj));
            sep = separator;
        }

        return sb.toString();
    }

    /**
     * Interface to be used to create a custom object-to-string converter.
     *
     * @param <T> object type
     */
    public interface StringConverter<T> {

        /**
         * Called then an object has to be converted to string.
         *
         * @param obj   object to be converted
         * @return string conversion
         */
        String convert(T obj);
    }

}
