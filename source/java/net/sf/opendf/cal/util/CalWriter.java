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

package net.sf.opendf.cal.util;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class CalWriter
{
  
  // Convert CALML (pre-canonicalization) to a readable string of CAL.
  public static String CalmlToString( Node node )
  {
    if( node.getNodeName().equals( TYPE ) )
      return TypeToString( node );
   
    if( node.getNodeName().equals( EXPR ) )
      return ExprToString( node );
    
    return "?";
  }
  
  public static String ExprToString( Node node )
  {
    if( ((Element)node).getAttribute( KIND ).equals( LITERAL ) )
      return ((Element)node).getAttribute( VALUE );
      
    if( ((Element)node).getAttribute( KIND ).equals( VAR ) )
      return ((Element)node).getAttribute( NAME );


    if( ((Element)node).getAttribute( KIND ).equals( UNARYOP ) ||
        ((Element)node).getAttribute( KIND ).equals( BINOPSEQ ) )
    {
      String s = "";
        
      for( Node n = node.getFirstChild(); n != null; n = n.getNextSibling() )
      {
        if( n.getNodeName().equals( OP ) )
        {
          s = s + ((Element)n).getAttribute( NAME );
          continue;
        }
          
        if( n.getNodeName().equals( EXPR ) )
        {
          s = s + CalmlToString( n );
          continue;
        }
      }
        
      return node.getParentNode().getNodeName().equals( EXPR ) ? "(" + s + ")" : s;
    }

    if( ((Element)node).getAttribute( KIND ).equals( IF ))
    {
      Node expr1 = null;
      Node expr2 = null;
      Node expr3 = null;
        
      for( Node n = node.getFirstChild(); n != null; n = n.getNextSibling() )
      {
        if( n.getNodeName().equals( "Expr") )
        {
          expr3 = expr2;
          expr2 = expr1;
          expr1 = n;
        }
      }
        
      return "if " + ExprToString( expr3 ) + " then " + ExprToString( expr2 ) + " else " + ExprToString( expr1 ) + " end";
    }

    if( ((Element)node).getAttribute( KIND ).equals( APPLICATION ))
    {
      String s = "";
      
      for( Node e = node.getFirstChild(); e != null; e = e.getNextSibling() )
        if( e.getNodeName().equals( EXPR ) )
        {
          s = ((Element)e).getAttribute( KIND ).equals( VAR ) ? ((Element)e).getAttribute( NAME ) : "?";
          break;
        }

      s = s + "(";
      
      for( Node a = node.getFirstChild(); a != null; a = a.getNextSibling() )
        if( a.getNodeName().equals( ARGS ) )
        {
          int nArgs = 0;
          for( Node e = a.getFirstChild(); e != null; e = e.getNextSibling() )
            if( e.getNodeName().equals( EXPR ) )
            {
              if( nArgs > 0 ) s = s + ",";
              s = s + ExprToString( e );
              nArgs ++;
            }
          break;
        }
          
      s = s + ")";
      
      return s;

    }
    
    return "?";
  }

  public static String TypeToString( Node node )
  {
    String s = ((Element)node).getAttribute( NAME );
    
    int nEntries = 0;
    
    for( Node n = node.getFirstChild(); n != null; n = n.getNextSibling() )
    {
      if( ! n.getNodeName().equals( ENTRY ) ) continue;
      
      if( ((Element)n).getAttribute( KIND ).equals( TYPE ) )
      {
        for( Node t = n.getFirstChild(); t != null; t = t.getNextSibling() )
          if( t.getNodeName().equals( TYPE ) )
          {
            s = s + (nEntries == 0 ? "(" : ",") + "type:" + TypeToString( t );
            nEntries ++;
            break;
          }
      }
      else if ( ((Element)n).getAttribute( KIND ).equals( EXPR ) )
      {
        for( Node e = n.getFirstChild(); e != null; e = e.getNextSibling() )
          if( e.getNodeName().equals( EXPR ) )
          {
            s = s + (nEntries == 0 ? "(" : ",") + ((Element)n).getAttribute( NAME ) + "=" + ExprToString( e );
            nEntries ++;
            break;
          }
      }
    }
    
    if( nEntries > 0 )
      s = s + ")";
    
    return s;
  }
  
  private static final String EXPR        = "Expr";
  private static final String ENTRY       = "Entry";
  private static final String TYPE        = "Type";
  private static final String KIND        = "kind";
  private static final String NAME        = "name";
  private static final String OP          = "Op";
  private static final String IF          = "If";
  private static final String BINOPSEQ    = "BinOpSeq";
  private static final String UNARYOP     = "UnaryOp";
  private static final String LITERAL     = "Literal";
  private static final String VAR         = "Var";
  private static final String VALUE       = "value";
  private static final String APPLICATION = "Application";
  private static final String ARGS        = "Args";

}
