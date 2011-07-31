package com.vensella.bombers.game.dynamicObjects;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.dispatcher.PricelistManager;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.DynamicObjectManager;
import com.vensella.bombers.game.PlayerGameProfile;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class ResourceBonus extends DynamicObject {

	//Constructor
	
	public ResourceBonus(BombersGame game, int x, int y, int resourceType, int count) {
		super(game, true, true);
		f_x = x;
		f_y = y;
		f_resourceType = resourceType;
		f_count = count;
	}
	
	//Fields
	
	private int f_x;
	private int f_y;
	
	private int f_resourceType;
	private int f_count;
	
	//Methods
	
	public int getResourceType() { return f_resourceType; }
	public int getCount() { return f_count; }
	
	@Override
	public GameEvent getActivateEvent() {
		return new GameEvent(getGame()) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				PlayerGameProfile profile = game.getGameProfile(getOwner());
				if (f_resourceType == PricelistManager.C_ResourceTypeGold) {
					profile.getBaseProfile().getSessionReward().addGoldReward(f_count);
					profile.getBaseProfile().addGold(f_count);
				} else if (f_resourceType == PricelistManager.C_ResourceTypeCrystal) {
					profile.getBaseProfile().getSessionReward().addCrystalReward(f_count);
					profile.getBaseProfile().addCrystal(f_count);
				} else if (f_resourceType == PricelistManager.C_ResourceTypeAdamantium) {
					profile.getBaseProfile().getSessionReward().addAdamantiumReward(f_count);
					profile.getBaseProfile().addAdamantium(f_count);
				} else if (f_resourceType == PricelistManager.C_ResourceTypeAntimatter) {
					profile.getBaseProfile().getSessionReward().addAntimatterReward(f_count);
					profile.getBaseProfile().addAntimatter(f_count);
				} else if (f_resourceType == PricelistManager.C_ResourceTypeEnergy) {
					profile.getBaseProfile().getSessionReward().addEnergyReward(f_count);
					profile.getBaseProfile().addEnergy(f_count);
				} 
				
				setActivated(true);
				map.removeDynamicObject(f_x, f_y);
				
				SFSObject params = new SFSObject();
				params.putUtfString("game.DOAct.f.userId", getOwner().getName());
				params.putInt("game.DOAct.f.type", DynamicObjectManager.BONUS_ADD_RESOURCE);
				params.putInt("game.DOAct.f.resourceType", f_resourceType);
				params.putInt("game.DOAct.f.count", f_count);
				params.putInt("game.DOAct.f.x", f_x);
				params.putInt("game.DOAct.f.y", f_y);
				params.putBool("game.DOAct.f.isRemoved", true);
				getGame().send("game.DOAct", params, getGame().getParentRoom().getPlayersList());	
			}
		};
	}
	
	@Override
	public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId) {
		
	}

}
