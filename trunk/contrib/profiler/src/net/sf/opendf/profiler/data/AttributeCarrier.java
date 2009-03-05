package net.sf.opendf.profiler.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AttributeCarrier {

	public void setAttribute(Object k, Object v) {
		if (attributes == null) {
			attributes = new HashMap();
		}
		attributes.put(k, v);
	}
	
	public Object getAttribute(Object k) {
		if (attributes == null) {
			return null;
		} else {
			return attributes.get(k);
		}
	}
	
	public boolean hasAttribute(Object k) {
		if (attributes == null)
			return false;
		return attributes.containsKey(k);
	}
	
	public Map  attributes() {
		if (attributes == null) {
			return Collections.EMPTY_MAP;
		} else {
			return attributes;
		}
	}
	
	protected  void cloneAttributes(AttributeCarrier ac) {
		if (ac.attributes == null) {
			attributes = null;
		} else {
			attributes = new HashMap(ac.attributes);
		}
	}
	
	private Map   attributes = null;
	

}
