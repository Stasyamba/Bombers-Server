package com.vensella.bombers.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartfoxserver.v2.entities.User;
import com.vensella.bombers.dispatcher.PlayerProfile;

public class PlayerGameProfile {

	//Fields
	
	private boolean f_isAlive = true;
	
	private PlayerProfile f_baseGameProfile;
	
	private int f_bomberId;
	private int f_auraOne;
	private int f_auraTwo;
	private int f_auraThree;
	
	private int f_maxHealth = 3;
	private int f_health = 3;
	private int f_bombPowerBonus = 0;
	private double f_speed = 100.0 / 1000.0;
	private int f_bombsLeft = 1;
	
//	private double f_x;
//	private double f_y;
//	private int f_direction;
	
	private User f_user;
	
	private Map<Integer, Integer> f_weapons;
	private Map<Integer, Boolean> f_weaponsUseTraking;
	private Map<Integer, Boolean> f_weaponsSwitchedOn;
	
	//Constructors
	
	public PlayerGameProfile(User user, PlayerProfile profile)
	{
		f_user = user;
		f_weapons = profile.getItems();
		f_weaponsUseTraking = new ConcurrentHashMap<Integer, Boolean>();
		f_weaponsSwitchedOn = new ConcurrentHashMap<Integer, Boolean>();
		
		f_baseGameProfile = profile;
		
		f_bomberId = profile.getCurrentBomberId();
		f_auraOne = profile.getAuraOne();
		f_auraTwo = profile.getAuraTwo();
		f_auraThree = profile.getAuraThree();
	}
	
	//Methods
	
	//TODO: Add parameters tuning in case of aura and bomber selected
	
	//Getters and setters
	
	public User getUser() { return f_user; }
	public PlayerProfile getBaseProfile() { return f_baseGameProfile; }
	
	public boolean isAlive() { return f_isAlive; }
	public void setIsAlive(boolean isAlive) { f_isAlive = isAlive; }
	
	public int getBomberId() { return f_bomberId; }
	public int getAuraOne() { return f_auraOne; }
	public int getAuraTwo() { return f_auraTwo; }
	public int getAuraThree() { return f_auraThree; }
	
	public int getHealth() { return f_health; }
	public void addHealth(int delta) {
		f_health = Math.min(f_maxHealth, f_health + delta);
		f_isAlive = !(f_health <= 0);
	}
	public void addMaxHealth(int delta) { f_maxHealth += delta; }
	
	public double getSpeed() { return f_speed; }
	public void addSpeed(double ratio) { f_speed *= ratio; }
	public void setSpeed(double speed) { f_speed = speed; }
	
	public int getBombPowerBonus() { return f_bombPowerBonus; }
	public void addBombPowerBonus(int delta) { f_bombPowerBonus += delta; }
	public void setBombPowerBonus(int bombPowerBonus) { f_bombPowerBonus = bombPowerBonus; }
	
	public boolean setBomb() { if (f_bombsLeft == 0) { return false; } else { f_bombsLeft--; return true; } }
	public void releaseBomb() { f_bombsLeft++; }
	
	public boolean useWeapon(int weaponId) {
		Integer count = f_weapons.get(weaponId);
		if (count == null || count == 0)
			return false;
		else
		{
			f_weapons.put(weaponId, count - 1);
			f_weaponsUseTraking.put(weaponId, true);
			f_baseGameProfile.setRegihtHandItem(weaponId);
			return true;
		}
	}
	public boolean useWeapon(int weaponId, int amount) {
		Integer count = f_weapons.get(weaponId);
		if (count == null || count < amount)
			return false;
		else
		{
			f_weapons.put(weaponId, count - amount);
			f_weaponsUseTraking.put(weaponId, true);
			f_baseGameProfile.setRegihtHandItem(weaponId);
			return true;
		}
	}
	
	public boolean isWeaponSwitchedOn(int weaponId) {
		Boolean flag = f_weaponsSwitchedOn.get(weaponId);
		return (flag != null && flag.booleanValue());
	}
	
	public void weaponSwitchOn(int weaponId) {
		f_weaponsSwitchedOn.put(weaponId, true);
	}
	
	public void weaponSwitchOff(int weaponId) {
		f_weaponsSwitchedOn.put(weaponId, false);
	}
	
	
}
