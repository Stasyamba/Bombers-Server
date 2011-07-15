package com.vensella.bombers.game;

import com.smartfoxserver.v2.entities.User;
import com.vensella.bombers.dispatcher.GameEvent;

public abstract class DynamicObject {
	
	//Constants
	
	public static final DynamicObject C_DummyUnwalkable = new DynamicObject(null, false, false) {
		@Override
		public GameEvent getActivateEvent() {
			return null;
		}
	};
	
	//Fields
	
	private boolean f_canBeActivatedByPlayer;
	private boolean f_canGoThrough;
	private boolean f_canBeDestroyed;
	
	private boolean f_activated;
	
	private User f_owner;
	
	private BombersGame f_game;
	
	//Constructors
	
	public DynamicObject(BombersGame game, boolean canBeActivatedByPlayer, boolean canGoThrough) {
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
	
	//Methods
	
	protected BombersGame getGame() { return f_game; }
	
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
	
	public abstract GameEvent getActivateEvent();

}
