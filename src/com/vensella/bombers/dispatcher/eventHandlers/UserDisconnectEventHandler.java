package com.vensella.bombers.dispatcher.eventHandlers;

import java.util.List;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class UserDisconnectEventHandler extends BaseServerEventHandler {

	//@SuppressWarnings("unchecked")
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		final BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		final User user = (User)event.getParameter(SFSEventParam.USER);
		@SuppressWarnings("unchecked")
		final List<Room> rooms = (List<Room>)event.getParameter(SFSEventParam.JOINED_ROOMS);

		dispatcher.processUserLeave(user, rooms);
	}

}
