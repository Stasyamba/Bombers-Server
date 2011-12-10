package com.vensella.bombers.game.weapons;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.PlayerGameProfile;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.WeaponsManager;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class NormalMine extends DynamicObject {

	//Constructor
	
	public NormalMine(BombersGame game, int x, int y) {
		super(game, false, true, false);
		this.x = x;
		this.y = y;
	}
	
	//Fields
	
	private int x;
	private int y;
	
	//Methods
	
	@Override
	public GameEvent getActivateEvent() {
		return new GameEvent(getGame()) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				setActivated(true);
				map.removeDynamicObject(x, y);
				
				SFSObject params = new SFSObject();
				if (getOwner() != null) {
					params.putUtfString("game.DOAct.f.userId", getOwner().getName());
					long time = System.currentTimeMillis();
					PlayerGameProfile player = game.getGameProfile(getOwner());
					if (player.getDamageTime() + PlayerGameProfile.C_ImmortalTime < time) {
						player.setDamageTime(time);
						game.damagePlayer(getOwner(), 3 * PlayerGameProfile.C_HealthQuantum, 0, false);
					}
				}
				params.putInt("game.DOAct.f.type", WeaponsManager.WEAPON_MINE_NORMAL);
				params.putInt("game.DOAct.f.x", x);
				params.putInt("game.DOAct.f.y", y);
				params.putInt("game.DOAct.f.id", getId());
				params.putBool("game.DOAct.f.isRemoved", true);
				getGame().send("game.DOAct", params, getGame().getParentRoom().getPlayersList());	
			}
		};
	}
	
	@Override
	public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId, int damage) {
		baseEvent.beginDestroyingDynamicObject(x, y, getId());
		map.removeDynamicObject(x, y);
	}

}
