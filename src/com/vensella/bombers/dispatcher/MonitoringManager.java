package com.vensella.bombers.dispatcher;

import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class MonitoringManager {
	
	//Nested types
		
		//Fields
		
		
		
		//Constructor
	
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	
	//Constructors
	
	public MonitoringManager(BombersDispatcher dispatcher) {
		f_dispatcher = dispatcher;
		f_dispatcher.trace(ExtensionLogLevel.WARN, "Starting monitoring manager");
		
		//WatchService s;
		//File file; file.get
	}
	
	

}
