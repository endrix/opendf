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



package net.sf.caltrop.hades.des;

/**
*   This interface extends DiscreteEventEntity by connector functionality. DECs can thus be
*   connected to other DECs and be scheduled relative to them.
*
*   The I/O signature of a DEC is a mapping from strings to input and output connectors, provided
*   by InputConnectors and OutputConnectors objects, respectively.
*
*   Additionally, a DEC may support a naming convention that allows a dedicated class (the
*   ComponentDescriptor) to extract its signature without instantiating a DEC (which in general will be
*   possible only _after_ extracting informations about the parameters etc).
*
*   @see    DiscreteEventEntity
*   @see    InputConnectors
*   @see    OutputConnectors
*/

public interface DiscreteEventComponent extends DiscreteEventEntity {

  public InputConnectors getInputConnectors();

  public OutputConnectors getOutputConnectors();

  /**
   *	disconnects the DEC from all currently registered inputs and outputs
   */
  public void disconnect();

  public String getName();
  
  public void  setName(String name);
}





