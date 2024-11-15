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

    private ItemStack createCustomDiamond() {
        ItemStack customDiamond = new ItemStack(Material.DIAMOND);
        ItemMeta meta = customDiamond.getItemMeta();
        meta.setDisplayName("Special Diamond");
        meta.setCustomModelData(5000); // Menetapkan Custom Model Data
        customDiamond.setItemMeta(meta);
        return customDiamond;
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
                sender.sendMessage("Usage: /valkycraft <open|reload|diamond>");
                return true;
            }

            if (args[0].equalsIgnoreCase("open")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    openDiamondSubmitGUI(player);
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

            if (args[0].equalsIgnoreCase("diamond")) {
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

                    ItemStack customDiamond = createCustomDiamond();
                    customDiamond.setAmount(jumlah);
                    player.getInventory().addItem(customDiamond);

                    player.sendMessage(diamondReceiveMessage.replace("%diamonds%", String.valueOf(jumlah)));
                    return true;
                } else {
                    sender.sendMessage(onlyPlayersMessage);
                    return true;
                }
            }
        }

        return false;
    }

    private void openDiamondSubmitGUI(Player player) {
        gui = Bukkit.createInventory(null, 4 * 9, guiTitle);

        // Set submit button in slot 16
        ItemStack submitItem = new ItemStack(Material.EMERALD);
        ItemMeta meta = submitItem.getItemMeta();
        meta.setDisplayName(submitButtonText);
        submitItem.setItemMeta(meta);
        gui.setItem(16, submitItem);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTopInventory().equals(event.getClickedInventory())) {
            int slot = event.getSlot();

            // Allow diamonds in slots 11-15 only
            if (slot >= 11 && slot <= 15) {
                ItemStack currentItem = event.getCursor();
                if (currentItem != null && currentItem.getType() == Material.DIAMOND) {
                    ItemMeta meta = currentItem.getItemMeta();
                    if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 5000) {
                        event.setCancelled(false); // Izinkan item dimasukkan
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(onlySpecialDiamondMessage);
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(onlySpecialDiamondMessage);
                }
            }
            if ((slot >= 0 && slot <= 10) || (slot >= 17 && slot <= 35)) {
                event.setCancelled(true);
            }

            if (slot == 16 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.EMERALD) {
                event.setCancelled(true);

                int diamondCount = 0;

                for (int i = 11; i <= 15; i++) {
                    ItemStack item = gui.getItem(i);
                    if (item != null && item.getType() == Material.DIAMOND) {
                        diamondCount += item.getAmount();
                        gui.clear(i);
                    }
                }

                if (diamondCount == 0) {
                    player.sendMessage(noDiamondsMessage);
                } else {
                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    String command = "eco give " + player.getName() + " " + diamondCount;
                    Bukkit.dispatchCommand(console, command);
                    player.sendMessage(submitSuccessMessage.replace("%diamonds%", String.valueOf(diamondCount)));
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
                if (item != null && item.getType() == Material.DIAMOND) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 5000) {
                        player.getInventory().addItem(item);
                    }
                }
            }

            gui.clear();
        }
    }

    private void loadConfig() {
        saveDefaultConfig();
        guiTitle = getConfig().getString("messages.gui_title", "Submit Diamonds");
        submitButtonText = getConfig().getString("messages.submit_button", "Submit");
        noDiamondsMessage = getConfig().getString("messages.no_diamonds", "Tidak ada diamond yang dikirimkan. Harap masukkan diamond terlebih dahulu.");
        submitSuccessMessage = getConfig().getString("messages.submit_success", "You submitted %diamonds% diamonds and received the equivalent balance.");
        reloadSuccessMessage = getConfig().getString("messages.reload_success", "Plugin configuration reloaded successfully.");
        noPermissionMessage = getConfig().getString("messages.no_permission", "You do not have permission to reload this plugin.");
        onlyPlayersMessage = getConfig().getString("messages.only_players", "Only players can use this command.");
        invalidNumberMessage = getConfig().getString("messages.invalid_number", "Please enter a valid number.");
        diamondReceiveMessage = getConfig().getString("messages.diamond_receive", "You have received %diamonds% Special Diamonds!");
        onlySpecialDiamondMessage = getConfig().getString("messages.only_special_diamond", "Only Special Diamonds can be inserted.");
    }
}
