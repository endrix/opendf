package net.sf.opendf.execution.harness;

import java.io.IOException;
import java.io.InputStream;

public interface TokenSource {
	
	/**
	 * Close this source.
	 */

	void close() throws IOException;

	/**
	 * Tests whether this source has an unread token available.
	 * 
	 * @return True if there is another token, false otherwise.
	 */

	boolean hasToken() throws IOException;

	/**
	 * Determines the time of the next input token. For untimed input streams, this is always 
	 * the current time.
	 * 
	 * @param currentTime The current time.
	 * @return The time stamp of the next token in this source.
	 */
	
	double  nextInputTime(double currentTime) throws IOException;

	/**
	 * Read the next token from this source.
	 * 
	 * @param currentTime
	 * @return
	 */

	Token  nextToken(double currentTime) throws IOException;
	
	
	interface Factory {
		TokenSource  create(InputStream s);
	}
}
