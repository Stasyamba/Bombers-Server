package com.vensella.bombers.dispatcher;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.game.Bombers;
import com.vensella.bombers.game.mapObjects.Locations;

public class PlayerProfile {

	//Constants
	
	public static final int C_EnergyPeriod = 10 * 60;
	public static final int C_MaximumFreeEnergy = 25;
	
	public static final int C_BetaMaximunExperience = 1000;
	
	public static final String C_Id = "Id";
	public static final String C_Nick = "Nick";
	public static final String C_Photo = "Photo";
	public static final String C_Experience = "Experience";
	public static final String C_Energy = "Energy";
	
	public static final String C_LastLogin = "LastLogin";
	public static final String C_LuckCount = "LuckCount";
	
	public static final String C_TrainingStatus = "TrainingStatus";
	
	public static final String C_CurrentBomberId = "BomberId";
	public static final String C_RightHandItem = "RightHand";
	public static final String C_AuraOne = "AuraOne";
	public static final String C_AuraTwo = "AuraTwo";
	public static final String C_AuraThree = "AuraThree";
	
	public static final String C_Votes = "Votes";
	public static final String C_Gold = "Gold";
	public static final String C_Crystal = "Crystal";
	public static final String C_Adamantium = "Adamantium";
	public static final String C_Antimatter = "Antimatter";
	
	public static final String C_GoldPrize = "GoldPrize";
	public static final String C_CrystalPrize = "CrystalPrize";
	public static final String C_AdamantiumPrize = "AdamantiumPrize";
	public static final String C_AntimatterPrize = "AntimatterPrize";
	
	public static final String C_UserId = "UserId";
	public static final String C_LocationId = "LocationId";
	public static final String C_WeaponId = "WeaponId";
	public static final String C_Count = "Count";
	public static final String C_BomberId = "BomberId";
	
	public static final String C_FldLocationsOpen = "LocationsOpen";
	public static final String C_FldBombersOpen = "BombersOpen";
	public static final String C_FldWeaponsOpen = "WeaponsOpen";
	public static final String C_FldMedals = "Medals";
	
	public static final String C_SmallCount = "C";
	
	//Static fields
	
	public static ArrayList<Integer> LevelTable;
	
	//Constructors
	
	public PlayerProfile(String userId)
	{
		f_id = userId;
		f_nick = "Игрок";
		f_photo = "";
		
		f_energy = C_MaximumFreeEnergy;
		
		f_locations = new ConcurrentHashMap<Integer, Object>();
		f_items = new ConcurrentHashMap<Integer, Integer>();
		f_bombers = new ConcurrentHashMap<Integer, Object>();
		
		f_medals = new ConcurrentHashMap<String, Integer>();
	}
	
	public PlayerProfile(ISFSArray profile, ISFSArray locations, ISFSArray items, ISFSArray bombers, ISFSArray medals)
	{
		ISFSObject p = profile.getSFSObject(0);
		f_id = p.getUtfString(C_Id);
		f_nick = p.getUtfString(C_Nick);
		f_photo = p.getUtfString(C_Photo);
		f_experience = p.getInt(C_Experience);
		
		f_currentBomberId = p.getInt(C_CurrentBomberId);
		f_rightHandItem = p.getInt(C_RightHandItem);
		f_auraOne = p.getInt(C_AuraOne);
		f_auraTwo = p.getInt(C_AuraTwo);
		f_auraThree = p.getInt(C_AuraThree);
		
		f_energy = p.getInt(C_Energy);
		f_gold = p.getInt(C_Gold);
		f_crystal = p.getInt(C_Crystal);
		f_adamantium = p.getInt(C_Adamantium);
		f_antimatter = p.getInt(C_Antimatter);
		f_votes = p.getInt(C_Votes);
		
		f_goldPrize = p.getInt(C_GoldPrize);
		f_crystalPrize = p.getInt(C_CrystalPrize);
		f_adamantiumPrize = p.getInt(C_AdamantiumPrize);
		f_antimatterPrize = p.getInt(C_AntimatterPrize);
		
		f_luckCount = p.getInt(C_LuckCount);
		f_lastLogin = p.getLong(C_LastLogin);
		
		f_trainingStatus = p.getInt(C_TrainingStatus);
		
		f_locations = new ConcurrentHashMap<Integer, Object>();
		for (int i = 0; i < locations.size(); ++i)
		{
			f_locations.put(locations.getInt(i), new Object());
		}
		
		f_items = new ConcurrentHashMap<Integer, Integer>();
		for (int i = 0; i < items.size(); ++i)
		{
			ISFSObject row = items.getSFSObject(i);
			f_items.put(row.getInt(C_Id), row.getInt(C_SmallCount));
		}
		
		f_bombers = new ConcurrentHashMap<Integer, Object>();
		for (int i = 0; i < bombers.size(); ++i)
		{
			f_bombers.put(bombers.getInt(i), new Object());
		}
		
		f_medals = new ConcurrentHashMap<String, Integer>();
		for (int i = 0; i < medals.size(); ++i)
		{
			f_medals.put(medals.getSFSArray(i).getUtfString(0), medals.getSFSArray(i).getInt(1));
		}
	}
	
	//Fields
	
	private String f_id;
	private String f_nick;
	private String f_photo;
	private int f_experience;
	
	private int f_currentBomberId;
	private int f_rightHandItem;
	private int f_auraOne;
	private int f_auraTwo;
	private int f_auraThree;
	
	private int f_energy;
	private int f_gold;
	private int f_crystal;
	private int f_adamantium;
	private int f_antimatter;
	private int f_votes;
	
	//private int f_energyPrize;
	private int f_goldPrize;
	private int f_crystalPrize;
	private int f_adamantiumPrize;
	private int f_antimatterPrize;
	
	private long f_lastLogin;
	
	private int f_luckCount;
	
	private int f_trainingStatus;
	
	private Map<Integer, Object> f_locations;
	private Map<Integer, Integer> f_items;
	private Map<Integer, Object> f_bombers;
	
	private Map<String, Integer> f_medals;
	
	private int f_missionToken;
	private long f_missionStartTime;
	
	//Getters and setters
	
	public String getId() { return f_id; }
	
	public int getTrainingStatus() { return f_trainingStatus; }
	public void setTrainingStatus(int status) { f_trainingStatus = status; }
	
	public long getLastLogin() { return f_lastLogin; }
	public void setLastLogin(long lastLogin) { f_lastLogin = lastLogin; }
	
	public int getLuckCount() { return f_luckCount; }
	public void setLuckCount(int luckCount) { f_luckCount = luckCount; }
	public void addLuckCount(int delta) { f_luckCount += delta; }
	
	public String getNick() { return f_nick; }
	public void setNick(String nick) { f_nick = nick; }
	
	public String getPhoto() { return f_photo; }
	public void setPhoto(String photo) { f_photo = photo; }
	
	public int getExperience() { return f_experience; }
	public void setExperience(int experience) { f_experience = Math.min(experience, C_BetaMaximunExperience); }
	public void addExperience(int delta) { f_experience = Math.min(f_experience + delta, C_BetaMaximunExperience); }
	
	public int getLevel() {
		if (LevelTable == null) return 1;
		int level = 1;
		for (int i = 0; i < LevelTable.size(); ++i) {
			if (f_experience >= LevelTable.get(i)) {
				level = i + 1;
				continue;
			}
			if (f_experience < LevelTable.get(i)) break;
		}
		return level;
	}
	
	public int getEnergy() { 
		long ts = System.currentTimeMillis() / 1000;
		long p = ts - f_lastLogin;
		if (p >= C_EnergyPeriod) {
			if (f_energy < C_MaximumFreeEnergy) {
				f_energy = Math.min(C_MaximumFreeEnergy, f_energy + (int)(p / C_EnergyPeriod));
			} 
			f_lastLogin = ts;
		}
		return f_energy; 
	}
	public void setEnergy(int energy) { f_energy = energy; }
	public void addEnergy(int delta) { f_energy += delta; }
	
	public int getGold() { return f_gold; }
	public void setGold(int gold) { f_gold = gold; }
	public void addGold(int delta) { f_gold += delta; }
	
	public int getCrystal() { return f_crystal; }
	public void setCrystal(int crystal) { f_crystal = crystal; }
	public void addCrystal(int delta) { f_crystal += delta; }
	
	public int getAdamantium() { return f_adamantium; }
	public void setAdamantium(int adamantium) { f_adamantium = adamantium; }
	public void addAdamantium(int delta) { f_adamantium += delta; }
	
	public int getAntimatter() { return f_antimatter; }
	public void setAntimatter(int antimatter) { f_antimatter = antimatter; }
	public void addAntimatter(int delta) { f_antimatter += delta; }
	
	public int getGoldPrize() { return f_goldPrize; }
	public void setGoldPrize(int goldPrize) { f_goldPrize = goldPrize; }
	public void addGoldPrize(int delta) { f_goldPrize += delta; }
	
	public int getCrystalPrize() { return f_crystalPrize; }
	public void setCrystalPrize(int crystalPrize) { f_crystalPrize = crystalPrize; }
	public void addCrystalPrize(int delta) { f_crystalPrize += delta; }
	
	public int getAdamantiumPrize() { return f_adamantiumPrize; }
	public void setAdamantiumPrize(int adamantiumPrize) { f_adamantiumPrize = adamantiumPrize; }
	public void addAdamantiumPrize(int delta) { f_adamantiumPrize += delta; }
	
	public int getAntimatterPrize() { return f_antimatterPrize; }
	public void setAntimatterPrize(int antimatterPrize) { f_antimatterPrize = antimatterPrize; }
	public void addAntimatterPrize(int delta) { f_antimatterPrize += delta; }
	
	public int getVotes() { return f_votes; }
	public void setVotes(int votes) { f_votes = votes; }
	public void addVotes(int delta) { f_votes += delta; }
	
	public int getRightHandItem() { return f_rightHandItem; }
	public void setRegihtHandItem(int rightHandItem) { f_rightHandItem = rightHandItem; }
	
	public int getCurrentBomberId() { return f_currentBomberId; }
	public void setCurrentBomberId(int currentBomberId) { f_currentBomberId = currentBomberId; }
	
	public int getAuraOne() { return f_auraOne; }
	public int getAuraTwo() { return f_auraTwo; }
	public int getAuraThree() { return f_auraThree; }
	public void setAuraOne(int aura) { f_auraOne = aura; }
	public void setAuraTwo(int aura) {  f_auraTwo = aura; }
	public void setAuraThree(int aura) {  f_auraThree = aura; }
	
	public Map<Integer, Integer> getItems() { return f_items; }
	public boolean hasItemInStack(int itemId) { return f_items.containsKey(itemId); }
	public boolean hasItems(int itemId, int count) { return f_items.containsKey(itemId) && f_items.get(itemId) >= count; }
	public int itemCount(int itemId) { return f_items.get(itemId); }
	public void addItems(int itemId, int delta) { 
		if (f_items.containsKey(itemId)) { 
			f_items.put(itemId, f_items.get(itemId) + delta); 
		} else { 
			f_items.put(itemId, delta); 
		} 
	}
	public void removeItem(int itemId) {
		f_items.remove(itemId);
	}

	public boolean isLocationOpened(int locationId) { 
		return (locationId == Locations.C_GrassFields) || f_locations.containsKey(locationId); 
	}
	
	public boolean isBomberOpened(int bomberId) {
		if (bomberId == Bombers.C_Bobmer_Fury_Joe || bomberId == Bombers.C_Bobmer_R2D3) {
			return true;
		}
		return f_bombers.containsKey(bomberId);
	}

	public boolean hasMedal(String missionId, int medal) {
		Integer medals = f_medals.get(missionId);
		return medals != null && (medals & medal) != 0;
	}
	public void setMedal(String missionId, int medal) {
		int medals = 0;
		if (f_medals.containsKey(missionId)) {
			medals = f_medals.get(missionId);
		}
		medals |= medal;
		f_medals.put(missionId, medals);
	}
	
	
	public int getMissionToken() { return f_missionToken; }
	public void setMissionToken(int token) { f_missionToken = token; }
	
	public long getMissionStartTime() { return f_missionStartTime; }
	public void setMissionStartTime(long ts) { f_missionStartTime = ts; }
	
	//Methods
	
	public SFSArray getBombersData() {
		SFSArray bombers = new SFSArray();
		bombers.addIntArray(f_bombers.keySet());
		return bombers;
	}
	
	public SFSArray getLocationsData() {
		SFSArray locations = new SFSArray();
		locations.addIntArray(f_locations.keySet());
		return locations;
	}
	
	public SFSArray getItemsData() {
		SFSArray items = new SFSArray();
		ArrayList<Integer> itemIds = new ArrayList<Integer>(f_items.keySet());
		int count = itemIds.size();
		for (int i = 0; i < count; ++i)
		{
			SFSObject itemInfo = new SFSObject();
			int itemId = itemIds.get(i);
			int itemCount = f_items.get(itemId);
			//if (itemCount > 0) {
			itemInfo.putInt(C_Id, itemId);
			itemInfo.putInt(C_SmallCount, itemCount);
			items.addSFSObject(itemInfo);
			//}
		}
		return items;
	}
	
	public SFSArray getMedalsData() {
		SFSArray medals = new SFSArray();
		for (String m : f_medals.keySet()) {
			SFSArray sfsm = new SFSArray();
			sfsm.addUtfString(m);
			sfsm.addInt(f_medals.get(m));
			medals.addSFSArray(sfsm);
		}
		return medals;
	}
	
	
	public ISFSObject toSFSObject()
	{
		ISFSObject profile = new SFSObject();
		
		profile.putUtfString(C_Id, f_id);
		profile.putUtfString(C_Nick, f_nick);
		profile.putUtfString(C_Photo, f_photo);
		profile.putInt(C_Energy, f_energy);
		profile.putInt(C_Experience, f_experience);
		profile.putInt(C_CurrentBomberId, f_currentBomberId);
		profile.putInt(C_RightHandItem, f_rightHandItem);
		profile.putInt(C_AuraOne, f_auraOne);
		profile.putInt(C_AuraTwo, f_auraTwo);
		profile.putInt(C_AuraThree, f_auraThree);
		profile.putInt(C_Votes, f_votes);
		profile.putInt(C_Gold, f_gold);
		profile.putInt(C_Crystal, f_crystal);
		profile.putInt(C_Adamantium, f_adamantium);
		profile.putInt(C_Antimatter, f_antimatter);
		
		profile.putInt(C_GoldPrize, f_goldPrize);
		profile.putInt(C_CrystalPrize, f_crystalPrize);
		profile.putInt(C_AdamantiumPrize, f_adamantiumPrize);
		profile.putInt(C_AntimatterPrize, f_antimatterPrize);
		
		profile.putInt(C_LuckCount, f_luckCount);
		profile.putLong(C_LastLogin, f_lastLogin);
		
		profile.putInt(C_TrainingStatus, f_trainingStatus);
		
		ISFSArray locations = new SFSArray();
		locations.addIntArray(f_locations.keySet());
		profile.putSFSArray(C_FldLocationsOpen, locations);
		
		ISFSArray bombers = new SFSArray();
		bombers.addIntArray(f_bombers.keySet());
		profile.putSFSArray(C_FldBombersOpen, bombers);		
		
		ISFSArray items = new SFSArray();
		ArrayList<Integer> itemIds = new ArrayList<Integer>(f_items.keySet());
		int count = itemIds.size();
		for (int i = 0; i < count; ++i)
		{
			ISFSObject itemInfo = new SFSObject();
			int itemId = itemIds.get(i);
			int itemCount = f_items.get(itemId);
//			if (itemCount > 0) {
			itemInfo.putInt(C_WeaponId, itemId);
			itemInfo.putInt(C_Count, itemCount);
			items.addSFSObject(itemInfo);
//			}
		}
		profile.putSFSArray(C_FldWeaponsOpen, items);
		
		SFSArray medals = new SFSArray();
		for (String missionId : f_medals.keySet()) {
			SFSArray sfsMedalInfo = new SFSArray();
			sfsMedalInfo.addUtfString(missionId);
			sfsMedalInfo.addInt(f_medals.get(missionId));
			medals.addSFSArray(sfsMedalInfo);
		}
		profile.putSFSArray(C_FldMedals, medals);
		
		
		return profile;
	}
	
}
