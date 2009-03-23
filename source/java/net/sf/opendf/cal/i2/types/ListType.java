package net.sf.opendf.cal.i2.types;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.types.BoundedIntegralType.TheClass;

public class ListType extends AbstractType implements Type {

	//
	//  Type
	//

	@Override
	public boolean contains(Object v) {
		if (! (v instanceof List)) {
			return false;
		}
		List vl = (List)v;
		if (size >= 0 && vl.size() != size) {
			return false;
		}
		if (tElement == null) {
			return true;
		}
		for (Object a : vl) {
			if (! tElement.contains(a)) {
				return false;
			}
		}
		return true;
	}


	@Override
	public Object convert(Object v) {
		if (tElement == null)
			return v;
		
		assert v instanceof List;  // follows from the definition of convertible.
		
		List vl = (List)v;
		List res = new ArrayList();
		for (Object a : vl) {
			res.add(tElement.convert(a));
		}
		return res;
	}

	@Override
	public boolean convertible(Object v) {
		if (! (v instanceof List)) {
			return false;
		}
		List vl = (List)v;
		if (size >= 0 && vl.size() != size) {
			return false;
		}
		if (tElement == null) {
			return true;
		}
		for (Object a : vl) {
			if (! tElement.convertible(a)) {
				return false;
			}
		}
		return true;
	}

	// 
	// Ctor
	//


	private ListType(TheClass typeClass, Type tElement, int sz) {
		super(typeClass);
		this.tElement = tElement;
		this.size = sz;
		
	}

	//
	//  data
	//

	private Type tElement;
	private int size;


	////////////////////////////////////////////////////////////////////////////
	////  TypeClass
	////////////////////////////////////////////////////////////////////////////

	public static class TheClass extends AbstractTypeClass  {

		@Override
		public Type createType(TypeExpr te, Evaluator eval) {
			int sz = getIntParameter(te, vpSize, -1, eval);
			Map<String, TypeExpr> tp = te.getTypeParameters();
			Type t = getTypeParameter(te, tpElement, null, eval);
			return new ListType(this, t, sz);
		}

		public TheClass(String name, TypeSystem typeSystem) {
			super(name, typeSystem);
		}

		final static String vpSize = "size"; 
		final static String tpElement = "element"; 
	}


}
