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

package net.sf.caltrop.eclipse.plugin.editors.scanners;

import java.util.*;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import net.sf.caltrop.eclipse.plugin.editors.*;
import net.sf.caltrop.eclipse.plugin.editors.rules.*;

// This is the scanner that really matters. It adds color token to
// all the syntax elements in the source file.

public class CALScanner extends RuleBasedScanner
{

	public CALScanner( CALColorManager colorManager  )
	{
		IToken commentToken = new Token( new TextAttribute( colorManager.getColor( ICALColorConstants.COMMENT ) ) );
		IToken constantToken = new Token( new TextAttribute( colorManager.getColor( ICALColorConstants.CONSTANT ) ) );

		List<IRule> rules = new ArrayList<IRule>();
		
		rules.add( new MultiLineRule( "/*", "*/", commentToken ) );
		rules.add( new EndOfLineRule( "//", commentToken ) );
		rules.add( new SingleLineRule( "\"", "\"", constantToken ) );
		rules.add( new SingleLineRule( "'", "'", constantToken ) );
		rules.add( new CALNumberRule( colorManager ) );
		rules.add( new CALWordRule( colorManager ) );
		rules.add( new CALOperatorRule( colorManager ) );
       
		IRule[] result= new IRule[ rules.size() ];
		rules.toArray( result );
		setRules( result );
	}
}
