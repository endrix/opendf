package net.sf.opendf.execution.harness.io;

import java.io.IOException;
import java.io.InputStream;

import net.sf.opendf.execution.harness.Token;
import net.sf.opendf.execution.harness.TokenSource;

/**
 * This decorator makes any timed token source an untimed token source by effectively ignoring the
 * time stamp information in the token stream.
 * 
 * 
 * @author jwj
 *
 */
public class ForgetfulTokenSourceDecorator implements TokenSource {

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public boolean hasToken() throws IOException {
		return delegate.hasToken();
	}

	@Override
	public double nextInputTime(double currentTime) throws IOException {
		return currentTime;
	}

	@Override
	public Token nextToken(double currentTime) throws IOException {
		Token tk = delegate.nextToken(currentTime);
		return new Token(currentTime, tk.getValue());
	}

	public ForgetfulTokenSourceDecorator(TokenSource delegate) {
		this.delegate = delegate;
	}
	
	private TokenSource delegate;
	
}
