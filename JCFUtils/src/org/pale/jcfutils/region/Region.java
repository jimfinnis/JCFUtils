package org.pale.jcfutils.region;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.BoundingBox;
import org.pale.jcfutils.Plugin;

@SerializableAs("JCFRegion")
public class Region implements ConfigurationSerializable {
	// REMEMBER TO ADD PERSISTENT DATA ITEMS TO THE SERIALIZE/DESERIALIZE STUFF
	private BoundingBox aabb; // region bounding box - keep private to avoid vol recalc being skipped
	public int id;
	public String name,desc;
	public Region link; // this part gets generated from linkID
	public int linkID=0; // this part gets saved 
	private double volume; // accessible only via a getter from outside - is volume of me + all things linked to me! See fixVolumes in RegionManager
	double myVolume; // my volume not including any links - used by RegionManager.fixVolumes
	public double getVolume() { return volume; }
	
	// note - keep package-private, so that it must be created through RegionManager
	Region(BoundingBox _aabb,int _id){
            aabb = _aabb;
            volume = aabb.getVolume();
            id = _id;
            name = "Region "+Integer.toString(id);
            desc = "";
	}


	boolean contains(Location loc) {
		// note the slight adjustment, to just above the player's feet.
		return aabb.contains(loc.getX(),loc.getY()+0.1,loc.getZ());
	}
	
	// clear volume to base level
	void clearVolume() {
		volume = myVolume;
	}
	
	// add linked volume to my volume, and subtract the intersection.
	void addLinkedVolume(Region r) {
		volume += r.myVolume;
		if(aabb.overlaps(r.aabb)) {
			BoundingBox inter = aabb.clone(); // intersection modifies the object, and we don't want that!
			volume -= inter.intersection(r.aabb).getVolume();
		}
	}

	///////////////////////// SERIALIZATION /////////////////////////////
	
	@Override
	public Map<String, Object> serialize() {
            Map<String,Object> m = new HashMap<String,Object>();
            m.put("aabb", aabb);
            m.put("name", name);
            m.put("desc",desc);
            m.put("id", id);
            m.put("linkID",linkID);
            return m;
	}
	// deserialization ctor
	public Region(Map<String,Object> m) {
            aabb = (BoundingBox)m.get("aabb");
            myVolume = aabb.getVolume();
            name = (String)m.get("name");
            if(m.containsKey("desc")){
                desc = (String)m.get("desc");
            } else {
                desc = "";
            }
            
            id = (int)m.get("id");
            if(m.containsKey("linkID"))
                linkID = (int)m.get("linkID");
            else
                linkID = 0;
	}


	// increases the AABB bounds to include this point. Only call from RM.
	void extend(Location loc) {
		Plugin.log("Extending from "+aabb.toString());
		Plugin.log("Including location "+loc.toString());
		aabb.union(loc);
		Plugin.log("AABB is now "+aabb.toString());
		myVolume = aabb.getVolume(); // fixVolumes and rebuildChunks must be run!
	}

	// change the bounding box
	void setAABB(BoundingBox aabb2) {
		aabb = aabb2;
		myVolume = aabb.getVolume(); // fixVolumes and rebuildChunks must be run!
	}
	
	public String getAABBString() {
		return String.format("X(%d,%d), Y(%d,%d), Z(%d,%d)", 
				(int)(aabb.getMinX()),(int)(aabb.getMaxX()),
				(int)(aabb.getMinY()),(int)(aabb.getMaxY()),
				(int)(aabb.getMinZ()),(int)(aabb.getMaxZ()));
	}
	
	@Override
	public String toString() {
		if(link!=null)
			return String.format("%d [parent %d]: %s [%s]", id,link.id,name,getAABBString());
		else
			return String.format("%d: %s [%s]", id,name,getAABBString());
	}
	
}
