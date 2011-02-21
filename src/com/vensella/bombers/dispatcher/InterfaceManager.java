package com.vensella.bombers.dispatcher;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;

public class InterfaceManager {
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	
	//Constructors
	
	public InterfaceManager(BombersDispatcher dispatcher)
	{
		f_dispatcher = dispatcher;
	}
	
	//Methods for shopping
	
	public void dropItem(User user, int itemId) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		profile.removeItem(itemId);
		String sql = "update `WeaponsOpen` set `WeaponsOpen` = ? where `UserId` = ?";
		f_dispatcher.getDbManager().ScheduleUpdateQuery(null, sql, new Object[]{ 
				profile.getItemsData().toJson(),
				profile.getId()
		});
	}
	
	public void buyItem(User user, int itemId)
	{
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		if (f_dispatcher.getPricelistManager().canBuyItem(itemId, profile)) {
			int stack = f_dispatcher.getPricelistManager().withdrawResourcesAndBuyItem(itemId, profile);
			
			SFSObject params = new SFSObject();
			params.putBool("interface.buyItem.result.fields.status", true);
			params.putInt("interface.buyItem.result.fields.resourceType0", profile.getGold());
			params.putInt("interface.buyItem.result.fields.resourceType1", profile.getCrystal());
			params.putInt("interface.buyItem.result.fields.resourceType2", profile.getAdamantium());
			params.putInt("interface.buyItem.result.fields.resourceType3", profile.getAntimatter());
			params.putInt("interface.buyItem.result.fields.itemId", itemId);
			params.putInt("interface.buyItem.result.fields.count", stack);
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
		int totalCost = f_dispatcher.getPricelistManager().getResourcesCost(rc0, rc1, rc2, rc3);
		f_dispatcher.trace("User " + user.getName() + " trying to buy resources for " + totalCost + " votes");
		
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
