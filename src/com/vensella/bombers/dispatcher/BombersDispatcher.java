package com.vensella.bombers.dispatcher;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;

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
	
	
	//Flags
	
	private static Boolean IsReleaseMode = false;
	//private static Boolean IsDebugMode = true;
	
	//Special
	
	private MessageDigest MD5; 
	
	//Constants
	
	private static final String C_GameGroupId = "Games";
	
	private static final int C_WorkingThreadCount = 4;
	private static final int C_DelayedEventTimersCount = 1;
	
	//Special volatile fields
	
	private volatile int TicksCount = 0;
	private volatile int GamesCount = 0;
	
	//Event model fields
	
	private Timer f_ticksTimer;
	private ScheduledThreadPoolExecutor f_delayedEventsExecutor;
	
	private Thread[] f_workingThreads;
	private LinkedBlockingQueue<GameEvent>[] f_workingQueues;
	//private Timer[] f_delayedEventsTimers;
	
	//Managers
	
	private DBQueryManager f_dbQueryManager;
	private InterfaceManager f_interfaceManager;
	private MoneyManager f_moneyManager;
	private MapManager f_mapManager;
	private PricelistManager f_pricelistManager;
	
	//User tracking
	
	private Map<String, PlayerProfile> f_profileCache;
	private Map<User, PlayerProfile> f_profiles;
	
	//Constructors and initializers
	
	@SuppressWarnings("unchecked")
	@Override
	public void init()
	{ 
		trace("Bombers zone dispatcher init() start");
		
		//Initialize fields
		
		//f_roomParametres = new ConcurrentHashMap<String, Integer>();
		
		f_dbQueryManager = new DBQueryManager(this);
		f_interfaceManager = new InterfaceManager(this);
		f_moneyManager = new MoneyManager(this);
		f_mapManager = new MapManager(this);
		f_pricelistManager = new PricelistManager(this);
		
		f_profileCache = new ConcurrentHashMap<String, PlayerProfile>();
		f_profiles = new ConcurrentHashMap<User, PlayerProfile>();
		
		//Initialize special fields
		
		try
		{
			MD5 = MessageDigest.getInstance("MD5");
		}
		catch (Exception ex)
		{
			trace("[ERROR] MD5 can't be initialized!");
			trace(ex.getMessage());
			trace((Object[])ex.getStackTrace());
		}
		
		
		//Initialize of internal structure
		
		f_ticksTimer = new Timer("TicksTimer");
		f_ticksTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TicksCount++;
			}
		}, 0, 1000);
		
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
							if (event != null && event.getCurrentGameId() == event.getEventGameId())
								event.Apply();
							else {
								trace ("[Notice] Event dropped by kernel");
							}
						} catch (Exception ex) {
							//TODO: Add trace attributes
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
		
//		addRequestHandler("interface.setAura", null);
//		addRequestHandler("interface.setRightHandItem", null);	
		addRequestHandler("interface.setBomber", InterfaceSetBomberEventHandler.class);	
		addRequestHandler("interface.setNick", InterfaceSetNickEventHandler.class);
		addRequestHandler("interface.setPhoto", InterfaceSetPhotoEventHandler.class);
		
//		addRequestHandler("inerface.openLocation", null);
//		addRequestHandler("inerface.playSingleGame", null);
//		addRequestHandler("inerface.takeSingleGamePrize", null);

		
		
		 		
		trace("Bombers zone dispatcher init() end");
	}

	@Override
	public void destroy()
	{
		//TODO: Inform users about server reset
		//TODO: Free all resources (threads, timers, etc..)
		super.destroy();
		trace("Bombers zone dispatcher destroy()");
	}
	
	//Special methods
	
	public int getTicksCount() { return TicksCount;	}
	
	public MessageDigest getMD5() { return MD5;	}
	
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
	
	private void saveProfileToDb(User user) {
		PlayerProfile profile = getUserProfile(user);
		String sql = DBQueryManager.SqlUpdateUserDataWhenUserDisconnects;
		f_dbQueryManager.ScheduleUpdateQuery(sql, new Object[] {
				profile.getExperience(),
				profile.getNick(),
				profile.getAuraOne(),
				profile.getAuraTwo(),
				profile.getAuraThree(),
				profile.getRightHandItem(),
				profile.getCurrentBomberId(),
				profile.getPhoto(),
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
					
					st = conn.prepareStatement(DBQueryManager.SqlInsertPlayerBombers);
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
				    st = conn.prepareStatement(DBQueryManager.SqlInsertPlayerItems);
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
					conn.commit();
					
					profile = new PlayerProfile(userId);
				} else {
					st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerLocations);
					st.setString(1, userId);
					ISFSArray locationsData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("LocationsOpen")
					);
					
					st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerBombers);
					st.setString(1, userId);
					ISFSArray bombersData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("BombersOpen")
					);
					
					st = conn.prepareStatement(DBQueryManager.SqlSelectPlayerItems);
					st.setString(1, userId);
					ISFSArray itemsData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("WeaponsOpen")
					);
					
					profile = new PlayerProfile(profileData, locationsData, itemsData, bombersData);
				}
			}
			catch (Exception ex) {
				trace ("Something bad happened during user load =(");
				trace(ex.toString());
				trace((Object[])ex.getStackTrace());
				
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
			trace ("Something VERY bad happened during user load =(");
			trace(ex.toString());
			trace((Object[])ex.getStackTrace());
			
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
			
			ISFSObject params = profile.toSFSObject();
			trace(params.toJson());
			params.putSFSObject("Pricelist", f_pricelistManager.toSFSObject());
			send("interface.gameProfileLoaded", params, user);
		}
	}
	
	public void processUserLeave(User user) {
		saveProfileToDb(user);
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
		return "r" + TicksCount + "_" + (int)(Math.random() * 10000.0);
	}
	
	public void fastJoin(User user) {
		PlayerProfile profile = getUserProfile(user);
		List<Room> rooms = new ArrayList<Room>(getParentZone().getRoomListFromGroup(C_GameGroupId));
		List<Integer> locations = Locations.findBestLocations(profile);
		
		//Find room with minimal experience difference
		
		Room bestRoom = null;
		int currentMinExpDiff = Integer.MAX_VALUE;
		for (Room room : rooms) {
			if (room.isFull() || room.isPasswordProtected()) continue;
			BombersGame game = (BombersGame)room.getExtension();
			if (game.isGameStarted()) continue;
			if (locations.contains(game.getLocationId())) {
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
		//TODO: Fast join on location
		SFSObject params = new SFSObject();
		params.putBool("interface.gameManager.fastJoin.result.fields.status", false);
		send("interface.gameManager.fastJoin.result", params, user);
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
			settings.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
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
	
	
	
}
