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

import net.sf.caltrop.eclipse.plugin.editors.CALColorManager;
import net.sf.caltrop.eclipse.plugin.editors.ICALColorConstants;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;

public class CALNumberRule implements IRule
{

	private void unread( ICharacterScanner s, int n )
	{
		while( n > 0 )
		{
			--n;
			s.unread();
		}
	}
	
	private boolean getZero( ICharacterScanner s )
	{
		if( s.read() == '0' ) return true;
		s.unread();
		return false;
	}

	private boolean getDecimalPoint( ICharacterScanner s )
	{
		if( s.read() == '.' ) return true;
		s.unread();
		return false;
	}

	private int getDecimalDigits( ICharacterScanner s )
	{ int c;
	  int n = 0;
	  
	  while( (c = s.read()) >= '0' && c <= '9' ) ++ n;
	  s.unread();
	  return n;
	}
	
	private int getOctalDigits( ICharacterScanner s )
	{ int c;
	  int n = 0;
	  
	  while( (c = s.read()) >= '0' && c <= '7' ) ++ n;
	  s.unread();
	  return n;
	}
	
	private int getHexDigits( ICharacterScanner s )
	{ int c;
	  int n = 0;
	  
	  while( ( (c = s.read()) >= '0' && c <= '9' ) ||
			  ( c >= 'a' && c <= 'f' ) || 
			  ( c >= 'A' && c <= 'F' ) ) ++ n;
	  s.unread();
	  return n;
	}
	
	private int getExponent( ICharacterScanner s )
	{ int c, n1, n2;
	
	  // Required preamble
	  if( (c = s.read()) != 'e' && c != 'E' )
	  {
		  s.unread();
		  return 0;
	  }
	  
	  n1 = 1;
	  
	  // Optional sign
	  if( (c = s.read()) == '+' || c == '-' ) n1 = 2;
	  else s.unread();
	  			  
	  // Required exponent value
	  n2 =  getDecimalDigits( s );
	  if( n2 == 0 )
	  { unread( s, n1 );
	    return 0;
	  }
	  
	  return n1 + n2;
	}
	
	private boolean getFloat( ICharacterScanner s )
	{
	  // Detect single zero followed by decimal point
	  if( getZero( s ) )
	  {
		if( !getDecimalPoint( s ) )
	    { 
		  s.unread( );
		  return false;
		}
		
	  }
	  else
	  {   // Detect any other set of digits followed by decimal point
		  int n = getDecimalDigits( s );
		  if( n == 0 ) return false;
		  if( !getDecimalPoint(s) )
		  {
			  unread( s, n );
			  return false;
		  }
	  }
	  
	  // The rest is optional
	  getDecimalDigits( s );
	  getExponent( s );
	  
	  return true;
	}
	
	private boolean getInt( ICharacterScanner s )
	{ int c;

	  // Leading zero
	  if( getZero( s ) )
	  {
		// Look for an octal
	    if( getOctalDigits(s) > 0) return true;
	  
	    // Look for a hex
	    if( (c = s.read()) == 'x' || c == 'X' )
	    { // If there are no digits after the 'x', ignore the 'x' too
		  if( getHexDigits(s) == 0 ) s.unread();
	    }
	    
	    // Was either a '0', an octal or a hex
	    return true;
	  }

	  return getDecimalDigits( s ) > 0;
	}
	
	IToken success;
	
    public CALNumberRule( CALColorManager colorManager )
    {
    	success = new Token( new TextAttribute( colorManager.getColor( ICALColorConstants.CONSTANT ) ) );
    }
			
	public IToken evaluate( ICharacterScanner s )
	{
	  // Try floats first to get the case where mantissa is a valid int
	  if( getFloat( s ) ) return success;
	  if( getInt( s ) ) return success;
				
	  return Token.UNDEFINED;
	}

}

