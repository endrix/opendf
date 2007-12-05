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

package net.sf.opendf.eclipse.plugin.editors.outline;

import java.util.*;

public class OpendfContentNode
{
	private String label;
	private OpendfContentNode parent;
	private ArrayList<OpendfContentNode> children;
  
  // Lines are 1-based, so 0 means "line range unavailable"
	private int startLine;
	private int endLine;
	
	public int getStart()
	{
		return startLine;
	}
	
	public int getEnd()
	{
		return endLine;
	}
	
	public OpendfContentNode[] getChildren()
	{
		return children.toArray( new OpendfContentNode[ children.size() ] );
	}

	public boolean hasChildren()
	{
		return children.size() > 0;
	}
	
	public OpendfContentNode getParent()
	{
		return parent;
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public OpendfContentNode( String s )
	{
		label = s;
		children = new ArrayList<OpendfContentNode>();
		startLine = 0;
	}

  public OpendfContentNode( String s, int start, int end )
  {
    label = s;
    children = new ArrayList<OpendfContentNode>();
    startLine = start;
    endLine = end;
  }
  
  public OpendfContentNode addChild( String s )
	{
    OpendfContentNode child = new OpendfContentNode( s );
		child.parent = this;
		children.add( child );
		return child;
	}

  public OpendfContentNode addChild( String s, int start, int end )
  {
    OpendfContentNode child = addChild( s );
    child.setLineRange( start, end );
    return child;
  }

  private void setLineRange( int start, int end )
  {
    startLine = end == 0 ? 0 : start;
    endLine = end;
  }
}
