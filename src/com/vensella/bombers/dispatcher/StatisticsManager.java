package com.vensella.bombers.dispatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.vensella.bombers.game.WeaponsManager;

public class StatisticsManager {
	
	//Constants
	
	public static final int C_ConnectionSourceDefault = 0;
	
	public static final int C_ConnectionSourceWallEnergy = 1;
	public static final int C_ConnectionSourceWallVictory = 2;
	public static final int C_ConnectionSourceWallInvite = 3;
	
	public static final int C_ConnectionSourceWall = 4;
	
	public static final int C_ConnectionSourceAd1 = 5;
	public static final int C_ConnectionSourceAd2 = 6;
	public static final int C_ConnectionSourceAd3 = 7;
	public static final int C_ConnectionSourceAd4 = 8;
	
	public static final String C_SqlInsertSessionStats = 
		"insert into `SessionStats` values (" +
		"?, ?, ?, ?, ?, " +
		"?, " +
		"?, ?, ?, ?, ?, ?, ?, ?, ?, " +
		"?, ?,  ?, ?,  ?, ?,  ?, " +
		"?, ?" +
		")";
	
	public static final String C_SqlInsertItemsStat = "insert into `ItemsStats` values (?, ?)";
	
	//Nested classes
	
	public class SessionStats {
		
		//Constructor
		
		public SessionStats(String userId, boolean isFirstTime) {
			f_startTime = (int)(System.currentTimeMillis() / 1000);
			f_userId = userId;
			f_isFirstTime = isFirstTime;
		}
		
		//Fields
		
		private int f_startTime;
		private int f_endTime;
		
		private String f_userId;
		
		private boolean f_isFirstTime;
		private int f_connectionSource;
		
		//Public fields
		
		public int votesSpent;
		
		public int goldBuyed;
		public int crystalBuyed;
		public int energyBuyed;
		
		public int goldEarned;
		public int crystalEarned;
		public int energyEarned;
		
		public int goldSpent;
		public int crystalSpent;
		public int energySpent;
		
		public int matchesPlayed;
		public int singleGamesPlayed;
		
		public int trainingStatusAtBegin;
		public int trainingStatusAtEnd;
		
		public int levelAtBegin;
		public int levelAtEnd;
		
		public int experienceEarned;
		
		public int shopOpened;
		public int postsPosted = 100100100;
		
		//Methods
		
		public int getStartTime() { return f_startTime; }
		public int getEndTime() { return f_endTime; }
		
		public String getUserId() { return f_userId; }
		
		public boolean getIsFirstTime() { return f_isFirstTime; }
		
		public int getConnectionSource() { return f_connectionSource; }
		public void setConnectionSource(int connectionSource) { f_connectionSource = connectionSource; }
		
		public void endSession() { f_endTime = (int)(System.currentTimeMillis() / 1000); }
		
	}
	
	//Constructors
	
	public StatisticsManager(BombersDispatcher dispatcher) {
		f_dispatcher = dispatcher;
		f_dbManager = new DBQueryManager(dispatcher, "Bombers Statistics Collector");	
		
		m_resetStatsCollections();
		
		int secondsInCurrentDay = (int)(System.currentTimeMillis() / 1000) % 86400;
		int secondsTo5Am = 0;
		if (secondsInCurrentDay <= 3600 * 5) {
			secondsTo5Am = 3600 * 5 - secondsInCurrentDay;
		} else {
			secondsTo5Am = 5 * 3600 + 86400 - secondsInCurrentDay;
		}
		
		SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					m_saveStatistics();
					m_resetStatsCollections();
				}
			}, secondsTo5Am, 86400, TimeUnit.SECONDS);
	}
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	private DBQueryManager f_dbManager;
	
	private Map<Integer, Integer> f_itemsBought;
	private Map<Integer, Integer> f_itemsUsed;
	
	//Private methods
	
	private void m_resetStatsCollections() {
		f_itemsBought = new ConcurrentHashMap<Integer, Integer>();
		f_itemsUsed = new ConcurrentHashMap<Integer, Integer>();
		f_itemsUsed.put(WeaponsManager.WEAPON_BOMB_NORMAL, 0);
		for (Integer itemId : f_dispatcher.getPricelistManager().getUseableItemsIds()) {
			f_itemsBought.put(itemId, 0);
			f_itemsUsed.put(itemId, 0);
		}
	}
	
	private void m_saveStatistics() {
		SFSObject stats = new SFSObject();
		stats.putSFSArray("ItemsUse", CommonHelper.createSFSArray(f_itemsUsed));
		stats.putSFSArray("ItemsBuy", CommonHelper.createSFSArray(f_itemsBought));
		f_dbManager.ScheduleUpdateQuery(C_SqlInsertItemsStat, new Object[] { System.currentTimeMillis(), stats.toJson() });
	}
	
	//Methods
	
	public BombersDispatcher getDispatcher() { return f_dispatcher; }
	
	public void initSession(PlayerProfile profile, boolean isFirstTime) {
		SessionStats sessionStats = new SessionStats(profile.getId(), isFirstTime);
		sessionStats.trainingStatusAtBegin = profile.getTrainingStatus();
		sessionStats.levelAtBegin = getDispatcher().getPricelistManager().getLevelFor(profile).getValue();
		profile.setSessionStats(sessionStats);
	}
	
	public void closeSession(PlayerProfile profile) {
		SessionStats s = profile.getSessionStats();
		profile.setSessionStats(null);
		s.trainingStatusAtEnd = profile.getTrainingStatus();
		s.levelAtEnd = getDispatcher().getPricelistManager().getLevelFor(profile).getValue();
		s.endSession();
		f_dbManager.ScheduleUpdateQuery(C_SqlInsertSessionStats, new Object[] {
			s.getStartTime(), s.getEndTime(), s.getUserId(), s.getIsFirstTime() ? 1 : 0, s.getConnectionSource(),
			s.votesSpent,
			s.goldBuyed, s.crystalBuyed, s.energyBuyed,
			s.goldEarned, s.crystalEarned, s.energyEarned,
			s.goldSpent, s.crystalSpent, s.energySpent,
			s.matchesPlayed, s.singleGamesPlayed,
			s.trainingStatusAtBegin, s.trainingStatusAtEnd,
			s.levelAtBegin, s.levelAtEnd,
			s.experienceEarned,
			s.shopOpened, s.postsPosted
		});
	}
	
	public void writeWeaponBuy(int weaponId) {
		f_itemsBought.put(weaponId, f_itemsBought.get(weaponId) + 1);
	}
	
	public void writeWeaponUse(int weaponId) {
		f_itemsUsed.put(weaponId, f_itemsUsed.get(weaponId) + 1);
	}
	
	public void destroy() { 
		m_saveStatistics();
	}
	
}
