/**
 * 
 */
package eu.actorsproject.xlim.type;

import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.util.XlimFeature;

/**
 * Packages support for the unsigned integer "uint"
 *
 */
public class UintTypeFeature extends XlimFeature {

	@Override
	public void addTypes(TypeSystem typeSystem) {
		TypeKind uintKind=new IntegerTypeKind("uint",false /*means unsigned*/);
		typeSystem.addTypeKind(uintKind);
	}
	
	@Override
	public void addTypeConversions(TypeSystem typeSystem) {
		TypeKind intKind=typeSystem.getTypeKind("int");
		TypeKind uintKind=typeSystem.getTypeKind("uint");
		
		typeSystem.addDefaultTypePromotion(new UintToIntConversion(uintKind, intKind));
		typeSystem.addTypeConversion(new TypeConversion(intKind, uintKind));
	}
}

class UintToIntConversion extends TypeConversion {

	public UintToIntConversion(TypeKind uintKind, 
				               TypeKind intKind) {
		super(uintKind, intKind);
	}

	@Override
	public XlimType apply(XlimType sourceT) {
		int width=sourceT.getSize() + 1;
		return getTargetTypeKind().createType(width);
	}
}
