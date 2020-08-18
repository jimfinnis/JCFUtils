package org.pale.jcfutils.Command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pale.jcfutils.Plugin;

public class Registry {
    static final String mainCommand="jcf";
    
    class Entry {
        String name;
        String permission;
        Method m;
        Object obj;
        private Cmd cmd;
        
        
        
        Entry(String name,String perm,Cmd cmd,Object o,Method m){
            this.permission=perm;
            this.cmd=cmd;
            this.m=m;
            this.obj=o;
            this.name = name;
        }
        
        private boolean checkPermission(Player p){
            return p==null || permission==null || p.hasPermission(permission);
        }
        
        public void invoke(CallInfo c){
            try {
                Player p = c.getPlayer();
                if(p==null && cmd.player()){
                    c.msg("That command requires a player");
                    return;
                }
                if(!checkPermission(p)){
                    c.msg("You do not have the permission "+permission);
                    return;
                }
                // -1 is "varargs"
                if(cmd.argc()>=0 && c.getArgs().length!=cmd.argc()){
                    showHelp(c,c.getCmd());
                    return;
                }
                m.invoke(obj, c);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
    
    private Map<String,Entry> registry = new TreeMap<String,Entry>();
    
    
    public void register(Object handler){
        for(Method m : sortedMethods(handler)){
            Cmd cmd = m.getAnnotation(Cmd.class);
            if(cmd!=null){
                Class<?> params[] = m.getParameterTypes();
                if(params.length != 1 || !params[0].equals(CallInfo.class)){
                    Plugin.warn("Error in @Sub on method "+m.getName()+": parameter must be one CallInfo");
                } else {					
                    String name = cmd.name();
                    if(name.equals(""))name = m.getName();
                    String perm = cmd.permission();
                    if(perm.equals(""))perm = null;
                    registry.put(name, new Entry(name,perm,cmd,handler,m));
                    Plugin.warn("Registered "+name);
                }
            }
        }
    }
    
    /**
     * Handle a command string, assuming the command name is correct for the plugin.
     * @param s
     */
    public void handleCommand(CommandSender sender,String[] args){
        if(args.length == 0){
            // if the user has just done /plugin, print all the subcommands.
            String cmds="";
            for(String cc: registry.keySet())cmds+=cc+" ";
            Plugin.sendCmdMessage(sender,cmds);
        } else {
            // otherwise the first arg is the command name.
            String cmdName = args[0];
            if(!registry.containsKey(cmdName)){
                Plugin.sendCmdMessage(sender,"unknown jcfutils command: "+cmdName);
            } else {
                Entry e = registry.get(cmdName);
                args = Arrays.copyOfRange(args, 1, args.length);
                e.invoke(new CallInfo(cmdName,sender,args));
            }
        }
    }
    
    
    /**
     * Reflect the methods on this object, sorted by name.
     * @param handler
     * @return an ArrayList of methods.
     */
    private ArrayList<Method> sortedMethods(Object handler) {
        TreeMap<String, Method> methodMap = new TreeMap<String, Method>();
        for (Method method : handler.getClass().getDeclaredMethods()) {
            methodMap.put(method.getName(), method);
        }
        return new ArrayList<Method>(methodMap.values());
    }
    
    public void listCommandsBrief(CallInfo c) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.AQUA);
        for(String cc:registry.keySet())
            sb.append(cc).append(' ');
        c.msg(sb.toString());
    }
    
    static final int PERPAGE=10;
    public void listCommandsPage(CallInfo c,int page) {
        int n=0;
        int start = (page-1)*PERPAGE;
        int end = start+PERPAGE;
        int maxPage = registry.size()/PERPAGE+1;
//        c.msg(ChatColor.YELLOW+Integer.toString(start)+"-"+Integer.toString(end));
        c.msg(ChatColor.RED+"Page " + Integer.toString(page)+ " of " + Integer.toString(maxPage));
        for(String cc:registry.keySet()){
            if(n>=start && n<end)
                showHelp(c,cc);
            n++;
        }
    }
    
    public void showHelp(CallInfo c, String cmdName) {
        if(registry.containsKey(cmdName)){
            Entry e = registry.get(cmdName);
            c.msg(ChatColor.AQUA+mainCommand+" "+e.name+" "+e.cmd.usage()+":"+ChatColor.GREEN+" "+e.cmd.desc());
        } else {
            c.msg("No such command. try \""+mainCommand+" help\" on its own.");
        }
    }
}
