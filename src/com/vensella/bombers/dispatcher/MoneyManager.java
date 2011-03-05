package com.vensella.bombers.dispatcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class MoneyManager {

	//Fields
	
	
	private BombersDispatcher f_dispatcher;
	private ScheduledThreadPoolExecutor f_executor;
	
	
	//Constructors
	
	public MoneyManager(BombersDispatcher dispatcher)
	{
		f_dispatcher = dispatcher;
		f_executor = new ScheduledThreadPoolExecutor(1);
	}
	
	//Methods
	
	public void beginTransactVotes(final PlayerProfile profile, final int votes, final Runnable success, final Runnable fail)
	{
		f_executor.schedule(new Runnable() {
			@Override
			public void run() {
				//Dummy realization
				if (vkWithdrawVotesHttpRequest(profile.getId(), votes))
					success.run();
				else
					fail.run();
			}
		}, 0, TimeUnit.MILLISECONDS);
	}
	
	
	
	private boolean vkWithdrawVotesHttpRequest(String userId, int votes) {
		int rand = (int)(10000 * Math.random());
		long time = System.currentTimeMillis() / 1000;
		
		StringBuilder r = new StringBuilder();
		r.append("api_id="); r.append(BombersDispatcher.C_ApiId);
		r.append("format=JSON");
		r.append("method=secure.withdrawVotes");
		r.append("random="); r.append(rand);
		r.append("timestamp="); r.append(time);
		r.append("uid="); r.append(userId);
		r.append("v=2.0");
		r.append("votes="); r.append(votes);
		r.append(BombersDispatcher.C_ApiSecret);
		
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			byte[] buffer = md5.digest(r.toString().getBytes("UTF-8"));
			BigInteger bigInt = new BigInteger(1, buffer);
			String sig = bigInt.toString(16);
			while(sig.length() < 32 ) {
				sig = "0" + sig;
			}
			
			r = new StringBuilder();
			r.append("http://api.vkontakte.ru/api.php?");
			r.append("api_id="); r.append(BombersDispatcher.C_ApiId); r.append("&");
			r.append("format=JSON"); r.append("&");
			r.append("method=secure.withdrawVotes"); r.append("&");
			r.append("random="); r.append(rand); r.append("&");
			r.append("timestamp="); r.append(time); r.append("&");
			r.append("uid="); r.append(userId); r.append("&");
			r.append("v=2.0"); r.append("&");
			r.append("votes="); r.append(votes); r.append("&");
			r.append("sig="); r.append(sig); 
			
			URL url = new URL(r.toString());
			InputStream is = url.openStream();
			try {
		        BufferedReader in = new BufferedReader(new InputStreamReader(is));
		        String inputLine;
		        while ((inputLine = in.readLine()) != null) {
		        	f_dispatcher.trace(
		        			ExtensionLogLevel.WARN, 
		        			"VkRequest(security.withdrawVotes) result for user login "+ userId +": " + inputLine
		        		);
		        	
		        	SFSObject response = SFSObject.newFromJsonData(inputLine);
		        	if (response.containsKey("response") && response.getInt("response") == votes) {
		        		return true;
		        	} 
		        	return false;
		        }
		        return false;
			} finally {
			  is.close();
			}
		}
		catch (Exception e) {
			f_dispatcher.trace(ExtensionLogLevel.ERROR, "While trying to withdraw " + votes + " from user " + userId);
			f_dispatcher.trace(ExtensionLogLevel.ERROR, e.toString());
			f_dispatcher.trace(ExtensionLogLevel.ERROR, (Object[])e.getStackTrace());
			return false;
		}
	}
	
}
