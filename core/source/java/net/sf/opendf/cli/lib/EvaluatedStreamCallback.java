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


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.interpreter.ExprEvaluator;
import net.sf.opendf.cal.interpreter.environment.Environment;
import net.sf.opendf.cal.interpreter.util.Platform;
import net.sf.opendf.cal.util.SourceReader;
import net.sf.opendf.hades.simulation.IOCallback;
import net.sf.opendf.util.logging.Logging;

/**
 *
 * <p>Created: Thu Jan 11 10:31:54 2007
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class EvaluatedStreamCallback extends IOCallback
{

    /**
     * The platform which is used in evaluation of the expressions.
     * This defines the context in which they are evaluated and
     * consequently the type of object returned from evaluation.
     */
    private Platform platform;
    /**
     * The environment in which the token is going to be evaluated.
     * The environment determines what the resulting type (class) of
     * the evaluated expression is going to be.  Derived during
     * construction from the platform as a global environment.
     */
    private Environment evalEnvironment;

    /** The tokenizer used to parse the input stream */
    private LineSegmentTokenizer lsTokenizer;
    
    /**
     * Build a new EvaluatedStreamCallback
     */
    public EvaluatedStreamCallback (InputStream inStream, OutputStream outStream, Platform plat)
    {
        super(new OutputStreamWriter(outStream));

        if (plat == null)
            throw new IllegalArgumentException("Evaluation platform must be non-null");
        
        this.platform = plat;
        this.evalEnvironment = this.platform.createGlobalEnvironment();

        // Define our own custom tokenizer.  The first 2 tokens are
        // white-space delimited.  The 3rd token is 'everything else.
        String delims[] = {" \t\n\r\f", " \t\n\r\f", ""};
        
        this.lsTokenizer = new LineSegmentTokenizer(new InputStreamReader(inStream), delims);
        // Return the EOL at the end of each line so that we can use
        // it for verification below.
        this.lsTokenizer.setEOLIsToken(true);

        // Prime the callback
        setHasNextToken(true);
        readNextInput();
    }

    /**
     * Overides the superclass to evaluate the read tokens.
     */
    protected void readNextInput()
    {
        // If a prior invocation failed to parse a next token then
        // there must not be any additional tokens (failure occurs at
        // EOF).  Fail-fast.
        if (!hasNextToken())
            return;

        setHasNextToken(false);

	    if (!this.lsTokenizer.hasMoreTokens())
            return;

        // The format of each line is <connection> <time> <value expression>
        final String conn = lsTokenizer.nextToken();
        final String timeStr = lsTokenizer.nextToken();
        final String stringExpr = lsTokenizer.nextToken();
        final String checkEOL = lsTokenizer.nextToken();

        // Check that our delimiters (set in constructor) parse each
        // line into exactly 3 pieces.  Otherwise it is an internal
        // programming error (since our delimiters are 2 fields plus
        // everything else).
        if (!checkEOL.equals("\n")) throw new RuntimeException("Illegal stimulus format exception");
        
        double time = -1;
        try {
            time = Double.parseDouble(timeStr);
        }catch (NumberFormatException nfe){
            Logging.user().warning("Illegal time field in stimulus data: \'" + timeStr + "\'");
            throw nfe;
        }
        
        Logging.dbg().finer("Stimuli input: " + conn + " " + time + " \'" + stringExpr + "\'");
        
        Expression expr = null;
        try
        {
            expr = SourceReader.readExpr(stringExpr);
        }
        catch (Exception e)
        { 
            Logging.user().warning("Could not parse expression: \'" + stringExpr);
            // By returning here we do not switch hasNextToken to true
            // which will effectively end parsing of the stream.
            return;
        }

        Object evaluatedValue = null;
    	ExprEvaluator eval = new ExprEvaluator(this.platform.context(), this.evalEnvironment);
        try
        {
            evaluatedValue = eval.evaluate(expr);
        }
        catch (Exception e)
        {
            // By returning here we do not switch hasNextToken to true
            // which will effectively end parsing of the stream.
            Logging.user().warning("Could not evaluate expression: \'" + stringExpr);
            Logging.dbg().throwing("EvaluatedStreamCallback", "readNextInput", e);
            return;
        }
        
        Logging.user().finer("evaluated stimulus: " + conn + " " + time + " " + evaluatedValue);
        setNextTuple(conn, time, evaluatedValue);
        
        setHasNextToken(true);
    }
    
}


