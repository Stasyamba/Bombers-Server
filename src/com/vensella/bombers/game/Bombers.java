package com.vensella.bombers.game;

public class Bombers {

	//Constants
	
	public static final int C_Bobmer_Fury_Joe = 10000;
	public static final int C_Bobmer_R2D3 = 10001;
	public static final int C_Bobmer_Zombaster = 10002;
	
	public static void setBomberParameters(PlayerGameProfile gameProfile) {
		if (gameProfile.getBomberId() == C_Bobmer_Zombaster) {
			gameProfile.addMaxHealth(PlayerGameProfile.C_HealthQuantum);
			gameProfile.addHealth(PlayerGameProfile.C_HealthQuantum);
		}
	}
	
}
