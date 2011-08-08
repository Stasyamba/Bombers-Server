package com.vensella.bombers.game.dynamicObjects;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.DynamicObjectManager;
import com.vensella.bombers.game.PlayerGameProfile;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class ItemBonus extends DynamicObject {

	//Constructor
	
	public ItemBonus(BombersGame game, int x, int y, int itemType, int count) {
		super(game, true, true);
		f_x = x;
		f_y = y;
		f_itemType = itemType;
		f_count = count;
	}
	
	//Fields
	
	private int f_x;
	private int f_y;
	
	private int f_itemType;
	private int f_count;
	
	//Methods
	
	public int getItemType() { return f_itemType; }
	public int getCount() { return f_count; }
	
	@Override
	public GameEvent getActivateEvent() {
		return new GameEvent(getGame()) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				PlayerGameProfile profile = game.getGameProfile(getOwner());
				profile.getBaseProfile().addItems(f_itemType, f_count);
				setActivated(true);
				map.removeDynamicObject(f_x, f_y);
				
				SFSObject params = new SFSObject();
				params.putUtfString("game.DOAct.f.userId", getOwner().getName());
				params.putInt("game.DOAct.f.type", DynamicObjectManager.BONUS_ADD_ITEM);
				params.putInt("game.DOAct.f.itemType", f_itemType);
				params.putInt("game.DOAct.f.count", f_count);
				params.putInt("game.DOAct.f.x", f_x);
				params.putInt("game.DOAct.f.y", f_y);
				params.putBool("game.DOAct.f.isRemoved", true);
				getGame().send("game.DOAct", params, getGame().getParentRoom().getPlayersList());	
			}
		};
	}
	
	@Override
	public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId, int damage) {
		
	}


}
