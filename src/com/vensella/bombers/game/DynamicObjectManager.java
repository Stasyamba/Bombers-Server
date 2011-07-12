package com.vensella.bombers.game;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class DynamicObjectManager {
	
	//Constants
	
	public final int BONUS_ADD_BOMB = 101;
	public final int BONUS_ADD_BOMB_POWER = 102;
	public final int BONUS_ADD_SPEED = 103;
	public final int BONUS_ADD_HEAL = 104;
	
	//Fields
	
	private BombersGame f_game;
	
	private int f_wallBlocks;
	
	private int f_countAddBomb = 8;
	private int f_countAddPower = 8;
	private int f_countAddSpeed = 8;
	private int f_countAddHealth = 4;
	
	//Constructors
	
	public DynamicObjectManager(BombersGame game) {
		f_game = game;
	}
	
	//Methods
	
	public void setWallBlocksCount(int wallBlocks) {
		f_countAddBomb = 8;
		f_countAddPower = 8;
		f_countAddSpeed = 8;
		f_countAddHealth = 4;
		
		f_wallBlocks = wallBlocks;
	}
	
	/*
	 * Warning - can be only called from event model context
	 */
	public void possiblyAddRandomBonus(final int x, final int y) {
		
		int bonusesLeft = f_countAddBomb + f_countAddPower + f_countAddSpeed + f_countAddHealth;
		double C = 1.0 * bonusesLeft / f_wallBlocks;
		double p = Math.random();
		f_wallBlocks--;
		
		if (p < C) {
			DynamicObject bonus = null;
			int bonusType;
			double r1 = Math.random() * f_countAddBomb;
			double r2 = Math.random() * f_countAddPower;
			double r3 = Math.random() * f_countAddSpeed;
			double r4 = Math.random() * f_countAddHealth;
			
			if (r1 > r2 && r1 > r3 && r1 > r4) {
				bonusType = BONUS_ADD_BOMB;
				bonus = createDynamicObject(bonusType, x, y);
				f_countAddBomb--;
			}
			else if (r2 > r1 && r2 > r3 && r2 > r4)	{
				bonusType = BONUS_ADD_BOMB_POWER;
				bonus = createDynamicObject(bonusType, x, y);
				f_countAddPower--;
			}
			else if (r3 > r1 && r3 > r2 && r3 > r4)	{
				bonusType = BONUS_ADD_SPEED;
				bonus = createDynamicObject(bonusType, x, y);
				f_countAddSpeed--;
			}
			else {
				bonusType = BONUS_ADD_HEAL;
				bonus = createDynamicObject(bonusType, x, y);
				f_countAddHealth--;
			}
			f_game.getGameMap().setDynamicObject(x, y, bonus);
				
			SFSObject params = new SFSObject();
			params.putInt("game.DOAdd.f.type", bonusType);
			params.putInt("game.DOAdd.f.x", x);
			params.putInt("game.DOAdd.f.y", y);
			f_game.send("game.DOAdd", params, f_game.getParentRoom().getPlayersList());
		}
	}
	
	public void activateDynamicObject(final User user, final int x, final int y) {
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				DynamicObject obj = f_game.getGameMap().getDynamicObject(x, y);
				if (obj != null && obj.getCanBeActivatedByPlayer()) {
					obj.setOwner(user);
					f_game.addGameEvent(obj.getActivateEvent());
				}
			}
		});
	}
	
	//Dynamic objects realization
	
	protected DynamicObject createDynamicObject(final int type, final int x, final int y) {
		switch (type) {
		case BONUS_ADD_BOMB:
			return new DynamicObject(f_game, true, true) {
				@Override
				public GameEvent getActivateEvent() {
					return new GameEvent(f_game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							PlayerGameProfile profile = game.getGameProfile(getOwner());
							profile.releaseBomb();
							setActivated(true);
							map.removeDynamicObject(x, y);
							
							SFSObject params = new SFSObject();
							params.putUtfString("game.DOAct.f.userId", getOwner().getName());
							params.putInt("game.DOAct.f.type", type);
							params.putInt("game.DOAct.f.x", x);
							params.putInt("game.DOAct.f.y", y);
							params.putBool("game.DOAct.f.isRemoved", true);
							f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
						}
					};
				}
			};
		case BONUS_ADD_BOMB_POWER:
			return new DynamicObject(f_game, true, true) {
				@Override
				public GameEvent getActivateEvent() {
					return new GameEvent(f_game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							PlayerGameProfile profile = game.getGameProfile(getOwner());
							profile.addBombPowerBonus(1);
							setActivated(true);
							map.removeDynamicObject(x, y);
							
							SFSObject params = new SFSObject();
							params.putUtfString("game.DOAct.f.userId", getOwner().getName());
							params.putInt("game.DOAct.f.type", type);
							params.putInt("game.DOAct.f.x", x);
							params.putInt("game.DOAct.f.y", y);
							params.putBool("game.DOAct.f.isRemoved", true);
							f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
						}
					};
				}
			};
		case BONUS_ADD_SPEED:
			return new DynamicObject(f_game, true, true) {
				@Override
				public GameEvent getActivateEvent() {
					return new GameEvent(f_game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							PlayerGameProfile profile = game.getGameProfile(getOwner());
							profile.addSpeed(1.1);
							setActivated(true);
							map.removeDynamicObject(x, y);
							
							SFSObject params = new SFSObject();
							params.putUtfString("game.DOAct.f.userId", getOwner().getName());
							params.putInt("game.DOAct.f.type", type);
							params.putInt("game.DOAct.f.x", x);
							params.putInt("game.DOAct.f.y", y);
							params.putBool("game.DOAct.f.isRemoved", true);
							f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
						}
					};
				}
			};
		case BONUS_ADD_HEAL:
			return new DynamicObject(f_game, true, true) {
				@Override
				public GameEvent getActivateEvent() {
					return new GameEvent(f_game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							PlayerGameProfile profile = game.getGameProfile(getOwner());
							profile.addHealth(PlayerGameProfile.C_HealthQuantum);
							setActivated(true);
							map.removeDynamicObject(x, y);
							
							SFSObject params = new SFSObject();
							params.putUtfString("game.DOAct.f.userId", getOwner().getName());
							params.putInt("game.DOAct.f.type", type);
							params.putInt("game.DOAct.f.x", x);
							params.putInt("game.DOAct.f.y", y);
							params.putBool("game.DOAct.f.isRemoved", true);
							f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
						}
					};
				}
			};
//		case BONUS_ADD_HEAL:
//			return new DynamicObject(f_game, true, true) {
//				@Override
//				public GameEvent getActivateEvent() {
//			return new GameEvent(f_game) {
//				@Override
//				protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
//					
//					
//				}
//			};
//				}
//			};		
		default:
			return null;
		}
	}
	
}
