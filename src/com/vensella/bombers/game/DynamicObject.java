package com.vensella.bombers.game;

import com.smartfoxserver.v2.entities.User;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

import java.util.concurrent.atomic.*;

public abstract class DynamicObject {
	
	
	//Constants
	
	private static final AtomicInteger C_IdSeed = new AtomicInteger();
	
	public static final DynamicObject C_DummyUnwalkable = new DynamicObject(null, false, false) {
		@Override
		public GameEvent getActivateEvent() {
			return null;
		}
		@Override
		public void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId, int damage) {
			
		}
	};
	
	//Fields
	
	private int f_id;
	
	private boolean f_canBeActivatedByPlayer;
	private boolean f_canGoThrough;
	private boolean f_canBeDestroyed;
	private boolean f_staysAfterActivationByPlayer;
	private boolean f_stopsExplosion;
	
	private boolean f_activated;
	
	private User f_owner;
	
	private BombersGame f_game;
	
	//Constructors
	
	public DynamicObject(BombersGame game, boolean canBeActivatedByPlayer, boolean canGoThrough) {
		f_id = C_IdSeed.incrementAndGet();
		f_game = game;
		f_canBeActivatedByPlayer = canBeActivatedByPlayer;
		f_canGoThrough = canGoThrough;
	}
	
	public DynamicObject(
			BombersGame game, 
			boolean canBeActivatedByPlayer, 
			boolean canGoThrough,
			boolean canBeDestroyed) 
	{
		this(game, canBeActivatedByPlayer, canGoThrough);
		f_canBeDestroyed = canBeDestroyed;
	}
	
	public DynamicObject(
			BombersGame game, 
			boolean canBeActivatedByPlayer, 
			boolean canGoThrough,
			boolean canBeDestroyed,
			boolean staysAfterActivationByPlayer) 
	{
		this(game, canBeActivatedByPlayer, canGoThrough, canBeDestroyed);
		f_staysAfterActivationByPlayer = staysAfterActivationByPlayer;
	}
	
	public DynamicObject(
			BombersGame game, 
			boolean canBeActivatedByPlayer, 
			boolean canGoThrough,
			boolean canBeDestroyed,
			boolean staysAfterActivation,
			boolean stopsExplosion) 
	{
		this(game, canBeActivatedByPlayer, canGoThrough, canBeDestroyed, staysAfterActivation);
		f_stopsExplosion = stopsExplosion;
	}
	
	//Methods
	
	protected BombersGame getGame() { return f_game; }
	
	public int getId() { return f_id; }
	
	public boolean getActivated() { return f_activated; }
	public void setActivated(boolean activated) { f_activated = activated; }
	
	public User getOwner() { return f_owner; }
	public void setOwner(User owner) { f_owner = owner; }
	
	public boolean getCanBeActivatedByPlayer() { return f_canBeActivatedByPlayer; }
	public void setCanBeActivatedByPlayer(boolean canBeActivatedByPlayer) { f_canBeActivatedByPlayer = canBeActivatedByPlayer; }
	
	public boolean getCanGoThrough() { return f_canGoThrough; }
	public void setCanGoThrough(boolean canGoThrough) { f_canGoThrough = canGoThrough; }
	
	public boolean getCanBeDestroyed() { return f_canBeDestroyed; }
	public void setCanBeDestroyed(boolean canBeDestroyed) { f_canBeDestroyed = canBeDestroyed; }
	
	public boolean getStaysAfterActivationByPlayer() { return f_staysAfterActivationByPlayer; }
	public void setStaysAfterActivation(boolean staysAfterActivation) { f_staysAfterActivationByPlayer = staysAfterActivation; }
	
	public boolean getStopsExplosion() { return f_stopsExplosion; }
	public void setStopsExplosion(boolean stopsExplosion) { f_stopsExplosion = stopsExplosion; }
	
	public abstract GameEvent getActivateEvent();
	
	/*
	 * When canBeDestoryed == true this methods called by weapon event if weapon affects this object
	 */
	public abstract void destoyEvent(WeaponActivateEvent baseEvent, BombersGame game, DynamicGameMap map, int weaponId, int damage);

}
