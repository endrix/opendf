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

package net.sf.opendf.hades.simulation;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;

import net.sf.opendf.hades.des.AbstractMessageListener;
import net.sf.opendf.hades.des.DiscreteEventComponent;
import net.sf.opendf.hades.des.InputConnectors;
import net.sf.opendf.hades.des.MessageEvent;
import net.sf.opendf.hades.des.MessageListener;
import net.sf.opendf.hades.des.MessageProducer;
import net.sf.opendf.hades.des.OutputConnectors;

public abstract class IOCallback implements SequentialSimulatorCallback
{
    private PrintWriter		outputWriter;
    
    private InputConnectors	inputConnectors;
    
    private boolean hasNextToken = false;
    private double  nextInputTime = Double.POSITIVE_INFINITY;
    private String  nextInputConnector = null;
    private Object  nextInputValue = null;

    private class OutputListener extends AbstractMessageListener {
	
        private String		name;
	
        public void message(MessageEvent e) {
            if (outputWriter != null)
                outputWriter.println(name + "\t" + e.time + "\t" + e.value);
        }
	
        public OutputListener(String name) {
            this.name = name;
        }
    }


    //
    // SSCB
    //

    public void	    connect(DiscreteEventComponent dec) {
        inputConnectors = dec.getInputConnectors();
	
        OutputConnectors oc = dec.getOutputConnectors();
        Iterator cons = oc.keySet().iterator();
        while (cons.hasNext()) {
            String nm = (String)cons.next();
            MessageProducer mp = oc.getConnector(nm);
            mp.addMessageListener(new OutputListener(nm));
        }
    }

    public boolean  hasInputBefore(double tm) {
        return hasNextToken() && nextInputTime < tm;
    }

    public void	    nextInput() {
        if (!hasNextToken())
            return;
	    
        MessageListener ml = inputConnectors.getConnector(nextInputConnector);
        if (ml != null)
            ml.message(new MessageEvent(this, nextInputTime, nextInputValue));

	
        readNextInput();
    }
    
    
    //
    // aux
    //
    protected void setHasNextToken (boolean value)
    {
        this.hasNextToken = value;
    }
    protected boolean hasNextToken ()
    {
        return this.hasNextToken;
    }
    /**
     * Set the next value tuple to be returned via the
     * {@link nextInput} method
     *
     */
    protected void setNextTuple (String conn, double time, Object value)
    {
        this.nextInputConnector = conn;
        this.nextInputValue = value;
        this.nextInputTime = time;
    }

    /**
     * Causes the next tuple to be obtained and registered via the
     * {@link #setNextTuplel} method.
     */
    protected abstract void readNextInput();
    
    //
    // ctor
    //
    public IOCallback (Writer outWriter)
    {
        if (outWriter != null)
            outputWriter = new PrintWriter(outWriter, true);
        else
            outputWriter = null;
    }
    
}



