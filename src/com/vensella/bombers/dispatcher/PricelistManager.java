package com.vensella.bombers.dispatcher;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

//TODO: Add concurrent locks
public class PricelistManager {

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
	
	//Constants
	
	private static final String PricelistPath = "/usr/local/nginx/html/main/bombers/pricelist/pricelist.xml";
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	
	private SFSObject f_sfsObject;
	
	private int f_goldCost;
	private int f_crystalCost;
	private int f_adamantiumCost;
	private int f_antimatterCost;
	
	private ArrayList<Integer[]> f_energyPacks;
	
	private ArrayList<Integer[]> f_discounts;
	
	private Map<Integer, ItemCost> f_items;
	
	private ArrayList<Integer> f_levels;
	
	
	//Constructor
	
	public PricelistManager(BombersDispatcher dispatcher) {
		f_dispatcher = dispatcher;
		f_energyPacks = new ArrayList<Integer[]>();
		f_discounts = new ArrayList<Integer[]>();
		f_items = new HashMap<Integer, ItemCost>();
		f_levels = new ArrayList<Integer>();
		try {
			parsePricelist(PricelistPath);
		}
		catch (Exception ex) {
			f_dispatcher.trace(ex.toString());
			f_dispatcher.trace((Object[])ex.getStackTrace());
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
		//Gold
		Element goldElement = (Element)(resourcesElement.getElementsByTagName("gold").item(0));
		f_goldCost = Integer.parseInt(goldElement.getAttribute("price"));
		//Crystal
		Element crystalElement = (Element)(resourcesElement.getElementsByTagName("crystal").item(0));
		f_crystalCost = Integer.parseInt(crystalElement.getAttribute("price"));
		//Adamantium
		Element adamantiumElement = (Element)(resourcesElement.getElementsByTagName("adamantium").item(0));
		f_adamantiumCost = Integer.parseInt(adamantiumElement.getAttribute("price"));
		//Antimatter
		Element antimatterElement = ((Element)resourcesElement.getElementsByTagName("antimatter").item(0));
		f_antimatterCost = Integer.parseInt(antimatterElement.getAttribute("price"));
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
		Element discountsElement = ((Element)resourcesElement.getElementsByTagName("discounts").item(0));
		nl = discountsElement.getElementsByTagName("discount");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element discountElement = (Element)nl.item(i);
			int from = Integer.parseInt(discountElement.getAttribute("from"));
			int value = Integer.parseInt(discountElement.getAttribute("value"));
			f_discounts.add(new Integer[] { from, value});
		}		
		//</resources>
		
		//<items>
		Element itemsElement = (Element)(rootElement.getElementsByTagName("items").item(0));
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
		nl = levelsElement.getElementsByTagName("levels");
		for (int i = 0; i < nl.getLength(); ++i) {
			Element levelElement = (Element)nl.item(i);
			f_levels.add(Integer.parseInt(levelElement.getAttribute("exp")));
		}		
		PlayerProfile.LevelTable = f_levels;
		//</levels>
	}
	
	//Methods
	
	public int getResourcesCost(int gold, int crystal, int adamantium, int antimatter) {
		int baseCost = f_goldCost * gold + f_crystalCost * crystal + f_adamantiumCost * adamantium + 
			f_antimatterCost * antimatter;
		int maxValue = Integer.MIN_VALUE;
		int discount = 0;
		for (Integer[] dis : f_discounts) {
			if (dis[0] <=  baseCost && dis[0] > maxValue) {
				maxValue = dis[0];
				discount = dis[1];
			}
		}
		return (int)(((100 - discount) / 100.0) * baseCost);
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
				profile.getLevel() >= itemCost.getLevel()
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
		
		String sql = "update `WeaponsOpen` set `WeaponsOpen` = ? where `UserId` = ?";
		f_dispatcher.getDbManager().ScheduleUpdateQuery(
				null, sql, new Object[] {profile.getItemsData().toJson(), profile.getId() });
		sql  = String.format(
				"update `Users` set `%1$s` = `%1$s` - %2$s, `%3$s` = `%3$s` - %4$s, `%5$s` = `%5$s` - %6$s, " +
				"`%7$s` = `%7$s` - %8$s where `%9$s` = ?", 
				PlayerProfile.C_Gold, itemCost.getGold(), 
				PlayerProfile.C_Crystal, itemCost.getCrystal(), 
				PlayerProfile.C_Adamantium, itemCost.getAdamantium(), 
				PlayerProfile.C_Antimatter, itemCost.getAntimatter(), 
				PlayerProfile.C_Id);
		f_dispatcher.getDbManager().ScheduleUpdateQuery(null, sql, new Object[] {profile.getId()});
		
		return itemCost.getStack();
	}
	
	public int getItemStack(int itemId) {
		ItemCost itemCost = f_items.get(itemId);
		if (itemCost == null) return 0;
		return itemCost.getStack();
	}
	
	public SFSObject toSFSObject() {
		if (f_sfsObject == null) {
			f_sfsObject = new SFSObject();
			
			f_sfsObject.putInt("GoldCost", f_goldCost);
			f_sfsObject.putInt("CrystalCost", f_crystalCost);
			f_sfsObject.putInt("AdamantiumCost", f_adamantiumCost);
			f_sfsObject.putInt("AntimatterCost", f_antimatterCost);
			
			SFSArray energyPacks = new SFSArray();
			for (Integer[] pack : f_energyPacks) {
				SFSObject energyPack = new SFSObject();
				energyPack.putInt("Count", pack[0]);
				energyPack.putInt("Price", pack[1]);
				energyPacks.addSFSObject(energyPack);
			}
			f_sfsObject.putSFSArray("EnergyCost", energyPacks);
			
			SFSArray discounts = new SFSArray();
			for (Integer[] discount : f_discounts) {
				SFSObject discountObject = new SFSObject();
				discountObject.putInt("From", discount[0]);
				discountObject.putInt("Value", discount[1]);
				discounts.addSFSObject(discountObject);
			}
			f_sfsObject.putSFSArray("Discounts", discounts);
			
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
			
			SFSArray levels = new SFSArray();
			for (Integer exp : f_levels) {
				levels.addInt(exp);
			}
			f_sfsObject.putSFSArray("Levels", items);
		}
		return f_sfsObject;
	}
	
	
	
	
}
