package com.vensella.bombers.game.mapObjects;

import java.util.ArrayList;

import com.smartfoxserver.v2.entities.Room;
import com.vensella.bombers.dispatcher.PlayerProfile;
import com.vensella.bombers.game.BombersGame;

public class Locations {

	//Constants
	
	public static int C_TotalLocatios = 9;
	
	public static int C_GrassFields = 0;
	public static int C_Castle = 1;
	public static int C_Mine = 2;
	public static int C_SnowPeak = 3;
	public static int C_Sea = 4;
	public static int C_Ufo = 6;
	public static int C_Rocket = 5;
	public static int C_Sattelite = 7;
	public static int C_Moon = 8;
	
	public static final int[] C_FirstGroup = { C_GrassFields, C_Castle, C_Mine };
	public static final int[] C_SecondGroup = { C_SnowPeak, C_Sea, C_Ufo };
	public static final int[] C_ThirdGroup = { C_Rocket, C_Sattelite, C_Moon };
	
	//Methods
	
	public static int getRoomLocationId(Room room) {
		BombersGame game = (BombersGame)room.getExtension();
		return game.getLocationId();
	}
	
	public static boolean isLocationIdValid(int locationId) {
		return (locationId >= 0 && locationId <= 8);
	}
	
	public static boolean canOpenLocation(PlayerProfile profile, int locationId)
	{
		if (locationId == C_GrassFields)
			return true;
		
		//TODO: Add conditions for more locations
		
		return false;
	}
	
	public static ArrayList<Integer> findBestLocations(PlayerProfile profile)
	{
		ArrayList<Integer> result = new ArrayList<Integer>();

		for (int i = 0; i < C_ThirdGroup.length; ++i)
			if (profile.isLocationOpened(C_ThirdGroup[i]))
				result.add(C_ThirdGroup[i]);
		if (result.isEmpty() == false) return result;
		for (int i = 0; i < C_SecondGroup.length; ++i)
			if (profile.isLocationOpened(C_SecondGroup[i]))
				result.add(C_SecondGroup[i]);
		if (result.isEmpty() == false) return result;
		for (int i = 0; i < C_FirstGroup.length; ++i)
			if (profile.isLocationOpened(C_FirstGroup[i]))
				result.add(C_FirstGroup[i]);
		return result;
	}
	
}
