/* 
BEGINCOPYRIGHT X,UC

	Copyright (c) 2009, EPFL
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
package net.sf.opendf.plugin.causation;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.jgrapht.ListenableGraph;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JGraphTLoader {
	
	private Map<String,String> nodes = new TreeMap<String,String>();
	
	private void getEdgeList(NodeList list, ListenableGraph result, String dest){
		for(int i=0;i<list.getLength();i++){
			Node item = list.item(i);
			if(item.getNodeName()=="dependency"){
				result.addEdge( nodes.get(item.getAttributes().getNamedItem("source").getNodeValue()), dest );
			}
		}
	}
	
	public ListenableGraph readXML(String file){
		try{
			Node doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(file));
			NodeList list = doc.getChildNodes();
			
			ListenableGraph result = new ListenableDirectedGraph( DefaultEdge.class );

			
			// Enter <causation-trace>
			for(int i=0;i<list.getLength();i++){
				Node item = list.item(i);
				if(item.getNodeName()=="causation-trace"){
					list = item.getChildNodes();
					break;
				}
			}
			
			for(int i=0;i<list.getLength();i++){
				Node item = list.item(i);
				if(item.getNodeName()=="step"){
					String id = item.getAttributes().getNamedItem("ID").getNodeValue();
					String name = item.getAttributes().getNamedItem("actor-name").getNodeValue() + ":" + id;
					nodes.put(id, name);
					result.addVertex(name);					
					NodeList nl = item.getChildNodes();
					getEdgeList(nl,result,name);
				}
			}
			return result;
		}
		catch(Exception err){
			return null;
		}
	}
}
