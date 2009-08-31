package net.sf.opendf.profiler.data;

import java.util.Map;

/**
 * 
 * @author jornj
 */
public class Dependency extends AttributeCarrier {
	
	public Integer  getSource() {
		return source;
	}
	
	public Integer  getDestination() {
		return destination;
	}
	
	public Map getAttributes() {
		return attributes;
	}
	
	
	public Dependency(Integer source, Integer destination, Map attributes) {
		this.source = source;
		this.destination = destination;
		this.attributes = attributes;
	}
	
	
	private Integer source;
	private Integer destination;
	private Map attributes;

}
