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

package net.sf.opendf.eclipse.plugin.editors.rules;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import net.sf.opendf.eclipse.plugin.editors.*;
import java.util.*;

// An IRule to distinguish keywords, word constants, word operators and identifiers

public class OpendfWordRule implements IRule
{

    private final static String[] keywords =
    {
    	// cal keywords
    	"action", "actor", "all",  "any", "at", "begin", "choose", "const",
		"delay", "do", "else", "end", "endaction", "endactor", "endchoose", "endforeach",
	    "endfunction", "endif", "endinitialize", "endinvariant", "endlambda", "endlet", "endpriority", "endproc", "endprocedure",
	    "endschedule", "endwhile", "ensure", "foreach", "fsm", "function", "guard", "import",
	    "in", "initialize", "invariant", "lambda", "let", "map", "multi", "mutable", "old",
	    "or", "priority", "proc", "procedure", "regexp", "repeat", "require", "schedule", "then",
	    "time", "while",
	    
	    // nl keywords
	    "network", "entities", "structure",
	    
	    // shared keywords
	    "if", "for", "var"
	};
		
	private final static String[] wordOperators =
	{
		"and", "div", "dom", "mod", "not", "rng"
	};

	private final static String[] wordConstants =
	{
		"false", "null", "true"
	};

	private IToken keywordToken;
	private IToken constantToken;
	private IToken operatorToken;
	private IToken identifierToken;

	private Map< String, IToken > map;
	
    public OpendfWordRule( OpendfColorManager colorManager )
    {
    	keywordToken =
    		new Token( new TextAttribute( colorManager.getColor( IOpendfColorDefaults.KEYWORD ) ) );
    	constantToken =
    		new Token( new TextAttribute( colorManager.getColor( IOpendfColorDefaults.CONSTANT ) ) );
    	operatorToken =
    		new Token( new TextAttribute( colorManager.getColor( IOpendfColorDefaults.OPERATOR ) ) );
    	identifierToken =
    		new Token( new TextAttribute( colorManager.getColor( IOpendfColorDefaults.IDENTIFIER ) ) );

    	map = new HashMap< String, IToken>();
    	
		int i;
			
		for( i=0; i< keywords.length; i++ )
		{
			map.put( keywords[i], keywordToken );
		}
			
		for( i=0; i< wordOperators.length; i++ )
		{
			map.put( wordOperators[i], operatorToken );
		}

		for( i=0; i< wordConstants.length; i++ )
		{
			map.put( wordConstants[i], constantToken );
		}
	}
    
    public IToken evaluate( ICharacterScanner scanner )
    {	int c;
    	StringBuffer buf = new StringBuffer();
    	IToken returnToken;

    	// Look for a valid start character
    	if( ! Character.isJavaIdentifierStart( c = scanner.read() ) )
    	{
        	scanner.unread();
    		return Token.UNDEFINED;
    	}
    	
    	buf.append( (char) c );
    	
    	// Continue consuming valid identifier characters
    	while( Character.isJavaIdentifierPart( c = scanner.read() ) )
    		buf.append( (char) c );
    	
     	scanner.unread();
     	returnToken = map.get( buf.toString() );

    	if( returnToken == null )
    	{
    		// Plain old identifier
    		return identifierToken;
    	}
    	
    	return returnToken;
    }
}
