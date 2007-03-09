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

package net.sf.caltrop.eclipse.plugin.editors.outline;

import org.w3c.dom.*;
import java.util.*;

public class CALContentNode implements Comparator<Node>
{
	private String label;
	private CALContentNode parent;
	private ArrayList<CALContentNode> children;
	private int startLine;
	private int endLine;

	// Provide a compare function to sort items using the interface Comparator<> required
	// by the ArrayList<> sort method. The items being sorted are dom Nodes, and
	// we sort on one or two attribute values.
	private static String primaryField;
	private static String secondaryField;
	
	public int compare( Node a, Node b )
	{
		int primary = ((Element)a).getAttribute(primaryField).compareTo( ((Element)b).getAttribute(primaryField) );
		return	(primary != 0) ? primary : 
				( secondaryField == null ? 0 :
				((Element)a).getAttribute(secondaryField).compareTo( ((Element)b).getAttribute(secondaryField) ));
	}
	
	public int getStart()
	{
		return startLine;
	}
	
	public int getEnd()
	{
		return endLine;
	}
	
	public CALContentNode[] getChildren()
	{
		return children.toArray( new CALContentNode[ children.size() ] );
	}

	public boolean hasChildren()
	{
		return children.size() > 0;
	}
	
	public CALContentNode getParent()
	{
		return parent;
	}
	
	public String getLabel()
	{
		return label;
	}
	
	private CALContentNode( String s )
	{
		label = s;
		children = new ArrayList<CALContentNode>();
		startLine = 0;
	}
	
	// The parser starts line numbering for 1, so we use 0 to mean non-existent. 
	private void setLineRange( Element e )
	{
		try
		{
			startLine = Integer.parseInt( e.getAttribute("text-begin-line") );
			endLine   = Integer.parseInt( e.getAttribute("text-end-line"  ) );
		}
		catch( NumberFormatException exc )
		{
			// Don't care if there is no line range
			startLine = 0;
		}
	}
	
	private CALContentNode addChild( String s )
	{
		CALContentNode child = new CALContentNode( s );
		child.parent = this;
		children.add( child );
		return child;
	}

	// Create a child and set its line number range
	private CALContentNode addChild( String s, Element e )
	{
		CALContentNode child = addChild( s );
		child.setLineRange( e );
		return child;
	}

	// Create an entire content tree
	public static CALContentNode contentTree( Element actor )
	{
		assert actor.getTagName().equals( "Actor" );

		CALContentNode root = new CALContentNode( "Actor " + actor.getAttribute( "name" ) );

		// ArrayLists to collect up the relevant parts of the dom tree
		ArrayList<Node> stateVars   = new ArrayList<Node>();
		ArrayList<Node> actions     = new ArrayList<Node>();
		ArrayList<Node> priorities  = new ArrayList<Node>();
		ArrayList<Node> fsms        = new ArrayList<Node>();

		// Collect up all the top-level elements for alphabetic sort
		for( Node e = actor.getFirstChild(); e != null; e = e.getNextSibling() )
		{
			String name = e.getNodeName();

			// Collect actions
			if( name.equals("Action") )
			{   
				// If this action has a QID, give it a name attribute using the QID
				// because the sorting mechanism requires the sort criterion to be
				// an attribute of the Nodes being sorted.
				String qid = "";
			    for( Node n = e.getFirstChild(); n != null; n = n.getNextSibling() )
			    {
			    	if( n.getNodeName().equals("QID") )
			    	{
			    		qid = ((Element)n).getAttribute( "name" );
			    		break;
			    	}
			    }
			    ((Element)e).setAttribute( "name", qid );
				actions.add( e );
				continue;
			}

			// Collect actor state variables
			if( name.equals("Decl") && ((Element)e).getAttribute( "kind" ).equals("Variable") )
			{
				stateVars.add( e );
				continue;
			}

			// Collect fsms
			if( name.equals("Schedule") && ((Element)e).getAttribute( "kind" ).equals("fsm") )
			{
				fsms.add( e );
				continue;
			}
			
			if( name.equals("Priority") )
			{
				priorities.add( e );
				continue;
			}
		}
		
		// List the state variables in alphabetic order
		if( stateVars.size() > 0 )
		{
			CALContentNode stateNode = root.addChild( "State Variables" );
			primaryField = "name";
			secondaryField = null;
			Collections.sort( stateVars, (Comparator<Node>) root );
			for( int i=0; i < stateVars.size(); i++ )
			{
				Element var = (Element) stateVars.get(i);
				stateNode.addChild( var.getAttribute( "name" ), var );
			}
		}
		
		// List the actions in alphabetic order
		if( actions.size() > 0 )
		{
			CALContentNode actionNode = root.addChild( "Actions" );
			primaryField   = "name";
			secondaryField = null;
			Collections.sort( actions, (Comparator<Node>) root );
			for( int i=0; i < actions.size(); i++ )
			{ 
				Element a = (Element) actions.get(i);
				String name = a.getAttribute( "name" );
				actionNode.addChild( name.length() == 0 ? "(unnamed)" : name, a );
			}
		}

		// List the FSMs
		for( int n=0; n < fsms.size(); n++ )
		{
			Element fsm = (Element) fsms.get( n );
			ArrayList<Node> transitions = new ArrayList<Node>();
			
			// Collect all the transitions
			for( Node t = fsm.getFirstChild(); t != null; t = t.getNextSibling() )
			{
				if( ! t.getNodeName().equals("Transition") ) continue;
				transitions.add( t );
			}
		
			// Add the states in alphabetical order
			String s = fsm.getAttribute( "initial-state" );
			CALContentNode fsmNode = root.addChild( "State Machine (start = " + s + ")", fsm );
			
			// Now get a list of all the transitions sorted first on "from", then on "to"
			primaryField   = "from";
			secondaryField = "to";
			Collections.sort( transitions, (Comparator<Node>) root );
			
			CALContentNode fromTo = null;
			String fromName = null;
			String toName = null;
			
			for( int i = 0; i < transitions.size(); i++ )
			{
				// For each transition get the from/to states. We are going to group all actions
				// associated with a particular transition under a single tree node, regardless
				// of how they are arranged in the source.
				Element t = (Element) transitions.get(i);
				
				if( fromTo == null || ! fromName.equals( t.getAttribute( "from" ) ) 
					|| ! toName.equals( t.getAttribute( "to" ) ) )
				{
					// New transition detected
					fromName = t.getAttribute( "from" );
					toName   = t.getAttribute( "to" );
					fromTo   = fsmNode.addChild( fromName + " --> " + toName );
				}

				// For each QID associated with the transition, list all matching actions
				for( Node ActionTag = t.getFirstChild(); ActionTag != null; ActionTag = ActionTag.getNextSibling() )
				{
					if( ! ActionTag.getNodeName().equals( "ActionTags" ) ) continue;
					
					for( Node QID = ActionTag.getFirstChild(); QID != null; QID = QID.getNextSibling() )
					{
						if( ! QID.getNodeName().equals("QID") ) continue;
						addMatchingActions( fromTo, QID, actions );
					}
				}
			}
		}
		
		if( priorities.size() > 0 )
		{
			CALContentNode priorityRoot = root.addChild( "Priorities" );
			for( int i = 0; i < priorities.size(); i++ )
			{
				CALContentNode inequality = priorityRoot.addChild( "Inequality" );
				for( Node QID = priorities.get(i).getFirstChild(); QID != null; QID = QID.getNextSibling() )
				{
					if( ! QID.getNodeName().equals("QID") ) continue;
					
					CALContentNode q = inequality.addChild( "QID " + ((Element) QID).getAttribute( "name" ) );
					addMatchingActions( q, QID, actions );
				}
			}
			
		}
		
		return root;
	}
	
	// Add all the actions that match some QID to some CALCOntentNode
	private static void addMatchingActions( CALContentNode n, Node QID, ArrayList<Node> actions )
	{
		String aname   = ((Element) QID).getAttribute( "name" );
		String aprefix = aname + ".";

		for( int j = 0; j < actions.size(); j++ )
		{ 
			Element ae = (Element) actions.get(j);
			String  a  = ae.getAttribute( "name" );
			if( a.equals( aname ) || a.startsWith( aprefix ))
				n.addChild( a, ae );
		}
	}
}
