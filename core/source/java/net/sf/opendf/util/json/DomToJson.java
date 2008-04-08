/**
 * 
 */
package net.sf.opendf.util.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author mwipliez
 * 
 */
public class DomToJson {

	/**
	 * 
	 */
	public DomToJson() {
	}

	public JSONArray jsonOfNode(Node node) throws JSONException {
		// Node name
		JSONArray element = new JSONArray();
		element.put(node.getNodeName());

		// Node attributes
		if (node.hasAttributes()) {
			NamedNodeMap attrs = node.getAttributes();
			JSONObject attributes = new JSONObject();
			for (int i = 0; i < attrs.getLength(); i++) {
				Node attr = attrs.item(i);
				String attrName = attr.getNodeName();
				String attrValue = attr.getNodeValue();
				attributes.put(attrName, attrValue);
			}
			element.put(attributes);
		}

		// Node children
		JSONArray jsonChildren = new JSONArray();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				String value = child.getTextContent();
				value = value.trim();
				if (!value.isEmpty()) {
					jsonChildren.put(value);
				}
			} else {
				JSONArray childElement = jsonOfNode(child);
				jsonChildren.put(childElement);
			}
		}
		element.put(jsonChildren);

		return element;
	}

}
