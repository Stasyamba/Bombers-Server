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
	
	public static final String C_Id = "Id";
	public static final String C_Nick = "Nick";
	public static final String C_Photo = "Photo";
	public static final String C_Experience = "Experience";
	public static final String C_Energy = "Energy";
	
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
	
	public static final String C_UserId = "UserId";
	public static final String C_LocationId = "LocationId";
	public static final String C_WeaponId = "WeaponId";
	public static final String C_Count = "Count";
	public static final String C_BomberId = "BomberId";
	
	public static final String C_FldLocationsOpen = "LocationsOpen";
	public static final String C_FldBombersOpen = "BombersOpen";
	public static final String C_FldWeaponsOpen = "WeaponsOpen";
	
	//Constructors
	
	public PlayerProfile(String userId)
	{
		f_id = userId;
		f_nick = "Игрок";
		f_photo = "";
		
		f_locations = new ConcurrentHashMap<Integer, Object>();
		f_items = new ConcurrentHashMap<Integer, Integer>();
		f_bombers = new ConcurrentHashMap<Integer, Object>();
	}
	
	public PlayerProfile(ISFSArray profile, ISFSArray locations, ISFSArray items, ISFSArray bombers)
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
		
		f_locations = new ConcurrentHashMap<Integer, Object>();
		for (int i = 0; i < locations.size(); ++i)
		{
			f_locations.put(locations.getSFSObject(i).getInt(C_LocationId), new Object());
		}
		
		f_items = new ConcurrentHashMap<Integer, Integer>();
		for (int i = 0; i < items.size(); ++i)
		{
			ISFSObject row = items.getSFSObject(i);
			f_items.put(row.getInt(C_WeaponId), row.getInt(C_Count));
		}
		
		f_bombers = new ConcurrentHashMap<Integer, Object>();
		for (int i = 0; i < bombers.size(); ++i)
		{
			f_bombers.put(bombers.getSFSObject(i).getInt(C_BomberId), new Object());
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
	
	private Map<Integer, Object> f_locations;
	private Map<Integer, Integer> f_items;
	private Map<Integer, Object> f_bombers;
	
	//Getters and setters
	
	public String getId() { return f_id; }
	
	public String getNick() { return f_nick; }
	public void setNick(String nick) { f_nick = nick; }
	
	public String getPhoto() { return f_photo; }
	public void setPhoto(String photo) { f_photo = photo; }
	
	public int getExperience() { return f_experience; }
	public void setExperience(int experience) { f_experience = experience; }
	public void addExperience(int delta) { f_experience += delta; }
	
	public int getEnergy() { return f_energy; }
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
	public int itemCount(int itemId) { return f_items.get(itemId); }
	public void addItems(int itemId, int delta) { 
		if (f_items.containsKey(itemId)) { 
			f_items.put(itemId, f_items.get(itemId) + delta); 
		} else { 
			f_items.put(itemId, delta); 
		} 
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

	
	//Methods
	
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
			itemInfo.putInt(C_WeaponId, itemId);
			itemInfo.putInt(C_Count, itemCount);
			items.addSFSObject(itemInfo);
		}
		profile.putSFSArray(C_FldWeaponsOpen, items);
		
		return profile;
	}
	
}
