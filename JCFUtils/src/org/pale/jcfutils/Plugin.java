package org.pale.jcfutils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.pale.jcfutils.Command.CallInfo;
import org.pale.jcfutils.Command.Cmd;
import org.pale.jcfutils.Command.Registry;
import org.pale.jcfutils.listeners.BlockDropAndPlaceListener;
import org.pale.jcfutils.listeners.CreatureSpawnListener;
import org.pale.jcfutils.listeners.PlayerInteractEntityListener;
import org.pale.jcfutils.listeners.PlayerInteractListener;
import org.pale.jcfutils.listeners.PlayerMoveListener;
import org.pale.jcfutils.region.Region;
import org.pale.jcfutils.region.RegionManager;



public class Plugin extends JavaPlugin {
	public static void log(String msg) {
		getInstance().getLogger().info(msg);
	}
	public static void warn(String msg) {
		getInstance().getLogger().warning(msg);
	}
	/**
	 * Make the plugin a weird singleton.
	 */
	static Plugin instance = null;
	static Random rand = new Random();

	// data stored per player for placing/dropping blocks to delimit a region
	public class RegionPlacingData {
		public Material m; // the material the player has indicated they will use
		public int placeCount; // the number of times the player has placed (on 2, the region is placed and this structure is deleted.)
		// first placed corner, which only means anything if placeCount>0 
		public Location loc1;
		public Region r; // if we're re-bounding a region this will be set, for new regions it's null
		RegionPlacingData(Material mat){
			m = mat;
			placeCount=0;
			r = null;
		}
		RegionPlacingData(Material mat,Region r){
			m = mat;
			placeCount=0;
			this.r =r; 
		}
	}

	public static Map<Player,RegionPlacingData> regionPlacingData = new HashMap<Player,RegionPlacingData>();

	private Registry commandRegistry=new Registry();

	/**
	 * Handy class for visiting blocks within a given range.
	 * @author white
	 *
	 */
	private abstract class BlockVisitor {
		// returns number of blocks modified
		public abstract int visit(World w,int bx,int by,int bz);
	}

	/**
	 * Handy method for using the handy class above.
	 * @param c
	 * @param distance
	 * @param v
	 * @return
	 */
	private int visit(CallInfo c,int distance,BlockVisitor v) {
		Location l = c.getPlayer().getLocation();
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		World w = c.getPlayer().getWorld();
		int ct =0;
		for(int xoffset=-distance;xoffset<=distance;xoffset++) {
			c.msg("Iteration...");
			for(int yoffset=-distance/2;yoffset<=distance/2;yoffset++) {
				for(int zoffset=-distance;zoffset<=distance;zoffset++) {
					int bx = x+xoffset;
					int by = y+yoffset;
					int bz = z+zoffset;
					ct += v.visit(w,bx,by,bz);
				}
			}
		}
		return ct;
	}

	/**
	 * Use this to get plugin instances - don't play silly buggers creating new
	 * ones all over the place!
	 */
	public static Plugin getInstance() {
		if (instance == null)
			throw new RuntimeException(
					"Attempt to get plugin when it's not enabled");
		return instance;
	}

	@Override
	public void onDisable() {
		RegionManager.saveRegionData(); // do this before disabling, it needs to get the plugin.
		instance = null;
		getLogger().info("JCFUtils has been disabled");
		saveConfig();
	}

	public Plugin(){
		super();
		if(instance!=null)
			throw new RuntimeException("oi! only one instance!");
	}

	@Override
	public void onEnable() {
		instance = this;
		ConfigurationSerialization.registerClass(Region.class,"JCFRegion");
		saveDefaultConfig();
		commandRegistry.register(this); // register commands
		getLogger().info("JCFUtils has been enabled");

		// load config (not regions, that's done elsewhere)
		loadConfigData();

		// NOW load the region data.
		RegionManager.loadRegionData();

		// register event listeners
		PluginManager mgr = Bukkit.getPluginManager();
		mgr.registerEvents(new CreatureSpawnListener(),this);
		mgr.registerEvents(new PlayerInteractListener(),this);
		mgr.registerEvents(new PlayerInteractEntityListener(),this);
		mgr.registerEvents(new BlockDropAndPlaceListener(),this);
		mgr.registerEvents(new PlayerMoveListener(),this);
	}


	public static void sendCmdMessage(CommandSender s,String msg){
		s.sendMessage(ChatColor.AQUA+"[JCFUtils] "+ChatColor.YELLOW+msg);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		String cn = command.getName();
		if(cn.equals("jcfutils")){
			commandRegistry.handleCommand(sender, args);
			return true;
		}
		return false;
	}

	/*
	 * Configuration
	 */

	void loadConfigData(){
		lightSpawnLevel = getConfig().getInt("lightspawnlevel");
	}

	/** return the block light level above which mobs will not naturally spawn */
	public static int getLightSpawnLevel(){
		return lightSpawnLevel;
	}
	private static int lightSpawnLevel;



	/*
	 * Various data
	 */
	public static int spawnsAttempted;
	public static int spawnsCancelled;

	/*
	 * Commands
	 */

	@Cmd(desc="show help for a command or list commands",argc=-1,usage="[<command name>]")
	public void help(CallInfo c){
		if(c.getArgs().length==0){
			commandRegistry.listCommands(c);
		} else {
			commandRegistry.showHelp(c,c.getArgs()[0]);
		}
	}

	@Cmd(desc="set block light level above which mobs won't spawn",usage="<level>",player=false,argc=1)
	public void light(CallInfo c){
		lightSpawnLevel = Integer.parseInt(c.getArgs()[0]);
		getConfig().set("lightspawnlevel",lightSpawnLevel);
		saveConfig();
	}

	@Cmd(desc="show information and config stuff",usage="",player=true,argc=0)
	public void info(CallInfo c){
		StringBuilder b = new StringBuilder();
		b.append(ChatColor.AQUA+"JCFUtils info: \n"+ChatColor.GREEN);
		b.append("LightSpawnLevel: "+lightSpawnLevel+"\n");
		b.append("number of entities cancelled vs. attempted: "+spawnsCancelled+"/"+spawnsAttempted);
		c.msg(b.toString());
	}

	@Cmd(desc="output material enums to file",usage="",player=false,argc=0)
	public void dumpmats(CallInfo c) {
		BufferedWriter w;
		try {
			w = new BufferedWriter(new FileWriter("/tmp/mats"));
			for(Material m: Material.values()) {
				w.write(m.name());
				w.newLine();
			}
			w.close();
			c.msg("materials written to /tmp/mats");
		} catch (IOException e) {
			c.msg("Cannot open /tmp/mats");
		}
	}



	@Cmd(desc="remove vines locally",usage="<distance>",player=true,argc=1)
	public void devine(CallInfo c){
		int distance = Integer.parseInt(c.getArgs()[0]);
		c.msg("de-vining");
		int ct=visit(c,distance,new BlockVisitor() {
			@Override
			public int visit(World w,int x, int y, int z) {
				Block b = w.getBlockAt(x,y,z);
				if(b.getType()==Material.VINE) {
					b.setType(Material.AIR);
					return 1;
				} else {
					return 0;
				}
			}});
	}

	@Cmd(desc="cover some stone with moss and cracks",usage="<distance>",player=true,argc=1)
	public void rot(CallInfo c) {
		int distance = Integer.parseInt(c.getArgs()[0]);
		c.msg("Rotting.");
		int ct=visit(c,distance,new BlockVisitor() {
			@Override
			public int visit(World w,int bx, int by, int bz) {
				int ct=0;
				Block b = w.getBlockAt(bx,by,bz);
				if(rand.nextDouble()<0.2) {// 1 in 20
					switch(b.getType()) {
					case STONE_BRICKS:
						if(rand.nextInt(4)==0) {
							ct+=grow(w,bx,by,bz,Material.MOSSY_STONE_BRICKS); // mossy
						} else
							ct+=grow(w,bx,by,bz,Material.CRACKED_STONE_BRICKS); // cracked
						break;
					case COBBLESTONE:
						ct++;
						if(rand.nextInt(2)==0) // rarer
							ct+=grow(w,bx,by,bz,Material.MOSSY_COBBLESTONE);
						break;
					case COBBLESTONE_WALL:
						if(rand.nextInt(2)==0) // rarer
							ct+=grow(w,bx,by,bz,Material.MOSSY_COBBLESTONE_WALL);
						break;
					default:
						break;
					}
				}
				return ct;
			}
		});
		StringBuilder sb = new StringBuilder();
		sb.append("Done: ");sb.append(ct);sb.append(" blocks changed.");
		c.msg(sb.toString());
	}

	@Cmd(desc="create a stick which will locate the item held",usage="",player=true,argc=0)
	public void dowse(CallInfo c) {
		Player p = c.getPlayer();
		PlayerInventory inv = p.getInventory();
		ItemStack st = inv.getItemInMainHand();
		if(st.getAmount()!=0) {
			Material m = st.getType();
			st = new ItemStack(Material.STICK);
			ItemMeta meta = st.getItemMeta();
			if(meta==null)c.msg("Meta is null");
			List<String> lore = new ArrayList<String>();
			// set the lore of the new stick to the name of the type we're looking for. Ugly.
			lore.add("Magic Stick");
			lore.add(m.name());
			meta.setDisplayName("Magic Stick for "+m.name());
			meta.setLore(lore);
			st.setItemMeta(meta);
			HashMap<Integer,ItemStack> couldntStore = inv.addItem(st);

			// drop remaining items at the player
			for(ItemStack s: couldntStore.values()){
				p.getWorld().dropItem(p.getLocation(), s);
			}

		}
	}
	
	@Cmd(desc="create a stick which will move mobs",usage="",player=true,argc=0)
	public void mobmover(CallInfo c) {
		Player p = c.getPlayer();
		PlayerInventory inv = p.getInventory();
		ItemStack st = inv.getItemInMainHand();
		if(st.getAmount()!=0) {
			Material m = st.getType();
			st = new ItemStack(Material.STICK);
			ItemMeta meta = st.getItemMeta();
			if(meta==null)c.msg("Meta is null");
			List<String> lore = new ArrayList<String>();
			// set the lore of the new stick to the name of the type we're looking for. Ugly.
			lore.add("Mob Mover Stick");
			lore.add(m.name());
			meta.setDisplayName("Mob Mover Stick");
			meta.setLore(lore);
			st.setItemMeta(meta);
			HashMap<Integer,ItemStack> couldntStore = inv.addItem(st);

			// drop remaining items at the player
			for(ItemStack s: couldntStore.values()){
				p.getWorld().dropItem(p.getLocation(), s);
			}

		}
	}

	@Cmd(desc="block in hand will delimit region AABB when dropped or placed",usage="",player=true,argc=0)
	public void regcreate(CallInfo c) {
		Player p = c.getPlayer();
		PlayerInventory inv = p.getInventory();
		ItemStack st = inv.getItemInMainHand();
		if(st.getAmount()!=0) {
			regionPlacingData.put(p,new RegionPlacingData(st.getType()));
			c.msg("Creating region with the item in hand");
		} else {
			c.msg("You have to be holding the block type which will be used to mark the region");
		}
	}

	@Cmd(desc="rename a region in this world",usage="<id|l(ast)> <name..>",player=true,argc=-1)
	public void regname(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		String[] args = c.getArgs();
		if(args.length<2) {
			c.msg("An ID (or l for last) and a some text must be provided");
			return;
		}
		Region r;
		if(args[0].equals("l"))r = RegionManager.getLastEdited(c.getPlayer());
		else r = rm.get(Integer.parseInt(args[0]));

		if(r==null)
			c.msg("Region unknown!");
		else {
			StringBuilder sb = new StringBuilder();
			int len = args.length;
			for(int i=1;i<len;i++) {
				sb.append(args[i]);
				if(i!=len-1)sb.append(" ");
			}
			r.name = sb.toString();
			c.msg("Region renamed to "+sb.toString());
			RegionManager.setLastEdited(c.getPlayer(), r);
		}
	}

	@Cmd(desc="re-bound a region in this world",usage="<id|l(ast)>",player=true,argc=1)
	public void regmod(CallInfo c)
	{
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		Region r;
		String[] args = c.getArgs();
		if(args[0].equals("l"))r = RegionManager.getLastEdited(c.getPlayer());
		else r = rm.get(Integer.parseInt(args[0]));
		if(r==null) {
			c.msg("Region unknown!");
			return;
		}
		Player p = c.getPlayer();
		PlayerInventory inv = p.getInventory();
		ItemStack st = inv.getItemInMainHand();
		if(st.getAmount()!=0) {
			regionPlacingData.put(p,new RegionPlacingData(st.getType(),r));
			c.msg("Creating region with the item in hand");
		} else {
			c.msg("You have to be holding the block type which will be used to mark the region");
		}
	}



	@Cmd(desc="delete a region in this world",usage="<id|l(ast)>",player=true,argc=1)
	public void regdel(CallInfo c)
	{
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		Region r;
		String[] args = c.getArgs();
		if(args[0].equals("l"))r = RegionManager.getLastEdited(c.getPlayer());
		else r = rm.get(Integer.parseInt(args[0]));
		if(r==null)
			c.msg("Region unknown!");
		else {
			RegionManager.setLastEdited(c.getPlayer(), null); // imagine the comedy if I didn't do this.
			rm.delete(r);
			c.msg("Region deleted");
		}		
	}

	@Cmd(desc="extend the given region to include my location",usage="<id|l(ast)>",player=true,argc=1)
	public void regext(CallInfo c)
	{
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		Region r;
		String[] args = c.getArgs();
		if(args[0].equals("l"))r = RegionManager.getLastEdited(c.getPlayer());
		else r = rm.get(Integer.parseInt(args[0]));
		if(r==null)
			c.msg("Region unknown!");
		else {
			RegionManager.setLastEdited(c.getPlayer(), r);
			rm.extend(r,c.getPlayer().getLocation());
			c.msg("Region extended:");
			showReg(c, r);
		}		
	}

	@Cmd(desc="unlink a region from its parent",usage="<id|l(ast)>",player=true,argc=1)
	public void regunlink(CallInfo c)
	{
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		Region r;
		String[] args = c.getArgs();
		if(args[0].equals("l"))r = RegionManager.getLastEdited(c.getPlayer());
		else r = rm.get(Integer.parseInt(args[0]));
		if(r==null)
			c.msg("Region unknown!");
		else {
			RegionManager.setLastEdited(c.getPlayer(), r);
			rm.unlink(r);
			c.msg("Region unlinked");
		}		
	}

	@Cmd(desc="link region(s) to another region, making it a child of that region",usage="<childid|l> <parentid>",player=true,argc=2)
	public void reglink(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		Region rthis;
		String[] args = c.getArgs();
		if(args[0].equals("l"))rthis = RegionManager.getLastEdited(c.getPlayer());
		else rthis = rm.get(Integer.parseInt(args[0]));
		if(rthis==null)
			c.msg("Region in first argument (child) unknown!");
		else {
			Region rparent = rm.get(Integer.parseInt(args[1]));
			if(rparent==null){
				c.msg("Region in second argument (parent) unknown!");
			} else {
				if(rparent == rthis) {
					c.msg("You can't link a region to itself");
					return;
				}
				if(rparent.link!=null) {
					c.msg("You can't link a region to a region which is itself linked");
					return;
				}

				RegionManager.setLastEdited(c.getPlayer(), rthis);
				rm.link(rparent,rthis);
				c.msg("Region linked");
			}
		}		

	}

	private void showReg(CallInfo c,Region r) {
		if(r.link!=null) {
			c.msg(String.format("%d [parent %d]: %s [vol %f]: %s",r.id,r.link.id,r.name,r.getVolume(),r.getAABBString()));				
		} else {
			c.msg(String.format("%d : %s [vol %f]: %s",r.id,r.name,r.getVolume(),r.getAABBString()));
		}
	}

	@Cmd(desc="show current region(s) the player is in or another if an ID is given",usage="",player=true,argc=-1)
	public void regshow(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		if(c.getArgs().length > 0) {
			Region r = rm.get(Integer.parseInt(c.getArgs()[0]));
			if(r==null) {
				c.msg("Unknown region ID");
				return;
			}
			showReg(c,r);
		} else {
			for(Region r: rm.getRegionList(c.getPlayer().getLocation())) {
				showReg(c,r);
			}
		}
	}

	@Cmd(desc="list all regions in this world, if arg provided will match names",usage="",player=true,argc=-1)
	public void reglist(CallInfo c)
	{
		String search=null;
		String[] args = c.getArgs();
		if(args.length>=1) {
			StringBuilder sb = new StringBuilder();
			int len = args.length;
			for(int i=0;i<len;i++) {
				sb.append(args[i]);
				if(i!=len-1)sb.append(" ");
			}
			search = sb.toString();
		}
		Plugin.log(Integer.toString(args.length));
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		List<Region> rl = rm.getAllRegions();
		c.msgAndLog(String.format("Region list (%d entries) [search=%s]",rl.size(),search));
		for(Region r: rm.getAllRegions()) {
			if(search == null || r.name.contains(search)) {
				if(r.link!=null)
					c.msgAndLog(String.format("%2d: %s [link to %d:%s]", r.id,r.name,r.link.id,r.link.name));
				else
					c.msgAndLog(String.format("%2d: %s", r.id,r.name));
			}
		}
	}

	private Set<Player> regDebugActivePlayers = new HashSet<Player>();
	@Cmd(desc="toggle show regions on enter/exit",usage="",player=true,argc=0)
	public void regdebug(CallInfo c) {
		if(regDebugActivePlayers.contains(c.getPlayer())) {
			regDebugActivePlayers.remove(c.getPlayer());
			c.msg("Debug now inactive");
		} else {
			regDebugActivePlayers.add(c.getPlayer());
			c.msg("Debug now active");
		}
	}

	public boolean isRegDebugActive(Player p) {
		return regDebugActivePlayers.contains(p);
	}


	@Cmd(desc="test command to save regions in this world",usage="",player=true,argc=0)
	public void regsave(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		rm.saveConfig();
		c.msg("regions saved OK (hopefully)");
	}

	@Cmd(desc="test command to load regions in this world",usage="",player=true,argc=0)
	public void regload(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		rm.loadConfig();
		c.msg("regions loaded OK (hopefully)");
	}


	@Cmd(desc="go to the nearest mob of given type (within 1000 blocks)",usage="<mobtype>",player=true,argc=1)
	public void gomob(CallInfo c){
		List<Entity> lst = c.getPlayer().getNearbyEntities(1000,1000,1000);
		EntityType t=null;
		try {
			t = EntityType.valueOf(c.getArgs()[0].toUpperCase());
		} catch(IllegalArgumentException e) {
			c.msg("No such entity type");return;
		}
		double dist=10000;
		Entity f=null;
		for(Entity e:lst) {
			if(e.getType() == t) {
				Location loc = e.getLocation();
				double d = loc.distance(c.getPlayer().getLocation());
				if(d<dist) {
					dist=d;
					f=e;
				}
			}
		}
		if(f!=null) {
			c.msg("Found one at "+(int)dist+" blocks away");
			c.getPlayer().teleport(f);
		} else
			c.msg("Didn't find one!");
	}
	
	@Cmd(desc="count mobiles within a given range",usage="<mobtype> <distance>",player=true,argc=2)
	public void countmobs(CallInfo c)
	{
		EntityType t=null;
		try {
			t = EntityType.valueOf(c.getArgs()[0].toUpperCase());
		} catch(IllegalArgumentException e) {
			c.msg("No such entity type");return;
		}
		int range = Integer.parseInt(c.getArgs()[1]);
		List<Entity> lst = c.getPlayer().getNearbyEntities(range,range,range);
		double dist=10000;
		int ct=0;
		Entity f=null;
		for(Entity e:lst) {
			if(e.getType() == t) {
				ct++;
				Location loc = e.getLocation();
				double d = loc.distance(c.getPlayer().getLocation());
				if(d<dist) {
					dist=d;
					f=e;
				}
			}
		}
		if(f!=null)
			c.msg("Found "+ct+", nearest at "+(int)dist+" blocks away");
		else
			c.msg("Didn't find any!");
		
	}
	
	private Map<Player,Entity> selectedMobMap = new HashMap<Player,Entity>();
	
	@Cmd(desc="select a mobile for moving",usage="",player=true,argc=0)
	public void selmob(CallInfo c) {
		Player p = c.getPlayer();
		List<Entity> lst = p.getNearbyEntities(20,20,20);
		for(Entity e:lst) {
			if(p.hasLineOfSight(e)) {
				for(Block b: p.getLineOfSight(null,100)) {
					if(e.getLocation().distance(b.getLocation())<3) {
						selectedMobMap.put(p,e);
						c.msg("Selected "+e.getName());
						return;						
					}
				}
			}
		}
		c.msg("No mobs in line of sight!");
	}
	
	@Cmd(desc="teleport selected mob to the block player is looking at",usage="",player=true,argc=0)
	public void telmob(CallInfo c) {
		Player p = c.getPlayer();
		Entity e = selectedMobMap.get(p);
		if(e==null)
			c.msg("no entity selected");
		else {
			List<Block> lst = p.getLastTwoTargetBlocks(null, 100);
			Block b;
			if(lst==null) {
				c.msg("no blocks in line of sight");
				return;
			}
			else if(lst.size()<2)
				b=lst.get(0);
			else
				b=lst.get(1);
			Location loc = b.getLocation();
			loc.setY(loc.getY()+1.1);
			e.teleport(loc);
		}
	}
	
	
	
	
	
	private int grow(World w,int x,int y,int z,Material newmat) {
		int ct=0;
		int n = rand.nextInt(15)+2;
		// record original type!
		Material origm = w.getBlockAt(x,y,z).getType();
		for(int i=0;i<n;i++) {
			x += rand.nextInt(3)-1;
			y += rand.nextInt(3)-1;
			z += rand.nextInt(3)-1;
			Block b = w.getBlockAt(x,y,z); 
			if(b.getType()==origm) {
				ct++;
				// this is to make sure we don't modify the block data for walls; the same faces need
				// to be enabled. When we do setType it completely resets the block data which contains
				// this info. So..
				BlockData bd = b.getBlockData(); // get the current block data
				if(bd!=null && bd instanceof Fence) { // if it's for a fence
					Fence of = (Fence)bd;              // get hold of it as a fence
					Fence f = (Fence)newmat.createBlockData(); // and make a fresh Fence block data with the new type
					for(BlockFace face: of.getFaces()) {
						f.setFace(face, true); // set the enabled faces in this new one to the same as the old
					}
					f.setWaterlogged(of.isWaterlogged()); // ditto the waterlogged data
					b.setBlockData(f); // and set the block to have the new block data (and material type)
				} else {
					b.setType(newmat);					
				}
			}
		}
		return ct;
	}
}
