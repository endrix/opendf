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

package net.sf.caltrop.eclipse.plugin.editors.rules;

import net.sf.caltrop.eclipse.plugin.editors.*;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;

public class CALOperatorRule implements IRule
{
	
	IToken operatorToken;
	IToken separatorToken;
	
    public CALOperatorRule( CALColorManager colorManager )
    {
    	operatorToken = new Token( new TextAttribute( colorManager.getColor( ICALColorConstants.OPERATOR) ) );
    	separatorToken = new Token( new TextAttribute( colorManager.getColor( ICALColorConstants.SEPARATOR ) ) );
    }

    private static final String operators  = "=+-*/<>!@$%^&#:?~|";
    private static final String separators = ".[]{}(),;";

	public IToken evaluate( ICharacterScanner scanner )
	{ IToken token = Token.UNDEFINED;
	
	  int c = scanner.read();

	  if( operators.indexOf( c ) >= 0 )
	  { token = operatorToken;
	    while( operators.indexOf( scanner.read() ) >= 0 ) /* */;
	  }
	  else if( separators.indexOf( c ) >= 0 )
	  { token = separatorToken;
	    while( separators.indexOf( scanner.read() ) >= 0 ) /* */;
	  }

	  scanner.unread();
      return token;
	}  
}

