package org.pale.jcfutils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
	public int getLightSpawnLevel(){
		return lightSpawnLevel;
	}
	private int lightSpawnLevel;
	
	
	
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

	@Cmd(desc="show information and config",player=true,argc=0)
	public void info(CallInfo c){
		StringBuilder b = new StringBuilder();
		b.append(ChatColor.AQUA+"JCFUtils info: \n"+ChatColor.GREEN);
		b.append("LightSpawnLevel: "+lightSpawnLevel+"\n");
		b.append("number of entities cancelled vs. attempted: "+spawnsCancelled+"/"+spawnsAttempted);
		c.msg(b.toString());
	}

}
