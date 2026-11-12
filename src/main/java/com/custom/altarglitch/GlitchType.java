package com.custom.altarglitch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public enum GlitchType {
    BEDROCK(
            "bedrock",
            "§8§l[GLITCH] §7Bedrock Protocol",
            Material.BEDROCK,
            Material.OBSIDIAN, 32,
            45,
            List.of("§7Placed blocks become unbreakable", "§7Bedrock for §e10 seconds§7.")
    ),
    REWIND(
            "rewind",
            "§d§l[GLITCH] §5Chrono Rewind",
            Material.CLOCK,
            Material.ENDER_PEARL, 16,
            40,
            List.of("§7Set an anchor point; reactivate", "§7within §e8s §7to rewind position & HP.")
    ),
    FREEZE(
            "freeze",
            "§b§l[GLITCH] §3Cryo Lock",
            Material.PACKED_ICE,
            Material.DIAMOND, 12,
            35,
            List.of("§7Traps your target in an", "§7inescapable box for §e5 seconds§7.")
    );

    public final String id;
    public final String displayName;
    public final Material icon;
    public final Material ingredient;
    public final int ingredientCost;
    public final int cooldownSeconds;
    public final List<String> lore;

    GlitchType(String id, String displayName, Material icon, Material ingredient, int ingredientCost, int cooldownSeconds, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.ingredient = ingredient;
        this.ingredientCost = ingredientCost;
        this.cooldownSeconds = cooldownSeconds;
        this.lore = lore;
    }

    public ItemStack createItem(NamespacedKey key) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    public static GlitchType fromId(String id) {
        for (GlitchType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
