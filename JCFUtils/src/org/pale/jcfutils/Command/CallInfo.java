package org.pale.jcfutils.Command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pale.jcfutils.Plugin;

public class CallInfo {
	private final CommandSender sender;
	private final Player p;
	private final String[] args;
	private String cmd;
	
	public CallInfo (String cmd,CommandSender p, String[] args){
		this.sender = p;
		this.p = (p instanceof Player)?(Player)p:null;
		this.args = args;
		this.cmd=cmd;
	}
	
	public String getCmd(){
		return cmd;
	}
	
	public Player getPlayer(){
		return p;
	}
	
	public String[] getArgs(){
		return args;
	}
	
	public void msg(String s){
		Plugin.sendCmdMessage(sender,s);
	}
	public void msgAndLog(String s) {
		Plugin.log(s);
		Plugin.sendCmdMessage(sender,s);
	}
}
