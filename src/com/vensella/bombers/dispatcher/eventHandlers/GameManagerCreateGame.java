package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class GameManagerCreateGame extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		int locationId = params.getInt("interface.gameManager.createGame.fields.locationId");
		String gameName = params.getUtfString("interface.gameManager.createGame.fields.gameName");
		String password = params.getUtfString("interface.gameManager.createGame.fields.password");
		
		dispatcher.createGame(user, locationId, gameName, password);
	}

}
