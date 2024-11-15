package org.ptr.valkycraftcrypto;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;  // Pastikan ChatColor diimpor
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

import java.util.ArrayList;
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

    private ItemStack createCustomItem() {
        ItemStack customItem = new ItemStack(validItemMaterial);
        ItemMeta meta = customItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', validItemName));  // Menambahkan dukungan kode warna
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
                sender.sendMessage( ChatColor.translateAlternateColorCodes( '&', "Usage: /valkycurrency <open|reload|give>" ) );
                return true;
            }

            if (args[0].equalsIgnoreCase( "open" )) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    openCryptSubmitGUI( player );
                    return true;
                } else {
                    sender.sendMessage( ChatColor.translateAlternateColorCodes( '&', onlyPlayersMessage ) );  // Menambahkan dukungan kode warna
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase( "reload" )) {
                if (sender.hasPermission( "valkycurrency.reload" )) {
                    reloadConfig();
                    loadConfig();
                    sender.sendMessage( ChatColor.translateAlternateColorCodes( '&', reloadSuccessMessage ) );  // Menambahkan dukungan kode warna
                    return true;
                } else {
                    sender.sendMessage( ChatColor.translateAlternateColorCodes( '&', noPermissionMessage ) );  // Menambahkan dukungan kode warna
                    return false;
                }
            }

            if (args[0].equalsIgnoreCase( "give" )) {
                // Memeriksa apakah pengirim adalah Player atau Console
                if (sender instanceof Player) {
                    Player player = (Player) sender;

                    // Memeriksa apakah pemain memiliki izin untuk memberi item
                    if (!player.hasPermission( "valkycurrency.give" )) {
                        player.sendMessage( ChatColor.translateAlternateColorCodes( '&', noPermissionMessage ) );  // Pesan jika tidak ada izin
                        return true;
                    }

                    int jumlah = 1; // Default jumlah 1 jika tidak ada argumen kedua

                    if (args.length > 1) {
                        try {
                            jumlah = Integer.parseInt( args[1] );
                            if (jumlah <= 0) {
                                player.sendMessage( ChatColor.translateAlternateColorCodes( '&', invalidNumberMessage ) );  // Menambahkan dukungan kode warna
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage( ChatColor.translateAlternateColorCodes( '&', invalidNumberMessage ) );  // Menambahkan dukungan kode warna
                            return true;
                        }
                    }

                    ItemStack customItem = createCustomItem();
                    customItem.setAmount( jumlah );
                    player.getInventory().addItem( customItem );

                    player.sendMessage( ChatColor.translateAlternateColorCodes( '&', diamondReceiveMessage.replace( "%crypt%", String.valueOf( jumlah ) ).replace( "%valid_item_name%", validItemName ) ) );  // Menambahkan dukungan kode warna
                    return true;
                } else if (sender instanceof ConsoleCommandSender) {
                    // Jika pengirim adalah konsol, tidak perlu izin khusus
                    ConsoleCommandSender console = (ConsoleCommandSender) sender;

                    if (args.length > 1) {
                        try {
                            int jumlah = Integer.parseInt( args[2] );
                            if (jumlah <= 0) {
                                console.sendMessage( ChatColor.translateAlternateColorCodes( '&', invalidNumberMessage ) );  // Menambahkan dukungan kode warna
                                return true;
                            }

                            // Mengirim item ke pemain yang diberikan dalam argumen pertama
                            Player targetPlayer = Bukkit.getPlayer( args[1] );
                            if (targetPlayer != null && targetPlayer.isOnline()) {
                                ItemStack customItem = createCustomItem();
                                customItem.setAmount( jumlah );
                                targetPlayer.getInventory().addItem( customItem );

                                targetPlayer.sendMessage( ChatColor.translateAlternateColorCodes( '&', diamondReceiveMessage.replace( "%crypt%", String.valueOf( jumlah ) ).replace( "%valid_item_name%", validItemName ) ) );  // Menambahkan dukungan kode warna
                                console.sendMessage( ChatColor.translateAlternateColorCodes( '&', "Item berhasil diberikan kepada " + targetPlayer.getName() + " sebanyak " + jumlah + " item." ) );
                            } else {
                                console.sendMessage( ChatColor.translateAlternateColorCodes( '&', "Pemain yang dimaksud tidak ditemukan atau offline." ) );
                            }
                        } catch (NumberFormatException e) {
                            console.sendMessage( ChatColor.translateAlternateColorCodes( '&', invalidNumberMessage ) );  // Menambahkan dukungan kode warna
                        }
                    }
                    return true;
                } else {
                    sender.sendMessage( ChatColor.translateAlternateColorCodes( '&', onlyPlayersMessage ) );  // Menambahkan dukungan kode warna
                    return true;
                }
            }

        }
        return false;
    }

    private void openCryptSubmitGUI(Player player) {
        gui = Bukkit.createInventory(null, 4 * 9, ChatColor.translateAlternateColorCodes('&', guiTitle));  // Menambahkan dukungan kode warna

        // Set submit button in slot 16
        String materialName = getConfig().getString("items.submit_button.material");
        int customModelData = getConfig().getInt("items.submit_button.custom_model_data");
        List<String> lore = getConfig().getStringList("items.submit_button.lore");

        // Mengonversi string material ke tipe Material
        Material material = Material.valueOf(materialName);

        // Membuat item dan mengatur item meta
        ItemStack submitItem = new ItemStack(material);
        ItemMeta meta = submitItem.getItemMeta();

        // Menambahkan Custom Model Data
        meta.setCustomModelData(customModelData);

        // Menambahkan Lore dengan kode warna
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));  // Menambahkan dukungan kode warna pada lore
        }
        meta.setLore(coloredLore);

        // Menambahkan nama item dengan kode warna
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', submitButtonText));

        // Menyimpan perubahan pada item
        submitItem.setItemMeta(meta);

        // Menambahkan item ke slot 16
        gui.setItem(35, submitItem);

        // Set valid item in slots 11-15
        ItemStack validItem = createCustomItem();

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTopInventory().equals(event.getClickedInventory())) {
            int slot = event.getSlot();
            String materialName = getConfig().getString("items.submit_button.material");
            Material configMaterial = Material.valueOf(materialName);

            // Allow only valid items in slots 11-15
            if (slot >= 11 && slot <= 15) {
                ItemStack currentItem = event.getCursor();
                if (currentItem != null && currentItem.getType() == validItemMaterial) {
                    ItemMeta meta = currentItem.getItemMeta();
                    if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == validItemCustomModelData) {
                        event.setCancelled(false); // Allow valid item
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage.replace("%valid_item_name%", validItemName)));  // Menambahkan dukungan kode warna
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage.replace("%valid_item_name%", validItemName)));  // Menambahkan dukungan kode warna
                }
            }
            if ((slot >= 0 && slot <= 10) || (slot >= 16 && slot <= 34)) {
                event.setCancelled(true);
            }

            if (event.getSlot() == 35 && event.getCurrentItem() != null &&
                    event.getCurrentItem().getType() == configMaterial) {
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
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', noDiamondsMessage));  // Menambahkan dukungan kode warna
                } else {
                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    String command = "eco give " + player.getName() + " " + cryptCount;
                    Bukkit.dispatchCommand(console, command);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', submitSuccessMessage.replace("%crypt%", String.valueOf(cryptCount))));  // Menambahkan dukungan kode warna
                }

                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(gui)) {
            gui = null;
        }
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        guiTitle = getConfig().getString("gui.title");
        submitButtonText = getConfig().getString("items.submit_button.name");
        noDiamondsMessage = getConfig().getString("messages.no_diamonds_message");
        submitSuccessMessage = getConfig().getString("messages.submit_success_message");
        reloadSuccessMessage = getConfig().getString("messages.reload_success_message");
        noPermissionMessage = getConfig().getString("messages.no_permission_message");
        onlyPlayersMessage = getConfig().getString("messages.only_players_message");
        invalidNumberMessage = getConfig().getString("messages.invalid_number_message");
        diamondReceiveMessage = getConfig().getString("messages.diamond_receive_message");
        onlySpecialDiamondMessage = getConfig().getString("messages.only_special_diamond_message");

        String material = getConfig().getString("items.valid_item.material");
        validItemMaterial = Material.valueOf(material);
        validItemCustomModelData = getConfig().getInt("items.valid_item.custom_model_data");
        validItemName = getConfig().getString("items.valid_item.name");
    }
}
