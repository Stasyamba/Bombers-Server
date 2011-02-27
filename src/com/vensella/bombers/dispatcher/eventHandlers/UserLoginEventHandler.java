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

@Instantiation(InstantiationMode.NEW_INSTANCE)
public class UserLoginEventHandler extends BaseServerEventHandler {

	//Fields
	
	private	MessageDigest md5;
	
	//Methods
	
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		if (md5 == null) {
			try {
				md5 = MessageDigest.getInstance("MD5");
			}
			catch (Exception ex) {
				trace("[ERROR] MD5 can't be initialized!");
				trace(ex.toString());
				trace((Object[])ex.getStackTrace());
		        SFSErrorData errData = new SFSErrorData(SFSErrorCode.LOGIN_SERVER_FULL);
			    throw new SFSLoginException("Server internal problem", errData);
			}
		}
		
		String login = (String)event.getParameter(SFSEventParam.LOGIN_NAME);
		String password = (String)event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		String hashText = "";
		
		try	{
			md5.reset();
			byte[] hashed = 
				md5.digest((BombersDispatcher.C_ApiId + "_" + login + "_" + BombersDispatcher.C_ApiSecret).getBytes("UTF-8"));
			BigInteger bigInt = new BigInteger(1, hashed);
			hashText = bigInt.toString(16);
			while(hashText.length() < 32 ) {
				hashText = "0" + hashText;
			}
		}
		catch (Exception ex) {
			hashText = "";
		}
		
		Session s = (Session)event.getParameter(SFSEventParam.SESSION);
		boolean accept = false;
		if (login.equals("1") || login.equals("2") || login.equals("3") || login.equals("4") || login.equals("5")) {
			trace("Using simple auth for user " + login);
			accept = getApi().checkSecurePassword(s, login, password);
			if (accept == false) {
				accept = getApi().checkSecurePassword(s, hashText, password);
			}
		} else {
			accept = getApi().checkSecurePassword(s, hashText, password);
		}
		if (!accept) {
	        SFSErrorData errData = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
	        errData.addParameter(login);
		    trace("[Notice] User " + login + " attemted to login with bad password");
		    throw new SFSLoginException("Bad login-password pair", errData);
		}	
	}

}
