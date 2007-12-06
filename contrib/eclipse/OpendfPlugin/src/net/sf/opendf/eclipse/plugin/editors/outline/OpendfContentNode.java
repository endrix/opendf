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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

public class OpendfContentNode
{
	private String label;
	private OpendfContentNode parent;
	private ArrayList<OpendfContentNode> children;
  
  // Lines are 1-based, so 0 means "line range unavailable"
	private int startLine    = 0;
	private int endLine      = 0;
  private boolean expanded = false;
  private Image image;
	
  public void setExpanded( boolean e )
  {
    expanded = e;
  }
  
  public boolean getExpanded()
  {
    return expanded;
  }
  
  public void setExpandedState( TreeViewer viewer )
  {
    viewer.setExpandedState( this, expanded );
    
    for( int i=0; i<children.size(); i++ )
      children.get(i).setExpandedState( viewer );
  }

    public Image getImage()
    {
      return image;
    }
    
  // Copy the expanded flags from another content tree to the extent that the trees are the same
  public boolean matchAndCopyExpanded( OpendfContentNode other )
  {
    if( other == null ) return false;
    
    if( ! label.equals( other.label ) ) return false;
    
    setExpanded( other.getExpanded() );
    
    int match = 0;
    for( int i=0; i<children.size(); i++ )
    {
      OpendfContentNode thisChild = children.get(i);
      
      for( int j=match; j<other.children.size(); j++ )
      {
        if( thisChild.matchAndCopyExpanded( other.children.get(j) ) )
        {  match = j+1;
           break;
        }
      }
      
    }
    
    return true;
  }
  
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
	
	public OpendfContentNode( String s, Image img )
	{
		label = s;
		children = new ArrayList<OpendfContentNode>();
		startLine = 0;
    image = img;
	}

  public OpendfContentNode( String s, int start, int end, Image img )
  {
    label = s;
    children = new ArrayList<OpendfContentNode>();
    startLine = start;
    endLine = end;
    image = img;
  }
  
  public OpendfContentNode addChild( String s, Image img )
	{
    OpendfContentNode child = new OpendfContentNode( s, img );
		child.parent = this;
		children.add( child );
		return child;
	}

  public OpendfContentNode addChild( String s, int start, int end, Image img )
  {
    OpendfContentNode child = addChild( s, img );
    child.setLineRange( start, end );
    return child;
  }

  private void setLineRange( int start, int end )
  {
    startLine = end == 0 ? 0 : start;
    endLine = end;
  }
}
