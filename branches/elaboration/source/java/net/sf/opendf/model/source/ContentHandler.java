package net.sf.opendf.model.source;

import java.io.InputStream;

public interface ContentHandler {

	Object		readSource(InputStream s);
	
	
}
