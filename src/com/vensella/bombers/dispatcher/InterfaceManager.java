package com.vensella.bombers.dispatcher;

import java.util.ArrayList;
import java.util.Collection;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

import com.vensella.bombers.dispatcher.PricelistManager.Mission;
import com.vensella.bombers.dispatcher.StatisticsManager.SessionStats;

public class InterfaceManager {
	
	//Constants
	
	public static final int C_GiveExperienceTrainingStatus = 3;
	public static final int C_GiveExperienceOnTraining = 30;
	public static final int C_FinalTrainingStatus = 5;
	public static final int C_EndTutorial = -1;
	
	public static final int C_DefaultMissionToken = 0;
	
	public static final int C_BronzeMedal = 1;
	public static final int C_SilverMedal = 2;
	public static final int C_GoldMedal = 4;
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	
	//Constructors
	
	public InterfaceManager(BombersDispatcher dispatcher)
	{
		f_dispatcher = dispatcher;
	}
	
	//Common methods
	
	public void setCustomParameter(User user, int key, int value) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		profile.setCustomParameter(key, value);
	}
	
	public void setCustomParameter(User user, int key, String value) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		profile.setCustomParameter(key, value);
	}
	
	//Methods for training
	
	public void setTrainingStatus(User user, int status) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		if (profile.getTrainingStatus() == C_EndTutorial) {
			return;
		}
		if (status == C_EndTutorial || (profile.getTrainingStatus() + 1 == status && status <= C_FinalTrainingStatus)) {
			profile.setTrainingStatus(status);
			SFSObject params = new SFSObject();
			params.putBool("interface.setTrainingStatus.result.f.status", true);
			params.putInt("interface.setTrainingStatus.result.f.trainingStatus", status);
			if (status == C_GiveExperienceTrainingStatus) {
				profile.addExperience(C_GiveExperienceOnTraining);
				profile.checkLevelUps(f_dispatcher.getPricelistManager());
				params.putInt("interface.setTrainingStatus.result.f.youNewExperience", profile.getExperience());
			}
			f_dispatcher.send("interface.setTrainingStatus.result", params, user);
		}
	}
	
	//Methods for shopping
	
	public void dropItem(User user, int itemId) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		profile.removeItem(itemId);
	}
	
	public void buyItem(User user, int itemId, int resourceType)
	{
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		int stack = f_dispatcher.getPricelistManager().withdrawResourcesAndBuyItem(itemId, profile, resourceType);
		if (stack > 0) {
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
		if (rc4 != 0) {
			if (rc0 != 0 || rc1 != 0 || rc2 != 0 || rc3 != 0) return;
			totalCost = f_dispatcher.getPricelistManager().getEnergyCost(rc4);
		}
		
		f_dispatcher.trace(
				ExtensionLogLevel.WARN, 
				"User " + user.getName() + " trying to buy resources for " + totalCost + " votes"
			);
		
		final int totalCostFinal = totalCost;
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
						profile.addVotes(totalCostFinal);
						
						SessionStats s = profile.getSessionStats();
						s.goldBuyed += rc0;
						s.crystalBuyed += rc1;
						s.energyBuyed += rc4;
						s.votesSpent += totalCostFinal;
						
						String sql = DBQueryManager.SqlAddPlayerResources;
						f_dispatcher.getDbManager().ScheduleUpdateQuery(sql, new Object[] {
								rc0,
								rc1,
								rc2,
								rc3,
								rc4,
								profile.getId()
							});
						
						SFSObject params = new SFSObject();
						params.putBool("interface.buyResources.result.fields.status", true);
						params.putInt("interface.buyResources.result.fields.resourceType0", profile.getGold());
						params.putInt("interface.buyResources.result.fields.resourceType1", profile.getCrystal());
						params.putInt("interface.buyResources.result.fields.resourceType2", profile.getAdamantium());
						params.putInt("interface.buyResources.result.fields.resourceType3", profile.getAntimatter());
						params.putInt("interface.buyResources.result.fields.resourceType4", profile.getEnergy());
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
	
	public void collectCollection(User user, int collectionId) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		SFSObject params = new SFSObject();
		if (f_dispatcher.getPricelistManager().collectCollection(collectionId, profile)) {
			params.putInt("interface.collectCollection.result.f.collectionId", collectionId);
			params.putBool("interface.collectCollection.result.f.status", true);
		} else {
			params.putBool("interface.collectCollection.result.f.status", false);
		}
		f_dispatcher.send("interface.collectCollection.result", params, user);
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
		if (nick.length() > 14 || nick.length() < 2) {
			SFSObject params = new SFSObject();
			params.putBool("interface.setNick.result.fields.status", false);
			f_dispatcher.send("interface.setNick.result", params, user);
		} else {
			f_dispatcher.getUserProfile(user).setNick(nick);
			
			SFSObject params = new SFSObject();
			params.putBool("interface.setNick.result.fields.status", true);
			f_dispatcher.send("interface.setNick.result", params, user);
		}
	}
	
	public void getUsersInfo(final User user, Collection<String> ids) {
		if (ids.size() < 12) {
			while (ids.size() < 12) {
				ids.add("0");
			}
		}
		else if (ids.size() > 12) {
			Collection<String> newIds = new ArrayList<String>();
			int i = 0;
			for (String id : ids) {
				if (i == 12) {
					break;
				}
				newIds.add(id);
				i++;
			}
			ids = newIds;
		}
		ArrayList<String> c = new ArrayList<String>(ids);
		DBQueryManager manager = f_dispatcher.getDbManager();
		manager.ScheduleQuery(
				DBQueryManager.SqlSelectUsersInfo, 
				new Object[] {
					c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5), 
					c.get(6), c.get(7), c.get(8), c.get(9), c.get(10), c.get(11)
				},
				manager.new QueryCallback() {
					@Override
					public void run(ISFSArray result) {
						SFSObject params = new SFSObject();
						params.putSFSArray("interface.getUsersInfo.result.f.infos", result);
						f_dispatcher.send("interface.getUsersInfo.result", params, user);
					}
				}
			);
	}
	
	//Methods for single games
	
	public void startMission(User user, String missionId) {
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		if (profile.getMissionToken() != C_DefaultMissionToken) {
			return;
		}
		Mission mission = f_dispatcher.getPricelistManager().getMission(missionId);
		SFSObject params = new SFSObject();
		if (profile.getEnergy() < mission.getEnergyCost()) {
			params.putBool("interface.missions.start.result.f.status", false);
		}
		else {
			profile.addEnergy(-mission.getEnergyCost());
			profile.setMissionStartTime(System.currentTimeMillis());
			profile.setMissionToken((int)(1000000 * Math.random()));
			
			profile.getSessionStats().energySpent += mission.getEnergyCost();
			profile.getSessionStats().singleGamesPlayed += 1;
		
			//f_dispatcher
			//	.trace("User " + user.getName() + " starting mission "+ missionId + ", token = " + profile.getMissionToken());
			
			params.putBool("interface.missions.start.result.f.status", true);
			params.putUtfString("interface.missions.start.result.f.missionId", missionId);
			params.putInt("interface.missions.start.result.f.token", profile.getMissionToken());
			params.putInt("interface.missions.start.result.f.youNewEnergy", profile.getEnergy());
		}
		f_dispatcher.send("interface.missions.start.result", params, user); 
	}
	
	public void submitMissionResult(
			User user, 
			int token, 
			String missionId, 
			boolean isBronze, 
			boolean isSilver, 
			boolean isGold,
			int missionTime) 
	{
		PlayerProfile profile = f_dispatcher.getUserProfile(user);
		
		//f_dispatcher
		//	.trace("User " + user.getName() + " end mission "+ missionId + ", token = " + profile.getMissionToken());
		
		if (!isBronze && !isSilver && !isGold) {
			profile.setMissionToken(C_DefaultMissionToken);
			
			SFSObject params = new SFSObject();
			params.putBool("interface.missions.submitResult.result.f.status", true);
			f_dispatcher.send("interface.missions.submitResult.result", params, user); 
		} else {
			if (token != profile.getMissionToken() || System.currentTimeMillis() < profile.getMissionStartTime() + 20000) {
				return;
			}
			Mission mission = f_dispatcher.getPricelistManager().getMission(missionId);
			if (mission == null) {
				return;
			}
			profile.setMissionToken(C_DefaultMissionToken);
			boolean medalTaken = false;
			
			SFSObject params = new SFSObject();
			params.putBool("interface.missions.submitResult.result.f.status", true);	
			
			int medalType = 0;
			if (isBronze && !profile.hasMedal(missionId, C_BronzeMedal)) {
				medalType = C_BronzeMedal;
				profile.setMedal(missionId, C_BronzeMedal);
				f_dispatcher.getPricelistManager().getReward(profile, mission.getBronzeReward());
				medalTaken = true;
				
				//f_dispatcher.trace("Bronze medal");
				
				params.putSFSObject("interface.missions.submitResult.result.f.bronze", mission.getBronzeReward().toSFSObject());
			} else {
				//params.putSFSObject("interface.missions.submitResult.result.f.bronze", SFSObject.newInstance());
			}
			if (isSilver && !profile.hasMedal(missionId, C_SilverMedal)) {
				medalType = C_SilverMedal;
				profile.setMedal(missionId, C_SilverMedal);
				f_dispatcher.getPricelistManager().getReward(profile, mission.getSilverReward());
				medalTaken = true;
				
				//f_dispatcher.trace("Silver medal");
				
				params.putSFSObject("interface.missions.submitResult.result.f.silver", mission.getSilverReward().toSFSObject());
			} else {
				//params.putSFSObject("interface.missions.submitResult.result.f.silver", SFSObject.newInstance());
			}
			if (isGold && !profile.hasMedal(missionId, C_GoldMedal)) {
				medalType = C_GoldMedal;
				profile.setMedal(missionId, C_GoldMedal);
				f_dispatcher.getPricelistManager().getReward(profile, mission.getGoldReward());
				medalTaken = true;
				
				//f_dispatcher.trace("Gold medal");
				
				params.putSFSObject("interface.missions.submitResult.result.f.gold", mission.getGoldReward().toSFSObject());
			} else {
				//params.putSFSObject("interface.missions.submitResult.result.f.gold", SFSObject.newInstance());
			}
			
			boolean timeImproved = profile.updateMissionTime(missionId, missionTime);
			
			if (isGold) {
				medalType = C_GoldMedal;
			} else if (isSilver) {
				medalType = C_SilverMedal;
			} else if (isBronze) {
				medalType = C_BronzeMedal;
			}
			
			if (medalTaken || timeImproved) {
				f_dispatcher.getRecordsManager().sumbitMissionResult(profile, missionId, missionTime, medalType);
				f_dispatcher.getDbManager().ScheduleUpdateQuery(
						DBQueryManager.SqlUpdatePlayerMedals, new Object[] { 
						profile.getMedalsData().toJson(), 
						profile.getId() 
					});				
			}
			
			f_dispatcher.send("interface.missions.submitResult.result", params, user); 
		}
	}
	
	
}
