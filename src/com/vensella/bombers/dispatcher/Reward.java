package com.vensella.bombers.dispatcher;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

public class Reward {
	
	//Constructor
	
	public Reward(Element rewardElement) {
		String resourcesRewardString = rewardElement.getAttribute("resources");
		if (!resourcesRewardString.isEmpty()) {
			String[] r = resourcesRewardString.split(",");
			f_goldReward = Integer.parseInt(r[0]);
			f_crystalReward = Integer.parseInt(r[1]);
			f_adamantiumReward = Integer.parseInt(r[2]);
			f_antimatterReward = Integer.parseInt(r[3]);
			f_energyReward = Integer.parseInt(r[4]);
		}
		String experienceRewardString = rewardElement.getAttribute("exp");
		if (!experienceRewardString.isEmpty()) {
			f_experienceReward = Integer.parseInt(experienceRewardString);
		}
		f_itemsReward = new HashMap<Integer, Integer>();
		String itemsRewardString = rewardElement.getAttribute("items");
		String itemsCountsString = rewardElement.getAttribute("itemsCounts");
		if (!itemsRewardString.isEmpty() && itemsCountsString.isEmpty()) {
			String[] r = itemsRewardString.split(",");
			for (int i = 0; i < r.length; ++i) {
				f_itemsReward.put(Integer.parseInt(r[i]), 1);
			}
		}
		else if (!itemsRewardString.isEmpty() && !itemsCountsString.isEmpty()) {
			String[] r = itemsRewardString.split(",");
			String[] c = itemsCountsString.split(",");
			for (int i = 0; i < r.length; ++i) {
				f_itemsReward.put(Integer.parseInt(r[i]), Integer.parseInt(c[i]));
			}
		}
	}
	
	public Reward(int r0, int r1, int r2, int r3, int r4, int exp, Map<Integer, Integer> w) {
		f_goldReward = r0;
		f_crystalReward = r1;
		f_adamantiumReward = r2;
		f_antimatterReward = r3;
		f_energyReward = r4;
		f_experienceReward = exp;
		f_itemsReward = w;
	}
	
	public Reward() {
		f_itemsReward = new HashMap<Integer, Integer>();
	}
	
	//Fields
	
	private int f_goldReward;
	private int f_crystalReward;
	private int f_adamantiumReward;
	private int f_antimatterReward;
	private int f_energyReward;
	
	private int f_experienceReward;
	
	private Map<Integer, Integer> f_itemsReward;
	
	//Methods
	
	public int getGoldReward() { return f_goldReward; }
	public int getCrystalReward() { return f_crystalReward; }
	public int getAdamantiumReward() { return f_adamantiumReward; }
	public int getAntimatterReward() { return f_antimatterReward; }
	public int getEnergyReward() { return f_energyReward; }
	
	public void addGoldReward(int delta) { f_goldReward += delta; } 
	public void addCrystalReward(int delta) { f_crystalReward += delta; } 
	public void addAdamantiumReward(int delta) { f_adamantiumReward += delta; } 
	public void addAntimatterReward(int delta) { f_antimatterReward += delta; } 
	public void addEnergyReward(int delta) { f_energyReward += delta; } 
	
	public int getExperienceReward() { return f_experienceReward; }
	
	public Map<Integer, Integer> getItemsReward() { return f_itemsReward; }
	
	public boolean isEmpty() {
		return (f_goldReward == 0 && f_crystalReward == 0 && f_adamantiumReward == 0 && f_antimatterReward == 0 &&
				f_energyReward == 0 && f_experienceReward == 0 && f_itemsReward.size() == 0);
	}
	
	public Reward getUnion(Reward r1, Reward r2) {
		Map<Integer, Integer> w = new HashMap<Integer, Integer>();
		for (Integer weaponId : r1.f_itemsReward.keySet()) {
			Integer c = w.get(weaponId);
			if (c == null) {
				w.put(weaponId, r1.f_itemsReward.get(weaponId));
			} else {
				w.put(weaponId, r1.f_itemsReward.get(weaponId) + c);
			}
		}
		for (Integer weaponId : r2.f_itemsReward.keySet()) {
			Integer c = w.get(weaponId);
			if (c == null) {
				w.put(weaponId, r2.f_itemsReward.get(weaponId));
			} else {
				w.put(weaponId, r2.f_itemsReward.get(weaponId) + c);
			}
		}
		return new Reward(
				r1.getGoldReward() + r2.getGoldReward(),
				r1.getCrystalReward() + r2.getCrystalReward(),
				r1.getAdamantiumReward() + r2.getAdamantiumReward(),
				r1.getAntimatterReward() + r2.getAntimatterReward(),
				r1.getEnergyReward() + r2.getEnergyReward(),
				r1.getExperienceReward() + r2.getExperienceReward(),
				w
			);
	}
	
	public SFSObject toSFSObject() {
		SFSObject r = new SFSObject();
		r.putInt("R0", f_goldReward);
		r.putInt("R1", f_crystalReward);
		r.putInt("R2", f_adamantiumReward);
		r.putInt("R3", f_antimatterReward);
		r.putInt("R4", f_energyReward);
		r.putInt("Exp", f_experienceReward);
		SFSArray items = new SFSArray();
		for (int item : f_itemsReward.keySet()) {
			SFSObject it = new SFSObject();
			it.putInt("Id", item);
			it.putInt("C", f_itemsReward.get(item));
			items.addSFSObject(it);
		}
		r.putSFSArray("Items", items);
		return r;
	}
	
}