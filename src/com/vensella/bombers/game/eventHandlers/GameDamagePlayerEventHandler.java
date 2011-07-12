package com.vensella.bombers.game.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.game.BombersGame;

@Deprecated
@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class GameDamagePlayerEventHandler extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersGame game = (BombersGame)getParentExtension();
		int damage = params.getInt("game.damagePlayer.fields.damage");
		//int effect = params.getInt("game.damagePlayer.fields.effect");
		int effect = 0;
		boolean isDead = params.getBool("game.damagePlayer.fields.isDead");

		game.damagePlayer(user, damage, effect, isDead);
	}

}
