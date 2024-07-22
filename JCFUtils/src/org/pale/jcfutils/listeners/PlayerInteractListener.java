package org.pale.jcfutils.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
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
    
    enum StickType {
        DOWSING,MOBMOVER,LEAFBLOWER
          }
    
    class StickData {
        StickData(StickType t,Material m){
            this.t = t;
            this.m = m;
        }
        StickType t;
        Material m;
    }
    
    StickData getMagicStickData(Player p) {
        ItemStack st = p.getInventory().getItemInMainHand();
        ItemMeta meta = st.getItemMeta();
        if(meta==null)return null;
        StickData d = null;
        List<String> lore = meta.getLore();
        if(lore==null)return null;
        if(lore.size()>0) {
            String name = lore.get(0);
            if(lore.size()>1 && name.equals("Magic Stick")) {
                d = new StickData(StickType.DOWSING,Material.getMaterial(lore.get(1))); 
            } else if(name.equals("Mob Mover Stick")) {
                d = new StickData(StickType.MOBMOVER,null);
            } else if(name.equals("Leafblower")) {
                d = new StickData(StickType.LEAFBLOWER,null);
            }
        }
        return d;
    }
    
    @EventHandler
          public void onPlayerInteract(PlayerInteractEvent e) {
              Player p = e.getPlayer();
              
              Action act = e.getAction();
              if(e.getHand()==null) // in the case of Action.PHYSICAL
                  return;
              
              boolean inHand = e.getHand().equals(EquipmentSlot.HAND);
              if((act==Action.RIGHT_CLICK_AIR || act==Action.RIGHT_CLICK_BLOCK) && inHand) {
                  StickData d = getMagicStickData(p);
                  if(d!=null) {
                      switch(d.t) {
                      case DOWSING:
                          dowsingScan(p,d.m);
                          e.setCancelled(true);
                          break;
                      case MOBMOVER:
                          mobMover(p);
                          e.setCancelled(true);
                          break;
                      case LEAFBLOWER:
                          leafBlower(p);
                          e.setCancelled(true);
                      default:break;
                          
                      }
                  } 
              }
              // left click actions
              if((act==Action.LEFT_CLICK_AIR || act==Action.LEFT_CLICK_AIR) && inHand) {
                  StickData d = getMagicStickData(p);
                  if(d!=null) {
                      switch(d.t) {
                      case MOBMOVER:
                          mobMoverClear(p);
                          e.setCancelled(true);
                          break;
                      default:break;
                      }
                  }
              }
          }
    
    private Map<Player,Entity> selectedMobMap = new HashMap<Player,Entity>();
    
    private boolean lookForMobSelect(Player p) {
        List<Entity> lst = p.getNearbyEntities(20,20,20);
        for(Entity e:lst) {
            if(p.hasLineOfSight(e)) {
                for(Block b: p.getLineOfSight(null,100)) {
                    if(e.getLocation().distance(b.getLocation())<1) {
                        selectedMobMap.put(p,e);
                        p.sendMessage("Selected "+e.getName());
                        return true;						
                    }
                }
            }
        }
        return false;
    }
    
    private void moveMob(Player p, Entity mob) {
        List<Block> lst = p.getLastTwoTargetBlocks(null, 100);
        Block b;
        if(lst==null) {
            p.sendMessage("no blocks in line of sight");
            return;
        }
        else if(lst.size()<2)
            b=lst.get(0);
        else
            b=lst.get(1);
        Location loc = b.getLocation();
        loc.setY(loc.getY()+1.1);
        mob.teleport(loc);
    }
    
    private void mobMover(Player p) {
        if(!lookForMobSelect(p)) {
            // no mob found on line of sight, see if we have one to move.
            if(selectedMobMap.containsKey(p)) {
                moveMob(p,selectedMobMap.get(p));
            } else {
                p.sendMessage("No mob on light of sight, and don't have one selected");
            }
        }
    }
    
    private void mobMoverClear(Player p) {
        selectedMobMap.remove(p);
        p.sendMessage("Mob mover selection cleared");
    }
    
    static final int LEAFBLOWER_RANGE = 30; // max distance for blower target
    static final int LEAFBLOWER_SIZE = 5;   // max distance of leaves blown in search 
    
    static int leafCt;
    
    private String locstr(Location loc){
        return "("+
              Integer.toString((int)loc.getX())+","+
              Integer.toString((int)loc.getY())+","+
              Integer.toString((int)loc.getZ())+")";
    }
    
    // blow all leaves around the given location, but stop when too far from startLoc.
    // Do this recursively following leaves only.
    private void leafBlow(Player p, Location loc, Location startLoc){
        if(loc.distance(startLoc)<LEAFBLOWER_SIZE){
            Block b = loc.getBlock();
//            p.sendMessage("Block "+locstr(loc)+" is "+b.getType().toString());
            if(b.getBlockData() instanceof Leaves) {
                // it's a leaf block, delete it.
                b.setType(Material.AIR);
                leafCt++;
                // and repeat process for surrounding blocks
                for(int dx=-1;dx<=1;dx++){
                    for(int dy=-1;dy<=1;dy++){
                        for(int dz=-1;dz<=1;dz++){
                            if(dx!=0 || dy!=0 || dz!=0){
//                                p.sendMessage("offset "+dx+","+dy+","+dz);
                                // note the clone; add() does not create a new location
                                leafBlow(p, loc.clone().add(dx,dy,dz),startLoc);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void leafBlower(Player p) {
        p.sendMessage("Leafblower");
        Block b = p.getTargetBlock(null, 100);
        if(b!=null){
            // check it's a leaf
            if(b.getBlockData() instanceof Leaves) {
                int ct=0;
                Location loc = b.getLocation();
                double dist = loc.distance(p.getLocation());
                if(dist<30){
                    // in range, so let's iterate around the found location looking for leaves.
                    leafCt=0;
                    leafBlow(p, loc,loc);
                }
                p.sendMessage(Integer.toString(leafCt)+" leaf blocks blown");
            }
        }
    }
    
    
    
    static final int DIST_NEAR=10;
    static final int DIST_MID=20;
    static final int DIST_FAR=60;
    static final double ANGLE=45 * (Math.PI/180.0);
    
    private void dowsingScan(Player p,Material m) {
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
        double closest = -1;
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
                        if(closest<0 || dist<closest)closest=dist;
                        ctfar++;
                        if(dist<DIST_MID)ctmid++;
                        if(dist<DIST_NEAR)ctnear++;
                        
                        // in view?
                        if(dist<DIST_MID) {
                            Vector v = new Vector(xoffset,yoffset,zoffset);
                            double ang = v.angle(lineOfSight);
                            //							p.sendMessage("Angle "+Double.toString(ang));
                            if(v.angle(lineOfSight)<ANGLE){
                                ctsight++;
                                if(dist<DIST_NEAR)
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
        p.sendMessage("Closest block is "+Integer.toString((int)closest));
    }
}
