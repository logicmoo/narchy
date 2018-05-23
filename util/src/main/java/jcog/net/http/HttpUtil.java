package jcog.net.http;

import java.io.File;
import java.lang.annotation.*;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 *
 * @author Joris
 */
public final class HttpUtil
{
        private HttpUtil()
        {
        }

        
        private static boolean isCHAR(byte by)
        {
                return by >= 0/* && by <= 127*/;
        }

        
        private static boolean isUPALPHA(byte by)
        {
                return by >= 'A' && by <= 'Z';
        }

        
        private static boolean isLOALPHA(byte by)
        {
                return by >= 'a' && by <= 'z';
        }

        
        public static boolean isALPHA(byte by)
        {
                return isUPALPHA(by) || isLOALPHA(by);
        }

        
        public static boolean isDIGIT(byte by)
        {
                return by >= '0' && by <= '9';
        }

        
        public static boolean isCTL(byte by)
        {
                return (by >= 0 && by <= 34) || by == 127;
        }

        
        public static boolean isCR(byte by)
        {
                return by == '\r'; // (13)
        }

        
        public static boolean isLF(byte by)
        {
                return by == '\n'; // (10)
        }

        
        private static boolean isSP(byte by)
        {
                return by == ' ';
        }

        
        private static boolean isHT(byte by)
        {
                return by == '\t';
        }

        
        public static boolean isQ(byte by)
        {
                return by == '"';
        }
        // Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
        // Request-URI    = "*" | absoluteURI | abs_path | authority
        public static final Pattern requestLine = Pattern.compile("^([A-Z]+) ([\\x20-\\x7E]+) HTTP/1\\.(\\d+)$");
        // message-header = field-name ":" [ field-value ]
        // field-name     = token
        // field-value    = *( TEXT | LWS )
        // TEXT           = <any OCTET except CTLs, but including LWS>
        public static final Pattern headerLine = Pattern.compile("^([!#$%&'*+\\-.0-9A-Z^_`a-z|~]+):[ \t\r\n]*([\\x20-\\x7E\n\r\t]+)$");
        // Simple range header (only read the first range)
        public static final Pattern simpleRange = Pattern.compile("^bytes[ \t\r\n]*=[ \t\r\n]*(\\d*)[ \t\r\n]*-[ \t\r\n]*(\\d*)?");

        /**
         * Attempts to read a line (up to CRLF) from the current position of buf. If there is no CRLF left in the
         * ByteBuffer, this method will return false and leave the position intact. If a line is found, it is append to
         * dest and the position of the buf is moved past CRLF.
         *
         * @param dest The string builder to put the read line
         * @param buf The buffer to read from
         * @param lws If set, [CRLF] 1*( SP | HT ) is not the end of a line.
         * @return false if no line was found (CRLF)
         */
        
        public static boolean readLine(StringBuilder dest, ByteBuffer buf, boolean lws)
        {
                int crlf = lws ? findCRLFIgnoreLWS(buf, buf.position()) : findCRLF(buf, buf.position());
                if (crlf < 0)
                {
                        return false;
                }

                while (buf.position() < crlf)
                {
                        byte by = buf.get();

                        if (isCHAR(by))
                        {
                                dest.append((char) by); // ASCII
                        }
                }

                // Place the position past CRLF
                buf.get();
                buf.get();

                return true;
        }

        
        public static int findCRLF(ByteBuffer buf, int offset)
        {
                //  do not loop over the last character, so that a + 1 does not fail
                for (int a = offset; a < buf.limit() - 1; a++)
                {
                        if (isCR(buf.get(a)) && isLF(buf.get(a + 1)))
                        {
                                return a;
                        }
                }

                return -1;
        }

        
        public static int findCRLFIgnoreLWS(ByteBuffer buf, int offset)
        {
                //  LWS            = [CRLF] 1*( SP | HT )

                //  do not loop over the last character, so that a + 2 does not fail
                for (int a = offset; a < buf.limit() - 2; a++)
                {
                        if (isCR(buf.get(a)) && isLF(buf.get(a + 1)))
                        {
                                // linear white space? Has the header been split over multiple lines?
                                if (!isSP(buf.get(a + 2)) && !isHT(buf.get(a + 2)))
                                {
                                        return a;
                                }
                        }
                }

                return -1;
        }

        
        public static int findSP(ByteBuffer buf, int offset)
        {
                for (int a = offset; a < buf.limit(); a++)
                {
                        if (isSP(buf.get(a)))
                        {
                                return a;
                        }
                }

                return -1;
        }

        public enum METHOD
        {
                UNKNOWN,
                OPTIONS,
                GET, // MUST be supported
                HEAD, // MUST be supported
                POST,
                PUT,
                DELETE,
                TRACE,
                CONNECT;

                public static METHOD fromRequestLine(String method)
                {
                        if ("GET".equals(method))
                        {
                                return GET;
                        }

                        if ("HEAD".equals(method))
                        {
                                return HEAD;
                        }

                        if ("POST".equals(method))
                        {
                                return POST;
                        }

                        if ("OPTIONS".equals(method))
                        {
                                return OPTIONS;
                        }

                        if ("PUT".equals(method))
                        {
                                return PUT;
                        }

                        if ("DELETE".equals(method))
                        {
                                return DELETE;
                        }

                        if ("TRACE".equals(method))
                        {
                                return TRACE;
                        }

                        if ("CONNECT".equals(method))
                        {
                                return CONNECT;
                        }

                        return UNKNOWN;
                }
        }

        public static class HttpException extends Exception
        {
                public final int status;
                public final boolean fatal;

                public HttpException(int status, boolean fatal, String message, Throwable cause)
                {
                        super(message, cause);
                        this.status = status;
                        this.fatal = fatal;
                }

                public HttpException(int status, boolean fatal, String message)
                {
                        this(status, fatal, message, null);
                }
        }

        
        public static int binarySizeUTF8(String str)
        {
                // Java strings are UTF-16

                int bytes = 0;
                for (int a = 0; a < str.length(); ++a)
                {
                        int codePoint = str.codePointAt(a);

                        if (codePoint <= 0x7F)
                        {
                                bytes += 1;
                        }
                        else if (codePoint <= 0x7FF)
                        {
                                bytes += 2;
                        }
                        else if (codePoint <= 0xFFFF) // End of BMP
                        {
                                bytes += 3;
                        }
                        else
                        {
                                bytes += 4;
                                a++; // skip the next one, becuase it will be a low surrogate
                        }
                }

                return bytes;
        }
        public final static Charset UTF8 = Charset.forName("UTF-8");

        
        public static File findDirectoryIndex(File dir)
        {
                File ret;
                assert dir.isDirectory();

                ret = new File(dir.getPath() + File.separator + "index.html");
                if (ret.isFile())
                {
                        return ret;
                }

                ret = new File(dir.getPath() + File.separator + "index.htm");
                if (ret.isFile())
                {
                        return ret;
                }

                ret = new File(dir.getPath() + File.separator + "index.xhtml");
                if (ret.isFile())
                {
                        return ret;
                }

                ret = new File(dir.getPath() + File.separator + "index.txt");
                if (ret.isFile())
                {
                        return ret;
                }

                return null;
        }

    /**
     * Denotes that the specified method is safe to be called by multiple threads at the same time.
     * If specified on an interface / abstract method, it denotes that any implementation must be thread safe.
     * @author Joris
     */
    @Documented
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    @interface ThreadSafe
    {

    }

    /**
     * A utility class for parsing and formatting HTTP dates as used in cookies and other headers. This class handles dates
     * as defined by RFC 2616 section 3.3.1 as well as some other common non-standard formats.
     *
     *
     * @since 4.0
     */
    static final class HttpDateUtils
    {
            /**
             * Date format pattern used to parse HTTP date headers in RFC 1123 format.
             */
            private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
            /**
             * Date format pattern used to parse HTTP date headers in RFC 1036 format.
             */
            private static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
            /**
             * Date format pattern used to parse HTTP date headers in ANSI C
             * <code>asctime()</code> format.
             */
            private static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
            private static final String[] DEFAULT_PATTERNS = new String[]
            {
                    PATTERN_RFC1123,
                    PATTERN_RFC1036,
                    PATTERN_ASCTIME
            };
            private static final Date DEFAULT_TWO_DIGIT_YEAR_START;
            private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

            static
            {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeZone(GMT);
                    calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
            }

            /**
             * Parses a date value. The formats used for parsing the date value are retrieved from the default http params.
             *
             * @param dateValue the date value to parse
             *
             * @return the parsed date
             *
             * @throws DateParseException if the value could not be parsed using any of the supported date formats
             */
            public static Date parseDate(String dateValue) throws DateParseException
            {
                    return parseDate(dateValue, null, null);
            }

            /**
             * Parses the date value using the given date formats.
             *
             * @param dateValue the date value to parse
             * @param dateFormats the date formats to use
             *
             * @return the parsed date
             *
             * @throws DateParseException if none of the dataFormats could parse the dateValue
             */
            public static Date parseDate(final String dateValue, String[] dateFormats)
                    throws DateParseException
            {
                    return parseDate(dateValue, dateFormats, null);
            }

            /**
             * Parses the date value using the given date formats.
             *
             * @param dateValue the date value to parse
             * @param dateFormats the date formats to use
             * @param startDate During parsing, two digit years will be placed in the range <code>startDate</code>
             * to <code>startDate + 100 years</code>. This value may be <code>null</code>. When <code>null</code> is given
             * as a parameter, year <code>2000</code> will be used.
             *
             * @return the parsed date
             *
             * @throws DateParseException if none of the dataFormats could parse the dateValue
             */
            private static Date parseDate(
                    String dateValue,
                    String[] dateFormats,
                    Date startDate) throws DateParseException
            {

                    if (dateValue == null)
                    {
                            throw new IllegalArgumentException("dateValue is null");
                    }
                    if (dateFormats == null)
                    {
                            dateFormats = DEFAULT_PATTERNS;
                    }
                    if (startDate == null)
                    {
                            startDate = DEFAULT_TWO_DIGIT_YEAR_START;
                    }
                    // trim single quotes around date if present
                    // see issue #5279
                    if (dateValue.length() > 1
                            && dateValue.startsWith("'")
                            && dateValue.endsWith("'"))
                    {
                            dateValue = dateValue.substring(1, dateValue.length() - 1);
                    }

                    for (String dateFormat : dateFormats)
                    {
                            SimpleDateFormat dateParser = DateFormatHolder.formatFor(dateFormat);
                            dateParser.set2DigitYearStart(startDate);
                            ParsePosition pos = new ParsePosition(0);
                            Date result = dateParser.parse(dateValue, pos);
                            if (pos.getIndex() != 0)
                            {
                                    return result;
                            }
                    }

                    // we were unable to parse the date
                    throw new DateParseException("Unable to parse the date " + dateValue);
            }

            /**
             * Formats the given date according to the RFC 1123 pattern.
             *
             * @param date The date to format.
             * @return An RFC 1123 formatted date string.
             *
             * @see #PATTERN_RFC1123
             */
            public static String formatDate(Date date)
            {
                    return formatDate(date, PATTERN_RFC1123);
            }

            /**
             * Formats the given date according to the specified pattern. The pattern must conform to that used by the {@link SimpleDateFormat simple date
             * format} class.
             *
             * @param date The date to format.
             * @param pattern The pattern to use for formatting the date.
             * @return A formatted date string.
             *
             * @throws IllegalArgumentException If the given date pattern is invalid.
             *
             * @see SimpleDateFormat
             */
            private static String formatDate(Date date, String pattern)
            {
                    if (date == null)
                    {
                            throw new IllegalArgumentException("date is null");
                    }
                    if (pattern == null)
                    {
                            throw new IllegalArgumentException("pattern is null");
                    }

                    SimpleDateFormat formatter = DateFormatHolder.formatFor(pattern);
                    return formatter.format(date);
            }

            /**
             * Clears thread-local variable containing {@link DateFormat} cache.
             */
            public static void clearThreadLocal()
            {
                    DateFormatHolder.clearThreadLocal();
            }

            /**
             * This class should not be instantiated.
             */
            private HttpDateUtils()
            {
            }

            /**
             * A factory for {@link SimpleDateFormat}s. The instances are stored in a threadlocal way because
             * SimpleDateFormat is not threadsafe as noted in {@link SimpleDateFormat its javadoc}.
             *
             */
            final static class DateFormatHolder
            {
                    private static final ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>> THREADLOCAL_FORMATS = ThreadLocal.withInitial(() -> new SoftReference<>(
                            new HashMap<>()));

                    /**
                     * creates a {@link SimpleDateFormat} for the requested format string.
                     *
                     * @param pattern a non-<code>null</code> format String according to {@link SimpleDateFormat}. The
                     * format is not checked against <code>null</code> since all paths go through {@link DateUtils}.
                     * @return the requested format. This simple dateformat should not be used to
                     * {@link SimpleDateFormat#applyPattern(String) apply} to a different pattern.
                     */
                    static SimpleDateFormat formatFor(String pattern)
                    {
                            SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
                            Map<String, SimpleDateFormat> formats = ref.get();
                            if (formats == null)
                            {
                                    formats = new HashMap<>();
                                    THREADLOCAL_FORMATS.set(new SoftReference<>(formats));
                            }

                            SimpleDateFormat format = formats.get(pattern);
                            if (format == null)
                            {
                                    format = new SimpleDateFormat(pattern, Locale.US);
                                    format.setTimeZone(TimeZone.getTimeZone("GMT"));
                                    formats.put(pattern, format);
                            }

                            return format;
                    }

                    static void clearThreadLocal()
                    {
                            THREADLOCAL_FORMATS.remove();
                    }
            }

            public static class DateParseException extends Exception
            {
                    private static final long serialVersionUID = 4417696455000643370L;

                    /**
                     *
                     */
                    public DateParseException()
                    {
                            super();
                    }

                    /**
                     * @param message the exception message
                     */
                    DateParseException(String message)
                    {
                            super(message);
                    }
            }
    }
}
