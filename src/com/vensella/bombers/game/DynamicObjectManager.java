package com.vensella.bombers.game;

import java.util.Map;

import com.vensella.bombers.game.dynamicObjects.*;

public class DynamicObjectManager {
	
	//Constants
	
	public static final int INVALID_DYNAMIC_OBJECT = 0;
	
	public static final int BONUS_ADD_BOMB = 101;
	public static final int BONUS_ADD_BOMB_POWER = 102;
	public static final int BONUS_ADD_SPEED = 103;
	public static final int BONUS_ADD_HEAL = 104;
	
	public static final int BONUS_ADD_ITEM = 100;
	public static final int BONUS_ADD_RESOURCE = 106;
	
	public static final int SPECIAL_WALL = 200;
	
	public static final int SPECIAL_SPIKES = 210;
	
	//Fields
	
	private BombersGame f_game;
	
	//Constructors
	
	public DynamicObjectManager(BombersGame game) {
		f_game = game;
	}
	
	//Methods

	public DynamicObject createDynamicObject(int type, int x, int y) {
		return createDynamicObject(type, x, y, null);
	}
	
	public DynamicObject createDynamicObject(int type, int x, int y, Map<String, String> attributes) {
		switch(type) {
			
			//Bonuses
			case BONUS_ADD_BOMB_POWER:
				return new BombPowerBonus(f_game, x, y);
			case BONUS_ADD_BOMB:
				return new BombCountBonus(f_game, x, y);
			case BONUS_ADD_HEAL:
				return new HealthBonus(f_game, x, y);
			case BONUS_ADD_SPEED:
				return new SpeedBonus(f_game, x, y);
			case BONUS_ADD_RESOURCE:
				int resourceType = Integer.parseInt(attributes.get("resourceType"));
				int count = Integer.parseInt(attributes.get("count"));
				return new ResourceBonus(f_game, x, y, resourceType, count);
			case BONUS_ADD_ITEM:
				int itemId = Integer.parseInt(attributes.get("itemId"));
			    count = Integer.parseInt(attributes.get("count"));				
				return new ItemBonus(f_game, x, y, itemId, count);
			
			//Special walls
			case SPECIAL_WALL:
				int destroysBy = Integer.parseInt(attributes.get("destroysBy"));
			    int life = Integer.parseInt(attributes.get("life"));				    
			    return new SpecialWall(f_game, x, y, destroysBy, life);
			
			//Special map objects
			case SPECIAL_SPIKES:
				int period = Integer.parseInt(attributes.get("period"));
				int damage = PlayerGameProfile.C_HealthQuantum;
				if (attributes.containsKey("damage")) {
					damage = Integer.parseInt(attributes.get("damage"));
				}
				boolean active = Boolean.parseBoolean(attributes.get("active"));
				return new SpecialSpikes(f_game, x, y, period, damage, active);
			   
			//Default
			default:
				return null;
		}
	} 
	
}
