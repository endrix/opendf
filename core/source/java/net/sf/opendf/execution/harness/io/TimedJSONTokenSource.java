package net.sf.opendf.execution.harness.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Map;

import net.sf.opendf.execution.harness.Token;
import net.sf.opendf.execution.harness.TokenSource;
import net.sf.opendf.util.json.JSONLib;

import static net.sf.opendf.execution.transport.PacketConstants.*;

/**
 * This token source reads a sequence of JSON objects of the form
 *     { ":time" : <time>, ":data" : <value> }
 * Additional fields may be present, so in particular the output streams of ports
 * are suitable input for this source.
 * 
 * @author jwj
 *
 */
public class TimedJSONTokenSource implements TokenSource {
	

	@Override
	public void close() throws IOException {
		reader.close();
	}

	@Override
	public boolean hasToken() throws IOException {
		if (eos)
			return false;
		if (bufferFull)
			return true;
		
		buffer = null;
		Object v = JSONLib.read(reader);
		if (v instanceof Map) {
			Map m = (Map)v;
			double tm = ((Number)m.get(fieldTime)).doubleValue();
			Object data = m.get(fieldData);
			buffer = new Token(tm, data);
		}
		bufferFull = true;
		eos = (buffer == null);
		
		return !eos;
	}

	@Override
	public double nextInputTime(double currentTime) throws IOException {
		if (!hasToken())
			return currentTime;  // value does not matter
		
		assert bufferFull && (buffer != null);
		
		return buffer.getTime();
	}

	@Override
	public Token nextToken(double currentTime) throws IOException {
		if (!hasToken())
			throw new  IOException("Cannot read past end of stream.");

		assert bufferFull && (buffer != null);
		
		Token b = buffer;
		bufferFull = false;
		buffer = null;
		return b;
	}

	public TimedJSONTokenSource(InputStream s) {
		reader = new PushbackReader(new InputStreamReader(s));
	}
	
	private Token 		buffer;
	private boolean		bufferFull;
	private boolean 	eos;
	
	private PushbackReader reader;
	
	static public class Factory implements TokenSource.Factory {

		@Override
		public TokenSource create(InputStream s) {
			return new TimedJSONTokenSource(s);
		}
		
	}

}
