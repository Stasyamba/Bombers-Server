package com.vensella.bombers.game.dynamicObjects;

import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.PlayerGameProfile;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class LavaObject extends DynamicObject {

	//Constructor
	
	public LavaObject(BombersGame game, int x, int y) {
		super(game, true, true, false, true, false);
	}
	
	//Methods override
	
	@Override
	public GameEvent getActivateEvent() {
		return new GameEvent(getGame()) {
			
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				long time = System.currentTimeMillis();
				
				PlayerGameProfile profile = game.getGameProfile(getOwner());
				if (profile.getDamageTime() + PlayerGameProfile.C_ImmortalTime < time) {
					profile.setDamageTime(time);
					game.damagePlayer(getOwner(), PlayerGameProfile.C_HealthQuantum, 0, false);
				}
			}
		};
	}

	@Override
	public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game,
			DynamicGameMap map, int weaponId) {

	}

}
