package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class InterfaceBuyItemEventHandler extends BaseClientRequestHandler {

	//Constants
	
	private static final String C_FldItemId = "interface.buyItem.fields.itemId";
	private static final String C_FldResourceType = "interface.buyItem.fields.resourceType";
	
	//Methods
	
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		int itemId = params.getInt(C_FldItemId);
		int resourceType = params.getInt(C_FldResourceType);
		
		dispatcher.getInterfaceManager().buyItem(user, itemId, resourceType);
	}

}
