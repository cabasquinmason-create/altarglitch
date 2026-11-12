package com.custom.altarglitch;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AltarGlitchPlugin extends JavaPlugin {
    private NamespacedKey glitchKey;
    private AltarManager altarManager;

    @Override
    public void onEnable() {
        this.glitchKey = new NamespacedKey(this, "glitch_id");
        this.altarManager = new AltarManager(this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this, altarManager), this);
        getLogger().info("AltarGlitchSMP enabled!");
    }

    public NamespacedKey getGlitchKey() {
        return glitchKey;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (command.getName().equalsIgnoreCase("glitch")) {
            altarManager.openLoadoutGUI(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("altar")) {
            if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
                GlitchType type = GlitchType.fromId(args[1]);
                if (type == null) {
                    player.sendMessage("§cValid types: bedrock, rewind, freeze");
                    return true;
                }
                altarManager.spawnAltar(player.getLocation(), type);
                player.sendMessage("§aSpawned " + type.displayName + " §aAltar!");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
                altarManager.resetCrafted();
                player.sendMessage("§eAll 1-of-1 server craft limits have been reset.");
                return true;
            }
            player.sendMessage("§eUsage: /altar spawn <bedrock|rewind|freeze> | /altar reset");
        }
        return true;
    }
}
