package com.github.axel012.ircbot;
import com.github.axel012.ircbot.Utils.Utils;
import com.github.axel012.ircbot.commands.CommandIrcBotStart;
import com.github.axel012.ircbot.commands.CommandIrcBotStop;
import com.github.axel012.ircbot.commands.CommandIrcBotToggleChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

class GamePlayerChatListener implements Listener {    
    /**
     *
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if(!this.isChatEnabled()) return;
        
        String name = event.getPlayer().getPlayerListName();
        String message = event.getMessage();
        BotManager.getInstance().sendChat("<" + name + "> " + message);
    }
}

public final class IrcMain extends JavaPlugin {
    FileConfiguration _config;
    @Override
    public void onEnable() {
        _config = this.getConfig();
        _config.options().copyDefaults(true);
        saveConfig();
        try {
            System.out.println("Loading plugin IRC BOT...");
            System.out.println("Generating items list ...");
            Utils.generateItemsList();
            System.out.println("Verifing config set");
            verifyConfigSet();
            BotManager.getInstance().setConfig(_config);
            getServer().getPluginManager().registerEvents(new GamePlayerChatListener(this), this);
            this.getCommand("start").setExecutor(new CommandIrcBotStart());
            this.getCommand("stop").setExecutor(new CommandIrcBotStop());
            this.getCommand("togglechat").setExecutor(new CommandIrcBotToggleChat());
        }catch (Exception e){
            e.printStackTrace();
            this.setEnabled(false);
        }
    }

    private void verifyConfigSet() throws  Exception{
        boolean isChannelEmpty = _config.getString("twitch.channel").isEmpty();
        boolean isUserNameEmpty = _config.getString("twitch.username").isEmpty();
        boolean isOauthPwdEmpty = _config.getString("twitch.oauth_pwd").isEmpty();
        if(isChannelEmpty || isUserNameEmpty || isOauthPwdEmpty){
            throw new Exception("You need to set up your plugin config file");
        }
    }

    @Override
    public void onDisable() {
        BotManager.getInstance().stop();
    }
}
