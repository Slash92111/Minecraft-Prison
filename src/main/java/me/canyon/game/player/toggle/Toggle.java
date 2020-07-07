package me.canyon.game.player.toggle;

import me.canyon.game.player.PlayerData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Toggle {

    private PlayerData playerData;

    private HashMap<ToggleEnum, Boolean> toggles = new HashMap<>();

    public Toggle(PlayerData playerData) {
        this.playerData = playerData;

        load();
    }

    private void load() {
        if (playerData.getToggleList().size() != 0)
            for (String toggle : playerData.getToggleList())
            toggles.put(ToggleEnum.getFromID(toggle.split(":")[0]), Boolean.valueOf(toggle.split(":")[1]));

        for (ToggleEnum toggleEnum : ToggleEnum.values()) {
            if (!toggles.containsKey(toggleEnum))
                toggles.put(toggleEnum, true);
        }
    }

    private HashMap<ToggleEnum, Boolean> getToggles() {
        return this.toggles;
    }

    public boolean get(ToggleEnum toggle) { return toggles.get(toggle); }

    public void set(ToggleEnum toggle, boolean value) { toggles.replace(toggle, value); }

    public List<String> toList() {
        List<String> toggles = new ArrayList<>();

        for (ToggleEnum toggle : this.toggles.keySet())
            toggles.add(toggle.getID() + ":" + this.toggles.get(toggle).toString());

        return toggles;
    }

    public Inventory getUI() {
        return new ToggleUI().getInventory();
    }

    private class ToggleUI {

        private Inventory inventory;

        private ItemStack TOGGLE_ON = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        private ItemStack TOGGLE_OFF = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        private ItemStack PLACE_HOLDER = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);

        public ToggleUI() {
            int amountOfToggles = ToggleEnum.values().length;
            int inventorySize;

            if (amountOfToggles <= 9)
                inventorySize = 9;
            else
                inventorySize = (int) Math.ceil((float) amountOfToggles / 9) * 9;

            this.inventory = Bukkit.createInventory(null, inventorySize, ChatColor.DARK_GRAY + "Toggles");

            for (int i = 0; i < inventorySize; i++)
                inventory.setItem(i, PLACE_HOLDER);

            TreeMap<String, Boolean> sortedToggles = new TreeMap<>();

            for (ToggleEnum toggleEnum : getToggles().keySet())
                sortedToggles.put(toggleEnum.getName(), get(toggleEnum));

            int i = 0;
            for (Map.Entry<String, Boolean> entry : sortedToggles.entrySet()) {
                ToggleEnum toggle = ToggleEnum.getFromName(entry.getKey());
                ItemStack item = TOGGLE_OFF;
                ChatColor color = ChatColor.RED;
                String currently = "Disable";
                String oppositeCurrently = "Enable";

                if (get(toggle)) {
                    item = TOGGLE_ON;
                    color = ChatColor.GREEN;
                    currently = "Enable";
                    oppositeCurrently = "Disable";
                }

                inventory.setItem(i, createItem(item, ChatColor.AQUA + "" + ChatColor.BOLD + toggle.getName(), toggle.getID(), new String[] {
                    " ",
                    ChatColor.WHITE + "" + ChatColor.BOLD + "CURRENTLY",
                    color + currently + " " + toggle.getName(),
                    " ",
                    ChatColor.GRAY + "(Click to " + oppositeCurrently + " " + toggle.getName()
                }));

                i++;
            }
        }

        public Inventory getInventory() { return this.inventory; }

        private ItemStack createItem(ItemStack item, String name, String toggleID, String[] lore) {
            ItemMeta im = item.getItemMeta();
            im.setDisplayName(name);

            if (lore != null)
                im.setLore(Arrays.asList(lore));

            item.setItemMeta(im);

            net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
            tagCompound.setString("id", toggleID);

            nmsItem.setTag(tagCompound);
            item = CraftItemStack.asBukkitCopy(nmsItem);

            return item;
        }
    }
}
