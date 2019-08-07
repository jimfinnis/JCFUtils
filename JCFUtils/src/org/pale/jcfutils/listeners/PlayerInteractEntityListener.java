package org.pale.jcfutils.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.Art;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class PlayerInteractEntityListener implements Listener {
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		Player p = e.getPlayer();
		Entity ent = e.getRightClicked();
		if(e.getHand().equals(EquipmentSlot.HAND) && ent.getType()==EntityType.PAINTING) {
			Painting painting = (Painting)ent;
			Art art = painting.getArt();
			int n = art.ordinal();
			if(++n == Art.values().length) {
				n=0;
			}
			art = Art.values()[n];
			painting.setArt(art,true);
		}
	}
	
}
