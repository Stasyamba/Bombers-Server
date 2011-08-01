package com.vensella.bombers.dispatcher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartfoxserver.v2.entities.data.*;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class RecordsManager {

	//Nested types
	
	public class MissionRecord {
		
		//Constructor
		
		public MissionRecord(String missionId, int medalType, int missionTime, String userId, String photoUrl) {
			f_missionId = missionId;
			f_medalType = medalType;
			f_missionTime = missionTime;
			f_userId = userId;
			f_photoUrl = photoUrl;
		}
		
		protected MissionRecord(ISFSObject data) {
			f_missionId = data.getUtfString("Id");
			f_medalType = data.getInt("MedalType");
			f_missionTime = data.getInt("Time");
			f_userId = data.getUtfString("Login");
			f_photoUrl = data.getUtfString("PhotoUrl");		
		}
		
		//Fields
		
		private String f_missionId;
		private int f_medalType;
		private int f_missionTime;
		private String f_userId;
		private String f_photoUrl;
		
		//Methods
		
		public String getMissionId() { return f_missionId; }
		public int getMedalType() { return f_medalType; }
		public int getMissionTime() { return f_missionTime; }
		public String getUserId() { return f_userId; }
		public String getPhotoUrl() { return f_photoUrl; }
		
		public SFSObject toSFSObject() {
			SFSObject result = new SFSObject();
			result.putUtfString("Id", f_missionId);
			result.putInt("MedalType", f_medalType);
			result.putInt("Time", f_missionTime);
			result.putUtfString("Login", f_userId);
			result.putUtfString("PhotoUrl", f_photoUrl);
			return result;
		}
		
	}
	
	//Constructors
	
	public RecordsManager(BombersDispatcher dispatcher) {
		f_dispatcher = dispatcher;
		f_dbManager = new DBQueryManager(dispatcher, "Bombers Records Collector");
		
		f_missionRecords = new ConcurrentHashMap<String, MissionRecord>();
		
		m_loadRecords();
		m_resetMissionRecordsData();
	}
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	private DBQueryManager f_dbManager;
	
	private Map<String, MissionRecord> f_missionRecords;
	private SFSArray f_missionRecordsData;
	
	//Private methods
	
	public void m_loadRecords() {
		Connection conn = null;
		try {
			try {
				conn = f_dispatcher.getParentZone().getDBManager().getConnection();
				
				PreparedStatement st = conn.prepareStatement(DBQueryManager.SqlSelectLastQuestRecords);

				SFSArray questRecordsData = SFSArray.newFromResultSet(st.executeQuery());
				if (questRecordsData.size() == 0) {
					
				} else {
					SFSArray records = SFSArray.newFromJsonData(questRecordsData.getSFSObject(0).getUtfString("Records"));
					for (int i = 0; i < records.size(); ++i) {
						ISFSObject recordData = records.getSFSObject(i);
						MissionRecord r = new MissionRecord(recordData);
						f_missionRecords.put(r.getMissionId(), r);
					}
				}
			}
			catch (Exception ex) {
				f_dispatcher.trace (ExtensionLogLevel.ERROR, "Something bad happened during records manager load");
				f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.toString());
				f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
				

			} finally {
				if (conn != null) {
					conn.close();
				}
			}
		} 
		catch (Exception ex) {
			f_dispatcher.trace (ExtensionLogLevel.ERROR, "Something VERY bad happened during records manager load");
			f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.toString());
			f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
		}
	}
	
	public void m_resetMissionRecordsData() { 
		SFSArray recordsData = new SFSArray();
		for (MissionRecord r : f_missionRecords.values()) {
			recordsData.addSFSObject(r.toSFSObject());
		}
		f_missionRecordsData = recordsData;
	}
	
	//Methods
	
	public BombersDispatcher getDispatcher() { return f_dispatcher; }
	public DBQueryManager getDbManager() { return f_dbManager; }
	
	public void sumbitMissionResult(PlayerProfile profile, String missionId, int missionTime, int medalType) {
		if (f_missionRecords.containsKey(missionId)) {
			MissionRecord r = f_missionRecords.get(missionId);
			if (medalType > r.f_medalType || (medalType == r.f_medalType && missionTime < r.f_missionTime)) {
				r = new MissionRecord(missionId, medalType, missionTime, profile.getId(), profile.getPhoto());
				f_missionRecords.put(missionId, r);
				m_resetMissionRecordsData();
			}
		} else {
			MissionRecord r = new MissionRecord(missionId, medalType, missionTime, profile.getId(), profile.getPhoto());
			f_missionRecords.put(missionId, r);		
			m_resetMissionRecordsData();
		}
	}
	
	public MissionRecord getMissionRecord(String missionId) {
		return f_missionRecords.get(missionId);
	}
	
	public SFSArray getMissionRecordsData() {
		return f_missionRecordsData;
	}
	
	public void destroy() {
		f_dbManager.ScheduleUpdateQuery(
				DBQueryManager.SqlInsertQuestRecords, 
				new Object[] { System.currentTimeMillis(), f_missionRecordsData.toJson()} 
			);
	}
	
}
