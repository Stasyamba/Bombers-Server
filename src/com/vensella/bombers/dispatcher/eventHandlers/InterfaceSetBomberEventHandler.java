package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

public class InterfaceSetBomberEventHandler extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		int bomberId = params.getInt("interface.setBomber.fields.bomberId");
		
		dispatcher.getInterfaceManager().setBomberId(user, bomberId);
	}

}
