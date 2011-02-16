package com.vensella.bombers.game.mapObjects;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vensella.bombers.dispatcher.MapManager;
import com.vensella.bombers.game.DynamicObject;

public class DynamicGameMap {
	
	//Nested types
	
	public enum ObjectType {
		EMPTY,
		WALL,
		STRONG_WALL,
		DEATH_WALL,
		BOMB,
		OUT
	}
	
	//Constants
	
	private static final double C_BlockSize = 40.0;
	
	//Fields
	
	private int f_mapId;
	private int f_locationId;
	
	private int f_maxX;
	private int f_maxY;
	
	private int f_maxPlayers;
	private int[] f_spawnX;
	private int[] f_spawnY;
	
	private ObjectType[][] f_map;
	
	//Constructors
	
	public DynamicGameMap(String path, MapManager manager) throws ParserConfigurationException, SAXException, IOException
	{
		//File file = new File(path);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(path);
		//doc.getDocumentElement().normalize(); 
		Element rootElement = doc.getDocumentElement();
		
		NodeList nl = null;
		
		//Id
		nl = rootElement.getElementsByTagName("id");
		Element idElement = (Element)nl.item(0);
		f_mapId = Integer.parseInt(idElement.getAttribute("val"));
		
		//Location
		nl = rootElement.getElementsByTagName("location");
		Element locationElement = (Element)nl.item(0);
		f_locationId = Integer.parseInt(locationElement.getAttribute("val"));
		
		//Size
		nl = rootElement.getElementsByTagName("size");
		Element sizeElement = (Element)nl.item(0);
		f_maxX = Integer.parseInt(sizeElement.getAttribute("width"));
		f_maxY = Integer.parseInt(sizeElement.getAttribute("height"));
		
		//Spawns
		nl = rootElement.getElementsByTagName("spawns");
		NodeList spawnsNodeList = ((Element)nl.item(0)).getElementsByTagName("Spawn");
		f_spawnX = new int[spawnsNodeList.getLength()];
		f_spawnY = new int[spawnsNodeList.getLength()];
		for (int i = 0; i < spawnsNodeList.getLength(); ++i)
		{
			Element spawnElement = (Element)spawnsNodeList.item(i);
			f_spawnX[i] = Integer.parseInt(spawnElement.getAttribute("x"));
			f_spawnY[i] = Integer.parseInt(spawnElement.getAttribute("y"));
		}
		f_maxPlayers = f_spawnX.length;
		
		//Rows
		nl = rootElement.getElementsByTagName("rows");
		NodeList rowsNodeList = ((Element)nl.item(0)).getElementsByTagName("Row");
		f_map = new ObjectType[f_maxX][];
		for (int i = 0; i < f_maxX; ++i)
			f_map[i] = new ObjectType[f_maxY];
		for (int y = 0; y < rowsNodeList.getLength(); ++y)
		{
			Element rowElement = (Element)rowsNodeList.item(y);
			String rowString = rowElement.getAttribute("val"); 
			for (int x = 0; x < f_maxX; ++x)
			{
				if (rowString.charAt(x) == 'f')
				{
					f_map[x][y] = ObjectType.EMPTY;
				}
				else if (rowString.charAt(x) == 'w')
				{
					f_map[x][y] = ObjectType.STRONG_WALL;
				}
				else if (rowString.charAt(x) == 'b')
				{
					f_map[x][y] = ObjectType.WALL;
				}
			}
		}
		
		f_dynamicObjects = new DynamicObject[f_maxX * f_maxY];
	}
	
	public DynamicGameMap(DynamicGameMap prototype) {
		f_mapId = prototype.f_mapId;
		f_locationId = prototype.f_locationId;
		
		f_maxPlayers = prototype.f_maxPlayers;
		f_maxX = prototype.f_maxX;
		f_maxY = prototype.f_maxY;
		f_spawnX = new int[f_maxPlayers];
		f_spawnY = new int[f_maxPlayers];
		for (int i = 0; i < f_maxPlayers; ++i) {
			f_spawnX[i] = prototype.f_spawnX[i];
			f_spawnY[i] = prototype.f_spawnY[i];
		}
		f_map = new ObjectType[f_maxX][];
		for (int i = 0; i < f_maxX; ++i)
			f_map[i] = new ObjectType[f_maxY];
		for (int x = 0; x < f_maxX; ++x) 
			for (int y = 0; y < f_maxY; ++y)
				f_map[x][y] = prototype.f_map[x][y];
		
		f_dynamicObjects = new DynamicObject[f_maxX * f_maxY];
	}
	
	//Getters and setters
	
	public int getMapId() { return f_mapId; }
	
	public int getLocationId() { return f_locationId; }
	
	public int getWidth() { return f_maxX; }
	
	public int getHeight() { return f_maxY; }
	
	public ObjectType getObjectTypeAt(int x, int y) {
		if (x < 0 || y < 0 || y >= f_maxY || x >= f_maxX)
			return ObjectType.OUT;
		return f_map[x][y];
	}
	
	public ObjectType getObjectTypeAt(double x, double y) { return getObjectTypeAt(getXCell(x), getYCell(y)); }
	
	public void setObjectTypeAt(int x, int y, ObjectType value) { f_map[x][y] = value; }
	
	public void setObjectTypeAt(double x, double y, ObjectType value) { f_map[getXCell(x)][getYCell(y)] = value; }
	
	public int getMaxPlayers() { return f_maxPlayers; }
	
	public int getStartXAt(int index) { return f_spawnX[index];	}
	
	public int getStartYAt(int index) {	return f_spawnY[index];	}
	
	public int getXCell(double x) {	int cellX = (int)Math.round(x / C_BlockSize); return cellX;	}
	
	public int getYCell(double y) {	int cellY = (int)Math.round(y / C_BlockSize); return cellY;	}
	
	public int getWallBlocksCount() {
		int count = 0;
		for (int x = 0; x < f_maxX; ++x)
			for (int y = 0; y < f_maxY; ++y)
			{
				if (f_map[x][y] == ObjectType.WALL)
					count++;
			}
		return count;
	}
	
	//Dynamic object system
	
	private DynamicObject[] f_dynamicObjects;
	
	public DynamicObject getDynamicObject(int x, int y) {
		return f_dynamicObjects[getWidth() * y + x];
	}
	
	public void setDynamicObject(int x, int y, DynamicObject obj) {
		f_dynamicObjects[getWidth() * y + x] = obj;
	}
	
	public void removeDynamicObject(int x, int y) {
		f_dynamicObjects[getWidth() * y + x] = null;
	}
	
	  
	
	
	
	
	
}