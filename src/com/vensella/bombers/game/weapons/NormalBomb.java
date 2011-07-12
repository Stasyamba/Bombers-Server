package com.vensella.bombers.game.weapons;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DamageObject;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.PlayerGameProfile;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.WeaponsManager;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

//Normal bomb
public class NormalBomb extends DynamicObject {

	public NormalBomb(BombersGame game, int x, int y, PlayerGameProfile owner) {
		super(game, false, false);
		this.x = x;
		this.y = y;
		this.profile = owner;
	}
	
	private int x;
	private int y;
	private PlayerGameProfile profile;

	@Override
	public GameEvent getActivateEvent() {
		return new WeaponActivateEvent(getGame()) {
			private static final int BASE_POWER = 1;
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				setActivated(true);
				int power = BASE_POWER + profile.getBombPowerBonus();
				
				long time = System.currentTimeMillis();
				DamageObject dam = new DamageObject(time) {
					@Override
					public long getLifetime() {
						return 1000;
					}
					@Override
					public GameEvent getDamageEvent(BombersGame game, final PlayerGameProfile player) {
						return new GameEvent(game) {
							@Override
							protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
								game.damagePlayer(
										player.getUser(), 
										PlayerGameProfile.C_HealthQuantum * 1, 
										0, 
										false);
							}
						};
					}
				};
				
				map.setDamageObject(x, y, dam);
				//Up
				for (int i = 1; i <= power; ++i) {
					if (map.getObjectTypeAt(x, y - i) == DynamicGameMap.ObjectType.WALL) {
						beginDestroyingStaticObject(x, y - i);
						break;
					}
					if (map.getObjectTypeAt(x, y - i) == DynamicGameMap.ObjectType.STRONG_WALL) {
						break;
					}
					if (map.getObjectTypeAt(x, y - i) == DynamicGameMap.ObjectType.EMPTY) {
						map.setDamageObject(x, y - i, dam);
					}
				}
				//Right
				for (int i = 1; i <= power; ++i) {
					if (map.getObjectTypeAt(x + i, y) == DynamicGameMap.ObjectType.WALL) {
						beginDestroyingStaticObject(x + i, y);
						break;
					}
					if (map.getObjectTypeAt(x + i, y) == DynamicGameMap.ObjectType.STRONG_WALL)	{
						break;
					} 
					if (map.getObjectTypeAt(x + i, y) == DynamicGameMap.ObjectType.EMPTY) {
						map.setDamageObject(x + i, y, dam);
					}
				}
				//Down
				for (int i = 1; i <= power; ++i) {
					if (map.getObjectTypeAt(x, y + i) == DynamicGameMap.ObjectType.WALL) {
						beginDestroyingStaticObject(x, y + i);
						break;
					}
					if (map.getObjectTypeAt(x, y + i) == DynamicGameMap.ObjectType.STRONG_WALL)	{
						break;
					} 
					if (map.getObjectTypeAt(x, y + i) == DynamicGameMap.ObjectType.EMPTY) {
						map.setDamageObject(x, y + i, dam);
					}
				}
				//Left
				for (int i = 1; i <= power; ++i) {
					if (map.getObjectTypeAt(x - i, y) == DynamicGameMap.ObjectType.WALL) {
						beginDestroyingStaticObject(x - i, y);
						break;
					}
					if (map.getObjectTypeAt(x - i, y) == DynamicGameMap.ObjectType.STRONG_WALL)	{
						break;
					} 
					if (map.getObjectTypeAt(x - i, y) == DynamicGameMap.ObjectType.EMPTY) {
						map.setDamageObject(x - i, y, dam);
					}
				}
				
				profile.releaseBomb();
				map.removeDynamicObject(x, y);
				
				SFSObject params = new SFSObject();
				params.putUtfString("game.DOAct.f.userId", profile.getUser().getName());
				params.putInt("game.DOAct.f.type", WeaponsManager.WEAPON_BOMB_NORMAL);
				params.putInt("game.DOAct.f.x", x);
				params.putInt("game.DOAct.f.y", y);
				params.putBool("game.DOAct.f.isRemoved", true);
				params.putInt("game.DOAct.f.s.power", power);
				params.putInt("game.DOAct.f.s.lifetime", (int)dam.getLifetime());
				params.putSFSArray("game.DOAct.f.s.destroyList", getDestroyList());
				//f_game.send("game.DOAct", params, f_game.getParentRoom().getPlayersList());	
				
				setSendResult(params);
				
				destroyAll((int)dam.getLifetime());
			}
		};
	}

}
