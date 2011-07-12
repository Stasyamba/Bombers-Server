package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class MissionStartEventHandler extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		String missionId = params.getUtfString("interface.missions.start.f.missionId");
		
		dispatcher.getInterfaceManager().startMission(user, missionId);

	}

}
