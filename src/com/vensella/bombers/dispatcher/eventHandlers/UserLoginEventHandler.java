package com.vensella.bombers.dispatcher.eventHandlers;

import java.math.BigInteger;
import java.security.MessageDigest;

import com.smartfoxserver.bitswarm.sessions.Session;
import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.annotations.Instantiation.InstantiationMode;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.exceptions.*;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.vensella.bombers.dispatcher.BombersDispatcher;

@Instantiation(InstantiationMode.SINGLE_INSTANCE)
public class UserLoginEventHandler extends BaseServerEventHandler {

	//Constants
	
	private static final String C_ApiId = "2027731";
	private static final String C_ApiSecret = "zzeDxfsoN98huG0tcfvD";
	
	//Methods
	
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		BombersDispatcher dispatcher = (BombersDispatcher)getParentExtension();
		
		String login = (String)event.getParameter(SFSEventParam.LOGIN_NAME);
		String password = (String)event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		String hashText = "";
		
		try
		{
			MessageDigest md5 = dispatcher.getMD5();
			md5.reset();
			
			byte[] hashed = md5.digest((C_ApiId + "_" + login + "_" + C_ApiSecret).getBytes("UTF-8"));
			BigInteger bigInt = new BigInteger(1, hashed);
			hashText = bigInt.toString(16);
			while(hashText.length() < 32 ){
				hashText = "0" + hashText;
			}
		}
		catch (Exception ex)
		{
			hashText = "";
		}
		
		Session s = (Session)event.getParameter(SFSEventParam.SESSION);
		
		//if (getApi().checkSecurePassword(s, hashText, password) == false)
		if (getApi().checkSecurePassword(s, login, password) == false)
		{
	        SFSErrorData errData = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
	        errData.addParameter(login);
		    trace("[Notice] User " + login + " attemted to login with password " + password);
		    throw new SFSLoginException("Bad login-password pair", errData);
		}	
	}

}
