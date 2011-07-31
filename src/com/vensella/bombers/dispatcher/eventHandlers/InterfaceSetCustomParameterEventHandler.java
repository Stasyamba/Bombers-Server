package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class InterfaceSetCustomParameterEventHandler extends
		BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispathcer = (BombersDispatcher)getParentExtension();
		int key = params.getInt("interface.setCustomParameter.f.key");
		if (params.containsKey("interface.setCustomParameter.f.intValue")) {
			int intValue = params.getInt("interface.setCustomParameter.f.intValue");
			dispathcer.getInterfaceManager().setCustomParameter(user, key, intValue);
		} else if (params.containsKey("interface.setCustomParameter.f.stringValue")) {
			String stringValue = params.getUtfString("interface.setCustomParameter.f.stringValue");
			dispathcer.getInterfaceManager().setCustomParameter(user, key, stringValue);
		}
	}

}
