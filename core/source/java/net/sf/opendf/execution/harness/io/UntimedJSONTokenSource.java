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


public class UntimedJSONTokenSource implements TokenSource {
	

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
		
		buffer = JSONLib.read(reader);
		bufferFull = true;
		eos = (buffer == null);
		
		return !eos;
	}

	@Override
	public double nextInputTime(double currentTime) throws IOException {
		return currentTime;
	}

	@Override
	public Token nextToken(double currentTime) throws IOException {
		if (!hasToken())
			throw new  IOException("Cannot read past end of stream.");

		assert bufferFull && (buffer != null);
		
		bufferFull = false;
		Token tk = new Token(currentTime, buffer);
		buffer = null;
		return tk;
	}

	public UntimedJSONTokenSource(InputStream s) {
		reader = new PushbackReader(new InputStreamReader(s));
	}
	
	private Object 		buffer;
	private boolean		bufferFull;
	private boolean 	eos;
	
	private PushbackReader reader;
	
	static public class Factory implements TokenSource.Factory {

		@Override
		public TokenSource create(InputStream s) {
			return new UntimedJSONTokenSource(s);
		}
		
	}

}
