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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
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
						return "";
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
			exporter.export(new PrintWriter(new FileOutputStream(path + "/causation_trace.dot")),lg);
			exporter2.export(new PrintWriter(new FileOutputStream(path + "/dependency_graph.dot")),sg);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
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
		String aid = "";

		pw.print("    <step ID=\"" + stepID + "\"");
		for (Iterator<Object> i = stepAttrs.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			Object v = stepAttrs.get(k);
			pw.print(" " + k.toString() + "=\"" + v.toString() + "\"");
			if(k.toString().equals("actor-name")){
				sname = v.toString();
			}
			if(k.toString().equals("action-tag")){
				aname = v.toString();
			}
			if(k.toString().equals("actor-id")){
				iname = v.toString();
			}
			if(k.toString().equals("action")){
				aid = v.toString();
			}
			
		}
		pw.println(">");
		if(aname.equals(""))
			aname= "action"+aid;
		actionTag.put(sname+"/"+aid, aname);
		
		String name = sname + "/" + aname + "*" + Long.toString(stepID);
		lg.addVertex(name);
		
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
		sg = new DefaultDirectedGraph<CountVertex,CountEdge>(CountEdge.class);
		msg = new HashMap<String,CountVertex>();
		msgi = new HashMap<String,CountVertex>();
		actionTag = new HashMap<String,String>();
		
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
	private DefaultDirectedGraph<CountVertex,CountEdge> sg;
	private Map<String,CountVertex> msg;
	private Map<String,CountVertex> msgi;
	private Map<String,String> nodes;
	private Map<String,String> actionTag;
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
	
	private static String path;
	
	/**
	 * Set the output
	 * @param newpath New ouput path
	 */
	public static void setPath(String newpath){
		path = newpath;
	}
}
