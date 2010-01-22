package net.sf.opendf.execution.harness.io;

import java.io.IOException;

import net.sf.opendf.execution.harness.Token;
import net.sf.opendf.execution.harness.TokenSink;
import net.sf.opendf.execution.harness.TokenSource;

public class ReferenceOutputDecorator implements TokenSink {

	@Override
	public void close() throws IOException {
		sink.close();
		reference.close();
	}

	@Override
	public boolean token(Token token) throws IOException {
		if (!sink.token(token))
			return false;
		
		if (reference.hasToken()) {
			Token ref = reference.nextToken(token.getTime());
			return (token.getTime() == ref.getTime()) && sameValue(token.getValue(), ref.getValue());
		} else {
			return !strict;    // signal an error if strict, since output sequence is too long
		}
	}

	/**
	 * Decorates a token sink with functionality to test the token stream against a reference output.
	 * If strict, it will signal an error if the output sequence is longer than the reference.
	 * 
	 * @param sink
	 * @param reference
	 * @param strict
	 */

	public ReferenceOutputDecorator (TokenSink sink, TokenSource reference, boolean strict) {
		this.sink = sink;
		this.reference = reference;
		this.strict = strict;
	}

	/**
	 * Constructs a non-strict decorator.
	 * 
	 * @param sink
	 * @param reference
	 */
	public ReferenceOutputDecorator(TokenSink sink, TokenSource reference) {
		this (sink, reference, false);
	}
	
	private TokenSink 		sink;
	private TokenSource		reference;
	private boolean 		strict;
	
	private boolean sameValue(Object a, Object b) {
		return  (a == null) ? b == null : a.equals(b);  // possibly need to account for structures other than lists and maps
		
	}
}
