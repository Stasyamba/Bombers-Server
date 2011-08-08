package com.vensella.bombers.dispatcher;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;


public class DBQueryManager {
	
	//Constants
	
	public static final String SqlSelectPlayerExistance = "select count(*) as `C` from `Users` where `Id` = ?";
	
	public static final String SqlSelectUsersInfo = "select `Id`, `Nick`, `Experience`, `Photo` from `Users` " +
													"where `Id` in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public static final String SqlSelectPlayerData = "select * from `Users` where `Id` = ?";
	public static final String SqlSelectPlayerLocations = "select * from `LocationsOpen` where `UserId` = ?";
	public static final String SqlSelectPlayerBombers = "select * from `BombersOpen` where `UserId` =  ?";
	public static final String SqlSelectPlayerItems = "select * from `WeaponsOpen` where `UserId` = ?";
	public static final String SqlSelectPlayerMedals = "select * from `Medals` where `UserId` = ?";
	public static final String SqlSelectCustomParameters = "select * from `CustomParameters` where `UserId` = ?";
	
	public static final String SqlSelectPrizeForInviting = "select * from `PrizesFromWall` where `PrizeActivatorId` = ?";
	
	public static final String SqlInsertPlayerData = "insert into `Users` (`Id`) values (?)";
	public static final String SqlInsertPlayerLocations = "insert into `LocationsOpen` (`UserId`, `LocationsOpen`) values (?, ?)";
	public static final String SqlInsertPlayerBombers = "insert into `BombersOpen` (`UserId`, `BombersOpen`) values (?, ?)";
	public static final String SqlInsertPlayerItems = "insert into `WeaponsOpen` (`UserId`, `WeaponsOpen`) values (?, ?)";
	public static final String SqlInsertPlayerMedals = "insert into `Medals` (`UserId`, `Medals`) values (?, ?)";
	public static final String SqlInsertCustomParameters = 
		"insert into `CustomParameters` (`UserId`, `CustomParameters`) values (?, ?)";
	
	
	public static final String SqlUpdateUserDataWhenUserDisconnects = 
		"update `Users` set `Experience` = ?,  `Energy` = ?, `Votes` = ?, " +
		"`Nick` = ?, `AuraOne` = ?, `AuraTwo` = ?, `AuraThree` = ?, " +
		"`RightHand` = ?, `BomberId` = ?, `Photo` = ?, `LastLogin` = ?, `LastLevelReward` = ?, " +
		"`TrainingStatus` = ? where `Id` = ?";
	
	public static final String SqlUpdatePlayerItems = "update `WeaponsOpen` set `WeaponsOpen` = ? where `UserId` = ?";
	public static final String SqlUpdatePlayerMedals = "update `Medals` set `Medals` = ? where `UserId` = ?";
	public static final String SqlUpdatePlayerCustomParameters = 
		"update `CustomParameters` set `CustomParameters` = ? where `UserId` = ?";
	
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
	
	
	public static final String SqlSelectLastQuestRecords = "select * from `RecordsQuests` order by `SnapshotTime` desc limit 0, 1";
	public static final String SqlInsertQuestRecords = "insert into `RecordsQuests` values (?, ?)";
	
	//Nested types
	
	private class QueuedQuery
	{
		//Fields
		
		private String f_query;
		private Object[] f_params;
		private QueryCallback f_callback;
		private boolean f_isUpdate;
		
		private boolean f_isCustomAction;
		private Runnable f_customAction;
		
		//Constructors
		
		public QueuedQuery(Boolean isUpdate, QueryCallback callback, String query, Object[] params)
		{
			f_isUpdate = isUpdate;
			f_query = query;
			f_params = params;
			f_callback = callback;
		}
		
		public QueuedQuery(Runnable customAction) {
			f_customAction = customAction;
			f_isCustomAction = true;
		}
		
		//Methods
		
		public boolean isCustomAction() { return f_isCustomAction; }
		public Runnable getCustomAction() { return f_customAction; }
		
		public boolean isUpdate() { return f_isUpdate; }
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
	
	private volatile boolean f_running;
	
	//Constructors
	
	public DBQueryManager(BombersDispatcher dispatcher) {
		this(dispatcher, "Bombers DB Query Thread");
	}
	
	public DBQueryManager(BombersDispatcher dispatcher, String name)
	{
		f_running = true;
		f_dispatcher = dispatcher;
		f_queue = new LinkedBlockingQueue<DBQueryManager.QueuedQuery>();
		f_workingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (f_running || !f_queue.isEmpty()){
					QueryCallback cb = null;
					String sql = null;
					Object[] params = null;
					try
					{
						QueuedQuery q = f_queue.take();
						
						if (q.isCustomAction()) {
							q.getCustomAction().run();
							continue;
						}
						
						sql = q.getQuery();
						params = q.getParams();
						cb = q.getCallback();
						Connection conn = f_dispatcher.getParentZone().getDBManager().getConnection();
						try {
							PreparedStatement st = conn.prepareStatement(q.getQuery());
							int i = 1;
							for (Object p : q.getParams()) {
								if (p instanceof Integer) {
									st.setInt(i++, (Integer)p);
								} else if (p instanceof Long) {
									st.setLong(i++, (Long)p);
								} else {
									st.setString(i++, p.toString());
								}
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
		}, name);
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
	
	public void ScheduleCustomAction(Runnable action) {
		QueuedQuery q = new QueuedQuery(action);
		f_queue.add(q);
	}
	
	public int getQueueSize() { return f_queue.size(); }
	
	public void beginDestroy() {
		SmartFoxServer.getInstance().getTaskScheduler().schedule(new Runnable() {
			@Override
			public void run() {
				f_running = false;
			}
		}, 200, TimeUnit.MILLISECONDS);
	}
	
	//Static methods
	
}
