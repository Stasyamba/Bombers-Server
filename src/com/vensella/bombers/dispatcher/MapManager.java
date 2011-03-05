package com.vensella.bombers.dispatcher;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class MapManager {
	
	//Constants
	
	private static final String MapFolder = "/usr/local/nginx/html/main/bombers/maps";
	private static final String MapExtension = "xml";
	
//	private static final String BigObjectFolder = "/usr/local/nginx/html/main/bombers/objects";
//	private static final String BigObjectExtension = "xml";
	
	//Fields
	
	private BombersDispatcher f_dispatcher; 
	
	private Map<Integer, ArrayList<DynamicGameMap>> f_mapPool;
	//private Map<String, DynamicBigObject> f_bigObjectPool;
	
	private Map<Room, Integer> f_mapRotation;
	
	
	//Constructors
	
	public MapManager(BombersDispatcher dispatcher) {
		f_dispatcher = dispatcher;
		f_mapPool = new HashMap<Integer, ArrayList<DynamicGameMap>>();
		//f_bigObjectPool = new HashMap<String, DynamicBigObject>();
		f_mapRotation = new ConcurrentHashMap<Room, Integer>();
		
		initializeBigObjectPool();
		initializeMapPool();
	}
	
	//Private methods
	
	private void initializeBigObjectPool() {
		
	}
	
	private void initializeMapPool() {
		File mapsDirectory = new File(MapFolder);
		FilenameFilter mapFileFilter = new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return !name.startsWith(".") && !name.startsWith("~") && name.endsWith(MapExtension);
			}
		};		
		File[] names = mapsDirectory.listFiles(mapFileFilter);
		
		for (File file : names) {
			try {
				DynamicGameMap map = new DynamicGameMap(file.getPath(), this);
				
				f_dispatcher.trace(
						ExtensionLogLevel.WARN, 
						"Loaded map, map id = " + map.getMapId() + ", location = " + map.getLocationId()
					);
				
				if (f_mapPool.containsKey(map.getLocationId())) {
					f_mapPool.get(map.getLocationId()).add(map);
				} else {
					ArrayList<DynamicGameMap> a = new ArrayList<DynamicGameMap>();
					a.add(map);
					f_mapPool.put(map.getLocationId(), a);
				}
			} 
			catch (Exception ex) {
				f_dispatcher.trace(ExtensionLogLevel.ERROR, "While load map at path " + file.getPath());
				f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.toString());
				f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
			}
		}
	}
	
	//Methods
	
	public DynamicGameMap getRandomMap(Room room, int locationId, int players) {
		if (f_mapPool.containsKey(locationId) == false) {
			return null;
		}
		ArrayList<DynamicGameMap> candidates = new ArrayList<DynamicGameMap>();
		for (DynamicGameMap map : f_mapPool.get(locationId)) {
			if (map.getMaxPlayers() >= players)
				candidates.add(map);
		}
		Integer index = f_mapRotation.get(room);
		if (index == null) {
			index = new Integer((int)(Math.random() * candidates.size()));
			f_mapRotation.put(room, index);
		} else {
			f_mapRotation.put(room, ++index);
		}
		
		return new DynamicGameMap(candidates.get(index % candidates.size()));
	}
	
	
}
