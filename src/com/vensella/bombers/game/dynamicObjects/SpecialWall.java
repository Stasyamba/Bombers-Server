package com.vensella.bombers.game.dynamicObjects;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.DynamicObjectManager;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class SpecialWall extends DynamicObject {

	//Constants
	
	public static final int C_DestroysByAnyWeapon = -1;
	
	//Constructor
	
	public SpecialWall(BombersGame game, int x, int y, int destroysBy, int life) {
		super(game, false, false, true, true, true);
		f_x = x;
		f_y = y;
		f_destroysBy = destroysBy;
		f_life = life;
	}
	
	//Fields
	
	private int f_x;
	private int f_y;
	
	private int f_destroysBy;
	private int f_life;
	
	private long f_lastDamaged;
	
	//Methods
	
	public int getDestroysBy() { return f_destroysBy; }
	public int getLife() { return f_life; }
	
	@Override
	public GameEvent getActivateEvent() {
		final int life = f_life;
		return new GameEvent(getGame()) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {				
				SFSObject params = new SFSObject();
				params.putInt("game.DOAct.f.type", DynamicObjectManager.SPECIAL_WALL);
				params.putInt("game.DOAct.f.lifeLeft", life);
				params.putInt("game.DOAct.f.x", f_x);
				params.putInt("game.DOAct.f.y", f_y);
				params.putBool("game.DOAct.f.isRemoved", life <= 0);
				getGame().send("game.DOAct", params, getGame().getParentRoom().getPlayersList());	
			}
		};
	}
	
	@Override
	public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId, int damage) {
		if (f_destroysBy != C_DestroysByAnyWeapon && weaponId != f_destroysBy) {
			return;
		}
		long time = System.currentTimeMillis();
		if (time - f_lastDamaged > 1000) {
			f_lastDamaged = time;
			f_life -= damage;
			if (f_life <= 0) {
				map.removeDynamicObject(f_x, f_y);
			}
			game.addGameEvent(getActivateEvent());
		}
	}
	


}
