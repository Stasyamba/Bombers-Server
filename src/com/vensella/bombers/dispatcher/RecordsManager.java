package com.vensella.bombers.dispatcher;

public class RecordsManager {

	//Constructors
	
	public RecordsManager(BombersDispatcher dispatcher) {
		f_dispatcher = dispatcher;
		f_dbManager = new DBQueryManager(dispatcher);
	}
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	private DBQueryManager f_dbManager;
	
	//Methods
	
	public BombersDispatcher getDispatcher() { return f_dispatcher; }
	public DBQueryManager getDbManager() { return f_dbManager; }
	
	public void sumbitMissionResult(PlayerProfile profile, String missionId, int missionTime, int medalType) {
		
	}
	
}
