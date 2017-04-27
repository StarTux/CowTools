package com.winthier.cow_tools;

import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.item.ItemDescription;
import com.winthier.custom.item.UncraftableItem;
import com.winthier.custom.util.Dirty;
import com.winthier.custom.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter
public final class CowToolItem implements CustomItem, UncraftableItem {
    public static final String CUSTOM_ID = "cowtools:tool";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_COMMANDS = "commands";
    private final CowToolsPlugin plugin;
    private final String customId = CUSTOM_ID;

    CowToolItem(CowToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.WOOD_PICKAXE, amount);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event, ItemContext context) {
        switch (event.getAction()) {
        case RIGHT_CLICK_AIR:
        case RIGHT_CLICK_BLOCK:
            break;
        default: return;
        }
        event.setCancelled(true);
        ItemStack item = context.getItemStack();
        Player player = context.getPlayer();
        if (!isOwner(player, item)) {
            Msg.sendActionBar(player, "&cThis tool does not belong to you!");
            return;
        }
        useCowTool(player, item);
    }

    static List<String> getCommands(ItemStack item) {
        if (!Dirty.TagWrapper.hasItemConfig(item)) new ArrayList<>();
        return Dirty.TagWrapper.getItemConfigOf(item).getStringList(KEY_COMMANDS);
    }

    static UUID getOwner(ItemStack item) {
        if (!Dirty.TagWrapper.hasItemConfig(item)) return null;
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        String owner = config.getString(KEY_OWNER);
        try {
            return UUID.fromString(owner);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    static boolean isOwner(Player player, ItemStack item) {
        return player.getUniqueId().equals(getOwner(item));
    }

    int useCowTool(Player player, ItemStack item) {
        List<String> commands = getCommands(item);
        if (commands.isEmpty()) return 0;
        int count = 0;
        for (String command: commands) {
            String label = command.split(" ", 2)[0].toLowerCase().substring(1);
            if (plugin.getBannedCommandList().contains(label)) {
                plugin.getLogger().warning(String.format("%s used item with banned command: %s", player.getName(), command));
            } else {
                // TODO: Replacements?
                Msg.send(player, "&9Issuing command: &r%s", command);
                plugin.getLogger().info(String.format("%s issues command: %s", player.getName(), command));
                player.performCommand(command.substring(1));
                count += 1;
            }
        }
        return count;
    }

    void storeCommands(Player player, ItemStack item, List<String> commands) {
        Dirty.TagWrapper.getItemConfigOf(item).setString(KEY_OWNER, player.getUniqueId().toString());
        Dirty.TagWrapper.getItemConfigOf(item).setStringList(KEY_COMMANDS, commands);
        ItemDescription desc = ItemDescription.of(plugin.getConfig().getConfigurationSection("Description"));
        desc.getStats().put("Owner", player.getName());
        StringBuilder sb = new StringBuilder(Msg.format("&aCOMMANDS"));
        int index = 0;
        for (String command: commands) sb.append("\n").append(Msg.format("&a%d)&r %s", ++index, command));
        desc.setDescription(sb.toString());
        desc.apply(item);
        ItemMeta meta = item.getItemMeta();
        for (ItemFlag flag: ItemFlag.values()) meta.addItemFlags(flag);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
    }
}
