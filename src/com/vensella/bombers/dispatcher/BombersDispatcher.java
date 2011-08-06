package com.vensella.bombers.dispatcher;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.CreateRoomSettings.RoomExtensionSettings;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.*;
import com.smartfoxserver.v2.entities.variables.*;
import com.smartfoxserver.v2.exceptions.*;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.util.ClientDisconnectionReason;

import com.vensella.bombers.dispatcher.eventHandlers.*;

import com.vensella.bombers.game.BombersGame;
import com.vensella.bombers.game.mapObjects.Locations;

public class BombersDispatcher extends SFSExtension {
	
	//Public constants
	//TODO: Load from configuration
	
	public static final String C_ApiId = "2141693";
	public static final String C_ApiSecret = "jqKgEDXPd4T2zojPaRcv";
	
	//Flags
	
	private static Boolean IsReleaseMode = false;
	//private static Boolean IsDebugMode = true;
	
	//Constants
	
	private static final String C_GameGroupId = "Games";
	
	private static final int C_WorkingThreadCount = 3;
	private static final int C_DelayedEventTimersCount = 1;
	
	//Special volatile fields
	private volatile int GamesCount = 0;
	
	//Event model fields
	
	private Thread f_shutDownHook;
	
	private ScheduledThreadPoolExecutor f_delayedEventsExecutor;
	
	private Thread[] f_workingThreads;
	private LinkedBlockingQueue<GameEvent>[] f_workingQueues;
	
	//Managers
	
	private RecordsManager f_recordsManager;
	private DBQueryManager f_dbQueryManager;
	private InterfaceManager f_interfaceManager;
	private MoneyManager f_moneyManager;
	private MapManager f_mapManager;
	private PricelistManager f_pricelistManager;
	
	//User tracking
	
	private Map<String, PlayerProfile> f_profileCache;
	private Map<User, PlayerProfile> f_profiles;
	
	//Game settings
	
	//Constructors and initializers
	
	@SuppressWarnings("unchecked")
	@Override
	public void init()
	{ 
		trace(ExtensionLogLevel.WARN, "Bombers zone dispatcher init() start");
		
		//Initialize fields
		
		f_dbQueryManager = new DBQueryManager(this);
		f_recordsManager = new RecordsManager(this);
		f_interfaceManager = new InterfaceManager(this);
		f_moneyManager = new MoneyManager(this);
		f_mapManager = new MapManager(this);
		f_pricelistManager = new PricelistManager(this);
		
		f_profileCache = new ConcurrentHashMap<String, PlayerProfile>();
		f_profiles = new ConcurrentHashMap<User, PlayerProfile>();

		//Initialize of internal structure
	
		f_delayedEventsExecutor = new ScheduledThreadPoolExecutor(C_DelayedEventTimersCount);
		
		f_workingQueues = (LinkedBlockingQueue<GameEvent>[])Array.newInstance(
				LinkedBlockingQueue.class, 
				C_WorkingThreadCount);
		f_workingThreads = new Thread[C_WorkingThreadCount];
		
		for (int i = 0; i < C_WorkingThreadCount; ++i) {
			final int index = i;
			f_workingQueues[i] = new LinkedBlockingQueue<GameEvent>();
			f_workingThreads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						try {
							GameEvent event = f_workingQueues[index].take();
							if (event != null && 
								(event.getForceExecute() ||
								 (event.getCurrentGameId() == event.getEventGameId() &&
								 event.getEventGameId() != GameEvent.INVALID_GAME_ID))
							) {
								event.Apply();
							} else {
								trace (ExtensionLogLevel.DEBUG, "[Notice] Event dropped by kernel");
							}
						} catch (Exception ex) {
							trace(ExtensionLogLevel.ERROR, "[Warning] " + ex.toString());
							trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
						}
					}
				}
			}, "Working thread " + i);
			f_workingThreads[i].start();
		}
		
		//Initialize system event handlers
		
		//addEventHandler(SFSEventType.ROOM_ADDED, RoomAddedEventHandler.class);
		
		addEventHandler(SFSEventType.USER_LOGIN, UserLoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, UserJoinZoneEventHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, UserDisconnectEventHandler.class);
		
		//addEventHandler(SFSEventType., theClass)
		
		//Initialize custom request handlers
		
		addRequestHandler("ping", PingEventHandler.class);
		
		addRequestHandler("interface.gameManager.createGame", GameManagerCreateGame.class);
		addRequestHandler("interface.gameManager.findGameName", GameManagerFindGameName.class);
		addRequestHandler("interface.gameManager.fastJoin", GameManagerFastJoin.class);
//		
		addRequestHandler("interface.buyResources", InterfaceBuyResourcesEventHandler.class);
		addRequestHandler("interface.buyItem", InterfaceBuyItemEventHandler.class);
		addRequestHandler("interface.dropItem", InterfaceDropItemEventHandler.class);
//		addRequestHandler("interface.buyBomber", InterfaceBuyBomberEventHandler.class);
		addRequestHandler("interface.collectCollection", InterfaceCollectCollectionEventHandler.class);
		
//		addRequestHandler("interface.setAura", null);
//		addRequestHandler("interface.setRightHandItem", null);	
		addRequestHandler("interface.setBomber", InterfaceSetBomberEventHandler.class);	
		addRequestHandler("interface.setNick", InterfaceSetNickEventHandler.class);
		addRequestHandler("interface.setPhoto", InterfaceSetPhotoEventHandler.class);
		
		addRequestHandler("interface.getUsersInfo", InterfaceGetUsersInfoEventHandler.class);
		
		addRequestHandler("interface.setTrainingStatus", InterfaceSetTrainingStatusEventHandler.class);
		addRequestHandler("interface.setCustomParameter", InterfaceSetCustomParameterEventHandler.class);
		
//		addRequestHandler("inerface.openLocation", null);
		addRequestHandler("interface.missions.start", MissionStartEventHandler.class);
		addRequestHandler("interface.missions.submitResult", MissionSubmitResultEventHandler.class);
		
		addRequestHandler("admin.reloadMapManager", AdminReloadMapManagerEventHandler.class);
		addRequestHandler("admin.reloadPricelistManager", AdminReloadPricelistManagerEventHandler.class);
		addRequestHandler("admin.resetUserProfile", AdminResetUserProfile.class);
		
		//addRequestHandler("stat.setLoginSource", null);
		
		f_shutDownHook = new Thread("Bombers shutdown hook") {
			public void run() {
				trace(ExtensionLogLevel.WARN, "Bombers zone dispatcher shutdown() begin");
				//getParentZone().setActive(false);
				//TODO: Inform users about server reset
				//TODO: Disconnect all users to connect them after few seconds
				//TODO: Free all resources (threads, timers, etc..)
				
				getRecordsManager().destroy();
				
				//Save all profiles to DB
				ArrayList<PlayerProfile> profiles = new ArrayList<PlayerProfile>();
				profiles.addAll(f_profiles.values());
				for (PlayerProfile profile : profiles) {
					Reward sessionReward = profile.getSessionReward();
					if (sessionReward.isEmpty() == false) {
						getDbManager().ScheduleUpdateQuery(
								DBQueryManager.SqlAddPlayerResources, new Object[] {
								sessionReward.getGoldReward(),
								sessionReward.getCrystalReward(),
								sessionReward.getAdamantiumReward(),
								sessionReward.getAntimatterReward(),
								sessionReward.getEnergyReward(),
								profile.getId()
							});
						profile.removeSessionReward();
					}
					saveProfileToDb(profile);
				}
				
				try {
					Thread.sleep(2000);
					while (getDbManager().getQueueSize() > 0) {
						trace(ExtensionLogLevel.WARN, "Bombers zone dispatcher shutdown() waits for DB Queue to be empty");	
						Thread.sleep(1000);
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					trace(ExtensionLogLevel.WARN, "Bombers zone dispatcher shutdown() sleep interrupted");
				}
				
				trace(ExtensionLogLevel.WARN, "Bombers zone dispatcher shutdown() end");				
			}
		};
		Runtime.getRuntime().addShutdownHook(f_shutDownHook);
		
		trace(ExtensionLogLevel.WARN, "Bombers zone dispatcher init() end");
	}

	@Override
	public void destroy()
	{
		Runtime.getRuntime().removeShutdownHook(f_shutDownHook);
		super.destroy();
	}
	
	//Special methods
	
	public RecordsManager getRecordsManager() { return f_recordsManager; }
	
	public MoneyManager getMoneyManager() { return f_moneyManager; }
	
	public DBQueryManager getDbManager() { return f_dbQueryManager; }
	
	public InterfaceManager getInterfaceManager() { return f_interfaceManager; }
	
	public MapManager getMapManager() { return f_mapManager; }
	
	public PricelistManager getPricelistManager() { return f_pricelistManager; }
	
	//Event dispatching
	
	public void addGameEvent(GameEvent event, int scheduleIndex)
	{
		f_workingQueues[scheduleIndex % C_WorkingThreadCount].add(event);
	}
	
	public void addDelayedGameEvent(final GameEvent event, final int scheduleIndex, int delay)
	{
		f_delayedEventsExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				addGameEvent(event, scheduleIndex);
			}
		}, delay, TimeUnit.MILLISECONDS);
	}
	
	//
	//Login
	//
	
	private void saveProfileToDb(PlayerProfile profile) {
		String sql = DBQueryManager.SqlUpdateUserDataWhenUserDisconnects;
		f_dbQueryManager.ScheduleUpdateQuery(sql, new Object[] {
				profile.getExperience(),
				profile.getEnergy(),
				profile.getNick(),
				profile.getAuraOne(),
				profile.getAuraTwo(),
				profile.getAuraThree(),
				profile.getRightHandItem(),
				profile.getCurrentBomberId(),
				profile.getPhoto(),
				profile.getLastLogin(),
				profile.getLastLevelReward(),
				profile.getTrainingStatus(),
				profile.getId()
		});
		f_dbQueryManager.ScheduleUpdateQuery(
				DBQueryManager.SqlUpdatePlayerItems, new Object[] { 
				profile.getItemsData().toJson(), 
				profile.getId() 
			});
		f_dbQueryManager.ScheduleUpdateQuery(
				DBQueryManager.SqlUpdatePlayerCustomParameters, new Object[] { 
				profile.getCustomParametersData().toJson(), 
				profile.getId() 
			});
	}
	
	private PlayerProfile loadProfileFromDb(User user) {
		String userId = user.getName();
		PlayerProfile profile = null;
		Connection conn = null;
		try {
			try {
				conn = getParentZone().getDBManager().getConnection();
				
				PreparedStatement st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerData);
				st.setString(1, userId);
				SFSArray profileData = SFSArray.newFromResultSet(st.executeQuery());
				if (profileData.size() == 0) {
					conn.setAutoCommit(false);
					
					st = conn.prepareStatement(DBQueryManager.SqlInsertPlayerData);
					st.setString(1, userId);
					st.executeUpdate();
					
					String dummyJson = SFSArray.newInstance().toJson();
					st = conn.prepareStatement(DBQueryManager.SqlInsertPlayerLocations);
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
//					st = conn.prepareStatement(DBQueryManager.SqlInsertPlayerBombers);
//					st.setString(1, userId);
//					st.setString(2, dummyJson);
//					st.executeUpdate();
					
				    st = conn.prepareStatement(DBQueryManager.SqlInsertPlayerItems);
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
				    st = conn.prepareStatement(DBQueryManager.SqlInsertPlayerMedals);
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
				    st = conn.prepareStatement(DBQueryManager.SqlInsertCustomParameters);
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
					conn.commit();
					conn.setAutoCommit(true);
					
					profile = new PlayerProfile(userId);
				} else {
					st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerLocations);
					st.setString(1, userId);
					ISFSArray locationsData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("LocationsOpen")
					);
					
//					st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerBombers);
//					st.setString(1, userId);
//					ISFSArray bombersData = SFSArray.newFromJsonData(
//						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("BombersOpen")
//					);
					
					st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerItems);
					st.setString(1, userId);
					ISFSArray itemsData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("WeaponsOpen")
					);
					
					st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerMedals);
					st.setString(1, userId);
					ISFSArray medalsData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("Medals")
					);
					
					st = conn.prepareStatement(DBQueryManager.SqlSelectCustomParameters);
					st.setString(1, userId);
					ISFSArray customParametersData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("CustomParameters")
					);
					
					profile = new PlayerProfile(
							profileData, 
							locationsData, 
							itemsData, 
							medalsData, 
							customParametersData
						);
				}
				
			}
			catch (Exception ex) {
				trace (ExtensionLogLevel.ERROR, "Something bad happened during user load, user login = " + userId);
				trace(ExtensionLogLevel.ERROR, ex.toString());
				trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
				
				if (conn != null && conn.getAutoCommit() == false) {
					conn.rollback();
				}
				profile = null;
			} finally {
				if (conn != null) {
					conn.setAutoCommit(true);
					conn.close();
				}
			}
		} 
		catch (Exception ex) {
			trace (ExtensionLogLevel.ERROR, "Something VERY bad happened during user load, user login = " + userId);
			trace(ExtensionLogLevel.ERROR, ex.toString());
			trace(ExtensionLogLevel.ERROR, (Object[])ex.getStackTrace());
			
			profile = null;
		}
		return profile;
	}
	
	public void loginUser(User user) {
		String userId =  user.getName();
		PlayerProfile profile = null;
		
		if (IsReleaseMode && f_profileCache.containsKey(userId))
		{
			profile = f_profileCache.get(userId);
		}
		else
		{			
			profile = loadProfileFromDb(user);
			if (profile == null) {
				user.disconnect(ClientDisconnectionReason.UNKNOWN);
			} else {
				f_profileCache.put(userId, profile);
			}
		}
		if (profile != null) {
			f_profiles.put(user, profile);
			
			profile.getEnergy();
			profile.setMissionToken(InterfaceManager.C_DefaultMissionToken);
			
			ISFSObject params = profile.toSFSObject();
			
			trace(ExtensionLogLevel.WARN, "User login, " + userId);
			trace(ExtensionLogLevel.WARN, params.toJson());
			
			params.putSFSObject("Pricelist", f_pricelistManager.toSFSObject());
			params.putSFSArray("MissionRecords", f_recordsManager.getMissionRecordsData());
			
			send("interface.gameProfileLoaded", params, user);	
		}
	}
	
	public void processUserLeave(User user) {
		trace(ExtensionLogLevel.WARN, "User leave, login = ", user.getName());
		PlayerProfile profile = getUserProfile(user);
		Reward sessionReward = profile.getSessionReward();
		if (sessionReward.isEmpty() == false) {
			getDbManager().ScheduleUpdateQuery(
					DBQueryManager.SqlAddPlayerResources, new Object[] {
					sessionReward.getGoldReward(),
					sessionReward.getCrystalReward(),
					sessionReward.getAdamantiumReward(),
					sessionReward.getAntimatterReward(),
					sessionReward.getEnergyReward(),
					profile.getId()
				});
			profile.removeSessionReward();
		}
		saveProfileToDb(profile);
		f_profiles.remove(user);
	}
	
	public PlayerProfile getUserProfile(User user) {
		return f_profiles.get(user);
	}
	
	//
	//Game manager
	//
	
	public void findGameName(User user) {
		SFSObject params = new SFSObject();
		params.putUtfString("interface.gameManager.findGameName.result.fields.gameName", findGameNameInternal());
		send("interface.gameManager.findGameName.result", params, user);
	}
	
	private String findGameNameInternal() {
		return "r" + (System.currentTimeMillis() / 1000) + "_" + (int)(Math.random() * 10000.0);
	}
	
	public void fastJoin(User user) {
		PlayerProfile profile = getUserProfile(user);
		List<Room> rooms = new ArrayList<Room>(getParentZone().getRoomListFromGroup(C_GameGroupId));
		List<Integer> locations = Locations.findBestLocations(profile);
		List<Room> candidates = new ArrayList<Room>();
		
		//Find room with minimal experience difference
		//Room bestRoom = null;
		//int currentMinExpDiff = Integer.MAX_VALUE;
		Room emptyRoom = null;
		for (Room room : rooms) {
			if (room.isFull() || room.isPasswordProtected()) continue;
			BombersGame game = (BombersGame)room.getExtension();
			if (game.isGameStarted()) continue;
			if (locations.contains(game.getLocationId())) {
				if (room.isEmpty()) { emptyRoom = room; continue; }
//				int expDiff = game.getAbsoluteExperienceDifference(profile.getExperience());
//				if (expDiff < currentMinExpDiff) {
//					currentMinExpDiff = expDiff;
//					bestRoom = room;
//				}
				candidates.add(room);
			}
		}
		try {
			//if (bestRoom != null) {
			//	getApi().joinRoom(user, bestRoom);
			if (candidates.isEmpty() == false) {
				getApi().joinRoom(user, candidates.get((int)(Math.random() * candidates.size())));
			} else if (emptyRoom != null) {
				getApi().joinRoom(user, emptyRoom);
			}
			else{
				createGameInternal(
						user, 
						locations.get((int)(locations.size() * Math.random())), 
						findGameNameInternal(), 
						"",
						false
					);
			}
		} 
		catch (SFSException ex) {
			SFSObject params = new SFSObject();
			params.putBool("interface.gameManager.fastJoin.result.fields.status", false);
			send("interface.gameManager.fastJoin.result", params, user);
		}
	}
	
	public void fastJoin(User user, int locationId) {
		PlayerProfile profile = getUserProfile(user);
		if (profile.isLocationOpened(locationId) == false) return;
		List<Room> rooms = new ArrayList<Room>(getParentZone().getRoomListFromGroup(C_GameGroupId));
		
		//Find room with minimal experience difference
		Room bestRoom = null;
		int currentMinExpDiff = Integer.MAX_VALUE;
		for (Room room : rooms) {
			if (room.isFull() || room.isPasswordProtected()) continue;
			BombersGame game = (BombersGame)room.getExtension();
			if (game.isGameStarted()) continue;
			if (game.getLocationId() == locationId) {
				int expDiff = game.getAbsoluteExperienceDifference(profile.getExperience());
				if (expDiff < currentMinExpDiff) {
					currentMinExpDiff = expDiff;
					bestRoom = room;
				}
			}
		}
		try {
			if (bestRoom != null) {
				getApi().joinRoom(user, bestRoom);
			} else {
				createGameInternal(
						user, 
						locationId, 
						findGameNameInternal(), 
						"",
						false
					);
			}
		} 
		catch (SFSException ex) {
			SFSObject params = new SFSObject();
			params.putBool("interface.gameManager.fastJoin.result.fields.status", false);
			send("interface.gameManager.fastJoin.result", params, user);
		}
	}
	
	public void fastJoin(User user, String gameName, String password) {
		try { 
			PlayerProfile profile = getUserProfile(user);
			Room room = getParentZone().getRoomByName(gameName);
			BombersGame game = (room == null) ? null : (BombersGame)room.getExtension();
			if (room == null || game.isGameStarted() || !profile.isLocationOpened(game.getLocationId())) {
				SFSObject params = new SFSObject();
				params.putBool("interface.gameManager.fastJoin.result.fields.status", false);
				send("interface.gameManager.fastJoin.result", params, user);
				return;
			}
			if (password.isEmpty()) {
				getApi().joinRoom(user, room);
			} else {
				getApi().joinRoom(user, room, password, false, null);
			}
		}
		catch (SFSJoinRoomException ex) {
			SFSObject params = new SFSObject();
			params.putBool("interface.gameManager.fastJoin.result.fields.status", false);
			send("interface.gameManager.fastJoin.result", params, user);
		}
	}
	
	public void createGame(User user, int locationId, String gameName, String password) {
		try {
			PlayerProfile profile = getUserProfile(user);
			if (profile.isLocationOpened(locationId)) {
				createGameInternal(user, locationId, gameName, password, true);
			}
		}
		catch (SFSException ex) {
			SFSObject params = new SFSObject();
			params.putBool("interface.gameManager.createGame.result.fields.status", false);
			send("interface.gameManager.createGame.result", params, user);
		}
	}                                                                                    
	
	private void createGameInternal(User user, int locationId, String gameName, String password, boolean createByUser) 
		throws SFSCreateRoomException, SFSJoinRoomException {
		CreateRoomSettings settings = new CreateRoomSettings();
		RoomExtensionSettings extensionSettings 
			= new RoomExtensionSettings("bombers", "com.vensella.bombers.game.BombersGame");
		settings.setExtension(extensionSettings);
		settings.setName(gameName);
		if (!createByUser)
			settings.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
		else
			settings.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY_AND_CREATOR_IS_GONE);
		settings.setMaxUsers(4);
		if (password.isEmpty() == false) {
			settings.setPassword(password);
			settings.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY_AND_CREATOR_IS_GONE);
		}
		settings.setGroupId(C_GameGroupId);
		settings.setGame(true);	
		settings.setDynamic(true);
		
		List<RoomVariable> roomVariables = new ArrayList<RoomVariable>();
		RoomVariable locationVariable = new SFSRoomVariable("LocationId", locationId, false, true, true);
		RoomVariable scheduleIndexVariable = new SFSRoomVariable("ScheduleIndex", GamesCount++, false, true, true);
		RoomVariable isGameStartedVariable = new SFSRoomVariable("IsGameStarted", false, false, true, true);
		roomVariables.add(locationVariable);
		roomVariables.add(scheduleIndexVariable);
		roomVariables.add(isGameStartedVariable);
		settings.setRoomVariables(roomVariables);

		getApi().createRoom(getParentZone(), settings, user, true, null);
	}
	
	//
	//Wall manager
	//
	
	public boolean isUserRegistered(String login) {
		if (f_profileCache.containsKey(login)) {
			return true;
		}
		Connection conn = null;
		try {
			conn = getParentZone().getDBManager().getConnection();
			PreparedStatement st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerExistance);
			st.setString(1, login);
			SFSArray data = SFSArray.newFromResultSet(st.executeQuery());
			ISFSObject row = data.getSFSObject(0);
			return row.getLong("C") > 0;
		}
		catch (Exception e) {
			trace(ExtensionLogLevel.ERROR, "While cheking isUserRegistered for login = " + login);
			trace(ExtensionLogLevel.ERROR, e.toString());
			trace(ExtensionLogLevel.ERROR, (Object[])e.getStackTrace());			
			return true;
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					trace(ExtensionLogLevel.ERROR, "While cheking isUserRegistered (second stage) for login = " + login);
					trace(ExtensionLogLevel.ERROR, e.toString());
					trace(ExtensionLogLevel.ERROR, (Object[])e.getStackTrace());
				}
			}
		}
	}
	
	//Admin tools
	
	private boolean isAdmin(String login) {
		return UserLoginEventHandler.isTestLogin(login);
	}
	
	public void adminForceMapsReload(User user) {
		if (isAdmin(user.getName())) {
			trace(ExtensionLogLevel.WARN, "Reloading MAP manager!");
			
			f_mapManager = new MapManager(this);
		}
	}
	
	public void adminForcePricelistReload(User user) {
		if (isAdmin(user.getName())) {
			trace(ExtensionLogLevel.WARN, "Reloading PRICELIST manager!");
			
			f_pricelistManager = new PricelistManager(this);
		}		
	}
	
	public void adminResetProfile(User user) {
		PlayerProfile emptyProfile = new PlayerProfile(user.getName());
		f_profiles.put(user, emptyProfile);
		f_profileCache.put(user.getName(), emptyProfile);
		getApi().disconnectUser(user);
	}
	
}
