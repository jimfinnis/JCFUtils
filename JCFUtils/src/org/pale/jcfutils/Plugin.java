package org.pale.jcfutils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
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
		RegionPlacingData(Material mat){
			m = mat;
			placeCount=0;
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

	@Cmd(desc="set block light level above which mobs won't spawn",player=false,argc=1)
	public void light(CallInfo c){
		lightSpawnLevel = Integer.parseInt(c.getArgs()[0]);
		getConfig().set("lightspawnlevel",lightSpawnLevel);
		saveConfig();
	}

	@Cmd(desc="show information and config stuff",player=true,argc=0)
	public void info(CallInfo c){
		StringBuilder b = new StringBuilder();
		b.append(ChatColor.AQUA+"JCFUtils info: \n"+ChatColor.GREEN);
		b.append("LightSpawnLevel: "+lightSpawnLevel+"\n");
		b.append("number of entities cancelled vs. attempted: "+spawnsCancelled+"/"+spawnsAttempted);
		c.msg(b.toString());
	}
	
	@Cmd(desc="output material enums to file",player=false,argc=0)
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



	@Cmd(desc="remove vines locally",player=true,argc=1)
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

	@Cmd(desc="cover some stone with moss and cracks",player=true,argc=1)
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
	
	@Cmd(desc="create a stick which will locate the item held",player=true,argc=0)
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
	
	@Cmd(desc="block in hand will delimit region AABB when dropped or placed",player=true,argc=0)
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
	
	@Cmd(desc="<id> <name..> rename a region in this world",player=true,argc=-1)
	public void regname(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		String[] args = c.getArgs();
		if(args.length<2) {
			c.msg("An ID and a some text must be provided");
			return;
		}
		Region r = rm.get(Integer.parseInt(args[0]));
		
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
		}
	}

	@Cmd(desc="delete a region in this world",player=true,argc=1)
	public void regdel(CallInfo c)
	{
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		int id = Integer.parseInt(c.getArgs()[0]);
		Region r = rm.get(id);
		if(r==null)
			c.msg("Region unknown!");
		else {
			rm.delete(r);
			c.msg("Region deleted");
		}		
	}
	
	@Cmd(desc="extend the given region to include my location",player=true,argc=1)
	public void regext(CallInfo c)
	{
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		int id = Integer.parseInt(c.getArgs()[0]);
		Region r = rm.get(id);
		if(r==null)
			c.msg("Region unknown!");
		else {
			r.extend(c.getPlayer().getLocation());
			c.msg("Region extended");
		}		
		
	}

	@Cmd(desc="list all regions in this world",player=true,argc=0)
	public void reglist(CallInfo c)
	{
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		List<Region> rl = rm.getAllRegions();
		c.msg(String.format("Region list (%d entries)",rl.size()));
		for(Region r: rm.getAllRegions()) {
			c.msg(String.format("%2d: %s", r.id,r.name));
		}
	}
	

	@Cmd(desc="test command to save regions in this world",player=true,argc=0)
	public void regsave(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		rm.saveConfig();
		c.msg("regions saved OK (hopefully)");
	}
	
	@Cmd(desc="test command to load regions in this world",player=true,argc=0)
	public void regload(CallInfo c) {
		RegionManager rm = RegionManager.getManager(c.getPlayer().getWorld());
		rm.loadConfig();
		c.msg("regions loaded OK (hopefully)");
	}
		
	
	@Cmd(desc="locate the nearest mob of given type",player=true,argc=1)
	public void lookmob(CallInfo c){
		List<Entity> lst = c.getPlayer().getNearbyEntities(60, 60, 60);
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
		if(f!=null)
			c.msg("Found one at "+(int)dist+" blocks away");
		else
			c.msg("Didn't find one!");
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
				b.setType(newmat);
			}
		}
		return ct;
	}
}
