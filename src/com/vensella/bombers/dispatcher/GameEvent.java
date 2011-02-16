package com.vensella.bombers.dispatcher;

import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public abstract class GameEvent {
	
	//Constants
	
	public static final int INVALID_GAME_ID = -1;
	
	//Fields
	
	private int f_gameId;
	private BombersGame f_game;
	private DynamicGameMap f_map;
	
	//Constructors
	
	public GameEvent(final BombersGame game) {
		//assert game.getGameId() != INVALID_GAME_ID;
		f_gameId = game.getGameId();
		f_game = game;
		f_map = game.getGameMap();
	}
	
	//Methods
	
	public int getEventGameId() { return f_gameId; }
	public int getCurrentGameId() { return f_game.getGameId(); }
	
	public void Apply() {
		ApplyOnGame(f_game, f_map);
	}
	
	protected abstract void ApplyOnGame(BombersGame game, DynamicGameMap map);

}
