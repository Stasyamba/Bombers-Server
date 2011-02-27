package com.vensella.bombers.game;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.extensions.SFSExtension;

import com.vensella.bombers.dispatcher.*;

import com.vensella.bombers.game.eventHandlers.*;
import com.vensella.bombers.game.mapObjects.DynamicGameMap;


public class BombersGame extends SFSExtension {

	//Fields
	
	private BombersDispatcher f_dispatcher;
	private int f_scheduleIndex;
	private int f_gameId = GameEvent.INVALID_GAME_ID;
	private int f_gamesCount = 0;
	private volatile int f_currentPlayerRank = 100;
	
	private ArrayList<PlayerGameProfile> f_dieSequence;
	
	private int f_locationId;
	private DynamicGameMap f_gameField;
	
	private WeaponsManager f_weaponsManager;
	private DynamicObjectManager f_dynamicObjectManager;
	
	private Map<User, PlayerProfile> f_players;
	private Map<User, PlayerGameProfile> f_gameProfiles;
	
	private String[] f_slotsToIds;
	
	private Object lock;
	//private Lock CriticalSection;
	private volatile int f_10secondToStart = 0;
	private boolean f_isGameStarted = false;
	
	//Override methods
	
	@Override
	public void init() {
		//CriticalSection = new ReentrantLock();
		lock = new Object();
		
		f_locationId = getParentRoom().getVariable("LocationId").getIntValue();
		f_scheduleIndex = getParentRoom().getVariable("ScheduleIndex").getIntValue();
		
		f_dispatcher = (BombersDispatcher)getParentRoom().getZone().getExtension();
		//f_dispatcherExtension = getParentRoom().getZone().getExtension();
		
		f_weaponsManager = new WeaponsManager(this);
		f_dynamicObjectManager = new DynamicObjectManager(this);
		
		f_dieSequence = new ArrayList<PlayerGameProfile>();
		f_players = new ConcurrentHashMap<User, PlayerProfile>();
		f_gameProfiles = new ConcurrentHashMap<User, PlayerGameProfile>();
		
		f_slotsToIds = new String[getParentRoom().getCapacity()];
		for (int i = 0; i < f_slotsToIds.length; ++i) f_slotsToIds[i] = null;
		
		//Initialize system event handlers
		
		addEventHandler(SFSEventType.USER_JOIN_ROOM, UserJoinRoomEventHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, UserLeaveRoomEventHandler.class);
		
		//Initialize custom request handlers
		
		addRequestHandler("game.lobby.userReady", LobbyUserReadyEventHandler.class);
		
		addRequestHandler("game.IDC", GameInputDirectionChangedEventHandler.class);
		addRequestHandler("game.damagePlayer", GameDamagePlayerEventHandler.class);
		
		addRequestHandler("game.AW", GameActivateWeaponEventHandler.class);
		addRequestHandler("game.actDO", GameActivateDynamicObject.class);
		
	}
	
	//Properties
	
	public int getLocationId() { return f_locationId; }
	
	public boolean isGameStarted() { return f_isGameStarted; }
	
	public int getGameId() { return f_gameId; }
	
	public int alivePlayersCount() { 
		int count = 0; 
		for (PlayerGameProfile gameProfile : f_gameProfiles.values()) {
			if (gameProfile.isAlive()) count++;
		}
		return count;
	}
	
	public DynamicGameMap getGameMap() { return f_gameField; }
	
	//Methods
	
	public PlayerGameProfile getGameProfile(User user) {
		return f_gameProfiles.get(user);
	}
	
	public int getAbsoluteExperienceDifference(int experience) {
		if (f_players.size() == 0) {
			return Integer.MAX_VALUE - 1;
		}
		int sum = 0;
		for (PlayerProfile profile : f_players.values()) {
			sum += profile.getExperience();
		}
		return Math.abs(sum / f_players.size() - experience);
	}
	
	private void adjustPlayerExperience(PlayerGameProfile profile, int rank) {
		profile.getBaseProfile().addExperience(f_gameProfiles.size() - rank);
	}
	
	private void savePlayerGameResultToDb(PlayerGameProfile profile) {
		String sql = DBQueryManager.SqlUpdatePlayerItems;
		f_dispatcher.getDbManager().ScheduleUpdateQuery(sql, new Object[]{ 
				profile.getBaseProfile().getItemsData().toJson(),
				profile.getBaseProfile().getId()
		});
	}
	
	private int putToSlot(String userId) {
		for (int i = 0; i < f_slotsToIds.length; ++i) {
			if (f_slotsToIds[i] == null) {
				f_slotsToIds[i] = userId;
				return i;
			}
		}	
		return -1;
	}
	
	private void removeFromSlot(String userId) {
		for (int i = 0; i < f_slotsToIds.length; ++i) {
			if (f_slotsToIds[i] == userId) {
				f_slotsToIds[i] = null;
			}
		}		
	}
	
	private int getSlot(String userId) {
		for (int i = 0; i < f_slotsToIds.length; ++i) {
			if (f_slotsToIds[i] == userId) {
				return i;
			}
		}
		return -1;
	}
	
	//Join/leave room
	
	private SFSArray getLobbyProfiles() {
		SFSArray usersInfo = new SFSArray();
		for (Entry<User, PlayerProfile> profile : f_players.entrySet()) {
			SFSObject userInfo = new SFSObject();
			userInfo.putUtfString("Id", profile.getValue().getId());
			userInfo.putInt("Experience", profile.getValue().getExperience());
			userInfo.putUtfString("Nick", profile.getValue().getNick());
			userInfo.putUtfString("Photo", profile.getValue().getPhoto());
			userInfo.putBool("IsReady", f_gameProfiles.containsKey(profile.getKey()));
			synchronized (lock) {
				userInfo.putInt("Slot", getSlot(profile.getKey().getName()));
			}
			usersInfo.addSFSObject(userInfo);
		}
		return usersInfo;
	}
	
	public void processUserJoin(User user) {
		if (f_isGameStarted == false)
		{
			f_players.put(user, f_dispatcher.getUserProfile(user));
			synchronized (lock) {
				putToSlot(user.getName());
			}
			
			SFSObject params = new SFSObject();
			params.putInt("LocationId", f_locationId);
			send("game.lobby.location", params, user);
			
			SFSArray usersInfo = null;
			synchronized (lock) {
				usersInfo = getLobbyProfiles();
			}
			params = new SFSObject();
			params.putSFSArray("profiles", usersInfo);
			send("game.lobby.playersProfiles", params, getParentRoom().getUserList());
			setUserReady(user, false);
		} else {
			getApi().leaveRoom(user, getParentRoom());
		}
	}
	
	public void processUserLeave(User user) {
		f_players.remove(user);
		synchronized (lock) {
			removeFromSlot(user.getName());
		}
		if (f_isGameStarted)
		{
			PlayerGameProfile player = getGameProfile(user);
			if (player != null && player.isAlive()) {
				killPlayer(player);
			}
		}
		setUserReady(user, false);
	}
	
	//Methods for Lobby
	
	public void setUserReady(User user, boolean isReady) {
		//CriticalSection.lock();
		synchronized (lock) {
			if (f_isGameStarted) {
				return;
			}
			f_10secondToStart++;	
		}
		//CriticalSection.unlock();
		
		final int situationId = f_10secondToStart;
		
		if (isReady) {
			PlayerGameProfile gameProfile = new PlayerGameProfile(user, f_players.get(user));
			f_gameProfiles.put(user, gameProfile);
		} else {
			f_gameProfiles.remove(user);
		}
		
		SFSObject params = new SFSObject();
		params.putUtfString("Id", user.getName());
		params.putBool("IsReady", isReady);
		send("game.lobby.readyChanged", params, getParentRoom().getPlayersList());
		if (f_gameProfiles.size() == f_players.size() &&  f_gameProfiles.size() >= getParentRoom().getCapacity() / 2) {
			trace("5 seconds to start situation");
			SmartFoxServer.getInstance().getTaskScheduler().schedule(new Runnable() {
				private int f_situationId = situationId;
				@Override
				public void run() {
							//CriticalSection.lock();
							synchronized (lock) {
								if (f_10secondToStart == f_situationId) {
									trace("Starting game because of 5 seconds passed");
									prepareToStartGame();
								} else {
									trace("5 seconds passed, but situation has changed!");
								}	
							}
							//CriticalSection.unlock();
						}
			}, 5000, TimeUnit.MILLISECONDS);
		}
	}
	
	private void prepareToStartGame() {
		if (f_isGameStarted) {
			return;
		}
		
		f_isGameStarted = true;
		
		RoomVariable isGameStartedVariable = new SFSRoomVariable("IsGameStarted", true, false, true, true);
		ArrayList<RoomVariable> roomVariables = new ArrayList<RoomVariable>();
		roomVariables.add(isGameStartedVariable);
		getApi().setRoomVariables(null, getParentRoom(), roomVariables);
		
		//Initialize different data
		
		f_dieSequence = new ArrayList<PlayerGameProfile>(f_gameProfiles.values());
		f_currentPlayerRank = f_gameProfiles.size();
		
		//Initialize map and start locations, and send it
		
		f_gameField = f_dispatcher.getMapManager().getRandomMap(f_locationId, f_gameProfiles.size());
		trace("f_gameField = " + f_gameField.toString());
		f_dynamicObjectManager.setWallBlocksCount(f_gameField.getWallBlocksCount());
		//TODO: Increase room's maximum capacity if necessary
		
		SFSObject params = new SFSObject();
		params.putInt("game.lobby.3SecondsToStart.fields.MapId", f_gameField.getMapId());
		SFSArray gameProfiles = new SFSArray();
		boolean[] locationBusy = new boolean[f_gameField.getMaxPlayers()];
		for (PlayerGameProfile gp : f_gameProfiles.values()) {
			int j = (int)(Math.random() * f_gameField.getMaxPlayers());
			while (locationBusy[j]) { j = (int)(Math.random() * f_gameField.getMaxPlayers()); }
			locationBusy[j] = true;
			
			SFSObject gameProfile = new SFSObject();
			gameProfile.putUtfString("UserId", gp.getUser().getName());
			gameProfile.putInt("StartX", f_gameField.getStartXAt(j));
			gameProfile.putInt("StartY", f_gameField.getStartYAt(j));
			gameProfile.putInt("BomberId", gp.getBomberId());
			gameProfile.putInt("AuraOne", gp.getAuraOne());
			gameProfile.putInt("AuraTwo", gp.getAuraTwo());
			gameProfile.putInt("AuraThree", gp.getAuraThree());
			
			gameProfiles.addSFSObject(gameProfile);
		}
		params.putSFSArray("game.lobby.3SecondsToStart.fields.PlayerGameProfiles", gameProfiles);
		send("game.lobby.3SecondsToStart", params, getParentRoom().getUserList());
		
		//Schedule run game
		
		SmartFoxServer.getInstance().getTaskScheduler().schedule(new Runnable() {
			@Override
			public void run() {
				trace("Starting game");
				startGame();
			}
		}, 3000, TimeUnit.MILLISECONDS);
		
	}
	
	private void startGame() {
		f_gameId = f_gamesCount++;
		
		initializeDeathBlocks();
		
		SFSObject params = new SFSObject();
		send("game.lobby.gameStarted", params, getParentRoom().getUserList());		
	}
	
	private void endGame() {
		//CriticalSection.lock();
		//synchronized (lock) {
		if (f_isGameStarted == false) {
			return;
		}
		f_gameId = GameEvent.INVALID_GAME_ID;

		SFSObject params = new SFSObject();
		if (f_dieSequence.size() > 0) {
			PlayerGameProfile lastMan = f_dieSequence.get(0);
			savePlayerGameResultToDb(lastMan);

			adjustPlayerExperience(lastMan, 1);
			params.putUtfString("game.gameEnded.WinnerId", lastMan.getUser()
					.getName());
			params.putInt("game.gameEnded.WinnerExperience", lastMan
					.getBaseProfile().getExperience());
		}
		// TODO: Flush match results to DB

		f_gameProfiles.clear();
		f_isGameStarted = false;

		RoomVariable isGameStartedVariable = new SFSRoomVariable(
				"IsGameStarted", false, false, true, true);
		ArrayList<RoomVariable> roomVariables = new ArrayList<RoomVariable>();
		roomVariables.add(isGameStartedVariable);
		getApi().setRoomVariables(null, getParentRoom(), roomVariables);

		SFSArray usersInfo = getLobbyProfiles();
		params.putSFSArray("profiles", usersInfo);
		send("game.gameEnded", params, getParentRoom().getPlayersList());
		trace("End game");
		//}
		//CriticalSection.unlock();
	}
	
	//Event model
	
	public void addGameEvent(GameEvent event) {
		f_dispatcher.addGameEvent(event, f_scheduleIndex);
	}
	
	public void addDelayedGameEvent(GameEvent event, int delay) {
		f_dispatcher.addDelayedGameEvent(event, f_scheduleIndex, delay);
	}
	
	//Game process methods
	
	private void initializeDeathBlocks() {
		addDelayedGameEvent(new GameEvent(this) {
			
			//Fields
			
			private int x;
			private int y;
			
			private int direction = 0;
			
			//Methods
			
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				if (map.getObjectTypeAt(x, y) == DynamicGameMap.ObjectType.DEATH_WALL) 	return;

				map.setObjectTypeAt(x, y, DynamicGameMap.ObjectType.DEATH_WALL);

				SFSObject params = new SFSObject();
				params.putInt("x", x);
				params.putInt("y", y);
				game.send("game.deathWallAppeared", params, game.getParentRoom().getPlayersList());					
			
				//RIGHT
				if (direction == 0) {
					if (x == map.getWidth() - 1 || map.getObjectTypeAt(x + 1, y) == DynamicGameMap.ObjectType.DEATH_WALL)
						direction = 1;
					else
						x += 1;
				}
				//DOWN
				if (direction == 1)	{
					if (y == map.getHeight() - 1 || map.getObjectTypeAt(x, y + 1) == DynamicGameMap.ObjectType.DEATH_WALL)
						direction = 2;
					else
						y += 1;						
				}
				//LEFT
				if (direction == 2)	{
					if (x == 0 || map.getObjectTypeAt(x - 1, y) == DynamicGameMap.ObjectType.DEATH_WALL)
						direction = 3;
					else
						x -= 1;						
				}
				//UP
				if (direction == 3)	{
					if (y == 0 || map.getObjectTypeAt(x, y - 1) == DynamicGameMap.ObjectType.DEATH_WALL) {	
						direction = 0;
						x += 1;
					}
					else
						y -= 1;						
				}	
				game.addDelayedGameEvent(this, 1250);
			}
		}, 2*45*1000);
	}
	
	private void killPlayer(PlayerGameProfile player) {
		synchronized (lock) {
			player.setIsAlive(false);
			adjustPlayerExperience(player, f_currentPlayerRank--);
			f_dieSequence.remove(player);
			savePlayerGameResultToDb(player);

			SFSObject params = new SFSObject();
			params.putUtfString("UserId", player.getUser().getName());
			params.putInt("Rank", f_currentPlayerRank + 1);
			params.putInt("Experience", player.getBaseProfile().getExperience());
			send("game.playerDied", params, getParentRoom().getPlayersList());
			if (alivePlayersCount() == 1) {
				endGame();
			}
		}
	}
	
	public void damagePlayer(final User user, final int damage, final int effect, final boolean isDead) {
		final PlayerGameProfile player = getGameProfile(user);
		addGameEvent(new GameEvent(this) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				if (player != null && player.isAlive()) {
					player.addHealth(-damage);
					if (isDead || player.getHealth() <= 0) {
						killPlayer(player);
					} else {
						SFSObject params = new SFSObject();
						params.putUtfString("UserId", user.getName());
						params.putInt("HealthLeft", player.getHealth());
						send("game.playerDamaged", params, getParentRoom().getPlayersList());
					}
				} 
			}
		});
	}
	
	//Weapons & dynamic objects system
	
	public void destroyWallAt(final int x, final int y) {
		if (f_gameField.getObjectTypeAt(x, y) == DynamicGameMap.ObjectType.OUT || 
			f_gameField.getObjectTypeAt(x, y) == DynamicGameMap.ObjectType.EMPTY ||
			f_gameField.getObjectTypeAt(x, y) == DynamicGameMap.ObjectType.DEATH_WALL) {
			return;
		}
			
		if (f_gameField.getDynamicObject(x, y) == null) {
			f_gameField.setDynamicObject(x, y, new DynamicObject(this, false, false) {
				@Override
				public GameEvent getActivateEvent() {
					return null;
				}
			});
		}
		else {
			return;
		}
		
		addDelayedGameEvent(new GameEvent(this) {
			@Override
			protected void ApplyOnGame(BombersGame game, DynamicGameMap map) {
				map.removeDynamicObject(x, y);
				if (map.getObjectTypeAt(x, y) == DynamicGameMap.ObjectType.WALL) {
					f_dynamicObjectManager.possiblyAddRandomBonus(x, y);
				}
				map.setObjectTypeAt(x, y, DynamicGameMap.ObjectType.EMPTY);
			}
		}, 50);
	}
	
	public void activateWeapon(User user, int weaponId, int x, int y) {
		f_weaponsManager.activateWeapon(user, weaponId, x, y);
	}
	
	public void activateDynamicObject(User user, int x, int y) {
		f_dynamicObjectManager.activateDynamicObject(user, x, y);
	}
	

}
