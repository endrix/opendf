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

package net.sf.opendf.plugin.causation;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;

/**
 * This class represents causation traces as XML files and prints them to the 
 * specified {@link PrintWriter}.
 * 
 * 
 * @author jornj
 */
public class XmlCausationTraceBuilder implements CausationTraceBuilder {
	
	//
	//  CausationTraceBuilder
	//
	
	public void beginTrace() {
		if (activeTrace)
			throw new RuntimeException("Cannot begin trace because trace is already active.");
		activeTrace = true;
		pw.println("<causation-trace>");
	}
	
	public void endTrace() {
		if (activeStep)
			endStep();
		pw.println("</causation-trace>");
		pw.flush();
		if (closeFlag)
			pw.close();
		activeTrace = false;
		
		// Display of JGrapht
		JGraphModelAdapter adapter = new JGraphModelAdapter( lg );
        JGraph jgraph = new JGraph( adapter );

        // Maps used to order JGraph
		Map<String, Integer> level = new HashMap<String, Integer>();
		Map<Integer, Integer> count = new HashMap<Integer, Integer>();
		
		// Draw the Graph, note : it's a Node^2 algorithm
		Iterator<String> it = nodesn.iterator();
		while (it.hasNext()) { // Draw each node
			String vertex = it.next();
			Iterator<String> it2 = nodesn.iterator();
			String vertex2 = it2.next();

			int clevel = 0;
			while (vertex!=vertex2){ // Get the level => is the Y location
				if(lg.getEdge(vertex2, vertex) != null){
					int temp = level.get(vertex2)+1;
					clevel = (clevel<temp)?temp:clevel;
				}
				vertex2 = it2.next();
			}
			// Get the count => is the X location
			int ccount = 0;
			if(count.get(clevel)!=null){
				ccount = count.get(clevel)+1;
			}
			count.put(clevel,ccount);
			level.put(vertex, clevel);
			// Put the node to the calculated position
			this.positionVertexAt(vertex, ccount*100, clevel*50, adapter);
		}
  
        JFrame f = new JFrame();
        f.setTitle("Causation Trace Graph");
        f.setPreferredSize(new Dimension(640, 480));
        f.setLocationByPlatform(true);
        
        JScrollPane sp = new JScrollPane(jgraph);
        f.add(sp);
        f.pack();
        f.setVisible(true);
	}
	
	protected void positionVertexAt(Object vertex, int x, int y, JGraphModelAdapter adapter) {
		DefaultGraphCell cell = adapter.getVertexCell(vertex);
		
		AttributeMap attr = cell.getAttributes();
		Rectangle2D bounds = GraphConstants.getBounds(attr);


		Rectangle2D newBounds = new Rectangle2D.Double(x, y, bounds.getWidth(),
				bounds.getHeight());

		GraphConstants.setBounds(attr, newBounds);

		// TODO: Clean up generics once JGraph goes generic
		AttributeMap cellAttr = new AttributeMap();
		cellAttr.put(cell, attr);
		adapter.edit(cellAttr, null, null, null);
	}

	

	public Object beginStep() {
		if (!activeTrace)
			throw new RuntimeException("Cannot begin new step because trace is not/no longer active.");
		if (activeStep)
			throw new RuntimeException("Cannot begin new step because previous step has not been ended.");

		activeStep = true;
		stepAttrs = new HashMap();
		dependencies = new ArrayList();
		return new Long(stepID);
	}

	public void setStepAttribute(String name, Object value) {
		assert name != null;
		
		if (!activeStep)
			throw new RuntimeException("Cannot set step attribute because no step is active.");
		stepAttrs.put(name, value);
	}

	public Object getStepAttribute(String name) {
		assert name != null;
		
		if (!activeStep)
			throw new RuntimeException("Cannot get step attribute because no step is active.");
		return stepAttrs.get(name);
	}

	public void removeStepAttribute(String name) {
		assert name != null;
		
		if (!activeStep)
			throw new RuntimeException("Cannot remove step attribute because no step is active.");
		stepAttrs.remove(name);
	}

	public Object currentStep() {
		if (!activeStep)
			return null;
		return new Long(stepID);
	}

	public void endStep() {
		if (!activeStep)
			throw new RuntimeException("Cannot end step because no step is active.");
		if (activeDependency != null)
			endDependency();
		
		String name = Long.toString(stepID);

		pw.print("    <step ID=\"" + stepID + "\"");
		for (Iterator i = stepAttrs.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			Object v = stepAttrs.get(k);
			pw.print(" " + k.toString() + "=\"" + v.toString() + "\"");
			if(k.toString().equals("actor-name"))
				name = v.toString() + ":" + name;
			
		}
		pw.println(">");
		lg.addVertex(name);
		nodes.put(Long.toString(stepID), name);
		nodesn.add(name);

		for (Iterator i = dependencies.iterator(); i.hasNext(); ) {
			Dependency d = (Dependency)i.next();
			if (d != null && d.step != null) {
				pw.print("        <dependency source=\"" + d.step.toString() + "\"");
				lg.addEdge( nodes.get(d.step.toString()), name );
				for (Iterator j = d.attrs.keySet().iterator(); j.hasNext(); ) {
					Object k = j.next();
					Object v = d.attrs.get(k);
					pw.print(" " + k.toString() + "=\"" + v.toString() + "\" ");			
				}
				pw.println("/>");
			} else {
				System.err.println("Malformed dependency: " + d + " " + (d == null ? null : d.step));
			}
		}
		
		pw.println("    </step>");

		pw.flush();
		activeStep = false;
		stepAttrs = null;
		dependencies = null;
		stepID += 1;
	}
	
	public void  cancelStep() {
		activeStep = false;
		stepAttrs = null;
		dependencies = null;
		stepID += 1;
	}

	public void beginDependency(Object stepID) {
		assert stepID != null;
		
		if (!activeStep)
			throw new RuntimeException("Cannot begin new dependency because no step is active.");
		if (activeDependency != null)
			throw new RuntimeException("Cannot begin new dependency because previous dependency has not been ended.");

		activeDependency = new Dependency(stepID);
	}

	public void setDependencyAttribute(String name, Object value) {
		assert name != null;
		
		if (activeDependency == null)
			throw new RuntimeException("Cannot set dependency attribute because no dependency is active.");
		activeDependency.attrs.put(name, value);
	}

	public Object getDependencyAttribute(String name) {
		assert name != null;
		
		if (activeDependency == null)
			throw new RuntimeException("Cannot get dependency attribute because no dependency is active.");
		return activeDependency.attrs.get(name);
	}

	public void removeDependencyAttribute(String name) {
		assert name != null;
		
		if (activeDependency == null)
			throw new RuntimeException("Cannot remove dependency attribute because no dependency is active.");
		activeDependency.attrs.remove(name);
	}

	public void endDependency() {
		if (activeDependency == null)
			throw new RuntimeException("Cannot end dependency because no dependency is active.");

		dependencies.add(activeDependency);
		activeDependency = null;
	}

	//
	//  Ctor
	//
	
	public XmlCausationTraceBuilder(PrintWriter pw, boolean closeFlag) {
		this.pw = pw;
		this.closeFlag = closeFlag;
		lg = new ListenableDirectedGraph( DefaultEdge.class );
		nodes = new HashMap<String,String>();
		nodesn = new Vector<String>();
	}
	
	
	//
	//  private data
	//

	private boolean     activeTrace = false;

	private long        stepID = 0;
	private boolean     activeStep = false;
	private Map         stepAttrs = null;
	private List        dependencies = null;
	
	private Dependency  activeDependency = null;
	
	private PrintWriter pw;
	private boolean     closeFlag;
	
	private ListenableGraph lg;
	private Map<String,String> nodes;
	private Vector<String> nodesn;
	
	private static class Dependency {
		public Object step;
		public Map    attrs;
		
		public Dependency(Object step) {
			this.step = step;
			attrs = new HashMap();
		}
	}
}
