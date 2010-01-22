package net.sf.opendf.util.json;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple reader/writer implementation for JSON, using the standard Java objects, i.e. Number (including 
 * BigInteger to represent unbounded size integers), Boolean, String, Collection, and Map. It represents the 
 * JSON "null" value using the Java "null", and also returns that value when it encounters the end of the
 * input stream. Consequently, it is indistinguishable from an infinite sequence of null values, which means
 * using null values as top-level (i.e. outermost) values does not work well if one relies on the return value 
 * of this reader to detect the end of the input stream. Using them inside containers is of course no problem.
 * 
 * When reading integral values, it will use and Integer or Long objects if the value fits into the respective
 * range. Only when it exceeds the Long range will a BigInteger be returned.
 * 
 * The writer supports arrays in addition to Collection objects.
 * 
 * Credits: Some of the code has been lifted from org.json.*.
 * 
 * Note on Readers and PushbackReaders: Reading JSON requires a one-character lookahead, which is realized
 * using a PushbackReader. If the Reader passed to the "read" method is a PushbackReader already, it is simply 
 * used. Otherwise, a PushbackReader is wrapped around it. For strings, lists and arrays, which terminate 
 * with defined characters, this should make no difference. However, for numbers it does. Reading the sequence
 * 			1234"null"
 * from a PushbackReader results in two entities, the number 1234 and the string "null". Reading the same 
 * sequence from a reader results in the number 1234 and the null token, with one character still to be read.
 * The reason is that the separating quote character went into the pushback buffer of the temporary 
 * PushbackReader created for the first object, and was lost when that was discarded. In order to obtain 
 * identical results in both cases, token-separating whitespace would have to be presented in the input 
 * stream:
 * 			1234 "null"
 * 
 * @author jwj
 *
 */


public class JSONLib {
	
	public static Object read (Reader r) throws IOException {
		PushbackReader r1 = (r instanceof PushbackReader) ? (PushbackReader)r : new PushbackReader(r);
		Object v = readValue(r1);
		return v;
	}
		
	private static Object readValue(PushbackReader r) throws IOException {
		
		int c = readNextNonWhitespace(r);
		
		switch (c) {
		case '\"' :
			return readString(r, '"');
		case '[':
			return readArray(r);
		case '{': 
			return readObject(r);
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
		case '-':
		case '.':
			r.unread(c);
			return readNumber(r);
		case 'n':
			return readNull(r);
		case 't':
			return readTrue(r);
		case 'f':
			return readFalse(r);			
		case -1:
			return null;
		default:
			throw new JSONException("Illegal start of token: " + (char)c);
		}
	}
	
	private static int readNextNonWhitespace(PushbackReader r) throws IOException {
		int c = r.read();
		while (c >= 0 && whitespace.indexOf(c) >= 0) {
			c = r.read();
		}
		return c;
	}

	private static final String whitespace = " \r\n\t";
	
	private static char next(PushbackReader r) throws IOException {
		int c = r.read();
		if (c < 0)
			throw new JSONException("Encountered end of stream, cannot finish reading.");
		return (char)c;
	}

	private static String next(int n, PushbackReader r) throws IOException {
		char b[] = new char [n];
		
		for (int i = 0; i < n; i++)
			b[i] = next(r);
		
		return new String(b);
	}
	
	private static char nextNonWhitespace(PushbackReader r) throws IOException {
		int c = readNextNonWhitespace(r);
		if (c < 0)
			throw new JSONException("Encountered end of stream, cannot finish reading.");
		return (char)c;
	}

	
	private static String readString(PushbackReader r, char quote) throws IOException {
        char c;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            c = next(r);
            switch (c) {
            case 0:
            case '\n':
            case '\r':
                throw new JSONException("Unterminated string.");
            case '\\':
                c = next(r);
                switch (c) {
                case 'b':
                    sb.append('\b');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'u':
                    sb.append((char)Integer.parseInt(next(4, r), 16));
                    break;
                case '"':
                case '\'':
                case '\\':
                case '/':
                	sb.append(c);
                	break;
                default:
                    throw new JSONException("Illegal escape.");
                }
                break;
            default:
                if (c == quote) {
                    return sb.toString();
                }
                sb.append(c);
            }
        }
    }
	
	private static List readArray(PushbackReader r) throws IOException {
		List v = new ArrayList();
		
		int c = nextNonWhitespace(r);
		if (c == ']')
			return v;
		
		r.unread(c);
		
		while (true) {
			Object a = readValue(r);
			v.add(a);
			c = nextNonWhitespace(r);
			switch (c) {
			case ',' :
				break;
			case ']':
				return v;
			default:
				throw new JSONException("Illegal array element separator: '" + (char)c + "' (expected ',' or ']').");
			}
		}
		
	}

	private static Map readObject(PushbackReader r) throws IOException {
		Map m = new HashMap();
		
		int c = nextNonWhitespace(r);
		if (c == '}')
			return m;
		
		r.unread(c);
		
		while (true) {
			Object k = readValue(r);
			c = nextNonWhitespace(r);
			if (c != ':')
				throw new JSONException("Illegal name/value separator: '" + (char)c + "' (expected ':').");
			Object v = readValue(r);
			m.put(k, v);
			c = nextNonWhitespace(r);
			switch (c) {
			case ',' :
				break;
			case '}':
				return m;
			default:
				throw new JSONException("Illegal object element separator: '" + (char)c + "' (expected ',' or '}').");
			}
		}	
	}
	
	private static Number  readNumber(PushbackReader r) throws IOException {
		StringBuffer sb = new StringBuffer();
		boolean isFloat = false;
		int c = r.read();
		while (c >= 0 && "0123456789eE+-.".indexOf(c) >= 0) {
			if (".eE".indexOf(c) >= 0)
				isFloat = true;
			sb.append((char)c);
			c = r.read();
		}
		r.unread(c);

		String s = sb.toString().trim();
		
		if (isFloat) {
			return Double.valueOf(s);
		} else {
			return createInteger(s);
		}		
	}
	
	private static Object readNull(PushbackReader r) throws IOException {
		if (!"ull".equals(next(3, r)))
			throw new JSONException("Error reading 'null' token.");
		return null;
	}
	
	private static Object readTrue(PushbackReader r) throws IOException {
		if (!"rue".equals(next(3, r)))
			throw new JSONException("Error reading 'true' token.");
		return Boolean.TRUE;
	}
	
	private static Object readFalse(PushbackReader r) throws IOException {
		if (!"alse".equals(next(4, r)))
			throw new JSONException("Error reading 'false' token.");
		return Boolean.FALSE;
	}
	
	
    static private Number  createInteger(String s) {
   	 	BigInteger b = new BigInteger(s);
   	 	if (isInteger(b))
   	 		return new Integer(b.intValue());
   	 
   	 	if (isLong(b))
   	 		return new Long(b.longValue());

   	 	return b;
    }

    static private boolean isInteger(BigInteger n) {
   	 	return n.compareTo(minInt) >= 0 && maxInt.compareTo(n) >= 0;
    }

    static private boolean isLong(BigInteger n) {
   	 	return n.compareTo(minLong) >= 0 && maxLong.compareTo(n) >= 0;
    }

    static private BigInteger minInt = BigInteger.valueOf(Integer.MIN_VALUE);
    static private BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
	static private BigInteger minLong = BigInteger.valueOf(Long.MIN_VALUE);
	static private BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);

	
	
	public static void write(Object v, Writer p) throws IOException {
		writeValue(v, p);
		p.flush();
	}
	
	public static void writeValue(Object v, Writer p) throws IOException {
		if (v == null) {
			p.write("null");
		} else if (v instanceof Map) {
			Map m = (Map)v;
			p.write("{");
			boolean isFirst = true;
			for (Object k : m.keySet()) {
				if (!isFirst)
					p.write(",");
				writeValue(k, p);
				p.write(":");
				writeValue(m.get(k), p);
				isFirst = false;
			}
			p.write("}");
		} else if (v instanceof Collection) {
			Collection c = (Collection)v;
			p.write("[");
			boolean isFirst = true;
			for (Object a : c) {
				if (!isFirst)
					p.write(",");
				writeValue(a, p);
				isFirst = false;
			}
			p.write("]");
		} else if (v instanceof String) {
			p.write(quote((String)v));
		} else if (v instanceof Boolean) {
			p.write(v.toString());
		} else if (v instanceof Number) {
			p.write(v.toString());
		} else if (v instanceof Object []) {
			Object [] a = (Object []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof boolean []) {
			boolean [] a = (boolean []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof byte []) {
			byte [] a = (byte []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof char []) {
			char [] a = (char []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof short []) {
			short [] a = (short []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof int []) {
			int [] a = (int []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof long []) {
			long [] a = (long []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof float []) {
			float [] a = (float []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else if (v instanceof double []) {
			double [] a = (double []) v;
			p.write("[");
			for (int i = 0; i < a.length; i++) {
				if (i > 0)
					p.write(",");
				writeValue(a[i], p);
			}
			p.write("]");
		} else {
			throw new JSONException("Cannot translate object of type " + v.getClass() + "(" + v + ").");
		}
	}
		
    public static String quote(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char         b;
        char         c = 0;
        int          i;
        int          len = string.length();
        StringBuffer sb = new StringBuffer(len + 4);
        String       t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
                if (b == '<') {
                    sb.append('\\');
                }
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') ||
                               (c >= '\u2000' && c < '\u2100')) {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
