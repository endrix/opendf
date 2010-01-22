package net.sf.opendf.execution.harness.io;

import static net.sf.opendf.execution.transport.PacketConstants.fieldData;
import static net.sf.opendf.execution.transport.PacketConstants.fieldStep;
import static net.sf.opendf.execution.transport.PacketConstants.fieldTime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.sf.opendf.execution.harness.Token;
import net.sf.opendf.execution.harness.TokenSink;
import net.sf.opendf.util.json.JSONLib;

public class UntimedJSONTokenSink implements TokenSink {

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public boolean token(Token token) throws IOException {
		JSONLib.write(token.getValue(), writer);
		return true;
	}

	public UntimedJSONTokenSink(OutputStream s) {
		this.writer = new OutputStreamWriter(s);
	}
	
	private Writer	writer;
	
	static public class Factory implements TokenSink.Factory {

		@Override
		public TokenSink create(OutputStream s) {
			return new UntimedJSONTokenSink(s);
		}
		
	}

}
