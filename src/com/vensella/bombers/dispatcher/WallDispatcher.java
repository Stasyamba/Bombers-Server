package com.vensella.bombers.dispatcher;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.vensella.bombers.dispatcher.eventHandlers.UserJoinWallZoneEventHandler;
import com.vensella.bombers.dispatcher.eventHandlers.UserLoginEventHandler;
import com.vensella.bombers.dispatcher.eventHandlers.WallSubmitPrizeEventHandler;

public class WallDispatcher extends SFSExtension {

	//Fields
	
	private BombersDispatcher f_bombersDispatcher;
	
	//Constructors and initializers
	
	@Override
	public void init() {
		//Initialize fields
		
		//Add event handlers
		
		addEventHandler(SFSEventType.USER_LOGIN, UserLoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, UserJoinWallZoneEventHandler.class);
		
		addRequestHandler("bombersWall.submitPrize", WallSubmitPrizeEventHandler.class);
		
		trace("Wall dispatcher init()");
	}
	
	//Methods
	
	private BombersDispatcher getBombersDispatcher() {
		if (f_bombersDispatcher == null) {
			f_bombersDispatcher =
				(BombersDispatcher)SmartFoxServer.getInstance().getZoneManager().getZoneByName("bombers").getExtension();	
		}
		return f_bombersDispatcher;
	}
	
	public void loginUser(User user) {
		boolean isRegistered = getBombersDispatcher().isUserRegistered(user.getName());
		trace("User " + user.getName() + " is registered = " + isRegistered);
		SFSObject params = new SFSObject();
		params.putBool("IsRegistered", isRegistered);
		send("bombersWall.isRegisteredLoaded", params, user);
	}
	
	public void submitPrize(User user, String postCreatorId) {
		trace ("User " + user.getName() + "submits prize, post creator id = " + postCreatorId);
		SFSObject prize = getRandomPrize();
		prize.putUtfString("PostCreatorId", postCreatorId);
		getBombersDispatcher().addPrize(user.getName(), prize);
		String sql = DBQueryManager.SqlInsertPrizeFromWall;
		getBombersDispatcher().getDbManager().ScheduleUpdateQuery(sql, new Object[] {
			postCreatorId,
			user.getName(),
			prize.toJson()
		});
	}
	
	private SFSObject getRandomPrize() {
		SFSObject prize = new SFSObject();
		prize.putInt("rc0", (int)(10 * Math.random() + 6.0));
		prize.putInt("rc1", (int)(3 * Math.random()));
		prize.putInt("rc2", (int)(1 * Math.random() + 0.00015));
		prize.putInt("rc3", (int)(1 * Math.random() + 0.00005));
		return prize;
	}

}