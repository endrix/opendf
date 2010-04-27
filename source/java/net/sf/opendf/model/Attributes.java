package net.sf.opendf.model;

import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;

public class Attributes {

	public Map<String, Expression>		valueAttributes() {
		return valueAttrs;
	}
	
	public Map<String, TypeExpr>		typeAttributes() {
		return typeAttrs;
	}
	
	
	public Attributes(Map<String, Expression> valueAttrs,
			Map<String, TypeExpr> typeAttrs) {

		this.valueAttrs = valueAttrs;
		this.typeAttrs = typeAttrs;
	}


	private Map<String, Expression>		valueAttrs;
	private Map<String, TypeExpr>		typeAttrs;	
}
