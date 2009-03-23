package net.sf.opendf.profiler.schedule.data;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.util.logging.Logging;

/**
 * 
 * @author jornj
 */
public class ResourceConfiguration {
	
	public InstanceMapping []       instanceMapping;
	public ClassMapping []  		classMapping;
	public ResourceConstraint []  	resourceConstraint;
	public boolean					allowClientSideMultiplexing;
	
	
	public ResourceConfiguration(InstanceMapping [] im, ClassMapping [] cm, ResourceConstraint [] rc, boolean allowClientSideMultiplexing) {
		this.instanceMapping = im;
		this.classMapping = cm;
		this.resourceConstraint = rc;
		this.allowClientSideMultiplexing = allowClientSideMultiplexing;
	}
	
	
	public static ResourceConfiguration  createConfiguration(InputStream is) {
		final List imappings = new ArrayList();
		final List cmappings = new ArrayList();
		final List constraints = new ArrayList();
		final Set flags = new HashSet();
		
		DefaultHandler saxHandler = new DefaultHandler() {
			public void startElement(String uri, String localName, String qName, Attributes attributes) {				
				try {
					if ("resource-configuration".equals(qName)) {			
						Map a = createAttributeMap(attributes);
						String allowCSM = ((String)a.get("allow-client-side-multiplexing"));
						allowCSM = (allowCSM == null) ? "no" : allowCSM.trim().toLowerCase();
						if ("yes".equals(allowCSM) || "true".equals(allowCSM)) {
							flags.add(ALLOWCLIENTSIDEMULTIPLEXING);
						}
						return;
					}					
					if ("instance-mapping".equals(qName)) {			
						Map a = createAttributeMap(attributes);
						int actorID = Integer.parseInt((String)a.get("actor-id"));
						String actorClassName = (String)a.get("actor-class");
						int action = Integer.parseInt((String)a.get("action"));
						Object resID = a.get("resource-class");
						int resourceID = Integer.parseInt((String)a.get("resource-id"));
						long latency = Long.parseLong((String)a.get("latency"));
						String s = (String)a.get("initiation-interval");
						long initiationInterval = (s == null) ? latency : Long.parseLong(s);
						imappings.add(new InstanceMapping(actorID, actorClassName, action, resID, resourceID, latency, initiationInterval));
						return;
					}					
					if ("class-mapping".equals(qName)) {			
						Map a = createAttributeMap(attributes);
						String actorClassName = (String)a.get("actor-class");
						int action = Integer.parseInt((String)a.get("action"));
						Object resID = a.get("resource-class");
						long latency = Long.parseLong((String)a.get("latency"));
						String s = (String)a.get("initiation-interval");
						long initiationInterval = (s == null) ? latency : Long.parseLong(s);
						cmappings.add(new ClassMapping(actorClassName, action, resID, latency, initiationInterval));
						return;
					}					
					if ("resource-constraint".equals(qName)) {
						Map a = createAttributeMap(attributes);
						Object resID = a.get("resource-class");
						int poolSize = Integer.parseInt(((String)a.get("pool-size")));		
						constraints.add(new ResourceConstraint(resID, poolSize));
						return;
					}
					return;
				}
				catch (Exception e) {
                    Logging.dbg().throwing("ResourceConfiguration", "createConfiguration", e);
					throw new RuntimeException(e);
				}
			}
			
			private Map  createAttributeMap(Attributes attributes) {
				Map a = new HashMap();
				for (int i = 0; i < attributes.getLength(); i++) {
					a.put(attributes.getQName(i), attributes.getValue(i));
				}
				return a;
			}			
		};
		
		try {
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			p.parse(is, saxHandler);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return new ResourceConfiguration((InstanceMapping [])imappings.toArray(new InstanceMapping[imappings.size()]), 
									     (ClassMapping [])cmappings.toArray(new ClassMapping[cmappings.size()]), 
										 (ResourceConstraint [])constraints.toArray(new ResourceConstraint[constraints.size()]),
										 flags.contains(ALLOWCLIENTSIDEMULTIPLEXING));

	}
	
	final static String ALLOWCLIENTSIDEMULTIPLEXING = "AllowClientSideMultiplexing";
}
