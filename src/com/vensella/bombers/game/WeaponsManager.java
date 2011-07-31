package com.vensella.bombers.game;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;
import com.vensella.bombers.game.weapons.NormalBomb;
import com.vensella.bombers.game.weapons.NormalDynamite;
import com.vensella.bombers.game.weapons.NormalMine;

public class WeaponsManager {
	
	//Constants
	
	public final static int WEAPON_BOMB_NORMAL = 0;
	public final static int WEAPON_BOMB_ATOMIC = 1;
	public final static int WEAPON_BOMB_BOX = 2;
	public final static int WEAPON_BOMB_DYNAMITE = 3;
	public final static int WEAPON_BOMB_SMOKE = 4;
	
	public final static int WEAPON_POISON_CHAMELEON = 21;
	public final static int WEAPON_POISON_SMALL_HEALTH = 22;
	public final static int WEAPON_POISON_MIDDLE_HEALTH = 23;
	
	public final static int WEAPON_MINE_NORMAL = 41;
	
	
	public final static int C_WallDestroyingTime = 50; 
	
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
			//addBombAtomic(user, weaponId, x, y);
			break;
		case WEAPON_BOMB_BOX:
			//addBombBox(user, weaponId, x, y);
			break;
		case WEAPON_BOMB_DYNAMITE:
			addBombDynamite(user, weaponId, x, y);
			break;
		case WEAPON_BOMB_SMOKE:
			addBombSmoke(user, weaponId, x, y);
			break;
	
		//Poisons	
		
		case WEAPON_POISON_CHAMELEON:
			drinkPoisonChameleon(user, weaponId, x, y);
			break;
			
		case WEAPON_POISON_SMALL_HEALTH:
			drinkPoisonSmallHealth(user, weaponId, x, y);
			break;
			
		case WEAPON_POISON_MIDDLE_HEALTH:
			drinkPoisonMiddleHealth(user, weaponId, x, y);
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
	
	//Grouping
	protected void addBombNormal(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(final BombersGame game, final DynamicGameMap map) {
				DynamicObject obj  = map.getDynamicObject(x, y);
				if (obj == null && withdrawWeapon(profile, weaponId)) {
					obj = new NormalBomb(game, x, y, profile);
					map.setDynamicObject(x, y, obj);
					final DynamicObject fobj = obj;
					f_game.addDelayedGameEvent(new GameEvent(game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							game.addBombExplosionEvent((WeaponActivateEvent)fobj.getActivateEvent());
						}
					}, 2000);

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
					obj = new NormalDynamite(game, x, y, profile);
					map.setDynamicObject(x, y, obj);
					final DynamicObject fobj = obj;
					f_game.addDelayedGameEvent(new GameEvent(game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							game.addBombExplosionEvent((WeaponActivateEvent)fobj.getActivateEvent());
						}
					}, 2000);

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
//		final PlayerGameProfile profile = f_game.getGameProfile(user);
//		f_game.addGameEvent(new GameEvent(f_game) {
//			@Override
//			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
//				DynamicObject obj  = map.getDynamicObject(x, y);
//				if (obj == null && withdrawWeapon(profile, weaponId)) {				
//					obj = new DynamicObject(f_game, false, false) {
//						@Override
//						public GameEvent getActivateEvent() {
//							return new GameEvent(f_game) {
//								@Override
//								protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
//									setActivated(true);
//									
//									//UP
//									int i = 1;
//									while (map.getObjectTypeAt(x, y - i) != DynamicGameMap.ObjectType.OUT) {
//										game.destroyWallAt(x, y - i);
//										if (i % 3 == 0) {
//											for (int j = -2; j <= 2; ++j) {
//												if (j == 0) continue;
//												game.destroyWallAt(x + j, y - i);
//											}
//										}
//										i++;
//									}
//									//DOWN
//									i = 1;
//									while (map.getObjectTypeAt(x, y + i) != DynamicGameMap.ObjectType.OUT) {
//										game.destroyWallAt(x, y + i);
//										if (i % 3 == 0) {
//											for (int j = -2; j <= 2; ++j) {
//												if (j == 0) continue;
//												game.destroyWallAt(x + j, y + i);
//											}
//										}
//										i++;
//									}
//									//LEFT
//									i = 1;
//									while (map.getObjectTypeAt(x - i, y) != DynamicGameMap.ObjectType.OUT) {
//										game.destroyWallAt(x - i, y);
//										if (i % 3 == 0) {
//											for (int j = -2; j <= 2; ++j) {
//												if (j == 0) continue;
//												game.destroyWallAt(x - i, y + j);
//											}
//										}
//										i++;
//									}
//									//RIGHT
//									i = 1;
//									while (map.getObjectTypeAt(x + i, y) != DynamicGameMap.ObjectType.OUT) {
//										game.destroyWallAt(x + i, y);
//										if (i % 3 == 0) {
//											for (int j = -2; j <= 2; ++j) {
//												if (j == 0) continue;
//												game.destroyWallAt(x + i, y + j);
//											}
//										}
//										i++;
//									}
//									map.removeDynamicObject(x, y);
//									
//									SFSObject params = new SFSObject();
//									params.putUtfString("game.DOAct.f.userId", user.getName());
//									params.putInt("game.DOAct.f.type", weaponId);
//									params.putInt("game.DOAct.f.x", x);
//									params.putInt("game.DOAct.f.y", y);
//									params.putBool("game.DOAct.f.isRemoved", true);
//									f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
//								}
//							};
//						}
//					};
//					map.setDynamicObject(x, y, obj);
//					f_game.addDelayedGameEvent(obj.getActivateEvent(), 2000);
//					
//					SFSObject params = new SFSObject();
//					params.putUtfString("game.DOAdd.f.userId", user.getName());
//					params.putInt("game.DOAdd.f.type", weaponId);
//					params.putInt("game.DOAdd.f.x", x);
//					params.putInt("game.DOAdd.f.y", y);
//					f_game.send("game.DOAdd", params, f_game.getParentRoom().getPlayersList());					
//				}				
//			}
//		});			
	}
	
	protected void addBombBox(final User user, final int weaponId, final int x, final int y) {
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
								
								private void setBoxAt(DynamicGameMap map, int x, int y) {
									
								}
								
								@Override
								protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
									setActivated(true);
									
									setBoxAt(map, x - 1, y);
									setBoxAt(map, x - 1, y - 1);
									setBoxAt(map, x, y - 1);
									setBoxAt(map, x + 1, y - 1);
									setBoxAt(map, x - 1, y);
									setBoxAt(map, x + 1, y + 1);
									setBoxAt(map, x, y + 1);
									setBoxAt(map, x - 1, y + 1);
									
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
						@Override
						public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId) {
							
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
	
	protected void addBombSmoke(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(final BombersGame game, final DynamicGameMap map) {
				if (withdrawWeapon(profile, weaponId)) {
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
	
	//Mines
	
	protected void addMineNormal(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				DynamicObject obj  = map.getDynamicObject(x, y);
				if (obj == null && withdrawWeapon(profile, weaponId)) {				
					obj = new NormalMine(game, x, y);
					map.setDynamicObject(x, y, obj);
					final DynamicObject fobj = obj;
					f_game.addDelayedGameEvent(new GameEvent(f_game) {
						@Override
						protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
							fobj.setCanBeActivatedByPlayer(true);
							fobj.setCanBeDestroyed(true);
						}
					}, 2000);

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
					}, 2 * 60 * 1000);
					
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
	
	protected void drinkPoisonSmallHealth(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				if (withdrawWeapon(profile, weaponId)) {
					profile.addHealth(PlayerGameProfile.C_HealthQuantum);
					
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
	
	protected void drinkPoisonMiddleHealth(final User user, final int weaponId, final int x, final int y) {
		final PlayerGameProfile profile = f_game.getGameProfile(user);
		f_game.addGameEvent(new GameEvent(f_game) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				if (withdrawWeapon(profile, weaponId)) {
					profile.addHealth(2 * PlayerGameProfile.C_HealthQuantum);
					
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
