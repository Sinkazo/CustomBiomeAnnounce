package org.dark.custombiomeannounce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Biome;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CustomBiomeAnnounce extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private List<String> blockedWorlds;
    private Map<String, BiomeConfig> biomeConfigs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        blockedWorlds = config.getStringList("blocked_world");
        loadBiomeConfigs();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadBiomeConfigs() {
        biomeConfigs = new HashMap<>();
        if (config.getConfigurationSection("biome_config") != null) {
            for (String biomeName : config.getConfigurationSection("biome_config").getKeys(false)) {
                String displayName = config.getString("biome_config." + biomeName + ".display_name", "Unknown Biome");
                String soundName = config.getString("biome_config." + biomeName + ".sound", "ENTITY_PLAYER_LEVELUP");
                biomeConfigs.put(biomeName, new BiomeConfig(displayName, soundName));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (blockedWorlds.contains(player.getWorld().getName())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Biome biome = to.getBlock().getBiome();
        Biome lastBiome = from.getBlock().getBiome();

        if (!biome.equals(lastBiome)) {
            String biomeName = biome.name().toLowerCase();
            BiomeConfig biomeConfig = biomeConfigs.getOrDefault(biomeName, new BiomeConfig("Unknown Biome", "ENTITY_PLAYER_LEVELUP"));
            String formattedBiomeName = translateColorCodes(biomeConfig.getDisplayName());

            if (config.getBoolean("title.enabled")) {
                String titleMessage = ChatColor.YELLOW + config.getString("title.message_prefix") + formattedBiomeName;
                player.sendTitle(titleMessage, "",
                        config.getInt("title.fade_in"),
                        config.getInt("title.stay"),
                        config.getInt("title.fade_out"));
            }

            if (config.getBoolean("message.enabled")) {
                String chatMessage = config.getString("message.text").replace("%biome%", formattedBiomeName);
                player.sendMessage(ChatColor.GOLD + chatMessage);
            }

            if (config.getBoolean("sound.enabled")) {
                playBiomeSound(player, biomeConfig.getSound());
            }
        }
    }

    private void playBiomeSound(Player player, String soundName) {
        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
        } catch (IllegalArgumentException e) {
            getLogger().warning("The sound " + soundName + " is not recognized. Using default sound.");
            sound = Sound.ENTITY_PLAYER_LEVELUP;
        }
        float volume = (float) config.getDouble("sound.volume");
        float pitch = (float) config.getDouble("sound.pitch");
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private String translateColorCodes(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("cba")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = getConfig();
                blockedWorlds = config.getStringList("blocked_world");
                loadBiomeConfigs();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /cba reload");
            }
        }
        return false;
    }

    private class BiomeConfig {
        private final String displayName;
        private final String sound;

        public BiomeConfig(String displayName, String sound) {
            this.displayName = displayName;
            this.sound = sound;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSound() {
            return sound;
        }
    }
}