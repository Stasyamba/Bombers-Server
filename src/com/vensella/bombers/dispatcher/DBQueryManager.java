package com.vensella.bombers.dispatcher;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;


public class DBQueryManager {
	
	//Constants
	
	//TODO: Make all queries as constants
	
	//Nested types
	
	private class QueuedQuery
	{
		//Fields
		
		private String f_query;
		private Object[] f_params;
		private QueryCallback f_callback;
		private Boolean f_isUpdate;
		
		//Constructors
		
		public QueuedQuery(Boolean isUpdate, QueryCallback callback, String query, Object[] params)
		{
			f_isUpdate = isUpdate;
			f_query = query;
			f_params = params;
			f_callback = callback;
		}
		
		//Methods
		
		public Boolean isUpdate() { return f_isUpdate; }
		public String getQuery() { return f_query; }
		public Object[] getParams() { return f_params; }
		public QueryCallback getCallback() { return f_callback; }

	}
	
	public abstract class QueryCallback
	{
		public abstract void run(ISFSArray result);
	}
	
	//Fields
	
	private BombersDispatcher f_dispatcher;
	private Thread f_workingThread;
	private BlockingQueue<QueuedQuery> f_queue;
	
	//Constructors
	
	public DBQueryManager(BombersDispatcher dispatcher)
	{
		f_dispatcher = dispatcher;
		f_queue = new LinkedBlockingQueue<DBQueryManager.QueuedQuery>();
		f_workingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true){
					QueryCallback cb = null;
					try
					{
						QueuedQuery q = f_queue.take();
						cb = q.getCallback();
						Connection conn = f_dispatcher.getParentZone().getDBManager().getConnection();
						try {
							PreparedStatement st = conn.prepareStatement(q.getQuery());
							int i = 1;
							for (Object p : q.getParams()) {
								st.setString(i++, p.toString());
							}
							if (q.isUpdate()) {
								SFSArray result = SFSArray.newInstance();
								result.addInt(st.executeUpdate());
								if (cb != null) {
									cb.run(result);
								}
							} else {
								SFSArray result = SFSArray.newFromResultSet(st.executeQuery());
								if (cb != null) {
									cb.run(result);
								}								
							}
						}
						finally {
							if (conn != null)
								conn.close();
						}
					}
					catch(Exception ex)
					{
						f_dispatcher.trace("[WARNING] Db query exception:");
						f_dispatcher.trace(ex.getMessage());
						f_dispatcher.trace((Object[])ex.getStackTrace());
						if (cb != null)
							cb.run(null);
					}
				}
			}
		}, "DB Query Thread");
		f_workingThread.start();
	}
			
	
	//Methods
	
	public void ScheduleUpdateQuery(QueryCallback callback, String query, Object[] params)
	{
		QueuedQuery q = new QueuedQuery(true, callback, query, params);
		f_queue.add(q);
	}
	
	public void ScheduleQuery(QueryCallback callback, String query, Object[] params)
	{
		assert callback != null;
		QueuedQuery q = new QueuedQuery(false, callback, query, params);
		f_queue.add(q);
	}
	
	//Static methods
	
}
