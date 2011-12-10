package com.vensella.bombers.game.dynamicObjects;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.DamageObject;
import com.vensella.bombers.game.DynamicObject;
import com.vensella.bombers.game.DynamicObjectManager;
import com.vensella.bombers.game.PlayerGameProfile;
import com.vensella.bombers.game.WeaponActivateEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class SpecialSpikes extends DynamicObject {

	public SpecialSpikes(BombersGame game, int x, int y, int period, int damage, boolean active, String direction) {
		super(game, false, !active, false, true, active);
		
		f_whenAddedToMapGameCount = game.getGamesCount();
		
		f_x = x;
		f_y = y;
		setActivated(active);
		f_damage = damage;
		f_period = period;
		
		f_direction = direction;
		
		game.addDelayedGameEvent(new GameEvent(game, true) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				//To prevent forceExecute on next game
				if (f_whenAddedToMapGameCount + 1 != game.getGamesCount() && game.getGameId() != GameEvent.INVALID_GAME_ID) {
					return;
				}
				game.addBombExplosionEvent((WeaponActivateEvent)getActivateEvent());
			}
		}, 3000 + period);
	}
	
	//Fields
	
	private int f_whenAddedToMapGameCount;
	
	private int f_period;
	private int f_damage;
	
	private String f_direction;
	
	private int f_x;
	private int f_y;
	
	//Override methods
	
	@Override
	public GameEvent getActivateEvent() {
		return new WeaponActivateEvent(getGame()) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				
				setActivated(!getActivated());
				setCanGoThrough(!getActivated());
				setStopsExplosion(getActivated());
				
				if (getActivated()) {
					long time = System.currentTimeMillis();
					DamageObject dam = new DamageObject(time) {
						@Override
						public long getLifetime() {
							return getPeriod();
						}
						@Override
						public GameEvent getDamageEvent(BombersGame game, final PlayerGameProfile player) {
							return new GameEvent(game) {
								@Override
								protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
									game.damagePlayer(
											player.getUser(), 
											getDamage(), 
											0, 
											false);
								}
							};
						}
					};
					map.setDamageObject(f_x, f_y, dam);
				}
				
				SFSObject params = new SFSObject();
				params.putInt("game.DOAct.f.type", DynamicObjectManager.SPECIAL_SPIKES);
				params.putInt("game.DOAct.f.x", f_x);
				params.putInt("game.DOAct.f.y", f_y);
				params.putInt("game.DOAct.f.id", getId());
				params.putBool("game.DOAct.f.isActive", getActivated());
				setSendResult(params);
				
				game.addDelayedGameEvent(new GameEvent(game) {
					@Override
					protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
						game.addBombExplosionEvent((WeaponActivateEvent)getActivateEvent());
					}
				}, getPeriod());				
			}
		};
	}

	@Override
	public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId, int damage) {

	}
	
	//Methods
	
	public int getPeriod() { return f_period; }
	public int getDamage() { return f_damage; }
	
	public String getDirection() { return f_direction; }

}
