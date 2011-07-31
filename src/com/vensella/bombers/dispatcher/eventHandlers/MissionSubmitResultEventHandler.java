package com.vensella.bombers.dispatcher.eventHandlers;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class MissionSubmitResultEventHandler extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		String missionId = params.getUtfString("interface.missions.submitResult.f.missionId");
		int token = params.getInt("interface.missions.submitResult.f.token");;
		boolean isBronze = params.getBool("interface.missions.submitResult.f.isBronze");
		boolean isSilver = params.getBool("interface.missions.submitResult.f.isSilver");
		boolean isGold = params.getBool("interface.missions.submitResult.f.isGold");
		int missionTime = params.getInt("interface.missions.submitResult.f.result");
		
		dispatcher.getInterfaceManager().submitMissionResult(user, token, missionId, isBronze, isSilver, isGold, missionTime);
	}

}
