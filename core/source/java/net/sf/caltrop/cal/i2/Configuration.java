package net.sf.caltrop.cal.i2;

import java.util.List;

import net.sf.caltrop.cal.ast.ExprIf;
import net.sf.caltrop.cal.ast.ExprLiteral;
import net.sf.caltrop.cal.ast.ExprVariable;
import net.sf.caltrop.cal.ast.StmtAssignment;

public interface Configuration {
	
	Object  createInteger(int n);
	
	Object 	createLiteralValue(ExprLiteral literal);
	
	Object  createList(List list);
	Object  createEmptyList();
	void	addListElement(Object list, Object element);
	
	Object  createEmptySet();
	void	addSetElement(Object set, Object element);
	
	Object  createEmptyMap();
	void    addMapping(Object map, Object key, Object value);
	
	Object	selectField(Object a, String fieldName);
	void    indexInto(Object structure, int nIndices, OperandStack stack);
	
	boolean booleanValue(Object v);
	int     intValue(Object v);
	List    getList(Object v);
	
	Object  cond(Evaluator evaluator, ExprIf expr);
	
	Object  lookupVariable(Environment env, ExprVariable expr);
	
	void  	assign(Object value, Environment env, StmtAssignment stmt);
	void	assign(Object value, Environment env, StmtAssignment stmt, int nIndices, OperandStack stack);
	void	assignField(Object value, Environment env, StmtAssignment stmt, String field);


	Object  convertJavaResult(Object res);
	Object  createClassObject(Class c);	
	
	boolean isAssignableToJavaType(Object v, Class c);
	Object  convertToJavaType(Object v, Class c);
}
