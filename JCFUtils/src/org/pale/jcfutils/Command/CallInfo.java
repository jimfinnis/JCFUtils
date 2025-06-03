package org.pale.jcfutils.Command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pale.jcfutils.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.World;

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
    
    // if there no player, the command is assumed to take place in the overworld.
    // This is used by the region commands.
    
    public World getWorld() {
        if(p==null)
            return Bukkit.getWorld("world");
        else
            return p.getWorld();
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
