/**
 * 
 */
package eu.actorsproject.xlim.implementation;

import eu.actorsproject.xlim.type.FixIntegerTypeRule;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.util.Session;
import eu.actorsproject.xlim.util.XlimFeature;

/**
 * Operations on unsigned int "uint"
 *
 */
public class UintOperations extends XlimFeature {

	@Override
	public void addOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind uintKind=fact.getTypeKind("uint");
		Signature unary=new Signature(uintKind);
		Signature binary=new Signature(uintKind,uintKind);
		TypeKind boolKind=fact.getTypeKind("bool");
		
		// $literal_Integer: void -> uint
		OperationKind literal=new IntegerAttributeOperationKind("$literal_Integer",
				new LiteralIntegerTypeRule(uintKind,true /* isSigned */),
				"value");
		s.registerOperation(literal);
		
		// $add: (uint,uint) -> uint
		TypeRule addRule=new AddTypeRule(binary,uintKind,33);
		OperationKind add=new OperationKind("$add", addRule);
		s.registerOperation(add);
		
		// $sub: (uint,uint) -> uint
		OperationKind sub=new OperationKind("$sub", addRule);
		s.registerOperation(sub);
				
		// $mul: (uint,uint) -> uint
		OperationKind mul=new OperationKind("$mul", new MulTypeRule(binary,uintKind,33));
		s.registerOperation(mul);
		
		// $div: (uint,uint) -> uint
		OperationKind div=new OperationKind("$div", new FirstInputTypeRule(binary,uintKind));
		s.registerOperation(div);
		
		// rshift: (uint,int) -> uint
		// urshift: (uint,int) -> uint
		String rShifts[]={"rshift", "urshift"};
		Signature binaryUintInt=new Signature(uintKind,fact.getTypeKind("int"));
		TypeRule rShiftRule=new FirstInputTypeRule(binaryUintInt,uintKind);
		registerAll(rShifts, rShiftRule, s);
		
		// lshift: (uint,uint) -> uint(33)
		OperationKind lshift=new OperationKind("lshift", 
                new FixIntegerTypeRule(binary,uintKind,33));
		s.registerOperation(lshift);
		
		// bitand: (uint,uint) -> uint
		// bitor: (uint,uint) -> uint
		// bitxor: (uint,uint) -> uint
		TypeRule bitOpRule=new WidestInputTypeRule(binary,uintKind);
		String bitOps[]={"bitand", "bitor", "bitxor"};
		registerAll(bitOps, bitOpRule, s);
		
		// bitnot: uint -> uint
		OperationKind bitnot=new OperationKind("bitnot", 
				                               new WidestInputTypeRule(unary,uintKind));
		s.registerOperation(bitnot);
		
		// noop: uint->uint
		OperationKind noop=new OperationKind("noop", 
                new FirstInputTypeRule(unary,uintKind));
		s.registerOperation(noop);
		
		// $selector: (bool,uint,uint) -> uint
		Signature ternarySignature=new Signature(boolKind, uintKind, uintKind);
		OperationKind selector=new OperationKind("$selector", 
				new WidestInputTypeRule(ternarySignature,uintKind));
		s.registerOperation(selector);
		
		// mod: uint x uint -> uint (there's also int x int --> int and real x real -> real)
		OperationKind mod=new OperationKind("$mod", new ModTypeRule(binary,uintKind));
		s.registerOperation(mod);
	}
	
	private void registerAll(String[] xlimOps, TypeRule typeRule, InstructionSet s) {
		for (String name: xlimOps) {
		    OperationKind op=new OperationKind(name, typeRule);
		    s.registerOperation(op);
		}
	}
}
