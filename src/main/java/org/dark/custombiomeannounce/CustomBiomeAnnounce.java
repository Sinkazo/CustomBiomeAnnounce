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

public class CustomBiomeAnnounce extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private List<String> blockedWorlds;

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        config = getConfig();
        blockedWorlds = config.getStringList("blocked_world"); // Load blocked worlds

        // Register the listener
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Logic for when the plugin is disabled (if necessary)
    }

    // Event triggered when a player moves
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if the player's world is in the blocked worlds list
        if (blockedWorlds.contains(player.getWorld().getName())) {
            return; // Ignore if the world is blocked
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if the player moved to a different block
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Get the biome at the new location
        Biome biome = to.getBlock().getBiome();
        Biome lastBiome = from.getBlock().getBiome();

        // If the biome has changed
        if (!biome.equals(lastBiome)) {
            String biomeName = biome.name().replace('_', ' ').toLowerCase();
            String formattedBiomeName = ChatColor.GREEN + capitalize(biomeName);

            // Show title if enabled
            if (config.getBoolean("title.enabled")) {
                String titleMessage = ChatColor.YELLOW + config.getString("title.message_prefix") + formattedBiomeName;
                player.sendTitle(titleMessage, "",
                        config.getInt("title.fade_in"),
                        config.getInt("title.stay"),
                        config.getInt("title.fade_out"));
            }

            // Send chat message if enabled
            if (config.getBoolean("message.enabled")) {
                String chatMessage = config.getString("message.text").replace("%biome%", formattedBiomeName);
                player.sendMessage(ChatColor.GOLD + chatMessage);
            }

            // Play sound if enabled
            if (config.getBoolean("sound.enabled")) {
                String soundName = config.getString("sound.type").toUpperCase().replace(".", "_");
                Sound sound;

                try {
                    sound = Sound.valueOf(soundName);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "The sound " + soundName + " is not recognized. Using default sound.");
                    sound = Sound.ENTITY_PLAYER_LEVELUP; // Default sound
                }

                float volume = (float) config.getDouble("sound.volume");
                float pitch = (float) config.getDouble("sound.pitch");
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    // Command handling for "cba"
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("cba")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                // Reload the configuration
                reloadConfig();
                config = getConfig(); // Update the config reference
                blockedWorlds = config.getStringList("blocked_world"); // Reload blocked worlds
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /cba reload");
            }
        }
        return false;
    }

    // Helper method to capitalize the first letter of each word
    private String capitalize(String str) {
        String[] words = str.split(" ");
        StringBuilder capitalizedWords = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                capitalizedWords.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return capitalizedWords.toString().trim();
    }
}
