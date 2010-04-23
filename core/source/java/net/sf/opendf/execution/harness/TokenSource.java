package net.sf.opendf.execution.harness;

import java.io.IOException;
import java.io.InputStream;

/**
 * A TokenSource translates the data in an input stream into a sequence of tokens. Depending
 * on the format of data in the stream, the tokens in the sequence may be associated with non-decreasing
 * time stamps, marking the time they are supposed to be sent as input.
 * 
 * If non time stamp information is present in the stream, it is said to be "untimed". In that case,
 * the tokens are always presented at "currentTime".
 * 
 * @author jwj
 *
 */

public interface TokenSource {
	
	/**
	 * Test whether this source contains time information attached to each token.
	 * 
	 * If it is timed, then each token in it has an  associated specific, and unalterable, time stamp.
	 * As a consequence, the nextInputTime is independent of the currentTime.
	 * 
	 * If it is untimed, the nextInputTime will usually be a function of the currentTime, and will
	 * usually be identical to it.
	 * 
	 */
	boolean isTimed();
	
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
