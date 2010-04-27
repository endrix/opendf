package net.sf.opendf.model;

import java.util.Collection;
import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;

import org.w3c.dom.Node;

/**
 * A NetworkDescriptor provides access to the content of a network, i.e. the 
 * 
 * @author jwj
 *
 */

public interface NetworkDescriptor {

	Collection<Instance>		nodes();
	Collection<Connection>	connections();
	
	Attributes				attributes();
	
	/**
	 * Returns a DOM Node that represents an XDF element which contains a description of the network. 
	 * 
	 * @return An XDF element.
	 */
	
	Node	getXDF();
		
	interface Instance {
		String	name();
		
		Attributes attributes();
	}
	
	interface Connection {
		String	source();
		String	sourcePort();
		String	destination();
		String	destinationPort();
		
		Attributes	attributes();
	}
	
	
	
}
