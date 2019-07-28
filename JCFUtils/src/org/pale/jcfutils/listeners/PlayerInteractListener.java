package org.pale.jcfutils.listeners;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;


public class PlayerInteractListener implements Listener {
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if(e.getAction()==Action.RIGHT_CLICK_BLOCK && e.getHand().equals(EquipmentSlot.HAND)) {
			Player p = e.getPlayer();
			ItemStack st = p.getInventory().getItemInMainHand();
			ItemMeta meta = st.getItemMeta();
			List<String> lore = meta.getLore();
			if(lore != null && lore.size()>1) {
				if(lore.get(0)=="Magic Stick"){
					Material m = Material.getMaterial(lore.get(1));
					scan(p,m);
					e.setCancelled(true);
				}
			}
		}
	}	

	static final int DIST_NEAR=10;
	static final int DIST_MID=20;
	static final int DIST_FAR=60;
	static final double ANGLE=45 * (Math.PI/180.0);

	private void scan(Player p,Material m) {
		Location l = p.getLocation();
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		World w = l.getWorld();

		Vector lineOfSight = p.getEyeLocation().getDirection();

		int ctnear =0;
		int ctmid = 0;
		int ctfar = 0;
		int ctsight=0;
		int ctsightnear=0;
		
		p.sendMessage("looking (insert mystical sounds here)");
		for(int xoffset=-DIST_FAR;xoffset<=DIST_FAR;xoffset++) {
			for(int yoffset=-DIST_FAR;yoffset<=DIST_FAR;yoffset++) {
				for(int zoffset=-DIST_FAR;zoffset<=DIST_FAR;zoffset++) {
					int bx = x+xoffset;
					int by = y+yoffset;
					int bz = z+zoffset;
					Block b = w.getBlockAt(bx,by,bz); 
					if(b.getType()==m) {
						double dist = Math.sqrt(xoffset*xoffset+yoffset*yoffset+zoffset*zoffset);
						ctfar++;
						if(dist<DIST_MID)ctmid++;
						if(dist<DIST_NEAR)ctnear++;

						// in view?
						if(dist<DIST_MID) {
							Vector v = new Vector(xoffset,yoffset,zoffset);
							double ang = v.angle(lineOfSight);
							p.sendMessage("Angle "+Double.toString(ang));
							if(v.angle(lineOfSight)<ANGLE)
								ctsight++;
							if(dist<DIST_NEAR) {
								ctsightnear++;
							}
						}
					}
				}
			}
		}
		p.sendMessage(Integer.toString(ctfar)+" blocks found within "+Integer.toString(DIST_FAR));
		p.sendMessage(Integer.toString(ctmid)+" blocks found within "+Integer.toString(DIST_MID)+ 
				" of which "+Integer.toString(ctsight)+" are in front of you");
		p.sendMessage(Integer.toString(ctnear)+" blocks found within "+Integer.toString(DIST_NEAR)+
				" of which "+Integer.toString(ctsightnear)+" are in front of you");
	}
}
