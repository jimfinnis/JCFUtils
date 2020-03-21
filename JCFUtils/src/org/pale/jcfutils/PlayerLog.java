package org.pale.jcfutils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.UUID;

public class PlayerLog implements Iterable<String> {
    private class LogIterator implements Iterator<String> {
        int current;
        @Override
        public boolean hasNext() {
            return current<events.size();
        }

        @Override
        public String next() {
            return events.get(current++).logOutput();
        }
    }


    @Override
    public Iterator<String> iterator() {
        return new LogIterator();
    }

    static class Event {
        enum Type { JOIN,QUIT,KICKED};
        Type type;
        Instant time;
        UUID player;

        Event(Type t,UUID p,Instant tt){
            type = t;
            player = p;
            time = tt;
        }

        Event(Type t,Player p){
            type = t;
            player = p.getUniqueId();
            time = Instant.now();
        }

        @Override public String toString(){
            return type.toString()+"!"+player.toString()+"!"+time.toString();
        }

        String logOutput(){
            String pname;
            OfflinePlayer p = Bukkit.getOfflinePlayer(player);
            if(p==null){
                pname = player.toString();
            }
            else pname = p.getName();

            LocalDateTime ldt = LocalDateTime.ofInstant(time, ZoneOffset.UTC);
            String t = ldt.toString();
            switch(type){
                case JOIN:
                    return String.format("JOIN:  %s at %s",pname,t);
                case QUIT:
                    return String.format("QUIT:  %s at %s",pname,t);
                case KICKED:
                    return String.format("KICKED: %s at %s",pname,t);
                default:
                    return String.format("??????: %s at %s",pname,t);
            }
        }

        public static Event deserialise(String s){
            String[] arr = s.split("\\!");
            if(arr.length!=3){
                throw new RuntimeException("Bad serial data: "+s);
            }
            return new Event(Type.valueOf(arr[0]),
                    UUID.fromString(arr[1]),
                    Instant.parse(arr[2]));
        }
    }
    private ArrayList<Event> events = new ArrayList<Event>();

    void save(){
        try {
            File logFile = new File(Plugin.getInstance().getDataFolder(), "playerlog.log");
            PrintWriter pw = new PrintWriter(logFile);
            for(Event e: events){
                pw.println(e.toString());
            }
            pw.println("END");
            pw.flush();
            Plugin.log("Log saved");
        } catch(java.io.IOException e){
            throw new RuntimeException("Error in saving log: "+e.getMessage());
        }
    }

    void load(){
        File logFile = new File(Plugin.getInstance().getDataFolder(),"playerlog.log");
        try {
            Scanner in = new Scanner(logFile);
            while(in.hasNext()){
                String s = in.nextLine();
                if(s.equals("END"))break;
                events.add(Event.deserialise(s));
            }
        } catch(FileNotFoundException e) {
            Plugin.log("No log file found");
        }
    }

    void clear(){
        events.clear();
    }

    public void addKick(Player p){
        events.add(new Event(Event.Type.KICKED,p));
    }
    public void addQuit(Player p){
        events.add(new Event(Event.Type.QUIT,p));
    }
    public void addJoin(Player p){
        events.add(new Event(Event.Type.JOIN,p));
    }



}
