package net.sf.opendf.model;

import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;

/**
 * A ModelInstance represents an instantiated part of the model, which is either an (atomic) actor or a network.
 * It is the result of "instantiating" a ModelClass, it may be contained within another instance (called its "parent"), 
 * and it may itself contain further instances (if it is a network), its "children".
 * 
 * Depending on whether it is an actor or a network, an ActorDescriptor or a NetworkDescriptor may be obtained from it.
 * 
 * @author jwj
 *
 */

public interface ModelInstance {
	
	/**
	 * Test whether this instance is atomic.
	 * 
	 * @return True, if this is an atomic actor, false if this is a network.
	 */

	boolean  					isAtomic();

	/**
	 * Obtain the model class that this instance was instantiated from.
	 * 
	 * @return This instance's model class.
	 */

	ModelClass  				getModelClass();
	
	/**
	 * 
	 * @return
	 */
	
	Map<String, Expression>  	getParameters();
	
	ModelInstance				getParent();
	
	Map<String, ModelInstance> 	getChildren();
	
	String						getLocalName();
	
	String []					getPath();
	
	Map<String, TypeExpr>		getInputPorts();
	
	Map<String, TypeExpr>		getOutputPorts();
	
	
	ActorDescriptor				getActorDescriptor();
	
	NetworkDescriptor			getNetworkDescriptor();
		
}
