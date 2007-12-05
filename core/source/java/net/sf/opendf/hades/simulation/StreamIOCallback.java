/* 
BEGINCOPYRIGHT X,ETH
	
	Copyright (c) 1999, Computer Engineering and Communication Networks Lab (TIK)
 	                    Swiss Federal Institute of Technology (ETH) Zurich, Switzerland	
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
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
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


package net.sf.caltrop.hades.simulation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;

public class StreamIOCallback extends IOCallback
{
    private StreamTokenizer	inputTokenizer;
    
    //
    // aux
    //
    protected void    readNextInput()
    {
        if (!hasNextToken())
            return;
	    
        setHasNextToken(false);

        String conn = null;
        double time = -1;
        Object value = null;
        
        try
        {
            switch (this.inputTokenizer.nextToken()) {
                case StreamTokenizer.TT_EOF:
                    return;
                case StreamTokenizer.TT_WORD:
                    conn = this.inputTokenizer.sval;
                    break;
                default:
                    return;
            }
            
            switch (this.inputTokenizer.nextToken()) {
                case StreamTokenizer.TT_EOF:
                    return;
                case StreamTokenizer.TT_NUMBER:
                    time = this.inputTokenizer.nval;
                    break;
                default:
                    return;
            }
            
            switch (this.inputTokenizer.nextToken()) {
                case StreamTokenizer.TT_EOF:
                    return;
                case StreamTokenizer.TT_NUMBER:
                    value = new Double(this.inputTokenizer.nval);
                    break;
                case StreamTokenizer.TT_WORD:
                    value = this.inputTokenizer.sval;
                    break;
                default:
                    return;
            }
        }
        catch (IOException ioe)
        {
            // Stream reading failed.  Return prior to setting the
            // 'hasNextToken' flag which effectively ends stream
            // processing. 
            return;
        }
        
        setNextTuple(conn, time, value);
        setHasNextToken(true);

        try
        {
            do {
                this.inputTokenizer.nextToken();
            }
            while (this.inputTokenizer.ttype != StreamTokenizer.TT_EOF && this.inputTokenizer.ttype != StreamTokenizer.TT_EOL);
        }
        catch (IOException ioe)
        {
            // OK to mask errror here.  The next call to this method
            // will fail on the first nextToken() call and leave the
            // system in the appropriate state.  The tuple we just
            // obtained is still valid.
        }
    }
    
    
    //
    // ctor
    //
    
    public StreamIOCallback(Reader inReader, Writer outWriter)
    {
        super(outWriter);
        
        if (inReader != null)
        {
            this.inputTokenizer = new StreamTokenizer(inReader);
            this.inputTokenizer.eolIsSignificant(true);

            // primes hasNextToken which is needed by readNextInput
            setHasNextToken(true);
        
            readNextInput();
        } else {
            setHasNextToken(false);
            this.inputTokenizer = null;
        }
    }

    public StreamIOCallback(InputStream inStream, OutputStream outStream) {
        this(new InputStreamReader(inStream), new OutputStreamWriter(outStream));
    }

}



