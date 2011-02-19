package com.vensella.bombers.dispatcher;

import java.util.HashMap;
import java.util.Map;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;

public class InterfaceManager {

	//Nested types
	
	private class ItemCost
	{
		//Fields
		
		private int f_gold;
		private int f_crystal;
		private int f_adamantium;
		private int f_antimatter;
		private int f_stack;
		
		//Constructors
		
		public ItemCost(int gold, int crystal, int adamantium, int antimatter, int stack)
		{
			f_gold = gold;
			f_crystal = crystal;
			f_adamantium = adamantium;
			f_antimatter = antimatter;		
			f_stack = stack;
		}
		
		//Methods
		
		public int getGold() { return f_gold; }
		public int getCrystal() { return f_crystal; }
		public int getAdamantium() { return f_adamantium; }
		public int getAntimatter() { return f_antimatter; }
		public int getStack() { return f_stack; }
		
	}
	
	//Constants
	
//	private static int C_ResourceTypeGold = 0;
//	private static int C_ResourceTypeCrystal = 1;
//	private static int C_ResourceTypeAdamantium = 2;
//	private static int C_ResourceTypeAntimatter = 3;
//	private static int C_ResourceTypeEnergy = 4;
	
	//TODO: Add costs of resources
	private static int C_GoldCost = 100;
	private static int C_CrystalCost = 200;
	private static int C_AdamantiumCost = 300;
	private static int C_AntimatterCost = 400;
	private static int C_EnergyCostCost = 100;
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	private Map<Integer, ItemCost> f_priceList;
	
	//Constructors
	
	public InterfaceManager(BombersDispatcher dispatcher)
	{
		f_dispatcher = dispatcher;
		
		//Price initialize
		//TODO: Load prices from file/db
		f_priceList = new HashMap<Integer, InterfaceManager.ItemCost>();
		f_priceList.put(1, new ItemCost(10, 2, 0, 0, 5));
		f_priceList.put(2, new ItemCost(5, 3, 0, 0, 5));
		f_priceList.put(3, new ItemCost(2, 0, 0, 0, 10));
		
		f_priceList.put(21, new ItemCost(3, 1, 0, 0, 3));
		f_priceList.put(41, new ItemCost(3, 0, 0, 0, 10));
		f_priceList.put(61, new ItemCost(0, 5, 1, 0, 1));
		f_priceList.put(1001, new ItemCost(1, 1, 1, 1, 1));
		
	}
	
	//Methods for shopping
	
	public void buyItem(User user, int itemId)
	{
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		ItemCost cost = f_priceList.get(itemId);
		if (cost != null &&
			profile.getGold() >= cost.getGold() &&
			profile.getCrystal() >= cost.getCrystal() &&
			profile.getAdamantium() >= cost.getAdamantium() &&
			profile.getAntimatter() >= cost.getAntimatter())
		{
			profile.addGold(cost.getGold());
			profile.addCrystal(-cost.getCrystal());
			profile.addAdamantium(-cost.getAdamantium());
			profile.addAntimatter(-cost.getAntimatter());
			profile.addItems(itemId, cost.getStack());
			
			String sql = "update `WeaponsOpen` set `WeaponsOpen` = ? where `UserId` = ?";
			f_dispatcher.getDbManager().ScheduleUpdateQuery(
					null, sql, new Object[] {profile.getItemsData().toJson(), profile.getId() });
			sql  = String.format(
					"update `Users` set `%1$s` = `%1$s` - %2$s, `%3$s` = `%3$s` - %4$s, `%5$s` = `%5$s` - %6$s, " +
					"`%7$s` = `%7$s` - %8$s where `%9$s` = ?", 
					PlayerProfile.C_Gold, cost.getGold(), 
					PlayerProfile.C_Crystal, cost.getCrystal(), 
					PlayerProfile.C_Adamantium, cost.getAdamantium(), 
					PlayerProfile.C_Antimatter, cost.getAntimatter(), 
					PlayerProfile.C_Id);
			f_dispatcher.getDbManager().ScheduleUpdateQuery(null, sql, new Object[] {profile.getId()});
			
			SFSObject params = new SFSObject();
			params.putBool("interface.buyItem.result.fields.status", true);
			params.putInt("interface.buyItem.result.fields.itemCostResourceType0", cost.getGold());
			params.putInt("interface.buyItem.result.fields.itemCostResourceType1", cost.getCrystal());
			params.putInt("interface.buyItem.result.fields.itemCostResourceType2", cost.getAdamantium());
			params.putInt("interface.buyItem.result.fields.itemCostResourceType3", cost.getAntimatter());
			params.putInt("interface.buyItem.result.fields.itemId", itemId);
			params.putInt("interface.buyItem.result.fields.count", cost.getStack());
			f_dispatcher.send("interface.buyItem.result", params, user);
		}
		else
		{
			SFSObject params = new SFSObject();
			params.putBool("interface.buyItem.result.fields.status", false);
			params.putInt("interface.buyItem.result.fields.itemId", itemId);
			f_dispatcher.send("interface.buyItem.result", params, user);	
		}
	}
	
	public void buyResources(final User user, final int rc0, final int rc1, final int rc2, final int rc3, final int rc4)
	{
		final PlayerProfile profile = f_dispatcher.getUserProfile(user);
		int totalCost = rc0 * C_GoldCost + rc1 * C_CrystalCost + rc2 + C_AdamantiumCost 
			+ rc3 * C_AntimatterCost + rc4 * C_EnergyCostCost;
		
		f_dispatcher.getMoneyManager().beginTransactVotes(profile, totalCost, 
				new Runnable() {
					@Override
					public void run() {
						//SUCCESS
						
						profile.addGold(rc0);
						profile.addCrystal(rc1);
						profile.addAdamantium(rc2);
						profile.addAntimatter(rc3);
						profile.addEnergy(rc4);
						
						String sql  = String.format(
								"update `Users` set `%1$s` = `%1$s` + %2$s, `%3$s` = `%3$s` + %4$s, `%5$s` = `%5$s` + %6$s, " +
								"`%7$s` = `%7$s` + %8$s, `%9$s` = `%9$s` + %10$s where `%11$s` = ?", 
								PlayerProfile.C_Gold, rc0, PlayerProfile.C_Crystal, rc1, PlayerProfile.C_Adamantium, rc2, 
								PlayerProfile.C_Antimatter, rc3, PlayerProfile.C_Energy, rc4,
								PlayerProfile.C_Id);
						f_dispatcher.getDbManager().ScheduleUpdateQuery(null, sql, new Object[] {profile.getId()});
						//f_dispatcher.getParentZone().getDBManager().executeUpdate(sql);
						
						SFSObject params = new SFSObject();
						params.putBool("interface.buyResources.result.fields.status", true);
						params.putInt("interface.buyResources.result.fields.resourceType0", rc0);
						params.putInt("interface.buyResources.result.fields.resourceType1", rc1);
						params.putInt("interface.buyResources.result.fields.resourceType2", rc2);
						params.putInt("interface.buyResources.result.fields.resourceType3", rc3);
						params.putInt("interface.buyResources.result.fields.resourceType4", rc4);
						f_dispatcher.send("interface.buyResources.result", params, user);
					}
				}, 
				new Runnable() {
					
					@Override
					public void run() {
						SFSObject params = new SFSObject();
						params.putBool("interface.buyResources.result.fields.status", false);
						f_dispatcher.send("interface.buyResources.result", params, user);
					}
				});
	}
	
	//Methods for private info
	
	public void setBomberId(User user, int bomberId) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		if (profile.isBomberOpened(bomberId)) {
			profile.setCurrentBomberId(bomberId);
			
			SFSObject params = new SFSObject();
			params.putBool("interface.setBomber.result.fields.status", true);
			f_dispatcher.send("interface.setBomber.result", params, user);
		} else {
			SFSObject params = new SFSObject();
			params.putBool("interface.setBomber.result.fields.status", false);
			f_dispatcher.send("interface.setBomber.result", params, user);			
		}
	}
	
	public void setPhotoUrl(User user, String photoUrl) {
		f_dispatcher.getUserProfile(user).setPhoto(photoUrl);
	}
	
	public void setNick(User user, String nick) {
		if (nick.length() > 10 || nick.length() < 2) {
			SFSObject params = new SFSObject();
			params.putBool("interface.setNick.result.fields.status", true);
			f_dispatcher.send("interface.setNick.result", params, user);
		} else {
			f_dispatcher.getUserProfile(user).setNick(nick);
			
			SFSObject params = new SFSObject();
			params.putBool("interface.setNick.result.fields.status", true);
			f_dispatcher.send("interface.setNick.result", params, user);
		}
	}
	
	
	
	
	
}
