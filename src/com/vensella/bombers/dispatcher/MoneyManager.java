package com.vensella.bombers.dispatcher;

public class MoneyManager {

	//Fields
	
	BombersDispatcher f_dispatcher;
	
	//Constructors
	
	public MoneyManager(BombersDispatcher dispatcher)
	{
		f_dispatcher = dispatcher;
	}
	
	//Methods
	
	public void beginTransactVotes(PlayerProfile profile, int votes, Runnable success, Runnable fail)
	{
		//Dummy realization
		if (votes < 10000)
			success.run();
		else
			fail.run();
	}
	
}
