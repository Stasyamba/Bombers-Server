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

import com.vensella.bombers.game.BombersGame;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class UserDisconnectEventHandler extends BaseServerEventHandler {

	//@SuppressWarnings("unchecked")
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		User user = (User)event.getParameter(SFSEventParam.USER);
		@SuppressWarnings("unchecked")
		List<Room> rooms = (List<Room>)event.getParameter(SFSEventParam.JOINED_ROOMS);
		
		for (Room room : rooms) {
			if (room.getExtension() instanceof BombersGame) {
				BombersGame game = (BombersGame) room.getExtension();
				game.processUserLeave(user);
			}
		}
	}

}
