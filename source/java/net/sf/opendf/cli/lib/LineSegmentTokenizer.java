/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
*/

package net.sf.opendf.cli.lib;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.StringTokenizer;

import net.sf.opendf.util.logging.Logging;

/**
 * The LineSegmentTokenizer consumes characters from an inputstream
 * until an EOL or EOF is found.  The sequence of characters is then
 * returned as a series of tokens.  The sequence is broken into tokens
 * based on a sequence of delimiters.  Each delimiter is matched one
 * time until the last delimiter.  The last delimiter is used
 * repeatedly to tokenize the remaining segment of the input line.
 * 
 * <p>C and C++ style comments may be recognized and skipped based on
 * configurable flags.
 * <p>EOL and EOF may be returned as tokens based on a configurable
 * flag. 
 *
 * <p>Created: Thu Jan 11 10:31:54 2007
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class LineSegmentTokenizer
{
    /** The line reader */
    private BufferedReader reader;

    // States identifying what is being currently parsed based on
    // what we have seen so far.
    private static final int ACTIVE = 1; // Normal processing
    /** Reader has no more data so we are done parsing */
    private static final int EOF = 2;
    /** Seeen a C++ comment open, waiting on close */
    private static final int IN_SLASH_STAR_COMMENT = 3;

    /** The current search state.  One of the above values. */
    private int searchState = ACTIVE;

    /** The array of delimiters.  Each is matched once starting with
     * index 0.  The last element in the array is matched n-many times*/
    private String[] delimiterSets;
    // Index into delimiterSets array
    private int delimiterIndex = 0;
    // The actual tokenizer being used to chunk up the line
    private StringTokenizer tokenizer;

    // Flags to control when/if EOL is sent.
    private boolean eolIsToken = false;
    private boolean needToSendEOL = false;

    // Flags for whether to parse comments.
    private boolean hideSlashSlashComments = true;
    private boolean hideSlashStarComments = true;

    /**
     * Create a new LineSegmentTokenizer whose input is the {@link
     * Reader} r and which uses the specified delimiters.
     *
     * @param r a non-null Reader.
     * @param an non-null array of String delimiters, may be 0 length
     * in which case the empty string will be the only delimiter.
     */
    public LineSegmentTokenizer (Reader r, String[] delims)
    {
        if (delims == null)
            throw new IllegalArgumentException("Delimiter array to LineSegmentTokenizer must be non null");

        if (r == null)
            throw new IllegalArgumentException("Reader to LineSegmentTokenizer cannot be null");

        if (r instanceof BufferedReader)
            reader = (BufferedReader)r;
        else
            reader = new BufferedReader(r);
        
        this.delimiterSets = (delims.length > 0) ? delims:new String[]{""};
        this.delimiterIndex = 0;

        // Set the tokenizer to have no tokens initially to force load
        // on first call.
        this.tokenizer = new StringTokenizer("");

        this.searchState = ACTIVE;
    }

    /**
     * Set to true to allow parser to skip ovver all C-style
     * comments.  Setting to false treats the C-style comment
     * characters to be ignored and returned as ordinary characters in
     * the stream.
     */
    public void slashSlashComments (boolean value)
    {
        this.hideSlashSlashComments = value;
    }
    
    /**
     * Set to true to allow parser to skip ovver all C++-style
     * comments.  Setting to false treats the C++-style comment
     * characters to be ignored and returned as ordinary characters in
     * the stream.
     */
    public void slashStarComments (boolean value)
    {
        this.hideSlashStarComments = value;
    }

    /**
     * Set to true for the tokenizer to return the EOL character
     * (&#92;n) when it is encountered in the stream.
     */
    public void setEOLIsToken (boolean value)
    {
        this.eolIsToken = value;
    }

    /**
     * Returns true if there are more tokens available in the stream,
     * ie calling {@link #nextToken} will return a valid token.
     */
    public boolean hasMoreTokens ()
    {
        if (this.tokenizer.hasMoreTokens())
            return true;

        if (searchState == EOF)
            return false;

        // Out of tokens.  Try to get more
        getMoreTokens();

        // Once we've attempted to get more tokens the state of the
        // tokenizer is what we've got.
        return this.tokenizer.hasMoreTokens();
    }

    /**
     * Returns the next valid token if {@link #hasMoreTokens} returns
     * true, or <code>null</code> otherwise.
     */
    public String nextToken ()
    {
        String nextToken;
        if (this.needToSendEOL)
        {
            nextToken = "\n";
            this.needToSendEOL = false;
        }
        else
        {
            if (!hasMoreTokens())
            {
                return null;
            }
            
            final String delimiters = delimiterSets[delimiterIndex];
            if (delimiterIndex < (delimiterSets.length-1))
            {
                delimiterIndex += 1;
            }
            
            nextToken = this.tokenizer.nextToken(delimiters);
            if (!this.tokenizer.hasMoreTokens())
            {
                this.delimiterIndex = 0;
                if (this.eolIsToken)
                    this.needToSendEOL = true;
            }
        }

        return nextToken;
    }

    /**
     * Obtains lines of data from the Reader, removing any comments as
     * determined by the flags, and sets up a StringTokenizer to
     * return the actual tokens.
     */
    private void getMoreTokens ()
    {
        while (searchState != EOF && !this.tokenizer.hasMoreTokens())
        {
            // Read the input stream until EOL or EOF
            String line = null;
            try
            {
                line = reader.readLine();
            } catch (IOException ioe)
            {
                Logging.user().warning("Could not read stream");
                line = null;
            }
            
            if (line == null)
            {
                searchState = EOF;
                continue;
            }

            if (this.hideSlashStarComments)
            {
                // Skip comments.
                // Does not handle C++ style comments within C++ style
                // comments. 
                if (searchState == IN_SLASH_STAR_COMMENT)
                {
                    // If the comment is not ended by this line, simply
                    // get the next line. 
                    if (line.indexOf("*/") < 0)
                        continue;
                }

                // Process C++ style comments first so they don't get
                // missed if embbedded in a C-style comment
                int openIndex = line.indexOf("/*");
                int closeIndex = line.indexOf("*/");
                while ((openIndex >= 0) && (closeIndex >= 0)) // allow for multiples
                {
                    // End of comment is in this line, strip out commented
                    // chunk and keep going
                    String pre = (openIndex > 0) ? line.substring(0,openIndex):"";
                    // + 2 b/c the closing char is a 2 char element!
                    String post = (closeIndex+2) < line.length() ? line.substring(closeIndex+2,line.length()):"";
                    line = pre + post;
                    openIndex = line.indexOf("/*");
                    closeIndex = line.indexOf("*/");
                }

                // At this point it is possible that openIndex or
                // closeIndex are >= 0 but NOT both.
            
                // If whole line is comment, get the next line
                if (openIndex == 0)
                {
                    searchState = IN_SLASH_STAR_COMMENT;
                    continue;
                }

                // Strip off trailing C++-style comment
                if (openIndex > 0)
                {
                    line = line.substring(0, openIndex);
                    searchState = IN_SLASH_STAR_COMMENT;
                }

                // Check for trailing valid line data
                if ((searchState == IN_SLASH_STAR_COMMENT) && closeIndex >= 0)
                {
                    line = line.substring(closeIndex+2, line.length());
                    searchState = ACTIVE;
                }
            }

            if (this.hideSlashSlashComments)
            {
                // Strip any trailing C-style comments
                int commentIndex = line.indexOf("//");
                if (commentIndex >= 0)
                {
                    if (commentIndex == 0)
                        continue; // Whole line is comment, iterate again
                    line = line.substring(0, commentIndex);
                }
            }
            

            // Match each delimiter set one time
            this.tokenizer = new StringTokenizer(line);
        }
    }


    /**
     * Just for testing
     */
    public static void main (String args[])
    {
        String testfilename = args[0];
        Reader rdr = null;
        try
        {
            rdr = new BufferedReader(new InputStreamReader(new FileInputStream(testfilename)));
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Could not find " + testfilename);
        }
        
        
        String delims[] = {" \t\n\r\f"," \t\n\r\f",""};
        
        LineSegmentTokenizer lst = new LineSegmentTokenizer(rdr, delims);
        lst.setEOLIsToken(true);
        lst.slashSlashComments(false);
        lst.slashStarComments(false);
        while (lst.hasMoreTokens())
        {
            System.out.print("."+lst.nextToken()+".");
        }
        
        System.out.println();

        System.out.println("========================");
        
        // Try again with different delimiters (more than are in a
        // given line)
        try
        {
            rdr = new BufferedReader(new InputStreamReader(new FileInputStream(testfilename)));
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("Could not find " + testfilename);
        }

        
        delims = new String[]{"A","m","e",""};
        
        lst = new LineSegmentTokenizer(rdr, delims);
        lst.setEOLIsToken(true);
        while (lst.hasMoreTokens())
        {
            System.out.print("."+lst.nextToken()+".");
        }
        
        System.out.println();
    }
    
}
