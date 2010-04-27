package net.sf.opendf.model;

import java.util.Map;

import net.sf.opendf.cal.ast.Decl;
import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.PortDecl;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.ast.TypeParDecl;

public interface ModelClass {
	
	ModelInstance  	instantiate(Map<String, Expression> vpars, Map<String, TypeExpr> tpars);
	ModelInstance  	instantiate(Map<String, Expression> vpars);
	
	Decl []			getParameterDecls();
	TypeParDecl	[]	getTypeParameterDecls();
	
	PortDecl []		getInputPortDecl();
	PortDecl []		getOutputPortDecl();
	
	Source			getSource();
}
