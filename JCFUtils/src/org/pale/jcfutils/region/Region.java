package org.pale.jcfutils.region;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class Region {
	private BoundingBox aabb; // region bounding box
	private int id;
	
	static int regionCt=1;
	
	Region(BoundingBox _aabb){
		aabb = _aabb;
		id = regionCt++;
	}
	
	boolean isInRegion(Location loc) {
		return aabb.contains(loc.getX(),loc.getY(),loc.getZ());
	}
}
