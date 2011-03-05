package com.vensella.bombers.dispatcher;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;


public class DBQueryManager {
	
	//Constants
	
	public static final String SqlSelectPlayerExistance = "select count(*) as `C` from `Users` where `Id` = ?";
	
	public static final String SqlSelectPlayerData = "select * from `Users` where `Id` = ?";
	public static final String SqlSelectPlayerLocations = "select * from `LocationsOpen` where `UserId` = ?";
	public static final String SqlSelectPlayerBombers = "select * from `BombersOpen` where `UserId` =  ?";
	public static final String SqlSelectPlayerItems = "select * from `WeaponsOpen` where `UserId` = ?";
	
	public static final String SqlSelectPrizeForInviting = "select * from `PrizesFromWall` where `PrizeActivatorId` = ?";
	
	public static final String SqlInsertPlayerData = "insert into `Users` (`Id`) values (?)";
	public static final String SqlInsertPlayerLocations = "insert into `LocationsOpen` (`UserId`, `LocationsOpen`) values (?, ?)";
	public static final String SqlInsertPlayerBombers = "insert into `BombersOpen` (`UserId`, `BombersOpen`) values (?, ?)";
	public static final String SqlInsertPlayerItems = "insert into `WeaponsOpen` (`UserId`, `WeaponsOpen`) values (?, ?)";
	
	public static final String SqlUpdateUserDataWhenUserDisconnects = 
		"update `Users` set `Experience` = ?,  `Energy` = ?, " +
		"`Nick` = ?, `AuraOne` = ?, `AuraTwo` = ?, `AuraThree` = ?, " +
		"`RightHand` = ?, `BomberId` = ?, `Photo` = ?, `LastLogin` = ?, `LuckCount` = ? where `Id` = ?";
	
	public static final String SqlUpdatePlayerItems = "update `WeaponsOpen` set `WeaponsOpen` = ? where `UserId` = ?";
	public static final String SqlAddPlayerResources = 
		"update `Users` set `Gold` = `Gold` + ?, `Crystal` = `Crystal` + ?, `Adamantium` = `Adamantium` + ?, " +
		"`Antimatter` = `Antimatter` + ?, `Energy` = `Energy` + ? where `Id` = ?";
	public static final String SqlSubtractPlayerResources = 
		"update `Users` set `Gold` = `Gold` - ?, `Crystal` = `Crystal` - ?, `Adamantium` = `Adamantium` - ?, " +
		"`Antimatter` = `Antimatter` - ?, `Energy` = `Energy` - ? where `Id` = ?";
	public static final String SqlAddPlayerResourcesPrize = 
		"update `Users` set `GoldPrize` = `GoldPrize` + ?, `CrystalPrize` = `CrystalPrize` + ?, " +
		"`AdamantiumPrize` = `AdamantiumPrize` + ?, " +
		"`AntimatterPrize` = `AntimatterPrize` + ? where `Id` = ?";
	public static final String SqlTakePrize = "update `Users` set " +
			"`Gold` = `Gold` + ?, `GoldPrize` = `GoldPrize` - ?, " + 
			"`Crystal` = `Crystal` + ?, `CrystalPrize` = `CrystalPrize` - ?, " + 
			"`Adamantium` = `Adamantium` + ?, `AdamantiumPrize` = `AdamantiumPrize` - ?, " + 
			"`Antimatter` = `Antimatter` + ?, `AntimatterPrize` = `AntimatterPrize` - ? " +
			"where `Id` = ?";
	
	public static final String SqlInsertPrizeFromWall = "insert into `PrizesFromWall` values (?, ?, ?)";
	
	//Nested types
	
	private class QueuedQuery
	{
		//Fields
		
		private String f_query;
		private Object[] f_params;
		private QueryCallback f_callback;
		private Boolean f_isUpdate;
		
		//Constructors
		
		public QueuedQuery(Boolean isUpdate, QueryCallback callback, String query, Object[] params)
		{
			f_isUpdate = isUpdate;
			f_query = query;
			f_params = params;
			f_callback = callback;
		}
		
		//Methods
		
		public Boolean isUpdate() { return f_isUpdate; }
		public String getQuery() { return f_query; }
		public Object[] getParams() { return f_params; }
		public QueryCallback getCallback() { return f_callback; }

	}
	
	public abstract class QueryCallback
	{
		public abstract void run(ISFSArray result);
	}
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	private Thread f_workingThread;
	private BlockingQueue<QueuedQuery> f_queue;
	
	//Constructors
	
	public DBQueryManager(BombersDispatcher dispatcher)
	{
		f_dispatcher = dispatcher;
		f_queue = new LinkedBlockingQueue<DBQueryManager.QueuedQuery>();
		f_workingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true){
					QueryCallback cb = null;
					String sql = null;
					Object[] params = null;
					try
					{
						QueuedQuery q = f_queue.take();
						sql = q.getQuery();
						params = q.getParams();
						cb = q.getCallback();
						Connection conn = f_dispatcher.getParentZone().getDBManager().getConnection();
						try {
							PreparedStatement st = conn.prepareStatement(q.getQuery());
							int i = 1;
							for (Object p : q.getParams()) {
								st.setString(i++, p.toString());
							}
							if (q.isUpdate()) {
								SFSArray result = SFSArray.newInstance();
								result.addInt(st.executeUpdate());
								if (cb != null) {
									cb.run(result);
								}
							} else {
								SFSArray result = SFSArray.newFromResultSet(st.executeQuery());
								if (cb != null) {
									cb.run(result);
								}								
							}
						}
						finally {
							if (conn != null)
								conn.close();
						}
					}
					catch(Exception ex)
					{
						f_dispatcher.trace(ExtensionLogLevel.ERROR, "Db query exception:");
						if (sql != null) {
							f_dispatcher.trace(ExtensionLogLevel.ERROR, sql);
						}
						if (params != null) {
							for (int i = 0; i < params.length; ++i) if (params[i] == null) params[i] = "$NULL$";
							f_dispatcher.trace(ExtensionLogLevel.ERROR, params);
						}
						f_dispatcher.trace(ExtensionLogLevel.ERROR, ex.getMessage());
						f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
						if (cb != null)
							cb.run(null);
					}
				}
			}
		}, "DB Query Thread");
		f_workingThread.start();
	}
			
	
	//Methods
	
	public void ScheduleUpdateQuery(String query, Object[] params) {
		ScheduleUpdateQuery(query, params, null);
	}
	
	public void ScheduleUpdateQuery(String query, Object[] params, QueryCallback callback)
	{
		QueuedQuery q = new QueuedQuery(true, callback, query, params);
		f_queue.add(q);
	}
	
	public void ScheduleQuery(String query, Object[] params, QueryCallback callback)
	{
		assert callback != null;
		QueuedQuery q = new QueuedQuery(false, callback, query, params);
		f_queue.add(q);
	}
	
	//Static methods
	
}
