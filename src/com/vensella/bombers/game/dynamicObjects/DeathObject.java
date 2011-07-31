package com.vensella.bombers.game.dynamicObjects;

import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.PlayerGameProfile;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class DeathObject extends DynamicObject {
	
	//Constructor
	
	public DeathObject(BombersGame game) {
		super(game, true, true);
	}
	
	//Fields
	
	//Methods
	
	@Override
	public GameEvent getActivateEvent() {
		return new GameEvent(getGame()) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				PlayerGameProfile profile = game.getGameProfile(getOwner());
				game.damagePlayer(profile.getUser(), profile.getHealth(), 0, true);	
			}
		};
	}
	
	@Override
	public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId) {
		
	}


}
