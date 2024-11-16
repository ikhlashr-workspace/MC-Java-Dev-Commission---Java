package org.ptr.valkycraftcrypto;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {

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

    // Multi-instance GUI mapping
    private final Map<Player, Inventory> playerInventories = new HashMap<>();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI is not installed or not enabled. Plugin cannot work properly!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
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
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Usage: /valkycraft <open|reload|give>"));
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
                if (sender.hasPermission("valkycraft.reload")) {
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
                handleGiveCommand(sender, args);
                return true;
            }
        }
        return false;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("valkycraft.give")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
                return;
            }

            int amount = 1;
            if (args.length > 1) {
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidNumberMessage));
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidNumberMessage));
                    return;
                }
            }

            ItemStack customItem = createCustomItem();
            customItem.setAmount(amount);
            player.getInventory().addItem(customItem);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', diamondReceiveMessage.replace("%crypt%", String.valueOf(amount))));
        } else if (sender instanceof ConsoleCommandSender) {
            ConsoleCommandSender console = (ConsoleCommandSender) sender;
            if (args.length < 3) {
                console.sendMessage("Usage: /valkycraft give <player> <amount>");
                return;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                console.sendMessage("Player not found or offline.");
                return;
            }

            try {
                int amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    console.sendMessage(invalidNumberMessage);
                    return;
                }

                ItemStack customItem = createCustomItem();
                customItem.setAmount(amount);
                target.getInventory().addItem(customItem);

                console.sendMessage("Successfully given " + amount + " items to " + target.getName() + ".");
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', diamondReceiveMessage.replace("%crypt%", String.valueOf(amount))));
            } catch (NumberFormatException e) {
                console.sendMessage(invalidNumberMessage);
            }
        }
    }

    private void openCryptSubmitGUI(Player player) {
        // Process the GUI title with PlaceholderAPI support
        Inventory gui = Bukkit.createInventory(
                null,
                4 * 9,
                ChatColor.translateAlternateColorCodes('&', PlaceholderUtil.applyPlaceholders(player, guiTitle))
        );

        // Submit button
        ItemStack submitItem = createSubmitButton();
        gui.setItem(35, submitItem);

        // Store player's GUI instance
        playerInventories.put(player, gui);
        player.openInventory(gui);
    }


    private ItemStack createSubmitButton() {
        ItemStack item = new ItemStack(Material.valueOf(getConfig().getString("items.submit_button.material")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setCustomModelData(getConfig().getInt("items.submit_button.custom_model_data"));
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', submitButtonText));
            List<String> lore = new ArrayList<>();
            for (String line : getConfig().getStringList("items.submit_button.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        Player player = (Player) event.getWhoClicked();
        Inventory gui = playerInventories.get(player);

        // Pastikan event ini hanya berjalan untuk top inventory
        if (event.getView().getTopInventory().equals(event.getClickedInventory())) {
            int slot = event.getSlot();

            // Cek apakah slot yang diklik adalah slot 11-15
            if (slot >= 11 && slot <= 15) {
                ItemStack currentItem = event.getCursor(); // Ambil item yang ada di cursor (item yang dibawa pemain)

                if (currentItem != null && currentItem.getType() == validItemMaterial) {
                    ItemMeta meta = currentItem.getItemMeta();
                    // Pastikan item memiliki CustomModelData yang valid
                    if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == validItemCustomModelData) {
                        event.setCancelled(false); // Biarkan item dimasukkan ke dalam slot
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage));
                    }
                } else {
                    event.setCancelled(true);
                }
            }
            // Cek untuk tombol submit di slot 35
            else if (slot == 35) {
                event.setCancelled(true); // Jangan biarkan pemain mengklik submit tombol jika tidak valid
                processSubmit(player, gui); // Proses pengiriman atau submit item
            }
            // Cegah klik di slot lain selain 11-15 dan 35
            else {
                event.setCancelled(true);
            }
            if ((slot >= 0 && slot <= 10) || (slot >= 17 && slot <= 35)) {
                event.setCancelled(true);
            }


            // Cegah pengambilan item yang valid hanya jika item tersebut valid di slot 11-15
            if (slot >= 11 && slot <= 15) {
                ItemStack itemInSlot = event.getClickedInventory().getItem(slot);
                if (itemInSlot != null && itemInSlot.getType() == validItemMaterial) {
                    ItemMeta meta = itemInSlot.getItemMeta();
                    // Pastikan item valid berdasarkan CustomModelData
                    if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == validItemCustomModelData) {
                        event.setCancelled(false); // Izinkan item untuk diambil
                    } else {
                        event.setCancelled(true); // Cegah pengambilan item jika tidak valid
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage));
                    }
                }
            }
        }
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory gui = playerInventories.get(player);

        if (gui == null) return;

        // Pastikan event hanya berlaku pada inventory GUI
        if (event.getView().getTopInventory().equals(event.getInventory())) {

            // Ambil item yang sedang didrag
            ItemStack draggedItem = event.getOldCursor();

            if (draggedItem != null && draggedItem.getType() == validItemMaterial) {
                ItemMeta meta = draggedItem.getItemMeta();

                // Pastikan item memiliki CustomModelData yang valid
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == validItemCustomModelData) {
                    // Cegah drag item ke luar slot yang diizinkan
                    if (event.getRawSlots().stream().anyMatch(slot -> slot < 11 || slot > 15)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage));
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', onlySpecialDiamondMessage));
                }
            } else {
                // Jika item yang didrag tidak sesuai, batalkan event
                event.setCancelled(true);
            }
        }
    }


    private void processSubmit(Player player, Inventory gui) {
        int total = 0;
        for (int i = 11; i <= 15; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() == validItemMaterial) {
                total += item.getAmount();
                gui.setItem(i, null);
            }
        }

        if (total == 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noDiamondsMessage));
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + total);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', submitSuccessMessage.replace("%crypt%", String.valueOf(total))));
        }

        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory gui = playerInventories.get(player);

        if (gui != null && event.getInventory().equals(gui)) {
            for (int i = 11; i <= 15; i++) {
                ItemStack item = gui.getItem(i);
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            }
            playerInventories.remove(player);
        }
    }

    private ItemStack createCustomItem() {
        ItemStack item = new ItemStack(validItemMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setCustomModelData(validItemCustomModelData);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', validItemName));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void loadConfig() {
        saveDefaultConfig();
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
        validItemMaterial = Material.valueOf(getConfig().getString("items.valid_item.material"));
        validItemCustomModelData = getConfig().getInt("items.valid_item.custom_model_data");
        validItemName = getConfig().getString("items.valid_item.name");
    }
}
