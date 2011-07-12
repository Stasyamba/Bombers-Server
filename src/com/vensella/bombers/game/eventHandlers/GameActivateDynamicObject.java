package com.vensella.bombers.game.eventHandlers;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.game.BombersGame;

@Deprecated
public class GameActivateDynamicObject extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersGame game = (BombersGame)getParentExtension();
		int x = params.getInt("game.actDO.f.x");
		int y = params.getInt("game.actDO.f.y");
		
		game.activateDynamicObject(user, x, y);
	}

}
