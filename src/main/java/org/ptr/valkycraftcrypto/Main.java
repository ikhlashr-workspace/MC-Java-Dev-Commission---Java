package org.ptr.valkycraftcrypto;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.util.List;

public class Main extends JavaPlugin implements Listener {

    private Inventory gui;
    private String guiTitle;
    private String submitButtonText;
    private String noDiamondsMessage;
    private String submitSuccessMessage;
    private String reloadSuccessMessage;
    private String noPermissionMessage;
    private String onlyPlayersMessage;
    private String invalidNumberMessage;
    private String diamondReceiveMessage;
    private String onlySpecialDiamondMessage;
    private Material validItemMaterial;
    private int validItemCustomModelData;
    private String validItemName;
    private List<String> validItemLore;  // List to hold lore lines

    private ItemStack createCustomItem() {
        ItemStack customItem = new ItemStack(validItemMaterial);
        ItemMeta meta = customItem.getItemMeta();
        if (meta != null) {
            // Apply color codes to the item name and lore
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', validItemName));

            meta.setCustomModelData(validItemCustomModelData);
            // Set lore from config with color codes
            if (validItemLore != null && !validItemLore.isEmpty()) {
                List<String> coloredLore = validItemLore.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .toList();
                meta.setLore(coloredLore);
            }
            customItem.setItemMeta(meta);
        }
        return customItem;
    }

    @Override
    public void onEnable() {
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, this);
        // Load configuration and messages
        loadConfig();
        getLogger().info("Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("valkycurrency")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /valkycurrency <open|reload|give>");
                return true;
            }

            if (args[0].equalsIgnoreCase("open")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    openCryptSubmitGUI(player);
                    return true;
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', onlyPlayersMessage));
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("valkycurrency.reload")) {
                    reloadConfig();
                    loadConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadSuccessMessage));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
                    return false;
                }
            }

            if (args[0].equalsIgnoreCase("give")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;

                    // Check if the player has the required permission
                    if (!player.hasPermission("valkycurrency.give")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "You do not have permission to use this command."));
                        return true;
                    }

                    int jumlah = 1; // Default jumlah 1 if no second argument is given

                    if (args.length > 1) {
                        try {
                            jumlah = Integer.parseInt(args[1]);
                            if (jumlah <= 0) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidNumberMessage));
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidNumberMessage));
                            return true;
                        }
                    }

                    ItemStack customItem = createCustomItem();
                    customItem.setAmount(jumlah);
                    player.getInventory().addItem(customItem);

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', diamondReceiveMessage)
                            .replace("%crypt%", String.valueOf(jumlah))
                            .replace("%valid_item_name%", validItemName));
                    return true;
                } else if (sender instanceof ConsoleCommandSender) {
                    ConsoleCommandSender console = (ConsoleCommandSender) sender;
                    int jumlah = 1; // Default jumlah 1 if no second argument is given

                    if (args.length > 1) {
                        try {
                            jumlah = Integer.parseInt(args[1]);
                            if (jumlah <= 0) {
                                console.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidNumberMessage));
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            console.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidNumberMessage));
                            return true;
                        }
                    }

                    if (args.length > 2) {
                        Player targetPlayer = Bukkit.getPlayer(args[2]); // Assuming the third argument is the player's name
                        if (targetPlayer != null) {
                            // Check if the target player has permission before giving the item
                            if (!targetPlayer.hasPermission("valkycurrency.give")) {
                                console.sendMessage(ChatColor.translateAlternateColorCodes('&', "Player does not have permission to receive the item."));
                                return true;
                            }

                            ItemStack customItem = createCustomItem();
                            customItem.setAmount(jumlah);
                            targetPlayer.getInventory().addItem(customItem);

                            targetPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', diamondReceiveMessage)
                                    .replace("%crypt%", String.valueOf(jumlah))
                                    .replace("%valid_item_name%", validItemName));
                            console.sendMessage("Gave " + jumlah + " " + validItemName + " to " + targetPlayer.getName());
                        } else {
                            console.sendMessage(ChatColor.translateAlternateColorCodes('&', "Player not found."));
                        }
                    } else {
                        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "You must specify a player name."));
                    }

                    return true;
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', onlyPlayersMessage));
                    return true;
                }
            }
        }

        return false;
    }

    private void openCryptSubmitGUI(Player player) {
        gui = Bukkit.createInventory(null, 4 * 9, ChatColor.translateAlternateColorCodes('&', guiTitle));

        // Set submit button in slot 16
        ItemStack submitItem = new ItemStack(Material.EMERALD);
        ItemMeta meta = submitItem.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', submitButtonText));
        submitItem.setItemMeta(meta);
        gui.setItem(16, submitItem);

        // Set valid item in slots 11-15
        ItemStack validItem = createCustomItem();
        for (int i = 11; i <= 15; i++) {
            gui.setItem(i, validItem);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTopInventory().equals(event.getClickedInventory())) {
            int slot = event.getSlot();

            // Allow only valid items in slots 11-15
            if (slot >= 11 && slot <= 15) {
                ItemStack currentItem = event.getCursor();
                if (currentItem != null && currentItem.getType() == validItemMaterial) {
                    ItemMeta meta = currentItem.getItemMeta();
                    if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == validItemCustomModelData) {
                        event.setCancelled(false); // Allow valid item
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage.replace("%valid_item_name%", validItemName)));
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage.replace("%valid_item_name%", validItemName)));
                }
            }
            if ((slot >= 0 && slot <= 10) || (slot >= 17 && slot <= 35)) {
                event.setCancelled(true);
            }

            if (slot == 16 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.EMERALD) {
                event.setCancelled(true);

                int cryptCount = 0;

                // Count the number of valid crypt items in slots 11-15
                for (int i = 11; i <= 15; i++) {
                    ItemStack item = gui.getItem(i);
                    if (item != null && item.getType() == validItemMaterial) {
                        cryptCount += item.getAmount();
                        gui.clear(i);  // Clear the slot
                    }
                }

                if (cryptCount == 0) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', noDiamondsMessage)); // No crypts added
                } else {
                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    console.sendMessage(ChatColor.translateAlternateColorCodes('&', submitSuccessMessage));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', submitSuccessMessage));
                    player.getInventory().addItem(new ItemStack(Material.DIAMOND, cryptCount)); // Reward player with diamonds
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Clean up GUI when it’s closed
        if (event.getInventory().equals(gui)) {
            gui = null;
        }
    }

    private void loadConfig() {
        // Load settings from configuration
        getConfig().options().copyDefaults(true);
        saveConfig();

        guiTitle = getConfig().getString("guiTitle", "ValkyCraft Crypto");
        submitButtonText = getConfig().getString("submitButtonText", "Submit");
        noDiamondsMessage = getConfig().getString("noDiamondsMessage", "&cNo diamonds in slots!");
        submitSuccessMessage = getConfig().getString("submitSuccessMessage", "&aTransaction successful!");
        reloadSuccessMessage = getConfig().getString("reloadSuccessMessage", "&aPlugin reloaded!");
        noPermissionMessage = getConfig().getString("noPermissionMessage", "&cYou do not have permission!");
        onlyPlayersMessage = getConfig().getString("onlyPlayersMessage", "&cThis command can only be run by players.");
        invalidNumberMessage = getConfig().getString("invalidNumberMessage", "&cInvalid number specified.");
        diamondReceiveMessage = getConfig().getString("diamondReceiveMessage", "&aYou received %crypt% %valid_item_name%!");
        onlySpecialDiamondMessage = getConfig().getString("onlySpecialDiamondMessage", "&cThis is not a valid %valid_item_name%.");

        // Item properties
        validItemMaterial = Material.getMaterial(getConfig().getString("validItem.material", "DIAMOND"));
        validItemCustomModelData = getConfig().getInt("validItem.customModelData", 1);
        validItemName = getConfig().getString("validItem.name", "&bSpecial Crypt Item");
        validItemLore = getConfig().getStringList("validItem.lore");
    }
}
