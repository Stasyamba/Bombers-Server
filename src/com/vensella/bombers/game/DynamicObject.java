package com.vensella.bombers.game;

import com.smartfoxserver.v2.entities.User;
import com.vensella.bombers.dispatcher.GameEvent;

public abstract class DynamicObject {
	
	//Fields
	
	private boolean f_canBeActivatedByPlayer;
	private boolean f_canGoThrough;
	private boolean f_activated;
	
	private User f_owner;
	
	private BombersGame f_game;
	
	//Constructors
	
	public DynamicObject(BombersGame game, boolean canBeActivatedByPlayer, boolean canGoThrough) {
		f_game = game;
		f_canBeActivatedByPlayer = canBeActivatedByPlayer;
		f_canGoThrough = canGoThrough;
	}
	
	//Methods
	
	protected BombersGame getGame() { return f_game; }
	
	public boolean getActivated() { return f_activated; }
	public void setActivated(boolean activated) { f_activated = activated; }
	
	public User getOwner() { return f_owner; }
	public void setOwner(User owner) { f_owner = owner; }
	
	public boolean getCanBeActivatedByPlayer() { return f_canBeActivatedByPlayer; }
	
	public boolean getCanGoThrough() { return f_canGoThrough; }
	
	public abstract GameEvent getActivateEvent();

}
