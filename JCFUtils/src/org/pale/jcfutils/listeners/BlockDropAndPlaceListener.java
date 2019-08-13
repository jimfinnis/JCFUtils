package org.pale.jcfutils.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.util.BoundingBox;
import org.pale.jcfutils.Plugin;
import org.pale.jcfutils.Plugin.RegionPlacingData;
import org.pale.jcfutils.region.Region;
import org.pale.jcfutils.region.RegionManager;

/**
 * Used to handle drops and placings for region selection.
 * @author white
 *
 */
public class BlockDropAndPlaceListener implements Listener {
	
	/**
	 * Handle a block drop or place, for dealing with region creation after /jcf regcreate
	 * @param p the player
	 * @param m the item being dropped or placed to set AABB of region
	 * @param loc the location of the player dropping or the placed block
	 * @return whether the drop/place should be cancelled
	 */
	private boolean handle(Player p,Material m,Location loc) {
		if(!Plugin.regionPlacingData.containsKey(p)) {
			return false; // just a normal block place.
		}
		RegionPlacingData d = Plugin.regionPlacingData.get(p);
		if(m==d.m) {
			// it's our nominated block!
			d.placeCount++;
			if(d.placeCount == 1) {
				p.sendMessage("Now mark the second corner");
				d.loc1 = loc.clone();
			} else {
				// getting the second corner. Make sure it's in the same world!
				if(d.loc1.getWorld() != loc.getWorld()) {
					p.sendMessage("The two corners aren't in the same world - aborting!");
					Plugin.regionPlacingData.remove(p);					
					return true; // wrong, but still cancel the event
				}
				// get the AABB
				BoundingBox aabb = BoundingBox.of(d.loc1,loc);
				// create the region
				Region r = RegionManager.getManager(loc.getWorld()).add(aabb);
				p.sendMessage("Region created: ID "+Integer.toString(r.id));
				p.sendMessage("AABB: "+aabb.toString());
				// and delete the entry
				Plugin.regionPlacingData.remove(p);
			}
			return true; // cancel the event
		} else {
			p.sendMessage("Snark - some failure, mat="+m.toString());
			p.sendMessage("  and nominated mat="+d.m.toString());
		}
		return false;
	}
	
	@EventHandler 
	public void drop(final PlayerDropItemEvent evt){
		if(handle(evt.getPlayer(),evt.getItemDrop().getItemStack().getType(),evt.getPlayer().getLocation()))
			evt.setCancelled(true);		
	}
	
	@EventHandler
	public void place(final BlockPlaceEvent evt) {
		if(handle(evt.getPlayer(),evt.getBlockPlaced().getType(),evt.getBlockPlaced().getLocation())) {
			evt.setCancelled(true);
		}
	}
}
