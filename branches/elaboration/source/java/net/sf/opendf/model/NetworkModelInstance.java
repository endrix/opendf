package net.sf.opendf.model;

import java.util.Collection;
import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import org.w3c.dom.Node;

public class NetworkModelInstance implements ModelInstance, NetworkDescriptor {
	
	//
	//  ModelInstance
	//
	
	@Override
	public boolean isAtomic() {
		return false;
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModelClass getModelClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkDescriptor getNetworkDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, TypeExpr> getOutputPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Expression> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModelInstance getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ActorDescriptor getActorDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ModelInstance> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, TypeExpr> getInputPorts() {
		// TODO Auto-generated method stub
		return null;
	}


	//
	//  NetworkDescriptor
	//


	@Override
	public Attributes attributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Connection> connections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Instance> nodes() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Node getXDF() {
		return xdf;
	}


	//
	//  ctor
	//
	
	public NetworkModelInstance(String localName, Node xdf) {
		this.localName = localName;
		this.xdf = xdf;
	}
	
	
	private String 	localName;
	private Node	xdf;

}
