package com.vensella.bombers.game;

import java.util.ArrayList;

import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.dispatcher.GameEvent;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

//Event for activate such weapons as bombs, support destroying walls and grouping results
public abstract class WeaponActivateEvent extends GameEvent {
	
	//Constructors
	
	public WeaponActivateEvent(BombersGame game) {
		super(game);
		
		f_xCoords = new ArrayList<Integer>();
		f_yCoords = new ArrayList<Integer>();
		f_destroyList = new SFSArray();
	}
	
	//Fields
	
	private ArrayList<Integer> f_xCoords;
	private ArrayList<Integer> f_yCoords;
	
	private SFSArray f_destroyList;
	
	private SFSObject f_sendResult;
	
	//Methods
	
	//Get data being sent to client
	public SFSObject getSendResult() { return f_sendResult; }
	protected void setSendResult(SFSObject sendResult) { f_sendResult = sendResult; }
	
	protected void beginDestroyingStaticObject(int x, int y) {
		SFSObject coords = new SFSObject();
		coords.putInt("X", x);
		coords.putInt("Y", y);
		coords.putBool("isS", true);
		f_destroyList.addSFSObject(coords);
		
		//getGame().getGameMap().setDynamicObject(x, y, DynamicObject.C_DummyUnwalkable);
		f_xCoords.add(x);
		f_yCoords.add(y);
	}
	
	public void beginDestroyingDynamicObject(int x, int y, int id) {
		SFSObject coords = new SFSObject();
		coords.putInt("X", x);
		coords.putInt("Y", y);
		coords.putInt("ID", id);
		coords.putBool("isS", false);
		f_destroyList.addSFSObject(coords);		
	}
	
	protected SFSArray getDestroyList() { return f_destroyList; }
	
	//Destroy walls being affected by weapon
	protected void destroyAll(int delay) {
		getGame().addDelayedGameEvent(new GameEvent(getGame()) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				int size = f_xCoords.size();
				int x = 0;
				int y = 0;
				for (int i = 0; i < size; ++i) {
					x = f_xCoords.get(i);
					y = f_yCoords.get(i);	
//					map.removeDynamicObject(x, y);
//					if (map.getObjectTypeAt(x, y) == DynamicGameMap.ObjectType.WALL) {
//						game.getDynamicObjectManager().possiblyAddRandomBonus(x, y);
//					}
					if (map.getObjectTypeAt(x, y) != DynamicGameMap.ObjectType.DEATH_WALL) {
						map.setObjectTypeAt(x, y, DynamicGameMap.ObjectType.EMPTY);
					}
				}
			}
		}, delay);
	}

}
