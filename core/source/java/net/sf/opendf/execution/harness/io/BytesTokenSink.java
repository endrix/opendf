package net.sf.opendf.execution.harness.io;

import java.io.IOException;
import java.io.OutputStream;

import net.sf.opendf.execution.harness.Token;
import net.sf.opendf.execution.harness.TokenSink;

public class BytesTokenSink implements TokenSink {

	@Override
	public void close() throws IOException {
		s.close();
	}

	@Override
	public boolean token(Token token) throws IOException {
		int v = ((Number)token.getValue()).byteValue();
		s.write(v);
		return true;		// FIXME
	}
	
	public BytesTokenSink(OutputStream s) {
		this.s = s;
	}
	
	private OutputStream s;
	
	static public class Factory implements TokenSink.Factory {

		@Override
		public TokenSink create(OutputStream s) {
			return new BytesTokenSink(s);
		}
		
	}

}
