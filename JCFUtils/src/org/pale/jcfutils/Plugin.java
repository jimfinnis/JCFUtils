package org.pale.jcfutils;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.pale.jcfutils.Command.CallInfo;
import org.pale.jcfutils.Command.Cmd;
import org.pale.jcfutils.Command.Registry;
import org.pale.jcfutils.listeners.CreatureSpawnListener;



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

	private Registry commandRegistry=new Registry();

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
		saveDefaultConfig();
		commandRegistry.register(this); // register commands
		getLogger().info("JCFUtils has been enabled");

		// load config
		loadConfigData();

		// register event listeners
		PluginManager mgr = Bukkit.getPluginManager();
		mgr.registerEvents(new CreatureSpawnListener(),this);
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

	@Cmd(desc="cover some stone with moss and cracks",player=true,argc=1)
	public void rot(CallInfo c) {
		int distance = Integer.parseInt(c.getArgs()[0]);
		c.msg("Rotting.");
		Location l = c.getPlayer().getLocation();
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		World w = c.getPlayer().getWorld();
		int ct=0;
		for(int xoffset=-distance;xoffset<=distance;xoffset++) {
			c.msg("Iteration...");
			for(int yoffset=-distance/2;yoffset<=distance/2;yoffset++) {
				for(int zoffset=-distance;zoffset<=distance;zoffset++) {
					int bx = x+xoffset;
					int by = y+yoffset;
					int bz = z+zoffset;
					Block b = w.getBlockAt(bx,by,bz);
					if(rand.nextDouble()<0.2) {// 1 in 20
						StringBuilder ssb = new StringBuilder();
						switch(b.getType()) {	
						case SMOOTH_BRICK:
							if(b.getData() == 0) {
								if(rand.nextInt(4)==0) {
									ct+=grow(w,bx,by,bz,Material.SMOOTH_BRICK,1); // mossy
								} else
									ct+=grow(w,bx,by,bz,Material.SMOOTH_BRICK,2); // cracked
							}
							break;
						case COBBLESTONE:
							ct++;
							if(rand.nextInt(2)==0) // rarer
								ct+=grow(w,bx,by,bz,Material.MOSSY_COBBLESTONE,0);
							break;
						case COBBLE_WALL:
							if(b.getData()==0 && rand.nextInt(2)==0) // rarer
								ct+=grow(w,bx,by,bz,Material.COBBLE_WALL,1);
							break;
						}
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Done: ");sb.append(ct);sb.append(" blocks changed.");
		c.msg(sb.toString());
	}
	
	private int grow(World w,int x,int y,int z,Material newmat,int newdata) {
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
				b.setData((byte)newdata);
			}
		}
		return ct;
	}

}
