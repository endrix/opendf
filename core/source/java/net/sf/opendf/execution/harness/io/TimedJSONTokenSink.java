package net.sf.opendf.execution.harness.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.sf.opendf.execution.harness.Token;
import net.sf.opendf.execution.harness.TokenSink;
import net.sf.opendf.util.json.JSONLib;

import static net.sf.opendf.execution.transport.PacketConstants.*;

public class TimedJSONTokenSink implements TokenSink {

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public boolean token(Token token) throws IOException {
		Map m = new HashMap();
		m.put(fieldTime, token.getTime());
		m.put(fieldData, token.getValue());
		m.put(fieldStep, token.getStep());
		JSONLib.write(m, writer);
		return true;
	}

	public TimedJSONTokenSink(OutputStream s) {
		this.writer = new OutputStreamWriter(s);
	}
	
	private Writer	writer;
	
	static public class Factory implements TokenSink.Factory {

		@Override
		public TokenSink create(OutputStream s) {
			return new TimedJSONTokenSink(s);
		}
		
	}

}
