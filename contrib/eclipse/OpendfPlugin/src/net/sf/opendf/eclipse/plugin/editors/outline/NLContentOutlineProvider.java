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

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.w3c.dom.*;
import java.util.*;
import net.sf.opendf.eclipse.plugin.OpendfPlugin;

public class NLContentOutlineProvider extends OpendfContentOutlineProvider
{

	public NLContentOutlineProvider( IDocumentProvider provider )
	{
		super( provider );
	}
  
  public static final String START_LINE_ATTR = "text-begin-line";
  public static final String END_LINE_ATTR   = "text-end-line";
  public static final String NETWORK_TAG     = "Network";
  public static final String QID_TAG         = "QID";
  public static final String DECL_TAG        = "Decl";
  public static final String ENTITY_TAG      = "EntityDecl";
  public static final String KIND_ATTR       = "kind";
  public static final String NAME_ATTR       = "name";
  public static final String VARIABLE_KIND   = "Variable";

  // Class to provide a compare function to sort Nodes using the ArrayList<> sort method.
  private class nodeComparator implements Comparator<Node>
  {
    private String primaryField;
    private String secondaryField;
    
    public nodeComparator( String pf, String sf )
    {
      primaryField   = pf;
      secondaryField = sf;
    }
  
    public int compare( Node a, Node b )
    {
      int primary = ((Element)a).getAttribute(primaryField).compareTo( ((Element)b).getAttribute(primaryField) );
      return  (primary != 0) ? primary : 
        ( secondaryField == null ? 0 :
        ((Element)a).getAttribute(secondaryField).compareTo( ((Element)b).getAttribute(secondaryField) ));
    }
  }
  
  // The parser starts line numbering for 1, so we use 0 to mean non-existent. 
  private static int getStart( Element e )
  {
    int start;
    try
    {
      start = Integer.parseInt( e.getAttribute( START_LINE_ATTR ) );
    }
    catch( NumberFormatException exc )
    {
      start = 0;
    }
    
    return start;
  }
  
  // The parser starts line numbering for 1, so we use 0 to mean non-existent. 
  private static int getEnd( Element e )
  {
    int end;
    try
    {
      end  = Integer.parseInt( e.getAttribute( END_LINE_ATTR  ) );
    }
    catch( NumberFormatException exc )
    {
      end = 0;
    }
    
    return end;
  }  
  public void updateDocument( Node document )
  {
    Element network = (Element) document.getFirstChild(); // .getDocumentElement();

    if( ! network.getTagName().equals( NETWORK_TAG ) )
    {
      setRoot( null );
      return;
    }
    
    String networkName = "Network";
    ArrayList<Node> vars     = new ArrayList<Node>();
    ArrayList<Node> entities = new ArrayList<Node>();

    for( Node node = network.getFirstChild(); node != null; node = node.getNextSibling() )
    {
      String name = node.getNodeName();
      
      if( name.equals( QID_TAG ) )
      {
        networkName = networkName + " " + ((Element)node).getAttribute( NAME_ATTR );
        continue;
      }
      
      if( name.equals( DECL_TAG ) && ((Element)node).getAttribute( KIND_ATTR ).equals( VARIABLE_KIND ) )
      {
        vars.add( node );
        continue;
      }
      
      if( name.equals( ENTITY_TAG ) )
      {
        entities.add( node );
        continue;
      }
    }
    
    OpendfContentNode root = new OpendfContentNode( networkName, getStart( network ), getEnd( network ),
                                   OpendfPlugin.getDefault().getImage( OpendfPlugin.IMAGE_network ) );
    root.setExpanded( true );

    // List the variables in alphabetic order
    if( vars.size() > 0 )
    {
      OpendfContentNode varNode = root.addChild( "Variables",
              OpendfPlugin.getDefault().getImage( OpendfPlugin.IMAGE_variable ) );
      
      Comparator<Node> comparator = new nodeComparator( NAME_ATTR, null );
      Collections.sort( vars, comparator );
      
      for( int i=0; i < vars.size(); i++ )
      {
        Element var = (Element) vars.get(i);
        varNode.addChild( var.getAttribute( NAME_ATTR ), getStart( var ), getEnd( var ),
            OpendfPlugin.getDefault().getImage( OpendfPlugin.IMAGE_variable ) );
      }
    }

    // List the entities in alphabetic order
    if( entities.size() > 0 )
    {
      OpendfContentNode entityNode = root.addChild( "Entities",
          OpendfPlugin.getDefault().getImage( OpendfPlugin.IMAGE_entity ) );
      
      Comparator<Node> comparator = new nodeComparator( NAME_ATTR, null );
      Collections.sort( vars, comparator );
      
      for( int i=0; i < entities.size(); i++ )
      {
        Element entity = (Element) entities.get(i);
        entityNode.addChild( entity.getAttribute( NAME_ATTR ), getStart( entity ), getEnd( entity ),
            OpendfPlugin.getDefault().getImage( OpendfPlugin.IMAGE_entity ) );
      }
    }

    setRoot( root );
  }

}
