package org.pale.jcfutils.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.pale.jcfutils.Plugin;

// listener to stop creatures spawning where there is light.
public class CreatureSpawnListener implements Listener {
	@EventHandler
	public void spawn(final CreatureSpawnEvent evt) {
		byte lev;
		Plugin.spawnsAttempted++;
		switch(evt.getSpawnReason()){
		case NATURAL:
			// shulkers are fine.
			if(evt.getEntityType() != EntityType.SHULKER) {
				lev = evt.getLocation().getBlock().getLightFromBlocks();
				if(lev>Plugin.getLightSpawnLevel()){
					// Plugin.log("Creature spawn aborted due to light: "+evt.getEntityType().name());
					evt.setCancelled(true);
					Plugin.spawnsCancelled++;
				}
			}
			break;
		default:break;
		}
	}
}
