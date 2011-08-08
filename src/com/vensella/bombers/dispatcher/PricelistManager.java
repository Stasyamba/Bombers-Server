package com.vensella.bombers.dispatcher;

import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

import com.vensella.bombers.dispatcher.StatisticsManager.SessionStats;
import com.vensella.bombers.game.mapObjects.Locations;

//TODO: Add concurrent locks
public class PricelistManager {

	//Constants
	
	public static final int C_UnknownItem = -1;
	
	public static final int C_ResourceTypeGold = 1;
	public static final int C_ResourceTypeCrystal = 2;
	public static final int C_ResourceTypeAdamantium = 4;
	public static final int C_ResourceTypeAntimatter = 8;
	public static final int C_ResourceTypeEnergy = 16;
	
	//Nested types
	
	private class ItemCost
	{
		//Fields
		
		private int f_itemId;
		
		private int f_gold;
		private int f_crystal;
		private int f_adamantium;
		private int f_antimatter;
		
		private int f_goldDelta;
		private int f_crystalDelta;
		
		private int f_stack;
		private int f_maxStack;
		private int f_level;
		private boolean f_specialOffer;
		
		//Constructors
		
		protected ItemCost(Element itemElement) {
			String goldAttr = itemElement.getAttribute("gold");
			if (goldAttr.contains(",")) {
				String[] parts = goldAttr.split(",");
				f_gold = Integer.parseInt(parts[0]);
				f_goldDelta = Integer.parseInt(parts[1]);
			} else {
				f_gold = Integer.parseInt(goldAttr);
			}
			f_gold = f_gold > 0 ? f_gold : Integer.MAX_VALUE;
			
			String crystalAttr = itemElement.getAttribute("crystal");
			if (crystalAttr.contains(",")) {
				String[] parts = crystalAttr.split(",");
				f_crystal = Integer.parseInt(parts[0]);
				f_crystalDelta = Integer.parseInt(parts[1]);
			} else {
				f_crystal = Integer.parseInt(crystalAttr);
			}
			f_crystal = f_crystal > 0 ? f_crystal : Integer.MAX_VALUE;
			
			f_adamantium = 
				itemElement.getAttribute("adamantium").isEmpty() ? 0 : Integer.parseInt(itemElement.getAttribute("adamantium"));
			f_adamantium = f_adamantium > 0 ? f_adamantium : Integer.MAX_VALUE;
			f_antimatter = 
				itemElement.getAttribute("antimatter").isEmpty() ? 0 : Integer.parseInt(itemElement.getAttribute("antimatter"));
			f_antimatter = f_antimatter > 0 ? f_antimatter : Integer.MAX_VALUE;
			
			if (itemElement.getAttribute("stack").isEmpty() == false) {
				f_stack = Integer.parseInt(itemElement.getAttribute("stack"));
			} else {
				f_stack = 1;
			}
			if (itemElement.getAttribute("maxStack").isEmpty() == false) {
				f_maxStack = Integer.parseInt(itemElement.getAttribute("maxStack"));
			} else {
				f_maxStack = 1000000;
			}
			f_level = Integer.parseInt(itemElement.getAttribute("level"));
			f_itemId = Integer.parseInt(itemElement.getAttribute("itemId"));
			f_specialOffer = itemElement.getAttribute("s") == "true";
		}
		
		protected ItemCost(
				int id, int gold, int crystal, int adamantium, int antimatter, int stack, int level, boolean specialOffer)
		{
			f_gold = gold;
			f_crystal = crystal;
			f_adamantium = adamantium;
			f_antimatter = antimatter;		
			f_stack = stack;
			f_level = level;
			f_specialOffer = specialOffer;
		}
		
		//Methods
		
		public int getId() { return f_itemId; }
		
		public int getGold() { return f_gold; }
		public int getCrystal() { return f_crystal; }
		public int getAdamantium() { return f_adamantium; }
		public int getAntimatter() { return f_antimatter; }
		
		/*
		 * Returns increase of item price per one purchase in gold
		 */
		public int getGoldDelta() { return f_goldDelta; }
		/*
		 * Returns increase of item price per one purchase in 1/100 crystal
		 */
		public int getCrystalDelta() { return f_crystalDelta; }
		
		public int getStack() { return f_stack; }
		public int getMaxStack() { return f_maxStack; }
		public int getLevel() { return f_level; }
		public boolean getSpecialOffer() { return f_specialOffer; }
		
		public ItemCost getItemCostInSpecifiedResource(int resourceType, int currentCount) {
			if ((resourceType & C_ResourceTypeGold) > 0) {
				//int gold = (f_gold == Integer.MAX_VALUE && currentCount * f_goldDelta > 0) ? 0 : f_gold;
				int gold = f_gold;
				return new ItemCost(f_itemId, 
									gold + f_goldDelta * currentCount, 0, 0, 0, 
									f_stack, f_level, f_specialOffer);
			} else if ((resourceType & C_ResourceTypeCrystal) > 0) {
				//int crystal = (f_crystal == Integer.MAX_VALUE && currentCount * f_crystalDelta >= 100) ? 0 : f_crystal;
				int crystal = f_crystal;
				return new ItemCost(f_itemId, 
									0, crystal + (currentCount * f_crystalDelta) / 100, 0, 0, 
									f_stack, f_level, f_specialOffer);
			} else if ((resourceType & C_ResourceTypeAdamantium) > 0) {
				return new ItemCost(f_itemId, 
									0, 0, f_adamantium, 0, 
									f_stack, f_level, f_specialOffer);
			} else if ((resourceType & C_ResourceTypeAntimatter) > 0) {
				return new ItemCost(f_itemId, 
									0, 0, 0, f_antimatter, 
									f_stack, f_level, f_specialOffer);
			} else {
				return null;
			}
		}
		
	}
	
	public class ItemColection {
		
		//Constructors
		
		//Can be found only on specific locations
		public ItemColection(int id, 
				ArrayList<Integer> parts, ArrayList<Integer> counts,
				ArrayList<ArrayList<Integer>> chances) {
			assert chances.size() == parts.size();
			assert parts.size() == counts.size();
			f_id = id;
			f_parts = parts;
			f_counts = counts;
			f_chances = chances;
		}
		
		//Fields
		
		private int f_id;
		private ArrayList<Integer> f_parts;
		private ArrayList<Integer> f_counts;
		private ArrayList<ArrayList<Integer>> f_chances;
		
		//Methods
		
		public int getId() { return f_id; }
		
		public int getItemsNamesCount() { return f_parts.size(); }
		
		public Iterable<Integer> getParts() { return f_parts; }
		public int getCountForItem(int itemId) { 
			for (int i = 0; i < f_parts.size(); ++i) {
				if (f_parts.get(i) == itemId) {
					return f_counts.get(i);
				}
			}
			return Integer.MAX_VALUE;
		}
		
		public int possibleGetRandomItem(int locationId) {
			int item = C_UnknownItem;
			for (int itemId = 0; itemId < f_chances.size(); ++itemId) {
				int random = (int)(10000 * Math.random());
				if (random < f_chances.get(itemId).get(locationId)) {
					item = itemId;
					break;
				}
			}
			return item;
		}
			
	}
	
	public class Mission {
		
		//Nested type
		
		//Constructors
		
		protected Mission(Element missionElement) {
			f_id = missionElement.getAttribute("id");
			f_locationId = Integer.parseInt(missionElement.getAttribute("location"));
			f_energyCost = Integer.parseInt(missionElement.getAttribute("energy")); 
			Element bronzeRewardElememt = (Element)(missionElement.getElementsByTagName("bronze")).item(0);
			f_bronzeReward = new Reward(bronzeRewardElememt);
			Element silverRewardElememt = (Element)(missionElement.getElementsByTagName("silver")).item(0);
			f_silverReward = new Reward(silverRewardElememt);
			Element goldRewardElememt = (Element)(missionElement.getElementsByTagName("gold")).item(0);
			f_goldReward = new Reward(goldRewardElememt);
		}
		
		//Fields
		
		private String f_id;
		private int f_locationId;
		
		private int f_energyCost;
		
		private Reward f_bronzeReward;
		private Reward f_silverReward;
		private Reward f_goldReward;
		
		//Methods
		
		public String getId() { return f_id; }
		public int getLocation() { return f_locationId; }
		
		public int getEnergyCost() { return f_energyCost; }
		
		public Reward getBronzeReward() { return f_bronzeReward; }
		public Reward getSilverReward() { return f_silverReward; }
		public Reward getGoldReward() { return f_goldReward; }
		
	}
	
	public class LevelDescription {
		
		//Constructor
		
		protected LevelDescription(Element levelElement) {
			f_value = Integer.parseInt(levelElement.getAttribute("value"));
			f_group = Integer.parseInt(levelElement.getAttribute("group"));
			f_experience = Integer.parseInt(levelElement.getAttribute("exp"));
			String[] earns = levelElement.getAttribute("earns").split(",");
			f_earnsForPlaceOne = Integer.parseInt(earns[0]);
			f_earnsForPlaceTwo = Integer.parseInt(earns[1]);
			f_earnsForPlaceThree = Integer.parseInt(earns[2]);			
			String[] benefits = levelElement.getAttribute("benefits").split(",");
			f_benefitPlusOne = Integer.parseInt(benefits[0]);
			f_benefitPlusTwo = Integer.parseInt(benefits[1]);
			String[] fines = levelElement.getAttribute("fines").split(",");
			f_fineMinusOne = Integer.parseInt(fines[0]);
			f_fineMinusTwo = Integer.parseInt(fines[1]);
			
			NodeList nl = levelElement.getElementsByTagName("reward");
			if (nl.getLength() > 0) {
				Element rewardElement = (Element)nl.item(0);
				f_reward = new Reward(rewardElement);
			}
		}
		
		//Fields
		
		public int f_value;
		public int f_group;
		
		public int f_experience;
		
		public int f_earnsForPlaceOne;
		public int f_earnsForPlaceTwo;
		public int f_earnsForPlaceThree;
		
		public int f_benefitPlusOne;
		public int f_benefitPlusTwo;
		
		public int f_fineMinusOne;
		public int f_fineMinusTwo;
		
		public Reward f_reward;
		
		//Methods
		
		public int getValue() { return f_value; }
		public int getGroup() { return f_group; }
		
		public int getExperience() { return f_experience; }
		
		public int getEarnsForPlaceOne() { return f_earnsForPlaceOne; }
		public int getEarnsForPlaceTwo() { return f_earnsForPlaceTwo; }
		public int getEarnsForPlaceThree() { return f_earnsForPlaceThree; }
		
		public int getBenefitPlusOne() { return f_benefitPlusOne; }
		public int getBenefitPlusTwo() { return f_benefitPlusTwo; }
		
		public int getFineMinusOne() { return f_fineMinusOne; }
		public int getFineMinusTwo() { return f_fineMinusTwo; }
		
		public Reward getReward() { return f_reward; }
		
		public int getDeltaFor(LevelDescription another) {
			int g = another.getGroup() - f_group;
			if (g == 1) { return f_benefitPlusOne; }
			else if (g >= 2) { return f_benefitPlusTwo; }
			else if (g == -1) { return f_fineMinusOne; }
			else if (g <= -2) { return f_fineMinusTwo; }
			else { return 0; }
		}
		
	}
	
	public class LocationDescription {
		
		//Constructor
		
		protected LocationDescription(Element locationElement) {
			
		}
		
		//Fields
		
		//Methods
		
	}
	
	//Constants
	
	//private static final String PricelistPath = "/usr/local/nginx/html/main/bombers/pricelist/pricelist.xml";
	private static final String C_PricelistUrl = "http://46.182.31.151/bombers/pricelist/pricelist.xml";
	
	//Static methods
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	
	private SFSObject f_sfsObject;
	
	private ArrayList<Integer[]> f_goldCostPacks;
	private ArrayList<Integer[]> f_crystalCostPacks;
	private ArrayList<Integer[]> f_adamantiumCostPacks;
	private ArrayList<Integer[]> f_antimatterCostPacks;
	
	private ArrayList<Integer[]> f_energyPacks;
	
	//private ArrayList<Integer[]> f_discounts;
	
	private Map<Integer, ItemCost> f_items;
	private Map<Integer, ItemColection> f_collections;
	
	private ArrayList<LevelDescription> f_levels;
	
	public Map<String, Mission> f_missions;
	
	
	//Constructor
	
	public PricelistManager(BombersDispatcher dispatcher) {
		f_dispatcher = dispatcher;
		
		f_goldCostPacks = new ArrayList<Integer[]>();
		f_crystalCostPacks = new ArrayList<Integer[]>();
		f_adamantiumCostPacks = new ArrayList<Integer[]>();
		f_antimatterCostPacks = new ArrayList<Integer[]>();
		
		f_energyPacks = new ArrayList<Integer[]>();
		//f_discounts = new ArrayList<Integer[]>();
		
		f_items = new HashMap<Integer, ItemCost>();
		f_collections = new HashMap<Integer, PricelistManager.ItemColection>();
		f_levels = new ArrayList<LevelDescription>();
		
		f_missions = new HashMap<String, PricelistManager.Mission>();
		
		try {
			f_dispatcher.trace(ExtensionLogLevel.WARN, "Download pricelist start");
			parsePricelist(C_PricelistUrl);
			f_dispatcher.trace(ExtensionLogLevel.WARN, "Download pricelist end");
		}
		catch (Exception ex) {
			f_dispatcher.trace(ExtensionLogLevel.ERROR, "While parsing pricelist at url " + C_PricelistUrl);
			f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.toString());
			f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
		}
	}
	
	//Internal methods
	
	private void parsePricelist(String uri) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(uri);
		//doc.getDocumentElement().normalize(); 
		Element rootElement = doc.getDocumentElement();
		
		NodeList nl = null;
		
		//<resources>
		nl = rootElement.getElementsByTagName("resources");
		Element resourcesElement = (Element)nl.item(0);
		m_initResourcePrices(resourcesElement);
		//</resources>
		
		//<items>
		
		Element itemsElement = (Element)(rootElement.getElementsByTagName("items").item(0));

		m_initItems(itemsElement);
		m_initCollections(itemsElement);
		
		//</items>
		
		//<bombers>
		Element bombersElement = (Element)(rootElement.getElementsByTagName("bombers").item(0));
		nl = bombersElement.getElementsByTagName("bomber");
		for (int i = 0; i < nl.getLength(); ++i) {
			
		}
		//</bombers>
		
		//<locations>
		Element locationsElement = (Element)(rootElement.getElementsByTagName("locations").item(0));
		nl = locationsElement.getElementsByTagName("location");
		for (int i = 0; i < nl.getLength(); ++i) {
			
		}
		//</locations>
		
		//<levels>
		Element levelsElement = (Element)(rootElement.getElementsByTagName("levels").item(0));
		nl = levelsElement.getElementsByTagName("level");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element levelElement = (Element)nl.item(i);
			f_levels.add(new LevelDescription(levelElement));
		}		
		//</levels>
		
		//<missions>
		Element missionsElement = (Element)(rootElement.getElementsByTagName("missions").item(0));
		nl = missionsElement.getElementsByTagName("mission");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element missionElement = (Element)nl.item(i);
			Mission mission = new Mission(missionElement);
			f_missions.put(mission.getId(), mission);
		}		
		//</missions>
		
	}
	
	//Private initialization methods 
	
	private void m_initResourcePrices(Element resourcesElement) {
		NodeList nl = null;
		//Gold
		Element goldElement = (Element)(resourcesElement.getElementsByTagName("gold").item(0));
		nl = goldElement.getElementsByTagName("pack");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element packElement = (Element)nl.item(i);
			int price = Integer.parseInt(packElement.getAttribute("price"));
			int count = Integer.parseInt(packElement.getAttribute("count"));
			f_goldCostPacks.add(new Integer[] { count, price });
		}
		//Crystal
		Element crystalElement = (Element)(resourcesElement.getElementsByTagName("crystal").item(0));
		nl = crystalElement.getElementsByTagName("pack");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element packElement = (Element)nl.item(i);
			int price = Integer.parseInt(packElement.getAttribute("price"));
			int count = Integer.parseInt(packElement.getAttribute("count"));
			f_crystalCostPacks.add(new Integer[] { count, price });
		}
		//Adamantium
		Element adamantiumElement = (Element)(resourcesElement.getElementsByTagName("adamantium").item(0));
		nl = adamantiumElement.getElementsByTagName("pack");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element packElement = (Element)nl.item(i);
			int price = Integer.parseInt(packElement.getAttribute("price"));
			int count = Integer.parseInt(packElement.getAttribute("count"));
			f_adamantiumCostPacks.add(new Integer[] { count, price });
		}
		//Antimatter
		Element antimatterElement = ((Element)resourcesElement.getElementsByTagName("antimatter").item(0));
		nl = antimatterElement.getElementsByTagName("pack");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element packElement = (Element)nl.item(i);
			int price = Integer.parseInt(packElement.getAttribute("price"));
			int count = Integer.parseInt(packElement.getAttribute("count"));
			f_antimatterCostPacks.add(new Integer[] { count, price });
		}
		//Energy
		Element energyElement = ((Element)resourcesElement.getElementsByTagName("energy").item(0));
		nl = energyElement.getElementsByTagName("pack");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element packElement = (Element)nl.item(i);
			int price = Integer.parseInt(packElement.getAttribute("price"));
			int count = Integer.parseInt(packElement.getAttribute("count"));
			f_energyPacks.add(new Integer[] { count, price });
		}
		//Discounts
//		Element discountsElement = ((Element)resourcesElement.getElementsByTagName("discounts").item(0));
//		nl = discountsElement.getElementsByTagName("discount");
//		for (int i = 0; i < nl.getLength(); ++i) {
//			Element discountElement = (Element)nl.item(i);
//			int from = Integer.parseInt(discountElement.getAttribute("from"));
//			int value = Integer.parseInt(discountElement.getAttribute("value"));
//			f_discounts.add(new Integer[] { from, value});
//		}		
	}
	
	private void m_initItems(Element itemsElement) {
		NodeList nl = null;
		nl = itemsElement.getElementsByTagName("itemPrice");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element itemElement = (Element)nl.item(i);
			ItemCost itemCost = new ItemCost(itemElement); 
			f_items.put(itemCost.getId(), itemCost);
		}
	}
	
	private void m_initCollections(Element itemsElement) {
		NodeList nl = null;
		nl = itemsElement.getElementsByTagName("collection");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element collectionElement = (Element)nl.item(i);
			int collectionId = Integer.parseInt(collectionElement.getAttribute("id"));
			ArrayList<Integer> parts = new ArrayList<Integer>();
			ArrayList<Integer> counts = new ArrayList<Integer>();
			ArrayList<ArrayList<Integer>> chances = new ArrayList<ArrayList<Integer>>();
			NodeList partsNl = collectionElement.getElementsByTagName("part");
			for (int j = 0; j < partsNl.getLength(); ++ j) {
				Element partElement = (Element)partsNl.item(j);
				int partId = Integer.parseInt(partElement.getAttribute("id"));
				String countString = partElement.getAttribute("count");
				if (countString.isEmpty()) {
					counts.add(1);
				} else {
					counts.add(Integer.parseInt(countString));
				}
				String chancesString = partElement.getAttribute("chance");
				ArrayList<Integer> chancesOnLocations = new ArrayList<Integer>();
				if (chancesString.contains(",")) {
					String[] chancesStrings = chancesString.split(",");
					for (int k = 0; k < chancesStrings.length; ++k) {
						chancesOnLocations.add(Integer.parseInt(chancesStrings[k]));
					}
				} else {
					int chanceOnAllLocations = Integer.parseInt(chancesString);
					for (int k = 0; k < Locations.C_TotalLocatios; ++k) {
						chancesOnLocations.add(chanceOnAllLocations);
					}
				}
				chances.add(chancesOnLocations);
				parts.add(partId);
			}
			ItemColection coll = new ItemColection(collectionId, parts, counts, chances);
			f_collections.put(collectionId, coll);
		}
	}
	
	//Methods
	
	public LevelDescription getLevelFor(PlayerProfile profile) {
		int currMax = -1;
		LevelDescription r = null;
		for (LevelDescription level : f_levels) {
			if (level.getExperience() > currMax && profile.getExperience() >= level.getExperience()) {
				r = level;
				currMax = level.getExperience();
			}
		}
		return r;
	}
	
	public LevelDescription getLevel(int level) {
		for (LevelDescription l : f_levels) {
			if (l.getValue() == level) {
				return l;
			}
		}
		return null;
	}
	
	public void getReward(PlayerProfile profile, Reward reward) {
		if (reward.getExperienceReward() > 0) {
			profile.addExperience(reward.getExperienceReward());
			profile.getSessionStats().experienceEarned += reward.getExperienceReward();
			profile.checkLevelUps(this);
		}
		
		profile.addGold(reward.getGoldReward());
		profile.addCrystal(reward.getCrystalReward());
		profile.addAdamantium(reward.getAdamantiumReward());
		profile.addAntimatter(reward.getAntimatterReward());
		profile.addEnergy(reward.getEnergyReward());
		
		SessionStats s = profile.getSessionStats();
		s.goldEarned += reward.getGoldReward();
		s.crystalEarned += reward.getCrystalReward();
		s.energyEarned += reward.getEnergyReward();
		
		f_dispatcher.getDbManager().ScheduleUpdateQuery(
				DBQueryManager.SqlAddPlayerResources, new Object[] {
				reward.getGoldReward(),
				reward.getCrystalReward(),
				reward.getAdamantiumReward(),
				reward.getAntimatterReward(),
				reward.getEnergyReward(),
				profile.getId()
			});
		if (reward.getItemsReward().size() > 0) {
			for (int itemId : reward.getItemsReward().keySet()) {
				profile.addItems(itemId, reward.getItemsReward().get(itemId));
			}
			String sql = DBQueryManager.SqlUpdatePlayerItems;
			f_dispatcher.getDbManager().ScheduleUpdateQuery(
					sql, new Object[] {profile.getItemsData().toJson(), profile.getId() });			
		}
	}
	
	public void adjustExperience(PlayerProfile placeOne, PlayerProfile placeTwo) {
		LevelDescription levelOne = getLevelFor(placeOne);
		LevelDescription levelTwo = getLevelFor(placeTwo);
		int expOne = levelOne.getEarnsForPlaceOne() + levelOne.getDeltaFor(levelTwo);
		placeOne.addExperience(expOne);
		placeOne.getSessionStats().experienceEarned += expOne;
		int expTwo = levelTwo.getEarnsForPlaceTwo() + levelTwo.getDeltaFor(levelOne);
		placeTwo.addExperience(expTwo);
		placeTwo.getSessionStats().experienceEarned += expTwo;
	}
	
	public void adjustExperience(PlayerProfile placeOne, PlayerProfile placeTwo, PlayerProfile placeThree) {
		LevelDescription levelOne = getLevelFor(placeOne);
		LevelDescription levelTwo = getLevelFor(placeTwo);
		LevelDescription levelThree = getLevelFor(placeThree);
		int expOne = levelOne.getEarnsForPlaceOne() + levelOne.getDeltaFor(levelTwo) + levelOne.getDeltaFor(levelThree);
		placeOne.addExperience(expOne);
		placeOne.getSessionStats().experienceEarned += expOne;
		int expTwo = levelTwo.getEarnsForPlaceTwo() + levelTwo.getDeltaFor(levelOne) + levelTwo.getDeltaFor(levelThree);
		placeTwo.addExperience(expTwo);
		placeTwo.getSessionStats().experienceEarned += expTwo;
		int expThree = levelThree.getEarnsForPlaceThree() + levelThree.getDeltaFor(levelOne) + levelThree.getDeltaFor(levelTwo);
		placeThree.addExperience(expThree);
		placeThree.getSessionStats().experienceEarned += expThree;
	}

	public int getResourcesCost(int gold, int crystal, int adamantium, int antimatter) {
		int cost = 0;
		
		boolean isValidCount = false;
		if (gold != 0) {
			for (Integer[] pack : f_goldCostPacks) {
				if (pack[0] == gold) {
					cost += pack[1];
					isValidCount = true;
				}
			}	
			if (!isValidCount) {
				return Integer.MAX_VALUE;
			}
		}
		if (crystal != 0) {
			isValidCount = false;
			for (Integer[] pack : f_crystalCostPacks) {
				if (pack[0] == crystal) {
					cost += pack[1];
					isValidCount = true;
				}
			}
			if (!isValidCount) {
				return Integer.MAX_VALUE;
			}
		}
		if (adamantium != 0) {
			isValidCount = false;
			for (Integer[] pack : f_adamantiumCostPacks) {
				if (pack[0] == adamantium) {
					cost += pack[1];
					isValidCount = true;
				}
			}	
			if (!isValidCount) {
				return Integer.MAX_VALUE;
			}
		}
		if (antimatter != 0) {
			isValidCount = false;
			for (Integer[] pack : f_antimatterCostPacks) {
				if (pack[0] == antimatter) {
					cost += pack[1];
					isValidCount = true;
				}
			}	
			if (!isValidCount) {
				return Integer.MAX_VALUE;
			}
		}
		
		return cost;
	}
	
	public int getEnergyCost(int energy) {
		for (Integer[] pack : f_energyPacks) {
			if (pack[0] == energy)
				return pack[1];
		}
		return Integer.MAX_VALUE;
	}
	
	public int withdrawResourcesAndBuyItem(int itemId, PlayerProfile profile, int resourceType) {
		ItemCost itemCost = f_items.get(itemId);
		int playerLevel = getLevelFor(profile).getValue();
		if (itemCost == null || profile.itemCount(itemId) + itemCost.getStack() > itemCost.getMaxStack()) {
			return 0;
		}
		itemCost = itemCost.getItemCostInSpecifiedResource(resourceType, profile.itemCount(itemId));
		if (!(profile.getGold() >= itemCost.getGold() &&
			  profile.getCrystal() >= itemCost.getCrystal() &&
			  profile.getAdamantium() >= itemCost.getAdamantium() &&
			  profile.getAntimatter() >= itemCost.getAntimatter() &&
			  playerLevel >= itemCost.getLevel())) {
			return 0;
		}
		
		profile.addGold(-itemCost.getGold());
		profile.addCrystal(-itemCost.getCrystal());
		profile.addAdamantium(-itemCost.getAdamantium());
		profile.addAntimatter(-itemCost.getAntimatter());
		profile.addItems(itemId, itemCost.getStack());
		
		f_dispatcher.getStatisticsManager().writeWeaponBuy(itemId);
		
		SessionStats s = profile.getSessionStats();
		s.goldSpent += itemCost.getGold();
		s.crystalSpent += itemCost.getCrystal();
		
		String sql = DBQueryManager.SqlUpdatePlayerItems;
		f_dispatcher.getDbManager().ScheduleUpdateQuery(
				sql, new Object[] {profile.getItemsData().toJson(), profile.getId() });
		sql  = DBQueryManager.SqlSubtractPlayerResources;
		f_dispatcher.getDbManager().ScheduleUpdateQuery(sql, new Object[] {
				itemCost.getGold(), 
				itemCost.getCrystal(), 
				itemCost.getAdamantium(), 
				itemCost.getAntimatter(), 
				0,
				profile.getId()
			});
		
		return itemCost.getStack();
	}
	
	public boolean collectCollection(int collectionId, PlayerProfile profile) {
		ItemColection coll = f_collections.get(collectionId);
		if (coll == null) {
			return false;
		}
		for (int partId : coll.getParts()) {
			if (!profile.hasItems(partId, coll.getCountForItem(partId))) {
				return false;
			}
		}
		for (int partId : coll.getParts()) {
			profile.addItems(partId, -coll.getCountForItem(partId));
		}
		profile.addItems(collectionId, 1);
		return true;
	}
	
	public Mission getMission(String missionId) { return f_missions.get(missionId); } 
	
	public int getItemStack(int itemId) {
		ItemCost itemCost = f_items.get(itemId);
		if (itemCost == null) return 0;
		return itemCost.getStack();
	}
	
	public SFSObject toSFSObject() {
		if (f_sfsObject == null) {
			f_sfsObject = new SFSObject();
			
			//Resources prices
			
			SFSArray goldPacks = new SFSArray();
			for (Integer[] pack : f_goldCostPacks) {
				SFSObject pk = new SFSObject();
				pk.putInt("Count", pack[0]);
				pk.putInt("Price", pack[1]);
				goldPacks.addSFSObject(pk);
			}
			f_sfsObject.putSFSArray("GoldCost", goldPacks);
			
			SFSArray crystalPacks = new SFSArray();
			for (Integer[] pack : f_crystalCostPacks) {
				SFSObject pk = new SFSObject();
				pk.putInt("Count", pack[0]);
				pk.putInt("Price", pack[1]);
				crystalPacks.addSFSObject(pk);
			}
			f_sfsObject.putSFSArray("CrystalCost", crystalPacks);
			
			SFSArray adamantiumPacks = new SFSArray();
			for (Integer[] pack : f_adamantiumCostPacks) {
				SFSObject pk = new SFSObject();
				pk.putInt("Count", pack[0]);
				pk.putInt("Price", pack[1]);
				adamantiumPacks.addSFSObject(pk);
			}
			f_sfsObject.putSFSArray("AdamantiumCost", adamantiumPacks);
			
			SFSArray antimatterPacks = new SFSArray();
			for (Integer[] pack : f_antimatterCostPacks) {
				SFSObject pk = new SFSObject();
				pk.putInt("Count", pack[0]);
				pk.putInt("Price", pack[1]);
				antimatterPacks.addSFSObject(pk);
			}
			f_sfsObject.putSFSArray("AntimatterCost", antimatterPacks);
			
			SFSArray energyPacks = new SFSArray();
			for (Integer[] pack : f_energyPacks) {
				SFSObject energyPack = new SFSObject();
				energyPack.putInt("Count", pack[0]);
				energyPack.putInt("Price", pack[1]);
				energyPacks.addSFSObject(energyPack);
			}
			f_sfsObject.putSFSArray("EnergyCost", energyPacks);
			
			//Items prices
			
			SFSArray items = new SFSArray();
			for (Entry<Integer, ItemCost> item : f_items.entrySet()) {
				SFSObject itemObject = new SFSObject();
				itemObject.putInt("Id", item.getKey());
				itemObject.putInt("Gold", item.getValue().getGold());
				itemObject.putInt("Crystal", item.getValue().getCrystal());
				itemObject.putInt("Adamantium", item.getValue().getAdamantium());
				itemObject.putInt("Antimatter", item.getValue().getAntimatter());
				itemObject.putInt("GoldDelta", item.getValue().getGoldDelta());
				itemObject.putInt("CrystalDelta", item.getValue().getCrystalDelta());
				itemObject.putInt("Level", item.getValue().getLevel());
				itemObject.putInt("Stack", item.getValue().getStack());
				itemObject.putInt("MaxStack", item.getValue().getMaxStack());
				itemObject.putBool("SpecialOffer", item.getValue().getSpecialOffer());
				items.addSFSObject(itemObject);
			}
			f_sfsObject.putSFSArray("Items", items);
			
			//levels
			
			SFSArray levels = new SFSArray();
			for (LevelDescription level : f_levels) {
				SFSObject levelObject = new SFSObject();
				levelObject.putInt("Level", level.getValue());
				levelObject.putInt("Exp", level.getExperience());
				if (level.getReward() != null) {
					levelObject.putSFSObject("Reward", level.getReward().toSFSObject());
				}
				levels.addSFSObject(levelObject);
			}
			f_sfsObject.putSFSArray("Levels", levels);
			
			//Missions
			
			SFSArray missions = new SFSArray();
			for (Mission m : f_missions.values()) {
				SFSObject sfsM = new SFSObject();
				
				sfsM.putUtfString("Id", m.getId());
				sfsM.putInt("L", m.getLocation());
				sfsM.putInt("E", m.getEnergyCost());
				sfsM.putSFSObject("Bronze", m.getBronzeReward().toSFSObject());
				sfsM.putSFSObject("Silver", m.getSilverReward().toSFSObject());
				sfsM.putSFSObject("Gold", m.getGoldReward().toSFSObject());
				
				missions.addSFSObject(sfsM);
			}
			f_sfsObject.putSFSArray("Missions", missions);
			
		}
		return f_sfsObject;
	}
	
	public Collection<Integer> getUseableItemsIds() { return f_items.keySet(); }
	
	
}
