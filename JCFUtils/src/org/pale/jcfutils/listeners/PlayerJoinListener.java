package org.pale.jcfutils.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.pale.jcfutils.PlayerLog;

import java.util.ArrayList;

public class PlayerJoinListener implements Listener {
    private PlayerLog log;

    public PlayerJoinListener(PlayerLog log){
        this.log = log;
    }
    @EventHandler
    public void join(final PlayerJoinEvent e){
        log.addJoin(e.getPlayer());
    }
    @EventHandler
    public void kicked(final PlayerKickEvent e){
        log.addKick(e.getPlayer());

    }
    @EventHandler
    public void quit(final PlayerQuitEvent e){
        log.addQuit(e.getPlayer());

    }
}
