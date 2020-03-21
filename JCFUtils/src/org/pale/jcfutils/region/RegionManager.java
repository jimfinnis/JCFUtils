package org.pale.jcfutils.region;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.pale.jcfutils.Plugin;

/**
 * Handles all the regions for a particular world. As a namespace, handles the region
 * managers for different worlds via getManager().
 * @author white
 *
 */
public class RegionManager {
	static Map<World,RegionManager> managers = new HashMap<World,RegionManager>();
	World world;
	int idCtr=1; // ID of next region, is set to max ID of loaded regions on load.
	
	static Map<Player,Region> lastEditedRegion = new HashMap<Player,Region>();
	public static Region getLastEdited(Player p) {
		return lastEditedRegion.get(p);
	}
	public static void setLastEdited(Player p,Region r) {
		lastEditedRegion.put(p, r);
	}
	
	private RegionManager(World w) {
		world = w;
	}
	// get or create region manager for this world
	
	public static RegionManager getManager(World w) {
		RegionManager m;
		if(!managers.containsKey(w)) {
			m = new RegionManager(w);
			managers.put(w, m);
			return m;
		} else 
			return managers.get(w);
	}

	// load region data for all worlds
	public static void loadRegionData() {
		for(World w: Bukkit.getWorlds()) {
			RegionManager rm = RegionManager.getManager(w);
			rm.loadConfig();
		}
	}
	
	// save region data for all worlds.
	public static void saveRegionData() {
		for(World w: Bukkit.getWorlds()) {
			RegionManager rm = RegionManager.getManager(w);
			rm.saveConfig();
		}
	}

	// map of all regions in the world by ID
	Map<Integer,Region> regMap = new HashMap<Integer,Region>();
	
	public Region get(int id) {
		return regMap.get(id);
	}
	
	public Region add(BoundingBox aabb) {
		Region r = new Region(aabb,idCtr++);
		regMap.put(r.id,r);
		fixVolumes();
		return r;
	}
	
	// get all regions sorted by name
	public List<Region> getAllRegions(){
		Comparator<Region> comp = new Comparator<Region>() {
			@Override
			public int compare(Region a, Region b) {
				return a.name.compareTo(b.name);
			}
		};
		List<Region> list = new ArrayList<Region>(regMap.values());
		Collections.sort(list,comp);
		return list;
	}
	
	// get smallest region or null
	public Region getSmallestRegion(Location loc) {
		double vol=-1;
		Region found=null;
		for(Region r: getRegionSet(loc,false)) {
			if(vol<0 || r.getVolume() < vol) {
				vol = r.getVolume();
				found = r;
			}
		}
		return found;
	}
	
	// get regions containing this location
	public List<Region> getRegionList(Location loc, boolean ignoreLinks) {
		List<Region>list = new ArrayList<Region>(getRegionSet(loc,ignoreLinks));
		list.sort(new Comparator<Region>() {
			@Override
			public int compare(Region o1, Region o2) {
				return Double.compare(o1.getVolume(),o2.getVolume());
			}
		});
		return list;
	}
	
	
	private void rebuildChunks() {
		// placeholder - we'll do this whenever we reload the entire region list.
		// this will build the chunk data from the regMap.
		Plugin.warn("Regions are not chunked!");
	}

	// this gets the set of all regions which have that location.
	// TODO make this use chunks!
	public Set<Region> getRegionSet(Location loc,boolean ignoreLinks){
		Set<Region> set = new HashSet<Region>();
		for(Region r: regMap.values()){
			if(r.contains(loc)) {
				if(r.link!=null && !ignoreLinks)
					set.add(r.link);
				else
					set.add(r);
			}
		}
		return set;
	}
	
	// fix up the volumes by setting volume of regions to myVolume plus any linked regions.
	public void fixVolumes() {
		// first clear all volumes back
		Collection<Region> set = regMap.values();
		for(Region r:set)r.clearVolume();
		// then take care of links
		for(Region r:set) {
			if(r.link != null) {
				r.link.addLinkedVolume(r);
			}
		}
	}
	
	public void link(Region rparent, Region rthis) {
		rthis.link = rparent;
		rthis.linkID = rparent.id;
		if(rparent == rthis) {
			Plugin.warn("Attempt to link region to itself");;
			return;
		}
		if(rparent.linkID>0) {
			Plugin.warn("Attempt to link region to region which is already linked");
			return;
		}
		Plugin.log(String.format("Link: making %d the parent of %d",rparent.id,rthis.id));
		fixVolumes();
	}
	
	public void unlink(Region r) {
		r.link = null;
		r.linkID = 0;
		fixVolumes();
	}
	
	public void delete(Region r) {
		regMap.remove(r.id);
		rebuildChunks();
		fixVolumes();
	}
	
	public void extend(Region r, Location location) {
		r.extend(location);
		rebuildChunks();
		fixVolumes();
	}

	public void setRegBox(Region r, BoundingBox aabb) {
		r.setAABB(aabb);
		rebuildChunks();
		fixVolumes();		
	}

	
	public void loadConfig() {
		String name = world.getName().trim()+"_reg.yaml";
		// load the file, which will deserialise the region map into a string map of regions.
		// Yaml can't handle integer map keys.
		File f = new File(Plugin.getInstance().getDataFolder(),name);
		FileConfiguration data = YamlConfiguration.loadConfiguration(f);
		// get the integer-keyed region map. It may not exist if there's no save file yet!
		ConfigurationSection sec = data.getConfigurationSection("regions");
		if(sec!=null) {
			int maxID=-1;
			Map<String,Object> m = sec.getValues(false);
			// we rebuild the entire new regMap from this map
			Map<Integer,Region> newm = new HashMap<Integer,Region>(); 
			for(Map.Entry<String, Object> e: m.entrySet()) {
				Region r = (Region)e.getValue();
				if(r.id>maxID)maxID=r.id; // keep track of max ID
				newm.put(r.id, r);
				Plugin.log("Loaded region "+Integer.toString(r.id)+" name "+r.name+" at "+r.getAABBString());
			}
			// now it's done, we switch to the new map and recreate the chunks
			regMap = newm;
			idCtr = maxID+1; // replace ID counter
			rebuildChunks();
			// and rebuild the links
			for(Region r: regMap.values()) {
				if(r.linkID>0)
					r.link = regMap.get(r.linkID);
				else
					r.link = null;
			}
			fixVolumes();
		}
		
	}
	public void saveConfig() {
		String name = world.getName().trim()+"_reg.yaml";
		File f = new File(Plugin.getInstance().getDataFolder(),name);
		f.delete(); // effectively tell it to always make a new one
		FileConfiguration data = YamlConfiguration.loadConfiguration(f);
		
		// we want to save a hashmap of strings, because that's how it works.
		Map<String,Region> m = new HashMap<String,Region>();
		for(Map.Entry<Integer,Region> e:regMap.entrySet()) {
			m.put(Integer.toString(e.getKey()), e.getValue());
		}
		data.set("regions", m);
		try {
			data.save(f);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
