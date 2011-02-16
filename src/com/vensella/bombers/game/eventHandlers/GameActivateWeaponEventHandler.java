package com.vensella.bombers.game.eventHandlers;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.game.BombersGame;

public class GameActivateWeaponEventHandler extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersGame game = (BombersGame)getParentExtension();
		int x = params.getInt("game.AW.f.x");
		int y = params.getInt("game.AW.f.y");
		int t = params.getInt("game.AW.f.t");
		
		game.activateWeapon(user, t, x, y);
	}

}
