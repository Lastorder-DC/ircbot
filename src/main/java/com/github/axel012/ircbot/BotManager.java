package com.github.axel012.ircbot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jibble.pircbot.PircBot;
import java.util.HashMap;
import java.util.Random;


public class BotManager extends PircBot {

    private static BotManager _instance = null;
    private  HashMap<String,Long> _chatTimeout;
    private  HashMap<String,HashMap<String,Long>> _commandTimeOut;
    private  HashMap<String,CommandData> _commands;
    private int CHAT_TIMEOUT;
    private boolean _chatEnabled = false;
    FileConfiguration _config = null;

    private  Random _random;

    private final  String[] _advices = {
            "i think is useful",
            "i don't care what you think",
            "it means nothing to me",
            "and die early",
            "Honk Honk Honk!!",
            "i made with my love",
            "if you are a goose",
            "i have many",
            "and don't ask questions",
            "or are you afraid?",
            "now destroy this item before its too late!"
    };

    BotManager(){
        _chatTimeout = new HashMap<>();
        _commandTimeOut = new HashMap<>();
        _commands = new HashMap<>();
        _random = new Random();
    }

    public static  BotManager getInstance(){
        if(_instance == null){
            _instance = new BotManager();
        }
        return  _instance;
    }

    public  void setConfig(FileConfiguration config){
        _config = config;
        try {
            _chatEnabled = _config.getBoolean("chat.enable");
            CHAT_TIMEOUT = _config.getInt("chat.timeout");
            this.setName(_config.getString("twitch.username"));
            ConfigurationSection commands = _config.getConfigurationSection("commands");
            if(commands == null)
                throw new Exception("Commands path not found");
            for(String commandName : commands.getKeys(false)){
                _commands.put(
                        commandName,
                        new CommandData(commands
                                .getConfigurationSection(commandName)
                                .getValues(true))
                        );
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public  boolean toggleChat(){
        _chatEnabled = !_chatEnabled;
        _chatTimeout.clear();
        return  _chatEnabled;
    }

    public  boolean isChatEnabled(){
        return  _chatEnabled;
    }

    public  String getCurrentChannel(){
        return  _config.getString("twitch.channel");
    }

    private  void _connect() throws  Exception{
            if(_config == null) throw new Exception("Couldn't load configuration file");
            String pwd = _config.getString("twitch.oauth_pwd","");
            if(pwd.isEmpty()) throw new Exception("twitch oauth_pwd is empty");
            this.connect(
                    "irc.twitch.tv",
                    6667,
                     pwd
            );
            this.joinChannel("#"+_config.getString("twitch.channel"));
    }

    public  boolean start() throws Exception{
            if(this.isConnected()) {
                return false;
            }else{
            _connect();
            return true;
            }
    }

    private  void clearTimeouts(){
        _chatTimeout.clear();
        _commandTimeOut.clear();
    }

    public  boolean stop(){
        if(this.isConnected()) {
            this.disconnect();
            this.clearTimeouts();
          return true;
        }else{
         return  false;
        }
    }
    
    public void sendChat(String chat){
        sendMessage("#"+this.getCurrentChannel(), chat);
    }

    public   void parseCmd(String cmd,String params,String sender){
        System.out.println(cmd);
          if(!_commands.containsKey(cmd)) return;
        CommandData cmdData = _commands.get(cmd);
        System.out.println(cmdData.data);

        if (!cmdData.enable) return;
        if(cmdData.shared)
            sender = "shared";
        //TODO: refactor
        if(!canExecuteCommand(sender,cmd)) return;
          if(cmd.equalsIgnoreCase("give")) {
              String data[] = params.split(" ", 2);
              String itemName = data[0];
              if (itemName.contains("{")) return;
              Material material = getItemMaterial(itemName);
              System.out.println(itemName);
              if (material == null) return;
              String command = String.format("give @a %s", itemName);
            /*if(cmdData.vote && !cmdData.shared){
                addVote(sender,itemName);
            }else*/
              ChatUtils.execCommand(command);
              String advice = getRandomAdvice();
              if (material.isAir())
                  itemName = "Air";
              else {
                  ItemStack itemStack = new ItemStack(material);
                  itemName = itemStack.getItemMeta().hasDisplayName() ?
                          itemStack.getItemMeta().getDisplayName() : itemName.replace("_", " ").toLowerCase();
              }
              ChatUtils.twitchChatToMinecraft(sender,
                      String.format("take this " + ChatColor.GOLD + "[%s] " + ChatColor.WHITE + "%s", itemName, advice));
          }

        if(cmd.equalsIgnoreCase("items")){
          sendMessage("#"+this.getCurrentChannel()," " + cmdData.data);
        }
        addCommandTimeout(cmd,sender);

    }

    private void addCommandTimeout(String command,String user){
        if(!_commandTimeOut.containsKey(command)){
            HashMap<String,Long> hashMap = new HashMap<>();
            hashMap.put(user,System.currentTimeMillis());
            _commandTimeOut.put(command,hashMap);
        }else{
            HashMap<String,Long> cmdTimeout = _commandTimeOut.get(command);
            cmdTimeout.put(user,System.currentTimeMillis());
        }
    }

    private String getRandomAdvice(){
        int index = _random.nextInt(_advices.length);
        return  _advices[index];
    }

    private Material getItemMaterial(String name){
        Material m = Material.matchMaterial(name);
        if(m == null) return  null;
        if(m.isItem() || m.isAir()) return m;
        return  null;
    }
    //TODO: implement
    private  void addVote(String sender,String itemName){
        System.out.println("user "+ sender + " vote " + itemName);
    }

    private boolean canExecuteCommand(String sender,String cmd){
        if(!_commandTimeOut.containsKey(cmd)) return  true;
        HashMap<String,Long> userTimeout = _commandTimeOut.get(cmd);
        if(!userTimeout.containsKey(sender)) return true;
        return  ( System.currentTimeMillis() - userTimeout.get(sender) ) > _commands.get(cmd).timeout;
    }

    private boolean canSendChatMessage(String sender){
        if(!_chatTimeout.containsKey(sender)) return  true;
        long time = _chatTimeout.get(sender);
        return  ( System.currentTimeMillis() - time ) > CHAT_TIMEOUT;
    }

    public void onMessage(String channel, String sender,
                          String login, String hostname, String message) {
        //TODO: add or not reply with bot
          if(message.startsWith("!")) {
            String raw[] = message.split(" ",2);
              String cmd = raw[0].replace("!","");
            String params = null;
            if(raw.length > 1) {
                params = raw[1];
            }
            this.parseCmd(cmd,params,sender);
        }else{
            if(!this.isChatEnabled()) return;
            if(!sender.equalsIgnoreCase(this.getName()) &&
                    !sender.toLowerCase().contains("bot")){
                if(!canSendChatMessage(sender))
                    return;
                ChatUtils.twitchChatToMinecraft(sender,message);
                _chatTimeout.put(sender,System.currentTimeMillis());
            }
        }
    }

}
