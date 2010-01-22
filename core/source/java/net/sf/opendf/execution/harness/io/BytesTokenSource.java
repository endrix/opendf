package net.sf.opendf.execution.harness.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;

import net.sf.opendf.execution.harness.Token;
import net.sf.opendf.execution.harness.TokenSource;

public class BytesTokenSource implements TokenSource {
	

	@Override
	public void close() throws IOException {
		s.close();
	}

	@Override
	public boolean hasToken() throws IOException {
		if (eos)
			return false;
		if (bufferFull)
			return true;

		buffer = s.read();
		bufferFull = true;
		eos = (buffer < 0);
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
		assert bufferFull;
		
		bufferFull = false;
		return new Token(currentTime, Integer.valueOf(buffer));
	}

	public BytesTokenSource(InputStream s) {
		this.s = s;
		bufferFull = false;
		eos = false;
	}
	
	private boolean bufferFull;
	private int buffer;
	private boolean eos;	
	private InputStream s;
	
	static public class Factory implements TokenSource.Factory {

		@Override
		public TokenSource create(InputStream s) {
			return new BytesTokenSource(s);
		}
		
	}

}
