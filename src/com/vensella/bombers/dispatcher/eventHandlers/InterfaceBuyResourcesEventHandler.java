package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class InterfaceBuyResourcesEventHandler extends BaseClientRequestHandler {

	//Constants
	
	private static final String C_FldResourceCountType0 = "interface.buyResources.fields.resourceType0";
	private static final String C_FldResourceCountType1 = "interface.buyResources.fields.resourceType1";
	private static final String C_FldResourceCountType2 = "interface.buyResources.fields.resourceType2";
	private static final String C_FldResourceCountType3 = "interface.buyResources.fields.resourceType3";
	private static final String C_FldResourceCountType4 = "interface.buyResources.fields.resourceType4";
	
	//Methods
	
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		
		int resourceCountType0 = params.getInt(C_FldResourceCountType0);
		int resourceCountType1 = params.getInt(C_FldResourceCountType1);
		int resourceCountType2 = params.getInt(C_FldResourceCountType2);
		int resourceCountType3 = params.getInt(C_FldResourceCountType3);
		int resourceCountType4 = params.getInt(C_FldResourceCountType4);
		
		dispatcher.getInterfaceManager().buyResources(
				user, 
				resourceCountType0, 
				resourceCountType1, 
				resourceCountType2, 
				resourceCountType3, 
				resourceCountType4
			);
		
	}

}
