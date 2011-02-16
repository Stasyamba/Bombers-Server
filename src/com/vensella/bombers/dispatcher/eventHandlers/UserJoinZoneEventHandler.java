package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class UserJoinZoneEventHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		User user = (User)event.getParameter(SFSEventParam.USER);
		dispatcher.loginUser(user);	
	}

}
