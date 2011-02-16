package com.vensella.bombers.game;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class WeaponsManager {
	
	//Constants
	
	public final int WEAPON_BOMB_NORMAL = 0;
	public final int WEAPON_BOMB_ATOMIC = 1;
	public final int WEAPON_BOMB_BOX = 2;
	public final int WEAPON_BOMB_DYNAMITE = 3;
	
	public final int WEAPON_POISON_CHAMELEON = 21;
	
	public final int WEAPON_MINE_NORMAL = 41;
	
	
	//Fields
	
	private BombersGame f_game;
	
	//Constructors
	
	public WeaponsManager(BombersGame game) {
		f_game = game;
	}
	
	//Methods
	
	protected boolean withdrawWeapon(PlayerGameProfile profile, int weaponId) {
		if (weaponId == WEAPON_BOMB_NORMAL) {
			return profile.setBomb();
		} else {
			return profile.useWeapon(weaponId);
		}
	}
	
	public void activateWeapon(User user, int weaponId, int x, int y) {
		switch (weaponId) {
		
		//Bombs
		
		case WEAPON_BOMB_NORMAL:
			addBombNormal(user, weaponId, x, y);
			break;
		case WEAPON_BOMB_ATOMIC:
			addBombAtomic(user, weaponId, x, y);
			break;
		case WEAPON_BOMB_BOX:
			addBombBox(user, weaponId, x, y);
			break;
		case WEAPON_BOMB_DYNAMITE:
			addBombDynamite(user, weaponId, x, y);
			break;
	
		//Poisons	
		
		case WEAPON_POISON_CHAMELEON:
			drinkPoisonChameleon(user, weaponId, x, y);
			break;
		
		//Mines
		
		case WEAPON_MINE_NORMAL:
			addMineNormal(user, weaponId, x, y);
			break;
		
		default:
			break;
		}
	}
	
	//Weapons realization
	
	//Bombs
	
	protected void addBombNormal(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(final BombersGame game, final DynamicGameMap map) {
				DynamicObject obj  = map.getDynamicObject(x, y);
				if (obj == null && withdrawWeapon(profile, weaponId)) {
					obj = new DynamicObject(f_game, false, false) {
						@Override
						public GameEvent getActivateEvent() {
							return new GameEvent(f_game) {
								private static final int BASE_POWER = 1;
								@Override
								protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
									setActivated(true);
									int power = BASE_POWER + profile.getBombPowerBonus();
									//Up
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x, y - i) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x, y - i);
											break;
										}
										if (map.getObjectTypeAt(x, y - i) == DynamicGameMap.ObjectType.STRONG_WALL) {
											break;
										}
									}
									//Right
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x + i, y) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x + i, y);
											break;
										}
										if (map.getObjectTypeAt(x + i, y) == DynamicGameMap.ObjectType.STRONG_WALL)	{
											break;
										}
									}
									//Down
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x, y + i) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x, y + i);
											break;
										}
										if (map.getObjectTypeAt(x, y + i) == DynamicGameMap.ObjectType.STRONG_WALL)	{
											break;
										}
									}
									//Left
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x - i, y) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x - i, y);
											break;
										}
										if (map.getObjectTypeAt(x - i, y) == DynamicGameMap.ObjectType.STRONG_WALL)	{
											break;
										}
									}
									
									profile.releaseBomb();
									map.removeDynamicObject(x, y);
									
									SFSObject params = new SFSObject();
									params.putUtfString("game.DOAct.f.userId", user.getName());
									params.putInt("game.DOAct.f.type", weaponId);
									params.putInt("game.DOAct.f.x", x);
									params.putInt("game.DOAct.f.y", y);
									params.putBool("game.DOAct.f.isRemoved", true);
									params.putInt("game.DOAct.f.s.power", power);
									f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
								}
							};
						}
					};
					map.setDynamicObject(x, y, obj);
					f_game.addDelayedGameEvent(obj.getActivateEvent(), 2000);

					SFSObject params = new SFSObject();
					params.putUtfString("game.DOAdd.f.userId", user.getName());
					params.putInt("game.DOAdd.f.type", weaponId);
					params.putInt("game.DOAdd.f.x", x);
					params.putInt("game.DOAdd.f.y", y);
					f_game.send("game.DOAdd", params, f_game.getParentRoom().getPlayersList());
				}
			} 
		});
	}
	
	protected void addBombDynamite(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(final BombersGame game, final DynamicGameMap map) {
				DynamicObject obj  = map.getDynamicObject(x, y);
				if (obj == null && withdrawWeapon(profile, weaponId)) {
					obj = new DynamicObject(f_game, false, false) {
						@Override
						public GameEvent getActivateEvent() {
							return new GameEvent(f_game) {
								private static final int BASE_POWER = 4;
								@Override
								protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
									setActivated(true);
									int power = BASE_POWER;// + profile.getBombPowerBonus();
									//Up
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x, y - i) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x, y - i);
											break;
										}
										if (map.getObjectTypeAt(x, y - i) == DynamicGameMap.ObjectType.STRONG_WALL) {
											break;
										}
									}
									//Right
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x + i, y) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x + i, y);
											break;
										}
										if (map.getObjectTypeAt(x + i, y) == DynamicGameMap.ObjectType.STRONG_WALL)	{
											break;
										}
									}
									//Down
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x, y + i) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x, y + i);
											break;
										}
										if (map.getObjectTypeAt(x, y + i) == DynamicGameMap.ObjectType.STRONG_WALL)	{
											break;
										}
									}
									//Left
									for (int i = 1; i <= power; ++i) {
										if (map.getObjectTypeAt(x - i, y) == DynamicGameMap.ObjectType.WALL) {
											game.destroyWallAt(x - i, y);
											break;
										}
										if (map.getObjectTypeAt(x - i, y) == DynamicGameMap.ObjectType.STRONG_WALL)	{
											break;
										}
									}
									
									profile.releaseBomb();
									map.removeDynamicObject(x, y);
									
									SFSObject params = new SFSObject();
									params.putUtfString("game.DOAct.f.userId", user.getName());
									params.putInt("game.DOAct.f.type", weaponId);
									params.putInt("game.DOAct.f.x", x);
									params.putInt("game.DOAct.f.y", y);
									params.putBool("game.DOAct.f.isRemoved", true);
									params.putInt("game.DOAct.f.s.power", power);
									f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
								}
							};
						}
					};
					map.setDynamicObject(x, y, obj);
					f_game.addDelayedGameEvent(obj.getActivateEvent(), 2000);

					SFSObject params = new SFSObject();
					params.putUtfString("game.DOAdd.f.userId", user.getName());
					params.putInt("game.DOAdd.f.type", weaponId);
					params.putInt("game.DOAdd.f.x", x);
					params.putInt("game.DOAdd.f.y", y);
					f_game.send("game.DOAdd", params, f_game.getParentRoom().getPlayersList());
				}
			}  
		});		
	}

	protected void addBombAtomic(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				DynamicObject obj  = map.getDynamicObject(x, y);
				if (obj == null && withdrawWeapon(profile, weaponId)) {				
					obj = new DynamicObject(f_game, true, true) {
						@Override
						public GameEvent getActivateEvent() {
							return new GameEvent(f_game) {
								@Override
								protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
									setActivated(true);
									
									
									map.removeDynamicObject(x, y);
									
									SFSObject params = new SFSObject();
									params.putUtfString("game.DOAct.f.userId", user.getName());
									params.putInt("game.DOAct.f.type", weaponId);
									params.putInt("game.DOAct.f.x", x);
									params.putInt("game.DOAct.f.y", y);
									params.putBool("game.DOAct.f.isRemoved", true);
									f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
								}
							};
						}
					};
					map.setDynamicObject(x, y, obj);

					SFSObject params = new SFSObject();
					params.putUtfString("game.DOAdd.f.userId", user.getName());
					params.putInt("game.DOAdd.f.type", weaponId);
					params.putInt("game.DOAdd.f.x", x);
					params.putInt("game.DOAdd.f.y", y);
					f_game.send("game.DOAdd", params, f_game.getParentRoom().getPlayersList());					
				}				
			}
		});			
	}
	
	protected void addBombBox(final User user, final int weaponId, final int x, final int y) {
		
	}
	
	//Mines
	
	protected void addMineNormal(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				DynamicObject obj  = map.getDynamicObject(x, y);
				if (obj == null && withdrawWeapon(profile, weaponId)) {				
					obj = new DynamicObject(f_game, true, true) {
						@Override
						public GameEvent getActivateEvent() {
							return new GameEvent(f_game) {
								@Override
								protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
									setActivated(true);
									map.removeDynamicObject(x, y);
									
									SFSObject params = new SFSObject();
									params.putUtfString("game.DOAct.f.userId", getOwner().getName());
									params.putInt("game.DOAct.f.type", weaponId);
									params.putInt("game.DOAct.f.x", x);
									params.putInt("game.DOAct.f.y", y);
									params.putBool("game.DOAct.f.isRemoved", true);
									f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
								}
							};
						}
					};
					map.setDynamicObject(x, y, obj);

					SFSObject params = new SFSObject();
					params.putUtfString("game.DOAdd.f.userId", user.getName());
					params.putInt("game.DOAdd.f.type", weaponId);
					params.putInt("game.DOAdd.f.x", x);
					params.putInt("game.DOAdd.f.y", y);
					f_game.send("game.DOAdd", params, f_game.getParentRoom().getPlayersList());					
				}				
			}
		});		
	}
	
	//Poisons
	
	protected void drinkPoisonChameleon(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				if (!profile.isWeaponSwitchedOn(weaponId) && withdrawWeapon(profile, weaponId)) {
					profile.weaponSwitchOn(weaponId);
					
					f_game.addDelayedGameEvent(new GameEvent(f_game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							profile.weaponSwitchOff(weaponId);
							
							SFSObject params = new SFSObject();
							params.putUtfString("game.WDA.f.userId", user.getName());
							params.putInt("game.WDA.f.type", weaponId);
							f_game.send("game.WDA", params, f_game.getParentRoom().getPlayersList());							
						}
					}, 20000);
					
					SFSObject params = new SFSObject();
					params.putUtfString("game.WA.f.userId", user.getName());
					params.putInt("game.WA.f.type", weaponId);
					params.putInt("game.WA.f.x", x);
					params.putInt("game.WA.f.y", y);
					f_game.send("game.WA", params, f_game.getParentRoom().getPlayersList());
				}
			}
		});
	}
	
}
