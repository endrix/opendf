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
import java.awt.GridLayout;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;

import att.grappa.Grappa;
import att.grappa.GrappaSupport;

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
		
		// JGrapht to DOT exporter
		DOTExporter<String, DefaultEdge> exporter = new DOTExporter<String, DefaultEdge>(
				 new VertexNameProvider<String>(){
					 public String getVertexName(String arg0){
						 return "\"" + arg0 + "\"";
					 }
				 },
				 new VertexNameProvider<String>(){
					 public String getVertexName(String arg0){
						 return arg0;
					 }
				 },
				 new EdgeNameProvider<DefaultEdge>(){
					@Override
					public String getEdgeName(DefaultEdge arg0) {
						return arg0.toString();
					}
				 });
		
		// resumed JGrapht to DOT exporter
		DOTExporter<CountVertex,CountEdge> exporter2 = new DOTExporter<CountVertex,CountEdge>(
				 new VertexNameProvider<CountVertex>(){
					 public String getVertexName(CountVertex arg0){
						 return "\"" + arg0.toString() + "\"";
					 }
				 },
				 new VertexNameProvider<CountVertex>(){
					 public String getVertexName(CountVertex arg0){
						 return arg0.getFull();
					 }
				 },
				 new EdgeNameProvider<CountEdge>(){
					@Override
					public String getEdgeName(CountEdge arg0) {
						return arg0.getFull();
					}
				 });
		
		// Export the two DOT files
		try {
			exporter.export(new PrintWriter(new FileOutputStream("ct.dot")),lg);
			exporter.export(new PrintWriter(new FileOutputStream("ct3.dot")),lgnr);
			exporter2.export(new PrintWriter(new FileOutputStream("ct2.dot")),sg);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		InputStream input = null;
		try {
			input = new FileInputStream("ct2.dot");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		att.grappa.Parser program = new att.grappa.Parser(input,System.err);
		try {
			program.parse();
		} 
		catch(Exception ex) {
			System.err.println("Exception: " + ex.getMessage());
			ex.printStackTrace(System.err);
			System.exit(1);
		}

		att.grappa.Graph graph = program.getGraph();
		graph.setEditable(false);
		graph.setMenuable(false);
		
		att.grappa.GrappaPanel gp = new att.grappa.GrappaPanel(graph,null);
		
		JFrame fg = new JFrame();
        fg.setTitle("Causation Trace Graph");
        fg.setPreferredSize(new Dimension(640, 480));
        fg.setLocationByPlatform(true);
        
        JScrollPane sp2 = new JScrollPane(gp);
        sp2.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        fg.add(sp2);
        fg.setLayout(new GridLayout(1,1));
        
        //////////////////////////////
        
        Object connector = null;
 	   
		/*try {
		connector = Runtime.getRuntime().exec(Demo12.SCRIPT);
	    } catch(Exception ex) {
		System.err.println("Exception while setting up Process: " + ex.getMessage() + "\nTrying URLConnection...");
		connector = null;
	    }*/
	    if(connector == null) {
		try {
		    connector = (new URL("http://www.research.att.com/~john/cgi-bin/format-graph")).openConnection();
		    URLConnection urlConn = (URLConnection)connector;
		    urlConn.setDoInput(true);
		    urlConn.setDoOutput(true);
		    urlConn.setUseCaches(false);
		    urlConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		} catch(Exception ex) {
		    System.err.println("Exception while setting up URLConnection: " + ex.getMessage() + "\nLayout not performed.");
		    connector = null;
		}
	    }
	    if(connector != null) {
		if(!GrappaSupport.filterGraph(graph,connector)) {
		    System.err.println("ERROR: somewhere in filterGraph");
		}
		if(connector instanceof Process) {
		    try {
			int code = ((Process)connector).waitFor();
			if(code != 0) {
			    System.err.println("WARNING: proc exit code is: " + code);
			}
		    } catch(InterruptedException ex) {
			System.err.println("Exception while closing down proc: " + ex.getMessage());
			ex.printStackTrace(System.err);
		    }
		}
		connector = null;
	    }
	    graph.repaint();
        
        //////////////////////////////
        
        fg.pack();
        fg.setVisible(true);
		/*		
		// Display of JGrapht
		JGraphModelAdapter<String, DefaultEdge> adapter = new JGraphModelAdapter<String, DefaultEdge>( lg );
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
        f.setVisible(true);*/
	}
	
	protected void positionVertexAt(Object vertex, int x, int y, JGraphModelAdapter<String, DefaultEdge> adapter) {
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
		stepAttrs = new HashMap<Object, Object>();
		dependencies = new ArrayList<Dependency>();
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

		String sname = "";
		String aname = "";
		String iname = "";

		pw.print("    <step ID=\"" + stepID + "\"");
		for (Iterator<Object> i = stepAttrs.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			Object v = stepAttrs.get(k);
			pw.print(" " + k.toString() + "=\"" + v.toString() + "\"");
			if(k.toString().equals("actor-name")){
				sname = v.toString();
			}
			if(k.toString().equals("action")){
				aname = v.toString();
			}
			if(k.toString().equals("actor-id")){
				iname = v.toString();
			}
			
		}
		pw.println(">");
		String name = sname + "/" + aname + "*" + Long.toString(stepID);
		lg.addVertex(name);
		lgnr.addVertex(name);
		
		// Get the resumed graph Vertex
		sname = sname + "/" + aname;
		iname = sname + "/" + iname;
		CountVertex cv = msgi.get(iname);
		if(cv==null){
			// Create it if not already registered
			cv = new CountVertex(sname, iname);
			sg.addVertex(cv);
			msgi.put(iname, cv);
		}
		// Count execution of resumed graph nodes
		cv.count();
		msg.put(Long.toString(stepID), cv);
		// No double count checker
		Vector<Object> done = new Vector<Object>();
		
		nodes.put(Long.toString(stepID), name);
		nodesn.add(name);
		
		for (Iterator<Dependency> i = dependencies.iterator(); i.hasNext(); ) {
			Dependency d = i.next();
			if (d != null && d.step != null) {
				pw.print("        <dependency source=\"" + d.step.toString() + "\"");
				// Add dependency edge
				lg.addEdge( nodes.get(d.step.toString()), name );
				
				// No double count check
				if(!done.contains(d.step)){
					// Get the source node
					CountVertex cv2 = msg.get(d.step.toString());
					// Check it is no itself
					if(cv!=cv2){
						
						//////////////
						lgnr.addEdge( nodes.get(d.step.toString()), name );
						//////////////
						
						// Get the edge
						CountEdge e = sg.getEdge(cv2, cv);
						if(e==null){
							// Create it if not already registered
							e = new CountEdge();
							sg.addEdge(cv2, cv, e);
						}
						// Count dependencies between these nodes
						e.count();
					}
					// No double count flag
					done.add(d.step);
				}
				
				for (Iterator<String> j = d.attrs.keySet().iterator(); j.hasNext(); ) {
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
		lg = new ListenableDirectedGraph<String, DefaultEdge>( DefaultEdge.class );
		lgnr = new ListenableDirectedGraph<String, DefaultEdge>( DefaultEdge.class ); 
		sg = new DefaultDirectedGraph<CountVertex,CountEdge>(CountEdge.class);
		msg = new HashMap<String,CountVertex>();
		msgi = new HashMap<String,CountVertex>();
		
		nodes = new HashMap<String,String>();
		nodesn = new Vector<String>();
	}
	
	
	//
	//  private data
	//

	private boolean     activeTrace = false;

	private long        stepID = 0;
	private boolean     activeStep = false;
	private Map<Object, Object> stepAttrs = null;
	private List<Dependency> dependencies = null;
	
	private Dependency  activeDependency = null;
	
	private PrintWriter pw;
	private boolean     closeFlag;
	
	private ListenableGraph<String, DefaultEdge> lg;
	private ListenableGraph<String, DefaultEdge> lgnr;
	private DefaultDirectedGraph<CountVertex,CountEdge> sg;
	private Map<String,CountVertex> msg;
	private Map<String,CountVertex> msgi;
	private Map<String,String> nodes;
	private Vector<String> nodesn;
	
	private static class CountEdge {
		private int count=0;
		public CountEdge(){			
		}
		public String getFull(){
			return Integer.toString(count);
		}
		public void count(){
			count++;
		}
		public String toString(){
			return "";
		}
	}
	
	private static class CountVertex {
		private int count=0;
		private String name;
		private String id;
		public CountVertex(String name, String id){
			this.name = name;
			this.id = id;
		}
		public String toString(){
			return id;
		}
		public String getFull(){
			return name + ":" + count;
		}
		public void count(){
			count++;
		}
	}
	
	private static class Dependency {
		public Object step;
		public Map<String, Object> attrs;
		
		public Dependency(Object step) {
			this.step = step;
			attrs = new HashMap<String, Object>();
		}
	}
	
	{
		Grappa.antiAliasText = true;
		Grappa.useAntiAliasing = true;
	}
}
