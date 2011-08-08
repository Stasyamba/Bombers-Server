package com.vensella.bombers.dispatcher;

import java.util.Map;
import java.util.Map.Entry;

import com.smartfoxserver.v2.entities.data.SFSArray;

public class CommonHelper {

	public static SFSArray createSFSArray(Map<Integer, Integer> map) {
		SFSArray a = new SFSArray();
		for (Entry<Integer, Integer> entry : map.entrySet()) {
			SFSArray en = new SFSArray();
			en.addInt(entry.getKey());
			en.addInt(entry.getValue());
			a.addSFSArray(en);
		}
		return a;
	}
	
}
