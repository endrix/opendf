package net.sf.opendf.execution.harness;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public interface TokenSink {
	
	void  close() throws IOException;
	
	/**
	 * Write the next token to this sink. If this sink checks token values for acceptability,
	 * the return value specifies whether this token was acceptable. Otherwise, it alwasy returns
	 * true.
	 * 
	 * @param token The next token.
	 * @return True if token value is acceptable, false if it is considered to be in error.
	 */

	boolean  token(Token token) throws IOException;
	
	
	interface Factory {
		TokenSink  create(OutputStream s);
	}
	
}
