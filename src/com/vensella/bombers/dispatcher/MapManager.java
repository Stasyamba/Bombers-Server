package com.vensella.bombers.dispatcher;



import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class MapManager {
	
	//Constants
	
	//TODO: Load URL from ConfigurtionManager 
	private static final String C_MapsUrl = "http://46.182.31.151/bombers/maps/";
	private static final String C_MapList = "maps.xml";
	
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
		//f_mapRotation = new ConcurrentHashMap<Room, Integer>();
		f_mapRotation = new WeakHashMap<Room, Integer>();
		
		initializeBigObjectPool();
		initializeMapPool();
	}
	
	//Private methods
	
	private void initializeBigObjectPool() {
		
	}
	
	private void initializeMapPool() {
		ArrayList<String> mapList = new ArrayList<String>();
		
		f_dispatcher.trace(ExtensionLogLevel.WARN, "Download maps start");
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(C_MapsUrl + C_MapList);
			Element mapsElement = doc.getDocumentElement();
			NodeList nl = mapsElement.getElementsByTagName("map");
			for (int i = 0; i < nl.getLength(); ++i)
			{
				mapList.add(((Element)nl.item(i)).getAttribute("name"));
			}
		}
		catch (Exception ex) {
			f_dispatcher.trace(ExtensionLogLevel.ERROR, "While load map list from remote URL");
			f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.toString());
			f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());				
		}
		
		for (String mapName : mapList) {
			try {
				DynamicGameMap map = new DynamicGameMap(C_MapsUrl + mapName, this);
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
				f_dispatcher.trace(ExtensionLogLevel.ERROR, "While load map " + mapName);
				f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.toString());
				f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());				
			}
		}
		f_dispatcher.trace(ExtensionLogLevel.WARN, "Download maps end");
	}
	
	//Methods
	
	public synchronized DynamicGameMap getRandomMap(BombersGame game, int players) {
		Room room = game.getParentRoom();
		int locationId = game.getLocationId();
		
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
		
		DynamicGameMap map = null;
		DynamicGameMap prototype = candidates.get(index % candidates.size());
		try {
			map = new DynamicGameMap(game, prototype);
		}
		catch (Exception e) {
			f_dispatcher.trace(ExtensionLogLevel.ERROR, "While try to init map wtih id = " + prototype.getMapId());
			f_dispatcher.trace(ExtensionLogLevel.ERROR, e.toString());
			f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])e.getStackTrace());
			map = getRandomMap(game, players);
		}
		return map;
	}
	
	public BombersDispatcher getDispatcher() { return f_dispatcher; }
	
	
}
