package com.winthier.cow_tools;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.util.Msg;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class CowToolsPlugin extends JavaPlugin implements Listener {
    private CowToolItem cowToolItem;
    private int maxCommands;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        reloadConfig();
        maxCommands = getConfig().getInt("MaxCommands");
        cowToolItem = new CowToolItem(this);
        event.addItem(cowToolItem);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command bukkitCommand, String bukkitLabel, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return usage(sender);
        if (args.length == 0) return usage(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getAmount() == 0) {
            Msg.send(player, "&cYour main hand is empty!");
            return true;
        }
        CustomItem customItem = CustomPlugin.getInstance().getItemManager().getCustomItem(item);
        List<String> commands;
        if (customItem != null) {
            if (customItem == cowToolItem) {
                if (!CowToolItem.isOwner(player, item)) {
                    Msg.sendActionBar(player, "&cThis tool does not belong to you!");
                    return true;
                } else {
                    commands = CowToolItem.getCommands(item);
                }
            } else {
                Msg.send(player, "&cYou cannot use this type of item!");
                return true;
            }
        } else if (!new ItemStack(item.getType(), item.getAmount(), (short)item.getDurability()).isSimilar(item)) {
            Msg.send(player, "&cYou cannot use this item type!");
            return true;
        } else {
            commands = null;
        }
        int slash;
        Integer commandIndex;
        if (args[0].startsWith("/")) {
            slash = 0;
            commandIndex = null;
        } else if (args.length > 1 && args[1].startsWith("/")) {
            if (commands == null || commands.isEmpty()) {
                Msg.send(player, "&cNo commands found!");
                return true;
            }
            slash = 1;
            try {
                commandIndex = Integer.parseInt(args[0]) - 1;
            } catch (NumberFormatException nfe) {
                Msg.send(player, "&cInvalid number: %s", args[0]);
                return true;
            }
            if (commandIndex < 0 || commandIndex > commands.size()) {
                Msg.send(player, "&cInvalid command index: %s", args[0]);
                return true;
            }
        } else if (args.length == 2 && "delete".equalsIgnoreCase(args[0])) {
            if (commands == null || commands.isEmpty()) {
                Msg.send(player, "&cNo commands found!");
                return true;
            }
            int line;
            try {
                line = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException nfe) {
                Msg.send(player, "&cInvalid number: %s", args[1]);
                return true;
            }
            if (line < 0 || line >= commands.size()) {
                Msg.send(player, "&cInvalid command index: %s", args[1]);
                return true;
            }
            String command = commands.remove(line);
            cowToolItem.storeCommands(player, item, commands);
            Msg.send(player, "&aCommand deleted: &r%s", command);
            return true;
        } else if (args.length == 1 && "list".equalsIgnoreCase(args[0])) {
            if (commands == null || commands.isEmpty()) {
                Msg.send(player, "&cNo commands found!");
                return true;
            } else if (commands.size() == 1) {
                Msg.send(player, "&91 command:");
            } else {
                Msg.send(player, "&9%d commands:", commands.size());
            }
            int i = 0;
            for (String command: commands) {
                i += 1;
                Msg.send(player, "&a%d)&r %s", i, command);
            }
            return true;
        } else {
            return usage(sender);
        }
        if (commands == null) {
            item = CustomPlugin.getInstance().getItemManager().wrapItemStack(item, CowToolItem.CUSTOM_ID);
            if (!player.getInventory().getItemInMainHand().equals(item)) {
                player.getInventory().setItemInMainHand(item);
                item = player.getInventory().getItemInMainHand();
            }
            commands = new ArrayList<>();
        }
        StringBuilder sb = new StringBuilder(args[slash]);
        for (int i = slash + 1; i < args.length; i += 1) sb.append(" ").append(args[i]);
        String command = sb.toString();
        String label = command.split(" ", 2)[0].toLowerCase().substring(1);
        if (getBannedCommandList().contains(label)) {
            Msg.send(player, "&cBanned command: %s", command);
            return true;
        }
        if (commands.size() >= maxCommands) {
            Msg.send(player, "&cYou cannot store more than %d commands!", maxCommands);
            return true;
        } else if (commandIndex == null) {
            commands.add(command);
        } else {
            commands.add(commandIndex, command);
        }
        cowToolItem.storeCommands(player, item, commands);
        Msg.send(player, "&aCommand stored: &r%s", command);
        return true;
    }

    boolean usage(CommandSender sender) {
        Msg.send(sender, getCommand("ct").getUsage());
        return true;
    }

    List<String> getBannedCommandList() {
        return getConfig().getStringList("BannedCommands");
    }
}
