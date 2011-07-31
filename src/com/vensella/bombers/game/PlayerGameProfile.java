package com.vensella.bombers.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartfoxserver.v2.entities.User;
import com.vensella.bombers.dispatcher.PlayerProfile;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;

public class PlayerGameProfile {

	//Constants
	
	public static int C_HealthQuantum = 5;
	
	public static long C_ImmortalTime = 3000;
	
	//Fields
	
	private boolean f_isAlive = true;
	
	private PlayerProfile f_baseGameProfile;
	
	private int f_bomberId;
	private int f_auraOne;
	private int f_auraTwo;
	private int f_auraThree;
	
	private int f_maxHealth = 3 * C_HealthQuantum;
	private int f_health = 3 * C_HealthQuantum;
	private int f_bombPowerBonus = 0;
	private double f_speed = 100.0;
	private int f_bombsLeft = 1;
	
	private double f_x;
	private double f_y;
	
	private int f_xi;
	private int f_yi;
	
	private double f_offsetX;
	private double f_offsetY;
	private int f_cellX;
	private int f_cellY;
	
	private int f_inputDirection;
	private int f_viewDirection;
	
	private long f_lastMoveCalculation;
	
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
		
		Bombers.setBomberParameters(this);
	}
	
	//Methods
	
	//TODO: Add parameters tuning in case of aura and bomber selected
	
	//Move tracking
	
	public long getLastMoveCalculation() { return f_lastMoveCalculation; }
	public void setLastMoveCalculation(long lastMoveCalculation) { f_lastMoveCalculation = lastMoveCalculation; }
	
	public double getX() { return f_x; }
	public void setX(double X) { f_x = X; }
	public void addX(double delta) { f_x += delta; }
	
	public double getY() { return f_y; }
	public void setY(double Y) { f_y = Y; }
	public void addY(double delta) { f_y += delta; }
	
	public int getXi() { return f_xi; }
	public void setXi(int X) { f_xi = X; }
	public void addXi(int delta) { f_xi += delta; }
	
	public int getYi() { return f_yi; }
	public void setYi(int Y) { f_yi = Y; }
	public void addYi(int delta) { f_yi += delta; }
		
	public int getXCell() { return f_cellX; }
	public void setXCell(int cellX) { f_cellX = cellX; }
	public void addXCell(int delta) { f_cellX += delta; }
	
	public int getYCell() { return f_cellY; }
	public void setYCell(int cellY) { f_cellY = cellY; }
	public void addYCell(int delta) { f_cellY += delta; }
	
	public double getXOffset() { return f_offsetX; }
	public void setXOffset(double offsetX) { f_offsetX = offsetX; }
	public void addXOffset(double delta) { 
		f_offsetX += delta; 
		if (f_offsetX > DynamicGameMap.C_BlockSize) { 
			f_offsetX -= DynamicGameMap.C_BlockSize;
			f_cellX += 1;
		} else if (f_offsetX < 0.0) {
			f_offsetX += DynamicGameMap.C_BlockSize;
			f_cellX -= 1;
		}
	}
	
	public double getYOffset() { return f_offsetY; }
	public void setYOffset(double offsetY) { f_offsetY = offsetY; }
	public void addYOffset(double delta) { 
		f_offsetY += delta; 
		if (f_offsetY > DynamicGameMap.C_BlockSize) { 
			f_offsetY -= DynamicGameMap.C_BlockSize;
			f_cellY += 1;
		} else if (f_offsetY < 0.0) {
			f_offsetY += DynamicGameMap.C_BlockSize;
			f_cellY -= 1;
		} 
	}
	
	public int getViewDirection() { return f_viewDirection; }
	public void setViewDirection(int viewDirection) { f_viewDirection = viewDirection; }
	
	public int getInputDirection() { return f_inputDirection; }
	public void setInputDirection(int inputDirection) { f_inputDirection = inputDirection; }
	
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
	
	private long f_damageTime = 0;
	public long getDamageTime() { return f_damageTime; }
	public void setDamageTime(long time) { f_damageTime = time; }
	
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
