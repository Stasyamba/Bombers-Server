package com.vensella.bombers.game.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.game.BombersGame;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class GameInputDirectionChangedEventHandler extends
		BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersGame game = (BombersGame)getParentExtension();
		int inputDirection = params.getInt("dir");
		int x = params.getInt("x");
		int y = params.getInt("y");
		
		game.processInputDirectionChanged(user, inputDirection, x, y);
		
		//send("game.IDC", params, game.getParentRoom().getPlayersList());
	}

}
