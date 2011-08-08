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
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

import com.vensella.bombers.dispatcher.BombersDispatcher;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class UserDisconnectEventHandler extends BaseServerEventHandler {

	//@SuppressWarnings("unchecked")
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		final BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		final User user = (User)event.getParameter(SFSEventParam.USER);
		@SuppressWarnings("unchecked")
		List<Room> rooms = (List<Room>)event.getParameter(SFSEventParam.JOINED_ROOMS);

		boolean gameLeavingProcessed = false;
		for (Room room : rooms) {
			if (room.getExtension() instanceof BombersGame) {
				if (gameLeavingProcessed) {
					dispatcher.trace(ExtensionLogLevel.ERROR, "User " + user.getName() + " has more then one joined games!!!");
					return;
				}
				BombersGame game = (BombersGame) room.getExtension();
				game.processUserLeave(user);
				game.addGameEvent(new GameEvent(game) {
					@Override
					protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
						dispatcher.processUserLeave(user);	
					}
				});
				gameLeavingProcessed = true;
			}
		}
		if (gameLeavingProcessed == false) {
			dispatcher.processUserLeave(user);
		}
	}

}
