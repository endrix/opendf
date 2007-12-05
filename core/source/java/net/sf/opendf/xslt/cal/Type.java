package net.sf.caltrop.xslt.cal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class Type {

	public String getName() {
		return name;
	}
	
	public Map  getTypeParameters() {
		return typeParameters;
	}
	
	public Map  getValueParameters() {
		return valueParameters;
	}
		
	private Type(String name) {
		this (name, Collections.EMPTY_MAP, Collections.EMPTY_MAP);;
	}
	
	private  Type(String name, Map typeParameters, Map valueParameters) {
		this.name = name;
		this.typeParameters = typeParameters;
		this.valueParameters = valueParameters;
	}
	
	public  int hashCode() {
		int n = name.hashCode();
		if (typeParameters != null)
			n += typeParameters.hashCode();
		if (valueParameters != null)
			n += valueParameters.hashCode();
		return n;
	}
	
	public boolean equals(Object a) {
		if (! (a instanceof Type)) 
			return false;
		Type t = (Type)a;
		if (!name.equals(t.name))
			return false;
		if (!typeParameters.equals(t.typeParameters))
			return false;
		if (!valueParameters.equals(t.valueParameters))
			return false;
		return true;
	}
	
	public String toString() {
		return "<<" + name + " : " + typeParameters + " : " + valueParameters + ">>";
	}
	
	public static Type  create(String name) {
		return new Type(name);
	}
	
	public static Type  create(String name, Map typePars, Map valuePars) {
		return new Type(name, typePars, valuePars);
	}
	
	private String name;
	private Map  typeParameters;
	private Map  valueParameters;
	
	
    public final static String nameANY = "ANY";

    public final static String nameBool = "bool";
    public final static String nameClass = "_class";
    public final static String nameFunction = "function";
    public final static String nameInt = "int";
    public final static String nameList = "list";
    public final static String nameMethod = "_method";
    public final static String nameNull = "null";
    public final static String nameProcedure = "procedure";
    public final static String nameReal = "real";
    public final static String nameString = "string";

    public final static String tparType = "type";
    public final static String vparSize = "size";
    
    public final static Type typeANY = new Type(nameANY);

    public final static Type typeBool = new Type(nameBool);
    public final static Type typeClass = new Type(nameClass);
    public final static Type typeFunction = new Type(nameFunction); //FIXME: allow parameter/return types
    public final static Type typeMethod = new Type(nameMethod);
    public final static Type typeNull = new Type(nameNull);
    public final static Type typeProcedure = new Type(nameProcedure); // FIXME: allow parameter types
    public final static Type typeReal = new Type(nameReal);
    public final static Type typeString = new Type(nameString);
    
    public final static List basicTypes = Arrays.asList(new Type [] {
    	typeBool, typeNull, typeReal, typeString, 
    	typeFunction, typeProcedure
    });
    
    public static Type  lub(Type a, Type b) {
    	
    	if (!a.getName().equals(b.getName()))
    		return null;
    	
    	if (basicTypes.contains(a)) {
    		if (a.equals(b)) {
    			return a;
    		} else {
    			return null;
    		}
    	}
    	
    	if (nameInt.equals(a.getName())) {
    		int asz = ((Number)a.valueParameters.get(vparSize)).intValue();
    		int bsz = ((Number)b.valueParameters.get(vparSize)).intValue();
    		return (asz > bsz) ? a : b;
    	}
    	
    	if (nameList.equals(a.getName())) {
    		Type ta = (Type)a.typeParameters.get(tparType);
    		Type tb = (Type)b.typeParameters.get(tparType);
    		Type elementType = lub(ta, tb);
    		int asz = ((Number)a.valueParameters.get(vparSize)).intValue();
    		int bsz = ((Number)b.valueParameters.get(vparSize)).intValue();
    		int size = Math.max(asz, bsz); // FIXME: undefined if not equal?
    		return new Type(nameList,
    				        Collections.singletonMap(tparType, elementType),
    				        Collections.singletonMap(vparSize, new Integer(size)));
    	}
    	
    	if (nameANY.equals(a.getName()) || nameANY.equals(b.getName())) {
    		return typeANY;
    	}
    	
    	return null;
    }

}

