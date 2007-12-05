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

public class CALContentOutlineProvider extends OpendfContentOutlineProvider
{

	public CALContentOutlineProvider( IDocumentProvider provider )
	{
		super( provider );
	}
  
  public static final String START_LINE_ATTR    = "text-begin-line";
  public static final String END_LINE_ATTR      = "text-end-line";
  public static final String ACTOR_TAG          = "Actor";
  public static final String ACTION_TAG         = "Action";
  public static final String QID_TAG            = "QID";
  public static final String DECL_TAG           = "Decl";
  public static final String SCHEDULE_TAG       = "Schedule";
  public static final String PRIORITY_TAG       = "Priority";
  public static final String TRANSITION_TAG     = "Transition";
  public static final String ACTIONTAGS_TAG     = "ActionTags";
  public static final String KIND_ATTR          = "kind";
  public static final String NAME_ATTR          = "name";
  public static final String FROM_ATTR          = "from";
  public static final String TO_ATTR            = "to";
  public static final String INITIAL_STATE_ATTR = "initial-state";
  public static final String VARIABLE_KIND      = "Variable";
  public static final String FSM_KIND           = "fsm";
  
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
      end  = Integer.parseInt( e.getAttribute( END_LINE_ATTR ) );
    }
    catch( NumberFormatException exc )
    {
      end = 0;
    }
    
    return end;
  }
  
  public void updateDocument( Document document )
  {
    Element actor = document.getDocumentElement();
     
    if( ! actor.getTagName().equals( ACTOR_TAG ) )
    {
      setRoot( null );
      return;
    }
 
    OpendfContentNode root = new OpendfContentNode( "Actor " + actor.getAttribute( NAME_ATTR ),
                                        getStart( actor ), getEnd( actor ) );

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
      if( name.equals( ACTION_TAG ) )
      {   
        // If this action has a QID, give it a name attribute using the QID
        // because the sorting mechanism requires the sort criterion to be
        // an attribute of the Nodes being sorted.
        String qid = "";
          for( Node n = e.getFirstChild(); n != null; n = n.getNextSibling() )
          {
            if( n.getNodeName().equals( QID_TAG ) )
            {
              qid = ((Element)n).getAttribute( NAME_ATTR );
              break;
            }
          }
          ((Element)e).setAttribute( NAME_ATTR, qid );
        actions.add( e );
        continue;
      }

      // Collect actor state variables
      if( name.equals( DECL_TAG ) && ((Element)e).getAttribute( KIND_ATTR  ).equals( VARIABLE_KIND ) )
      {
        stateVars.add( e );
        continue;
      }

      // Collect fsms
      if( name.equals( SCHEDULE_TAG ) && ((Element)e).getAttribute( KIND_ATTR  ).equals( FSM_KIND ) )
      {
        fsms.add( e );
        continue;
      }
      
      if( name.equals( PRIORITY_TAG ) )
      {
        priorities.add( e );
        continue;
      }
    }
   
    // List the state variables in alphabetic order
    if( stateVars.size() > 0 )
    {
      OpendfContentNode stateNode = root.addChild( "State Variables" );
      
      Comparator<Node> comparator = new nodeComparator( NAME_ATTR, null );
      Collections.sort( stateVars, comparator );
      
      for( int i=0; i < stateVars.size(); i++ )
      {
        Element var = (Element) stateVars.get(i);
        stateNode.addChild( var.getAttribute( NAME_ATTR ), getStart( var ), getEnd( var ) );
      }
    }

    // List the actions in alphabetic order
    if( actions.size() > 0 )
    {
      OpendfContentNode actionNode = root.addChild( "Actions" );
      
      Comparator<Node> comparator = new nodeComparator( NAME_ATTR, null );
      Collections.sort( actions, comparator );
      
      for( int i=0; i < actions.size(); i++ )
      { 
        Element a = (Element) actions.get(i);
        String name = a.getAttribute( NAME_ATTR );
        actionNode.addChild( name.length() == 0 ? "(unnamed)" : name, getStart( a ), getEnd( a ) );
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
        if( ! t.getNodeName().equals( TRANSITION_TAG ) ) continue;
        transitions.add( t );
      }
    
      // Add the states in alphabetical order
      String s = fsm.getAttribute( INITIAL_STATE_ATTR );
      OpendfContentNode fsmNode = root.addChild( "State Machine (start = " + s + ")", getStart( fsm ), getEnd( fsm ) );
      
      // Now get a list of all the transitions sorted first on "from", then on "to"
      Comparator<Node> comparator = new nodeComparator( FROM_ATTR, TO_ATTR );
      Collections.sort( transitions, comparator );
      
      OpendfContentNode fromTo = null;
      String fromName = null;
      String toName = null;
      
      for( int i = 0; i < transitions.size(); i++ )
      {
        // For each transition get the from/to states. We are going to group all actions
        // associated with a particular transition under a single tree node, regardless
        // of how they are arranged in the source.
        Element t = (Element) transitions.get(i);
        
        if( fromTo == null || ! fromName.equals( t.getAttribute( FROM_ATTR) ) 
          || ! toName.equals( t.getAttribute( TO_ATTR ) ) )
        {
          // New transition detected
          fromName = t.getAttribute( FROM_ATTR );
          toName   = t.getAttribute( TO_ATTR );
          fromTo   = fsmNode.addChild( fromName + " --> " + toName );
        }

        // For each QID associated with the transition, list all matching actions
        for( Node ActionTag = t.getFirstChild(); ActionTag != null; ActionTag = ActionTag.getNextSibling() )
        {
          if( ! ActionTag.getNodeName().equals( ACTIONTAGS_TAG ) ) continue;
          
          for( Node QID = ActionTag.getFirstChild(); QID != null; QID = QID.getNextSibling() )
          {
            if( ! QID.getNodeName().equals( QID_TAG ) ) continue;
            addMatchingActions( fromTo, QID, actions );
          }
        }
      }
    }

    if( priorities.size() > 0 )
    {
      OpendfContentNode priorityRoot = root.addChild( "Priorities" );
      for( int i = 0; i < priorities.size(); i++ )
      {
        OpendfContentNode inequality = priorityRoot.addChild( "Inequality" );
        for( Node QID = priorities.get(i).getFirstChild(); QID != null; QID = QID.getNextSibling() )
        {
          if( ! QID.getNodeName().equals( QID_TAG ) ) continue;
          
          OpendfContentNode q = inequality.addChild( "QID " + ((Element) QID).getAttribute( NAME_ATTR ) );
          addMatchingActions( q, QID, actions );
        }
      }
      
    }

    setRoot( root );
  }

  // Add all the actions that match some QID to some CALCOntentNode
  private static void addMatchingActions( OpendfContentNode n, Node QID, ArrayList<Node> actions )
  {
    String aname   = ((Element) QID).getAttribute( NAME_ATTR );
    String aprefix = aname + ".";

    for( int j = 0; j < actions.size(); j++ )
    { 
      Element ae = (Element) actions.get(j);
      String  a  = ae.getAttribute( NAME_ATTR );
      if( a.equals( aname ) || a.startsWith( aprefix ))
        n.addChild( a, getStart( ae ), getEnd( ae ) );
    }
  }
}
