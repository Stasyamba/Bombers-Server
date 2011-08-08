package com.vensella.bombers.game.mapObjects;

import java.io.IOException;

import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

import com.vensella.bombers.dispatcher.*;
import com.vensella.bombers.game.*;
import com.vensella.bombers.game.dynamicObjects.*;

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
	
	protected class DynamicObjectCreationContainer 
	{
		
		//Static
		
		//Constructors
		
		protected DynamicObjectCreationContainer(Element element) {
			f_isComplex = element.getAttribute("isComplex").equals("true");
			f_x = Integer.parseInt(element.getAttribute("x"));
			f_y = Integer.parseInt(element.getAttribute("y"));
			if (f_isComplex) {
				f_nestedContainers = new ArrayList<DynamicObjectCreationContainer>();
				NodeList nl = element.getElementsByTagName("nestedObject");
				for (int i = 0; i < nl.getLength(); ++i) {
					Element nestedObjectElement = (Element)nl.item(i);
					f_nestedContainers.add(new DynamicObjectCreationContainer(f_x, f_y, nestedObjectElement));
				}
			} else {
				m_initFields(element);
			}
		}
		
		protected DynamicObjectCreationContainer(int x, int y, Element element) {
			f_x = x;
			f_y = y;
			m_initFields(element);
		}
		
		protected DynamicObjectCreationContainer(int x, int y, int type) {
			f_x = x;
			f_y = y;
			f_type = type;
			f_attribues = new HashMap<String, String>();
		}
		
		private void m_initFields(Element element) {
			f_type = Integer.parseInt(element.getAttribute("type"));
			String chanceString = element.getAttribute("chance");
			if (chanceString.isEmpty() == false) {
				f_chance = Integer.parseInt(chanceString);
			}
			f_attribues = new HashMap<String, String>();
			NamedNodeMap attrs = element.getAttributes();
			for (int i = 0; i < attrs.getLength(); ++i) {
				Attr attr = (Attr)attrs.item(i);
				f_attribues.put(attr.getName(), attr.getValue());
			}
			
		}
		
		//Fields
		
		private boolean f_isComplex;
		private ArrayList<DynamicObjectCreationContainer> f_nestedContainers;
		
		private int f_type;
		private int f_x;
		private int f_y;
		
		private Map<String, String> f_attribues;
		
		private int f_chance = 10000;
		
		//Methods
		
		public boolean getIsComplex() { return f_isComplex; }
		public ArrayList<DynamicObjectCreationContainer> getNestedContainers() { return f_nestedContainers; }
		
		public int getType() { return f_type; }
		public int getX() { return f_x; }
		public int getY() { return f_y; }
		
		public int getChance() { return f_chance; }
		
		public Map<String, String> getAttributes() { return f_attribues; }
		
	}
	
	
	//Constants
	
	public static final int DirectionStop = 0;
	public static final int DirectionLeft = 1;
	public static final int DirectionRight = 2;
	public static final int DirectionUp = 3;
	public static final int DirectionDown = 4;
	
	public static final double C_BlockSize = 40.0;
	
	public static final int C_BlockSizeInt = 40000;
	
	public static final int C_BlockOffsetToBeeingDamaged = 5000;
	public static final int C_BlockOffsetToActivateDynamicObject = 200;
	
	//Fields
	
	private int f_mapId;
	private int f_locationId;
	
	private int f_maxX;
	private int f_maxY;
	
	private int f_maxPlayers;
	private int[] f_spawnX;
	private int[] f_spawnY;
	
	private int[] f_mapObjects;
	
	private ArrayList<DynamicObjectCreationContainer> f_mapObjectsPrototypes;
	
	private ObjectType[][] f_map;
	
	private BombersGame f_game;
	
	
	//Constructors
	
	public DynamicGameMap(String uri, MapManager manager) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(uri);
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
		f_mapObjects = new int[f_maxX * f_maxY];
		f_mapObjectsPrototypes = new ArrayList<DynamicObjectCreationContainer>();
		
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
				else if (rowString.charAt(x) == 'L')
				{
					f_map[x][y] = ObjectType.EMPTY;
					f_mapObjectsPrototypes.add(new DynamicObjectCreationContainer(x, y, DynamicObjectManager.SPECIAL_LAVA));
				}
			}
		}
		
		f_dynamicObjects = new DynamicObject[f_maxX * f_maxY];
		f_damageObjects = new DamageObject[f_maxX * f_maxY];
		

		//TODO: Parse static map objects (bonuses, items, etc...)
		
		nl = rootElement.getElementsByTagName("objects");
		NodeList objectsNodeList = ((Element)nl.item(0)).getElementsByTagName("object");
		//manager.getDispatcher().trace("Objects count = " + objectsNodeList.getLength());
		for (int i = 0; i < objectsNodeList.getLength(); ++i)
		{
			Element element = (Element)objectsNodeList.item(i);	
			f_mapObjectsPrototypes.add(new DynamicObjectCreationContainer(element));
		}
	}
	
	public DynamicGameMap(BombersGame game, DynamicGameMap prototype) {
		f_game = game;
		
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
		f_damageObjects = new DamageObject[f_maxX * f_maxY];
		
		f_mapObjects = new int[f_maxX * f_maxY];
		for (int i = 0; i < f_maxX * f_maxY; ++i) {
			f_mapObjects[i] = prototype.f_mapObjects[i];
		}
		
		f_mapObjectsPrototypes = prototype.f_mapObjectsPrototypes;
		
		m_initRandomBonuses();
		m_initConfiguredBonuses();
	}
	
	//Initialization methods
	
	protected void m_initRandomBonuses() {
		
		//Standard bonuses
		
		int countAddBomb = 8;
		int countAddPower = 8;
		int countAddSpeed = 8;
		int countAddHealth = 4;
		
		//Resource bonuses
		
		int countGold1 = 0;
		int countGold2 = 0;
		int countGold5 = 0;
		
		int totalGold = 0;
		int maxGold = 10;
		while (totalGold < maxGold) {
			double r = Math.random();
			if (r < 0.2) {
				if (totalGold + 5 < maxGold) {
					countGold5++;
					totalGold += 5;
				}
			} else if (r < 0.55) {
				if (totalGold + 2 < maxGold) {
					countGold2++;
					totalGold += 2;
				}				
			} else {
				countGold1++;
				totalGold += 1;
			}
		}
		
		//Item bonuses
		
		int countItem = 0;
		//int itemId = WeaponsManager.WEAPON_BOMB_DYNAMITE;
		
		//Bonuses allocation
		
		int wallBlocks = getWallBlocksCount();
		for (int x = 0; x < f_maxX; ++x) {
			for (int y = 0; y < f_maxY; ++y) {
				if (f_map[x][y] == ObjectType.WALL) {
					int bonusesLeft = countAddBomb + countAddPower + countAddSpeed + countAddHealth;
					double C = 1.0 * bonusesLeft / wallBlocks;
					double p = Math.random();
					wallBlocks--;
					if (p < C) {
						DynamicObject bonus = null;
						int bonusType;
						double[] possibilites = { 
								Math.random() * countAddBomb,
								Math.random() * countAddPower,
								Math.random() * countAddSpeed,
								Math.random() * countAddHealth,
								Math.random() * countGold5,
								Math.random() * countGold2,
								Math.random() * countGold1,
								Math.random() * countItem
						};
						int maxIndex = 0;
						double maxRate = Double.MIN_VALUE;
						for (int i = 0; i < possibilites.length; ++i) {
							if (possibilites[i] > maxRate) {
								maxRate = possibilites[i];
								maxIndex = i;
							}
						}
						
						if (maxIndex == 0) {
							bonusType = DynamicObjectManager.BONUS_ADD_BOMB;
							f_mapObjects[f_maxX * y + x] = bonusType;
							bonus = f_game.getDynamicObjectManager().createDynamicObject(bonusType, x, y);
							countAddBomb--;
						}
						else if (maxIndex == 1)	{
							bonusType = DynamicObjectManager.BONUS_ADD_BOMB_POWER;
							f_mapObjects[f_maxX * y + x] = bonusType;
							bonus = f_game.getDynamicObjectManager().createDynamicObject(bonusType, x, y);
							countAddPower--;
						}
						else if (maxIndex == 2)	{
							bonusType = DynamicObjectManager.BONUS_ADD_SPEED;
							f_mapObjects[f_maxX * y + x] = bonusType;
							bonus = f_game.getDynamicObjectManager().createDynamicObject(bonusType, x, y);
							countAddSpeed--;
						}
						else if (maxIndex == 3) {
							bonusType = DynamicObjectManager.BONUS_ADD_HEAL;
							f_mapObjects[f_maxX * y + x] = bonusType;
							bonus = f_game.getDynamicObjectManager().createDynamicObject(bonusType, x, y);
							countAddHealth--;
						}
						else if (maxIndex == 4) {
							bonusType = DynamicObjectManager.BONUS_ADD_RESOURCE;
							f_mapObjects[f_maxX * y + x] = bonusType;
							bonus = new ResourceBonus(f_game, x, y, PricelistManager.C_ResourceTypeGold, 5);
							countGold5--;
						}
						else if (maxIndex == 5) {
							bonusType = DynamicObjectManager.BONUS_ADD_RESOURCE;
							f_mapObjects[f_maxX * y + x] = bonusType;
							bonus = new ResourceBonus(f_game, x, y, PricelistManager.C_ResourceTypeGold, 2);
							countGold2--;
						}
						else if (maxIndex == 6) {
							bonusType = DynamicObjectManager.BONUS_ADD_RESOURCE;
							f_mapObjects[f_maxX * y + x] = bonusType;
							bonus = new ResourceBonus(f_game, x, y, PricelistManager.C_ResourceTypeGold, 1);
							countGold1--;
						}
						else if (maxIndex == 7) {
							//bonusType = DynamicObjectManager.BONUS_ADD_ITEM;
							//f_mapObjects[f_maxX * y + x] = bonusType;
							//bonus = f_game.getDynamicObjectManager().createDynamicObject(bonusType, x, y);
							countItem--;
						}
						setDynamicObject(x, y, bonus);
					}
				}
			}
		}
	}
	
	protected void m_initConfiguredBonuses() {
		for (DynamicObjectCreationContainer docc : f_mapObjectsPrototypes) {
			if (docc.getIsComplex()) {
				for (DynamicObjectCreationContainer nestedDocc : docc.getNestedContainers()) {
					m_possiblyCreateDynamicObjectFromPrototype(nestedDocc);
				}
			} else {
				m_possiblyCreateDynamicObjectFromPrototype(docc);
			}
		}
	}
	
	private void m_possiblyCreateDynamicObjectFromPrototype(DynamicObjectCreationContainer docc) {
		int r = (int)(Math.random() * 10000);
		if (r < docc.getChance()) {
			DynamicObject dobj = getGame().getDynamicObjectManager().createDynamicObject(
						docc.getType(), 
						docc.getX(), 
						docc.getY(), 
						docc.getAttributes()
					);
			setDynamicObject(docc.getX(), docc.getY(), dobj);
			f_mapObjects[docc.getY() * f_maxX + docc.getX()] = docc.getType();
		}
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
	
	public int getXCell(double x) {	int cellX = (int)(x / C_BlockSize); return cellX;	}
	
	public int getYCell(double y) {	int cellY = (int)(y / C_BlockSize); return cellY;	}
	
	public boolean canGo(int x, int y) {
		boolean isFreeSpace = getObjectTypeAt(x, y) == ObjectType.EMPTY;
		if (!isFreeSpace) return false;
		DynamicObject dobj = getDynamicObject(x, y);
		if (dobj != null && !dobj.getCanGoThrough()) return false;
		return true;
	}
	
	public boolean canGo(double x, double y) {
		if (x < 0.0 || y < 0.0) return false;
		int ix = getXCell(x);
		int iy = getYCell(y);
		return canGo(ix, iy);
	}
	
	public boolean canGoInt(int xi, int yi) {
		if (xi < 0 || yi < 0) return false;
		int xcell = xi / C_BlockSizeInt;
		int ycell = yi / C_BlockSizeInt;
		return canGo(xcell, ycell);
	}
	
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
	
	public BombersGame getGame() { return f_game; }
	//public void setGame(BombersGame game) { f_game = game; }
	
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
	
	//Damage object system
	
	private DamageObject[] f_damageObjects;
	
	public DamageObject getDamageObject(int x, int y) {
		return f_damageObjects[getWidth() * y + x];
	}
	
	public void setDamageObject(int x, int y, DamageObject obj) {
		f_damageObjects[getWidth() * y + x] = obj;
	}
	
	public void removeDamageObject(int x, int y) {
		f_damageObjects[getWidth() * y + x] = null;
	}
	
	//Custom methods
	
	public SFSArray getBonusesDescription() {
		SFSArray bonuses = new SFSArray();
		for (int i = 0; i < f_maxX * f_maxY; ++i) {
			if (f_mapObjects[i] != DynamicObjectManager.INVALID_DYNAMIC_OBJECT) {
				SFSObject bonus = new SFSObject();
				bonus.putInt("X", i % f_maxX);
				bonus.putInt("Y", i / f_maxX);
				bonus.putInt("T", f_mapObjects[i]);
				
				//TODO: Rewrite very BAD code, add abstract method toSFSObject to DynamicObject
				DynamicObject dobj = getDynamicObject(i % f_maxX, i / f_maxX);
				if (dobj != null) {
					if (dobj instanceof ResourceBonus) {
						ResourceBonus rb = (ResourceBonus)dobj;
						bonus.putInt("P0", rb.getResourceType());
						bonus.putInt("P1", rb.getCount());
					} else if (dobj instanceof ItemBonus) {
						ItemBonus rb = (ItemBonus)dobj;
						bonus.putInt("P0", rb.getItemType());
						bonus.putInt("P1", rb.getCount());						 
					} else if (dobj instanceof SpecialWall) {
						SpecialWall rb = (SpecialWall)dobj;
						bonus.putInt("P0", rb.getDestroysBy());
						bonus.putInt("P1", rb.getLife());						
					}
				}
				
				bonuses.addSFSObject(bonus);
			}
		}
		return bonuses;
	}
	
	//Move calculation
	
	public void calculatePosition(PlayerGameProfile profile, long millsTicks) {
		if (profile.getInputDirection() == DirectionStop) {
			checkPlayerPosition(profile);
			return;
		}
		
		int distance = (int)(millsTicks * profile.getSpeed());
		
		if (distance >= C_BlockSizeInt) {
			//TODO: Log bad situation
		}
		//if (!canGo(profile.getX(), profile.getY())) return;
			
		
		int inputDirection = profile.getInputDirection();
		
		boolean isAlignedToGoVertical =  profile.getXi() % C_BlockSizeInt == 0;
		boolean isAlignedToGoHorizontal =  profile.getYi() % C_BlockSizeInt == 0;
		
		if (inputDirection == DirectionDown || inputDirection == DirectionUp ) {
			//We are already aligned due to the cell? So go strictly vertically if possible
			if (isAlignedToGoVertical) {
				if (inputDirection == DirectionDown) {
					//Go down
					int delta = profile.getYi() % C_BlockSizeInt;
					if (delta > 0) {
						int needToGoToCell = C_BlockSizeInt - delta;
						int go = Math.min(distance, needToGoToCell);
						
						distance -= go;
						profile.addYi(go);
					} 
					//Can go to the next block?
					if (canGoInt(profile.getXi() + 1, profile.getYi() + C_BlockSizeInt + 1)) {
						//Going all remaining distance - we assumed that distance is strictly less then block size
						profile.addYi(distance);
					}
					profile.setViewDirection(DirectionDown);
				} else {
					//Go up
					int delta = profile.getYi() % C_BlockSizeInt;
					if (delta > 0.0) {
						int needToGoToCell = delta;
						int go = Math.min(distance, needToGoToCell);
						
						distance -= go;
						profile.addYi(-go);
					} 
					//Can go to the next block?
					if (canGoInt(profile.getXi() + 1, profile.getYi() - 1)) {
						//Going all remaining distance - we assumed that distance is strictly less then block size
						profile.addYi(-distance);
					}
					profile.setViewDirection(DirectionUp);
				}
			}
			else {
				//Need to go around in some cases (if possible)
				if (inputDirection == DirectionDown) {
					//
					//Go DOWN walking AROUND blocks
					//
					int centerX = profile.getXi() + C_BlockSizeInt / 2;
					int centerXDelta = centerX % C_BlockSizeInt;
					
					if (centerXDelta < C_BlockSizeInt / 2) {
						if (canGoInt(centerX, profile.getYi() + C_BlockSizeInt + 1)) {
							//Go right and down
							//Go right
							int go = Math.min(distance, C_BlockSizeInt / 2 - centerXDelta);
							profile.addXi(go);
							distance -= go;
							profile.setViewDirection(DirectionRight);
							//Go down
							if (distance > 0) {
								profile.addYi(distance);
								profile.setViewDirection(DirectionDown);
							}
						} else if (canGoInt(centerX - C_BlockSizeInt, profile.getYi() + C_BlockSizeInt + 1)) {
							//Go left and down
							//Go left
							int go = Math.min(distance, centerXDelta + C_BlockSizeInt / 2);
							profile.addXi(-go);
							distance -= go;
							profile.setViewDirection(DirectionLeft);
							//Go down
							if (distance > 0) {
								profile.addYi(distance);
								profile.setViewDirection(DirectionDown);
							}
						}
						else {
							//Do nothing - just set view direction to down
							profile.setViewDirection(DirectionDown);
						}
					}
					else {
						if (canGoInt(centerX, profile.getYi() + C_BlockSizeInt + 1)) {
							//Go left and down
							//Go left
							int go = Math.min(distance, centerXDelta - C_BlockSizeInt / 2);
							profile.addXi(-go);
							distance -= go;
							profile.setViewDirection(DirectionLeft);
							//Go down
							if (distance > 0) {
								profile.addYi(distance);
								profile.setViewDirection(DirectionDown);
							}
						} else if (canGoInt(centerX + C_BlockSizeInt, profile.getYi() + C_BlockSizeInt + 1)) {
							//Go right and down
							//Go right
							int go = Math.min(distance, C_BlockSizeInt - centerXDelta + C_BlockSizeInt / 2);
							profile.addXi(go);
							distance -= go;
							profile.setViewDirection(DirectionRight);
							//Go down
							if (distance > 0) {
								profile.addYi(distance);
								profile.setViewDirection(DirectionDown);
							}
						}
						else {
							//Do nothing - just set view direction to down
							profile.setViewDirection(DirectionDown);
						}						
					}
				} else {
					//
					//Go UP walking AROUND blocks
					//
					int centerX = profile.getXi() + C_BlockSizeInt / 2;
					int centerXDelta = centerX % C_BlockSizeInt;
					
					if (centerXDelta < C_BlockSizeInt / 2) {
						if (canGoInt(centerX, profile.getYi() - 1)) {
							//Go right and up
							//Go right
							int go = Math.min(distance, C_BlockSizeInt / 2 - centerXDelta);
							profile.addXi(go);
							distance -= go;
							profile.setViewDirection(DirectionRight);
							//Go up
							if (distance > 0) {
								profile.addYi(-distance);
								profile.setViewDirection(DirectionUp);
							}
						} else if (canGoInt(centerX - C_BlockSizeInt, profile.getYi() - 1)) {
							//Go left and up
							//Go left
							int go = Math.min(distance, centerXDelta + C_BlockSizeInt / 2);
							profile.addXi(-go);
							distance -= go;
							profile.setViewDirection(DirectionLeft);
							//Go up
							if (distance > 0) {
								profile.addYi(-distance);
								profile.setViewDirection(DirectionUp);
							}
						}
						else {
							//Do nothing - just set view direction to down
							profile.setViewDirection(DirectionUp);
						}
					}
					else {
						if (canGoInt(centerX, profile.getYi() - 1)) {
							//Go left and up
							//Go left
							int go = Math.min(distance, centerXDelta - C_BlockSizeInt / 2);
							profile.addXi(-go);
							distance -= go;
							profile.setViewDirection(DirectionLeft);
							//Go up
							if (distance > 0) {
								profile.addYi(-distance);
								profile.setViewDirection(DirectionUp);
							}
						} else if (canGoInt(centerX + C_BlockSizeInt, profile.getYi() - 1)) {
							//Go right and up
							//Go right
							int go = Math.min(distance, C_BlockSizeInt - centerXDelta + C_BlockSizeInt / 2);
							profile.addXi(go);
							distance -= go;
							profile.setViewDirection(DirectionRight);
							//Go up
							if (distance > 0) {
								profile.addYi(-distance);
								profile.setViewDirection(DirectionUp);
							}
						}
						else {
							//Do nothing - just set view direction to down
							profile.setViewDirection(DirectionUp);
						}						
					}
				}			
			}
		}
		else if (inputDirection == DirectionLeft || inputDirection == DirectionRight) {
			//We are already aligned due to the cell? So go strictly horizontally if possible
			if (isAlignedToGoHorizontal) {
				if (inputDirection == DirectionRight) {
					//Go right
					int delta = profile.getXi() % C_BlockSizeInt;
					if (delta > 0) {
						int needToGoToCell = C_BlockSizeInt - delta;
						int go = Math.min(distance, needToGoToCell);
						
						distance -= go;
						profile.addXi(go);
					} 
					//Can go to the next block?
					if (canGoInt(profile.getXi() + C_BlockSizeInt + 1, profile.getYi() + 1)) {
						//Going all remaining distance - we assumed that distance is strictly less then block size
						profile.addXi(distance);
					}
					profile.setViewDirection(DirectionRight);
				} else {
					//Go left
					int delta = profile.getXi() % C_BlockSizeInt;
					if (delta > 0) {
						int needToGoToCell = delta;
						int go = Math.min(distance, needToGoToCell);
						
						distance -= go;
						profile.addXi(-go);
					} 
					//Can go to the next block?
					if (canGoInt(profile.getXi() - 1, profile.getYi() + 1)) {
						//Going all remaining distance - we assumed that distance is strictly less then block size
						profile.addXi(-distance);
					}
					profile.setViewDirection(DirectionLeft);
				}				
			} else {
				//Need to go around in some cases (if possible)
				if (inputDirection == DirectionRight) {
					//
					//Go RIGHT walking AROUND blocks
					//
					int centerY = profile.getYi() + C_BlockSizeInt / 2;
					int centerYDelta = centerY % C_BlockSizeInt;
					
					if (centerYDelta < C_BlockSizeInt / 2) {
						if (canGoInt(profile.getXi() + C_BlockSizeInt + 1, centerY)) {
							//Go down and right
							//Go down
							int go = Math.min(distance, C_BlockSizeInt / 2 - centerYDelta);
							profile.addYi(go);
							distance -= go;
							profile.setViewDirection(DirectionDown);
							//Go right
							if (distance > 0) {
								profile.addXi(distance);
								profile.setViewDirection(DirectionRight);
							}
						} else if (canGoInt(profile.getXi() + C_BlockSizeInt + 1, centerY - C_BlockSizeInt)) {
							//Go up and right
							//Go up
							int go = Math.min(distance, centerYDelta + C_BlockSizeInt / 2);
							profile.addYi(-go);
							distance -= go;
							profile.setViewDirection(DirectionUp);
							//Go right
							if (distance > 0) {
								profile.addXi(distance);
								profile.setViewDirection(DirectionRight);
							}
						}
						else {
							//Do nothing - just set view direction to RIGHT
							profile.setViewDirection(DirectionRight);
						}
					}
					else {
						if (canGoInt(profile.getXi() + C_BlockSizeInt + 1, centerY)) {
							//Go up and right
							//Go up
							int go = Math.min(distance, centerYDelta - C_BlockSizeInt / 2);
							profile.addYi(-go);
							distance -= go;
							profile.setViewDirection(DirectionUp);
							//Go right
							if (distance > 0.0) {
								profile.addXi(distance);
								profile.setViewDirection(DirectionRight);
							}
						} else if (canGoInt(profile.getXi() + C_BlockSizeInt + 1, centerY + C_BlockSizeInt)) {
							//Go down and right
							//Go down
							int go = Math.min(distance, C_BlockSizeInt - centerYDelta + C_BlockSizeInt / 2);
							profile.addYi(go);
							distance -= go;
							profile.setViewDirection(DirectionDown);
							//Go right
							if (distance > 0.0) {
								profile.addXi(distance);
								profile.setViewDirection(DirectionRight);
							}
						}
						else {
							//Do nothing - just set view direction to RIGHT
							profile.setViewDirection(DirectionRight);
						}						
					}
				} else {
					//
					//Go LEFT walking AROUND blocks
					//
					int centerY = profile.getYi() + C_BlockSizeInt / 2;
					int centerYDelta = centerY % C_BlockSizeInt;
					
					if (centerYDelta < C_BlockSizeInt / 2.0) {
						if (canGoInt(profile.getXi() -  1, centerY)) {
							//Go down and left
							//Go down
							int go = Math.min(distance, C_BlockSizeInt / 2 - centerYDelta);
							profile.addYi(go);
							distance -= go;
							profile.setViewDirection(DirectionDown);
							//Go left
							if (distance > 0) {
								profile.addXi(-distance);
								profile.setViewDirection(DirectionLeft);
							}
						} else if (canGoInt(profile.getXi() - 1, centerY - C_BlockSizeInt)) {
							//Go up and left
							//Go up
							int go = Math.min(distance, centerYDelta + C_BlockSizeInt / 2);
							profile.addYi(-go);
							distance -= go;
							profile.setViewDirection(DirectionUp);
							//Go right
							if (distance > 0) {
								profile.addXi(-distance);
								profile.setViewDirection(DirectionLeft);
							}
						}
						else {
							//Do nothing - just set view direction to RIGHT
							profile.setViewDirection(DirectionLeft);
						}
					}
					else {
						if (canGoInt(profile.getXi() - 1, centerY)) {
							//Go up and left
							//Go up
							int go = Math.min(distance, centerYDelta - C_BlockSizeInt / 2);
							profile.addYi(-go);
							distance -= go;
							profile.setViewDirection(DirectionUp);
							//Go left
							if (distance > 0) {
								profile.addXi(-distance);
								profile.setViewDirection(DirectionLeft);
							}
						} else if (canGoInt(profile.getXi() - 1, centerY + C_BlockSizeInt)) {
							//Go down and left
							//Go down
							int go = Math.min(distance, C_BlockSizeInt - centerYDelta + C_BlockSizeInt / 2);
							profile.addYi(go);
							distance -= go;
							profile.setViewDirection(DirectionDown);
							//Go left
							if (distance > 0) {
								profile.addXi(-distance);
								profile.setViewDirection(DirectionLeft);
							}
						}
						else {
							//Do nothing - just set view direction to RIGHT
							profile.setViewDirection(DirectionLeft);
						}						
					}
				}
			}
		}
		//Else - just stand on place =)
		
		//Activate bonuses, or damage player 
		checkPlayerPosition(profile);
	}
	  
	//Checks player position and activates DynamicObjects or/and damages player
	public void checkPlayerPosition(PlayerGameProfile profile) {
		int xc = profile.getXi() / C_BlockSizeInt;
		int yc = profile.getYi() / C_BlockSizeInt;
		
		int xOffs = profile.getXi() % C_BlockSizeInt;
		int yOffs = profile.getYi() % C_BlockSizeInt;
		
		if (xOffs != 0) {
			if (xOffs < C_BlockSizeInt - C_BlockOffsetToBeeingDamaged) {
				tryDamagePlayerAt(profile, xc, yc);
			}
			if (xOffs < C_BlockSizeInt - C_BlockOffsetToActivateDynamicObject) {
				tryActivateDynamicObjectAt(profile, xc, yc);
			}
			if (xOffs > C_BlockOffsetToBeeingDamaged) {
				tryDamagePlayerAt(profile, xc + 1, yc);
			}
			if (xOffs > C_BlockOffsetToActivateDynamicObject) {
				tryActivateDynamicObjectAt(profile, xc + 1, yc);
			}
		} else {
			if (yOffs < C_BlockSizeInt - C_BlockOffsetToBeeingDamaged) {
				tryDamagePlayerAt(profile, xc, yc);
			}
			if (yOffs < C_BlockSizeInt - C_BlockOffsetToActivateDynamicObject) {
				tryActivateDynamicObjectAt(profile, xc, yc);
			}
			if (yOffs > C_BlockOffsetToBeeingDamaged) {
				tryDamagePlayerAt(profile, xc, yc + 1);
			}
			if (yOffs > C_BlockOffsetToActivateDynamicObject) {
				tryActivateDynamicObjectAt(profile, xc, yc + 1);
			}			
		}	
	}
	
	protected void tryDamagePlayerAt(PlayerGameProfile profile, int x, int y) {
		long time = System.currentTimeMillis();
		DamageObject dam = getDamageObject(x, y);
//		if (dam != null) {
//			f_game.trace("Entered damage object at " +x + ", " + y + ", time = " + time + ", damage object set time = " +
//					dam.getSettedTime());
//		}
		
		if (dam != null && 
			profile.getDamageTime() + PlayerGameProfile.C_ImmortalTime < time &&
			dam.getSettedTime() + dam.getLifetime() > time) 
		{
			profile.setDamageTime(time);
			f_game.addGameEvent(dam.getDamageEvent(f_game, profile));
		}			
	}
	
	protected void tryActivateDynamicObjectAt(PlayerGameProfile profile, int x, int y) {
		DynamicObject dyn = getDynamicObject(x, y);
		if (dyn != null && dyn.getCanBeActivatedByPlayer()) {
			dyn.setOwner(profile.getUser());
			f_game.addGameEvent(dyn.getActivateEvent());
			if (!dyn.getStaysAfterActivationByPlayer()) {
				removeDynamicObject(x, y);
			}
		}			
	}
}