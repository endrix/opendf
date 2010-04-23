package net.sf.opendf.xslt.cal;

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
    public final static String nameList = "List";
    public final static String nameMethod = "_method";
    public final static String nameNull = "null";
    public final static String nameProcedure = "procedure";
    public final static String nameReal = "real";
    public final static String nameString = "String";

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
    		int asz = a.getBitLength(0);
    		int bsz = b.getBitLength(0);
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

    /**
     * Computes the bit length of an operation involving the integers <code>a</code>
     * and <code>b</code>. Simply put, if none has a size, 32 is returned. If one
     * has a size, and not the other, this size is returned. If both have a size,
     * the maximum size is returned.
     * @param a An integer type.
     * @param b An integer type.
     * @return See description above.
     */
	public static int computeBitLength(Type a, Type b) {
		int w1 = a.getBitLength(0);
		int w2 = b.getBitLength(0);
		
		int bitLength = 32;
		if (w1 > 0 || w2 > 0) {
			bitLength = Math.max(w1, w2);
		}
		
		return bitLength;
	}

	/**
	 * Returns the size of this type. This is only valid if this type is an integer.
	 * If this integer has no size parameter, the default size is returned.
	 * @param defaultSize The default size.
	 * @return The bit length of this integer, or defaultSize if it has none.
	 * @throws RuntimeException If this type is not an integer.
	 */
	public int getBitLength(int defaultSize) {
		if (nameInt.equals(getName())) {
			int bitLength = defaultSize;
			Object obj = getValueParameters().get(Type.vparSize);
			if (obj instanceof Integer) {
				bitLength = ((Integer) obj).intValue();
			}
			
			return bitLength;
		} else {
			throw new RuntimeException("Cannot get the bit length of a non-integer type!");
		}
	}

}

