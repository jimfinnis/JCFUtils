package org.pale.jcfutils.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.pale.jcfutils.Plugin;
import org.pale.jcfutils.region.Region;
import org.pale.jcfutils.region.RegionManager;

public class PlayerMoveListener implements Listener {
	private static final double DISTHRESHOLD = 2;
	
	Map<Player,Location> locsForPlayer = new HashMap<Player,Location>();
	Map<Player,Region> regionForPlayer = new HashMap<Player,Region>();
	
	@EventHandler
	public void move(final PlayerMoveEvent evt) {
		Player p = evt.getPlayer();
		Location l = evt.getTo();
		
		if(locsForPlayer.containsKey(p)) {
			if(locsForPlayer.get(p).distance(l)>DISTHRESHOLD) {
				checkRegions(p);
				locsForPlayer.put(p, l);
			}
		} else {
			checkRegions(p);
			locsForPlayer.put(p, l);
		}
	}
	
	private void checkRegions(Player p) {
		Region r = RegionManager.getManager(p.getWorld()).getSmallestRegion(p.getLocation());
		if(r!=null && r!=regionForPlayer.get(p)) {
			// region has changed
			regionForPlayer.put(p, r);
			// so display a funky title
			p.sendTitle(r.name, "",10,70,20);
		}
		
		// this may slow things down!
		if(Plugin.getInstance().isRegDebugActive(p)) {
			List<Region> lst = RegionManager.getManager(p.getWorld()).getRegionList(p.getLocation());
			if(lst.size()>0) {
				for(Region rr: lst) {
					p.sendMessage(rr.toString());
				}
			} else
				p.sendMessage("None");
		}
	}
}
