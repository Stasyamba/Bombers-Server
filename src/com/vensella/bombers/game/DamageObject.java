package com.vensella.bombers.game;

import com.vensella.bombers.dispatcher.GameEvent;

public abstract class DamageObject {

	//Constructors
	
	public DamageObject(long currentTime) {
		f_time = currentTime;
	}
	
	//Fields
	
	private long f_time;
	
	//Methods
	
	public long getSettedTime() { return f_time; }
	
	public abstract long getLifetime();
	public abstract GameEvent getDamageEvent(BombersGame game, PlayerGameProfile player);
	
}
