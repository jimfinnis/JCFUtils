package org.pale.jcfutils.region;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.BoundingBox;

@SerializableAs("JCFRegion")
public class Region implements ConfigurationSerializable {
	// REMEMBER TO ADD PERSISTENT DATA ITEMS TO THE SERIALIZE/DESERIALIZE STUFF
	private BoundingBox aabb; // region bounding box - keep private to avoid vol recalc being skipped
	public int id;
	public String name;
	double size;
	double volume;
	
	// note - keep package-private, so that it must be created through RegionManager
	Region(BoundingBox _aabb,int _id){
		aabb = _aabb;
		volume = aabb.getVolume();
		id = _id;
		name = "Region "+Integer.toString(id);
	}

	String getAABBString() {
		return aabb.toString();
	}
	boolean contains(Location loc) {
		// note the slight adjustment, to just above the player's feet.
		return aabb.contains(loc.getX(),loc.getY()+0.1,loc.getZ());
	}

	///////////////////////// SERIALIZATION /////////////////////////////
	
	@Override
	public Map<String, Object> serialize() {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("aabb", aabb);
		m.put("name", name);
		m.put("id", id);
		return m;
	}
	// deserialization ctor
	public Region(Map<String,Object> m) {
		aabb = (BoundingBox)m.get("aabb");
		volume = aabb.getVolume();
		name = (String)m.get("name");
		id = (int)m.get("id");
	}


	// increases the AABB bounds to include this point.
	public void extend(Location loc) {
		aabb = aabb.union(loc);
		volume = aabb.getVolume();
	}
	
}
