package com.vensella.bombers.game.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.game.BombersGame;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class LobbyUserReadyEventHandler extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		boolean isReady = params.getBool("game.lobby.userReady.fields.isReady");
		BombersGame game = (BombersGame)getParentExtension();
		
		if (isReady) {
			game.setUserReady(user, isReady);
		}
	}

}
