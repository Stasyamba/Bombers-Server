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

import com.vensella.bombers.game.mapObjects.Locations;

//TODO: Add concurrent locks
public class PricelistManager {

	//Constants
	
	public static final int C_UnknownItem = -1;
	
	//Nested types
	
	private class ItemCost
	{
		//Fields
		
		private int f_gold;
		private int f_crystal;
		private int f_adamantium;
		private int f_antimatter;
		private int f_stack;
		private int f_level;
		private boolean f_specialOffer;
		
		//Constructors
		
		public ItemCost(int gold, int crystal, int adamantium, int antimatter, int stack, int level, boolean specialOffer)
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
		
		public int getGold() { return f_gold; }
		public int getCrystal() { return f_crystal; }
		public int getAdamantium() { return f_adamantium; }
		public int getAntimatter() { return f_antimatter; }
		public int getStack() { return f_stack; }
		public int getLevel() { return f_level; }
		public boolean getSpecialOffer() { return f_specialOffer; }
		
		
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
	
	public class Reward {
		
		//Constructor
		
		protected Reward(Element rewardElement) {
			String resourcesRewardString = rewardElement.getAttribute("resources");
			if (!resourcesRewardString.isEmpty()) {
				String[] r = resourcesRewardString.split(",");
				f_goldReward = Integer.parseInt(r[0]);
				f_crystalReward = Integer.parseInt(r[1]);
				f_adamantiumReward = Integer.parseInt(r[2]);
				f_antimatterReward = Integer.parseInt(r[3]);
				f_energyReward = Integer.parseInt(r[4]);
			}
			String experienceRewardString = rewardElement.getAttribute("exp");
			if (!experienceRewardString.isEmpty()) {
				f_experienceReward = Integer.parseInt(experienceRewardString);
			}
			f_itemsReward = new HashMap<Integer, Integer>();
			String itemsRewardString = rewardElement.getAttribute("items");
			String itemsCountsString = rewardElement.getAttribute("itemsCounts");
			if (!itemsRewardString.isEmpty() && itemsCountsString.isEmpty()) {
				String[] r = itemsRewardString.split(",");
				for (int i = 0; i < r.length; ++i) {
					f_itemsReward.put(Integer.parseInt(r[i]), 1);
				}
			}
			else if (!itemsRewardString.isEmpty() && !itemsCountsString.isEmpty()) {
				String[] r = itemsRewardString.split(",");
				String[] c = itemsCountsString.split(",");
				for (int i = 0; i < r.length; ++i) {
					f_itemsReward.put(Integer.parseInt(r[i]), Integer.parseInt(c[i]));
				}
			}
		}
		
		//Fields
		
		private int f_goldReward;
		private int f_crystalReward;
		private int f_adamantiumReward;
		private int f_antimatterReward;
		private int f_energyReward;
		
		private int f_experienceReward;
		
		private Map<Integer, Integer> f_itemsReward;
		
		//Methods
		
		public int getGoldReward() { return f_goldReward; }
		public int getCrystalReward() { return f_crystalReward; }
		public int getAdamantiumReward() { return f_adamantiumReward; }
		public int getAntimatterReward() { return f_antimatterReward; }
		public int getEnergyReward() { return f_energyReward; }
		
		public int getExperienceReward() { return f_experienceReward; }
		
		public Map<Integer, Integer> getItemsReward() { return f_itemsReward; }
		
		public SFSObject toSFSObject() {
			SFSObject r = new SFSObject();
			r.putInt("R0", f_goldReward);
			r.putInt("R1", f_crystalReward);
			r.putInt("R2", f_adamantiumReward);
			r.putInt("R3", f_antimatterReward);
			r.putInt("R4", f_energyReward);
			r.putInt("Exp", f_experienceReward);
			SFSArray items = new SFSArray();
			for (int item : f_itemsReward.keySet()) {
				SFSObject it = new SFSObject();
				it.putInt("Id", item);
				it.putInt("C", f_itemsReward.get(item));
				items.addSFSObject(it);
			}
			r.putSFSArray("Items", items);
			return r;
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
	
	//Constants
	
	private static final String PricelistPath = "/usr/local/nginx/html/main/bombers/pricelist/pricelist.xml";
	
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
			parsePricelist(PricelistPath);
		}
		catch (Exception ex) {
			f_dispatcher.trace(ExtensionLogLevel.ERROR, "While parsing pricelist at path " + PricelistPath);
			f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.toString());
			f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
		}
	}
	
	//Internal methods
	
	private void parsePricelist(String path) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(path);
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
			int gold = Integer.parseInt(itemElement.getAttribute("gold"));
			int crystal = Integer.parseInt(itemElement.getAttribute("crystal"));
			int adamantium = Integer.parseInt(itemElement.getAttribute("adamantium"));
			int antimatter = Integer.parseInt(itemElement.getAttribute("antimatter"));
			int stack = Integer.parseInt(itemElement.getAttribute("stack"));
			int level = Integer.parseInt(itemElement.getAttribute("level"));
			int itemId = Integer.parseInt(itemElement.getAttribute("itemId"));
			boolean s = itemElement.getAttribute("itemId") == "true";
			f_items.put(itemId, new ItemCost(gold, crystal, adamantium, antimatter, stack, level, s));
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
			profile.checkLevelUps(this);
		}
		
		profile.addGold(reward.getGoldReward());
		profile.addCrystal(reward.getCrystalReward());
		profile.addAdamantium(reward.getAdamantiumReward());
		profile.addAntimatter(reward.getAntimatterReward());
		profile.addEnergy(reward.getEnergyReward());
		f_dispatcher.getDbManager().ScheduleUpdateQuery(
				DBQueryManager.SqlAddPlayerResources, new Object[] {
				reward.getGoldReward(),
				reward.getCrystalReward(),
				reward.getAdamantiumReward(),
				reward.getAntimatterReward(),
				reward.getEnergyReward(),
				profile.getId()
			});
		
		for (int itemId : reward.getItemsReward().keySet()) {
			profile.addItems(itemId, reward.getItemsReward().get(itemId));
		}
	}
	
	public void adjustExperience(PlayerProfile placeOne, PlayerProfile placeTwo) {
		LevelDescription levelOne = getLevelFor(placeOne);
		LevelDescription levelTwo = getLevelFor(placeTwo);
		int expOne = levelOne.getEarnsForPlaceOne() + levelOne.getDeltaFor(levelTwo);
		placeOne.addExperience(expOne);
		int expTwo = levelTwo.getEarnsForPlaceTwo() + levelTwo.getDeltaFor(levelOne);
		placeTwo.addExperience(expTwo);
	}
	
	public void adjustExperience(PlayerProfile placeOne, PlayerProfile placeTwo, PlayerProfile placeThree) {
		LevelDescription levelOne = getLevelFor(placeOne);
		LevelDescription levelTwo = getLevelFor(placeTwo);
		LevelDescription levelThree = getLevelFor(placeThree);
		int expOne = levelOne.getEarnsForPlaceOne() + levelOne.getDeltaFor(levelTwo) + levelOne.getDeltaFor(levelThree);
		placeOne.addExperience(expOne);
		int expTwo = levelTwo.getEarnsForPlaceTwo() + levelTwo.getDeltaFor(levelOne) + levelTwo.getDeltaFor(levelThree);
		placeTwo.addExperience(expTwo);
		int expThree = levelThree.getEarnsForPlaceThree() + levelThree.getDeltaFor(levelOne) + levelThree.getDeltaFor(levelTwo);
		placeThree.addExperience(expThree);
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
	
	public boolean canBuyItem(int itemId, PlayerProfile profile) {
		ItemCost itemCost = f_items.get(itemId);
		if (itemCost == null) return false;
		return (profile.getGold() >= itemCost.getGold() &&
				profile.getCrystal() >= itemCost.getCrystal() &&
				profile.getAdamantium() >= itemCost.getAdamantium() &&
				profile.getAntimatter() >= itemCost.getAntimatter() &&
				getLevelFor(profile).getValue() >= itemCost.getLevel()
			);
	}
	
	public int withdrawResourcesAndBuyItem(int itemId, PlayerProfile profile) {
		ItemCost itemCost = f_items.get(itemId);
		if (itemCost == null) return 0;
		profile.addGold(-itemCost.getGold());
		profile.addCrystal(-itemCost.getCrystal());
		profile.addAdamantium(-itemCost.getAdamantium());
		profile.addAntimatter(-itemCost.getAntimatter());
		profile.addItems(itemId, itemCost.getStack());
		
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
				itemObject.putInt("Level", item.getValue().getLevel());
				itemObject.putInt("Stack", item.getValue().getStack());
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
	
	
	
	
}
