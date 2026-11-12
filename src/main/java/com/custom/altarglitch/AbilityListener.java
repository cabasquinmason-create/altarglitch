package com.custom.altarglitch;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AbilityListener implements Listener {
    private final AltarGlitchPlugin plugin;
    private final AltarManager manager;

    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Set<UUID> bedrockActive = new HashSet<>();
    private final Set<Location> tempBedrock = new HashSet<>();
    private final Map<UUID, RewindState> rewindAnchors = new HashMap<>();

    private record RewindState(Location loc, double health) {}

    public AbilityListener(AltarGlitchPlugin plugin, AltarManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            if (manager.handleAltarInteract(e.getPlayer(), e.getClickedBlock())) {
                e.setCancelled(true);
                return;
            }
        }

        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.getPlayer().isSneaking()) {
            triggerSlot(e.getPlayer(), 0);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        if (e.getPlayer().isSneaking()) {
            e.setCancelled(true);
            triggerSlot(e.getPlayer(), 1);
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals("§8Glitch Loadout (2 Slots)")) return;
        Player player = (Player) e.getPlayer();
        GlitchType[] slots = manager.getSlots(player.getUniqueId());

        slots[0] = extractGlitch(e.getInventory().getItem(3));
        slots[1] = extractGlitch(e.getInventory().getItem(5));
        player.sendMessage("§aGlitch Loadout updated!");
    }

    private GlitchType extractGlitch(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(plugin.getGlitchKey(), PersistentDataType.STRING);
        return id != null ? GlitchType.fromId(id) : null;
    }

    private void triggerSlot(Player player, int slotIndex) {
        GlitchType glitch = manager.getSlots(player.getUniqueId())[slotIndex];
        if (glitch == null) return;

        if (glitch == GlitchType.REWIND && rewindAnchors.containsKey(player.getUniqueId())) {
            RewindState state = rewindAnchors.remove(player.getUniqueId());
            player.teleport(state.loc());
            player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), state.health()));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
            player.sendMessage("§dRewound to anchor point!");
            return;
        }

        String cdKey = player.getUniqueId() + ":" + glitch.id;
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(cdKey) && cooldowns.get(cdKey) > now) {
            long left = (cooldowns.get(cdKey) - now) / 1000;
            player.sendMessage("§c" + glitch.displayName + " §cis on cooldown for §e" + left + "s§c!");
            return;
        }

        cooldowns.put(cdKey, now + (glitch.cooldownSeconds * 1000L));
        executeAbility(player, glitch);
    }

    private void executeAbility(Player player, GlitchType glitch) {
        switch (glitch) {
            case BEDROCK -> {
                bedrockActive.add(player.getUniqueId());
                player.sendMessage("§8[§7Bedrock Protocol§8] §aPlaced blocks turn to Bedrock for 10s!");
                Bukkit.getScheduler().runTaskLater(plugin, () -> bedrockActive.remove(player.getUniqueId()), 200L);
            }
            case REWIND -> {
                rewindAnchors.put(player.getUniqueId(), new RewindState(player.getLocation(), player.getHealth()));
                player.sendMessage("§d[Chrono Rewind] §fAnchor set! Reactivate within 8s to snap back.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> rewindAnchors.remove(player.getUniqueId()), 160L);
            }
            case FREEZE -> {
                Entity target = player.getTargetEntity(15);
                if (!(target instanceof Player victim)) {
                    player.sendMessage("§cLook at a player within 15 blocks to freeze them!");
                    cooldowns.remove(player.getUniqueId() + ":" + glitch.id);
                    return;
                }
                trapInIceBox(victim);
                player.sendMessage("§b[Cryo Lock] §fFroze §b" + victim.getName() + " §ffor 5s!");
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (bedrockActive.contains(e.getPlayer().getUniqueId())) {
            Block b = e.getBlockPlaced();
            b.setType(Material.BEDROCK);
            tempBedrock.add(b.getLocation());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (tempBedrock.remove(b.getLocation())) b.setType(Material.AIR);
            }, 300L);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (tempBedrock.contains(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    private void trapInIceBox(Player victim) {
        Location center = victim.getLocation().getBlock().getLocation();
        List<Block> cage = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0 && (y == 0 || y == 1)) continue;
                    Block b = center.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.AIR) {
                        b.setType(Material.PACKED_ICE);
                        cage.add(b);
                    }
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> cage.forEach(b -> b.setType(Material.AIR)), 100L);
    }
}
