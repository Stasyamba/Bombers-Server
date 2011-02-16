package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;
import com.vensella.bombers.game.mapObjects.Locations;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class GameManagerFastJoin extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		int locationId = params.getInt("interface.gameManager.fastJoin.fields.locationId");
		String gameName = params.getUtfString("interface.gameManager.fastJoin.fields.gameName");
		String password = params.getUtfString("interface.gameManager.fastJoin.fields.password");
		
		if (gameName.isEmpty() == false) {
			dispatcher.fastJoin(user, gameName, password);
		} else if (Locations.isLocationIdValid(locationId)) {
			dispatcher.fastJoin(user, locationId);
		} else {
			dispatcher.fastJoin(user);
		}
	}

}
