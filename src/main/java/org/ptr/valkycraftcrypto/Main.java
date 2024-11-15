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

    private ItemStack createCustomItem() {
        ItemStack customItem = new ItemStack(validItemMaterial);
        ItemMeta meta = customItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(validItemName);
            meta.setCustomModelData(validItemCustomModelData);
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
        if (command.getName().equalsIgnoreCase("valkycraft")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /valkycraft <open|reload|crypt>");
                return true;
            }

            if (args[0].equalsIgnoreCase("open")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    openCryptSubmitGUI(player);
                    return true;
                } else {
                    sender.sendMessage(onlyPlayersMessage);
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("valkycraft.reload")) {
                    reloadConfig();
                    loadConfig();
                    sender.sendMessage(reloadSuccessMessage);
                    return true;
                } else {
                    sender.sendMessage(noPermissionMessage);
                    return false;
                }
            }

            if (args[0].equalsIgnoreCase("crypt")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    int jumlah = 1; // Default jumlah 1 jika tidak ada argumen kedua

                    if (args.length > 1) {
                        try {
                            jumlah = Integer.parseInt(args[1]);
                            if (jumlah <= 0) {
                                player.sendMessage(invalidNumberMessage);
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(invalidNumberMessage);
                            return true;
                        }
                    }

                    ItemStack customItem = createCustomItem();
                    customItem.setAmount(jumlah);
                    player.getInventory().addItem(customItem);

                    player.sendMessage(diamondReceiveMessage.replace("%crypt%", String.valueOf(jumlah)).replace("%valid_item_name%", validItemName));
                    return true;
                } else {
                    sender.sendMessage(onlyPlayersMessage);
                    return true;
                }
            }
        }

        return false;
    }

    private void openCryptSubmitGUI(Player player) {
        gui = Bukkit.createInventory(null, 4 * 9, guiTitle);

        // Set submit button in slot 16
        ItemStack submitItem = new ItemStack(Material.EMERALD);
        ItemMeta meta = submitItem.getItemMeta();
        meta.setDisplayName(submitButtonText);
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
                        player.sendMessage(onlySpecialDiamondMessage.replace("%valid_item_name%", validItemName));
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(onlySpecialDiamondMessage.replace("%valid_item_name%", validItemName));
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
                    player.sendMessage(noDiamondsMessage); // No crypts added
                } else {
                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    String command = "eco give " + player.getName() + " " + cryptCount;
                    Bukkit.dispatchCommand(console, command);

                    // Replace the %crypt% and %valid_item_name% placeholders in the success message
                    String successMessage = submitSuccessMessage
                            .replace("%crypt%", String.valueOf(cryptCount))  // Replace %crypt% with the count
                            .replace("%valid_item_name%", validItemName);   // Replace %valid_item_name% with the item name

                    player.sendMessage(successMessage);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(gui)) {
            Player player = (Player) event.getPlayer();

            for (int i = 11; i <= 15; i++) {
                ItemStack item = gui.getItem(i);
                if (item != null && item.getType() == validItemMaterial) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == validItemCustomModelData) {
                        player.getInventory().addItem(item);
                    }
                }
            }

            gui.clear();
        }
    }

    private void loadConfig() {
        saveDefaultConfig();
        guiTitle = getConfig().getString("messages.gui_title", "Submit Crypt");
        submitButtonText = getConfig().getString("messages.submit_button", "Submit");
        noDiamondsMessage = getConfig().getString("messages.no_diamonds", "Tidak ada crypt yang dikirimkan. Harap masukkan crypt terlebih dahulu.");
        submitSuccessMessage = getConfig().getString("messages.submit_success", "You submitted %crypt% %valid_item_name% and received the equivalent balance.");
        reloadSuccessMessage = getConfig().getString("messages.reload_success", "Plugin configuration reloaded successfully.");
        noPermissionMessage = getConfig().getString("messages.no_permission", "You do not have permission to reload this plugin.");
        onlyPlayersMessage = getConfig().getString("messages.only_players", "Only players can use this command.");
        invalidNumberMessage = getConfig().getString("messages.invalid_number", "Please enter a valid number.");
        diamondReceiveMessage = getConfig().getString("messages.diamond_receive", "You have received %crypt% %valid_item_name%!");
        onlySpecialDiamondMessage = getConfig().getString("messages.only_special_diamond", "Only %valid_item_name% can be inserted.");

        validItemMaterial = Material.getMaterial(getConfig().getString("diamond.material", "DIAMOND"));
        validItemCustomModelData = getConfig().getInt("diamond.custom_model_data", 5000);
        validItemName = getConfig().getString("diamond.display_name", "Crypt");
    }
}
