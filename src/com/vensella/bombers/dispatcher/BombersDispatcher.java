package com.vensella.bombers.dispatcher;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.CreateRoomSettings.RoomExtensionSettings;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
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
	
	private static final int C_WorkingThreadCount = 2;
	//private static final int C_DelayedEventTimersCount = 1;
	
	//Special volatile fields
	
	private volatile int TicksCount = 0;
	private volatile int GamesCount = 0;
	
	//Event model fields
	
	private Timer f_ticksTimer;
	
	private Thread[] f_workingThreads;
	private LinkedBlockingQueue<GameEvent>[] f_workingQueues;
	//private Timer[] f_delayedEventsTimers;
	
	//Managers
	
	private DBQueryManager f_dbQueryManager;
	private InterfaceManager f_interfaceManager;
	private MoneyManager f_moneyManager;
	private MapManager f_mapManager;
	
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
	
	//Event dispatching
	
	public void addGameEvent(GameEvent event, int scheduleIndex)
	{
		f_workingQueues[scheduleIndex % C_WorkingThreadCount].add(event);
	}
	
	public void addDelayedGameEvent(final GameEvent event, final int scheduleIndex, int delay)
	{
		SmartFoxServer sfs = SmartFoxServer.getInstance();	
		sfs.getTaskScheduler().schedule(new Runnable() {
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
		String sql = "update `Users` set `Experience` = ?, " +
				"`Nick` = ?, `AuraOne` = ?, `AuraTwo` = ?, `AuraThree` = ?, " +
				"`RightHand` = ?, `BomberId` = ?, `Photo` = ? where `Id` = ?";
		f_dbQueryManager.ScheduleUpdateQuery(null, sql, new Object[] {
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
				
				PreparedStatement st = conn.prepareStatement("select * from `Users` where `Id` = ?");
				st.setString(1, userId);
				SFSArray profileData = SFSArray.newFromResultSet(st.executeQuery());
				if (profileData.size() == 0) {
					conn.setAutoCommit(false);
					
					st = conn.prepareStatement("insert into `Users` (`Id`) values (?)");
					st.setString(1, userId);
					st.executeUpdate();
					
					String dummyJson = SFSArray.newInstance().toJson();
					st = conn.prepareStatement("insert into `LocationsOpen` (`UserId`, `LocationsOpen`) values (?, ?)");
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
					st = conn.prepareStatement("insert into `BombersOpen` (`UserId`, `BombersOpen`) values (?, ?)");
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
				    st = conn.prepareStatement("insert into `WeaponsOpen` (`UserId`, `WeaponsOpen`) values (?, ?)");
					st.setString(1, userId);
					st.setString(2, dummyJson);
					st.executeUpdate();
					
					conn.commit();
					
					profile = new PlayerProfile(userId);
				} else {
					st = conn.prepareStatement("select * from `LocationsOpen` where `UserId` = ?");
					st.setString(1, userId);
					ISFSArray locationsData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("LocationsOpen")
					);
					
					st = conn.prepareStatement("select * from `BombersOpen` where `UserId` =  ?");
					st.setString(1, userId);
					ISFSArray bombersData = SFSArray.newFromJsonData(
						SFSArray.newFromResultSet(st.executeQuery()).getSFSObject(0).getUtfString("BombersOpen")
					);
					
					st = conn.prepareStatement("select * from `WeaponsOpen` where `UserId` = ?");
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
			ISFSObject params = profile.toSFSObject();
			trace("User logged in");
			trace(params.toJson());
		
			f_profiles.put(user, profile);
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
		params.putUtfString("interface.gameManager.findGameName.result.gameName", findGameNameInternal());
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
		//TODO: Add features
		
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
						""
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
			createGameInternal(user, locationId, gameName, password);
		}
		catch (SFSException ex) {
			SFSObject params = new SFSObject();
			params.putBool("interface.gameManager.createGame.result.fields.status", false);
			send("interface.gameManager.createGame.result", params, user);
		}
	}                                                                                    
	
	private void createGameInternal(User user, int locationId, String gameName, String password) 
		throws SFSCreateRoomException, SFSJoinRoomException {
		CreateRoomSettings settings = new CreateRoomSettings();
		RoomExtensionSettings extensionSettings 
			= new RoomExtensionSettings("bombers", "com.vensella.bombers.game.BombersGame");
		settings.setExtension(extensionSettings);
		settings.setName(findGameNameInternal());
		settings.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY_AND_CREATOR_IS_GONE);
		settings.setMaxUsers(4);
		if (password.isEmpty() == false) {
			settings.setPassword(password);
			settings.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
		}
		settings.setGroupId(C_GameGroupId);
		settings.setGame(true);		
		
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
