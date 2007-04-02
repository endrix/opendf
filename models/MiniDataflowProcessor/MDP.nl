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

/**
	Mini Dataflow Processor
	
	This is the assembly of the various components of the MDP.
	
	@author JWJ
*/


network MDP () I, Din, Reset ==> IAddr, DAddr, Dout, MemW :


entities
	decoder = InstructionDecoder();
	rf = RegisterFile();
	alu = ALU();
	pc = PC();
	addrGen = MemoryAddressGenerator();
	cache = Cache0();
	
structure
	I --> decoder.I;
	Reset --> decoder.Reset;
	Din --> cache.MemDataOut;
	
	decoder.AluOp --> alu.Op;
	decoder.RfOp --> rf.Op;
	decoder.R1 --> rf.R1;
	decoder.R2--> rf.R2;
	decoder.RfVal --> rf.D1;
	decoder.PcOp --> pc.Op;
	decoder.PcVal --> pc.Val;
	decoder.W --> cache.W;
	decoder.Offset --> addrGen.Offset;
	
	alu.Res --> rf.D2;
	
	rf.A --> alu.A;
	rf.B --> alu.B;
	rf.PcAddr --> pc.Reg;
	rf.Test --> pc.Test;
	rf.MemBase --> addrGen.Base;
	rf.MemData --> cache.DataIn;
	
	pc.Link --> rf.D4;
	pc.Addr --> IAddr;
	
	addrGen.Addr --> cache.Addr;
	
	cache.MemDataIn --> Dout;
	cache.MemAddr --> DAddr;
	cache.MemW --> MemW;
	cache.DataOut --> rf.D3;
	
end
	
	
