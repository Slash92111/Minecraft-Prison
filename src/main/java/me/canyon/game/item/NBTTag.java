package me.canyon.game.item;

import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NBTTag {

    public static ItemStack setString(ItemStack item, String key, String value) {
        net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();

        tagCompound.setString(key, value);

        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    public static String getString(ItemStack item, String key) {
        if (item != null) {
            net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

            if (nmsItem.hasTag())
                return nmsItem.getTag().getString(key);
        }

            return null;
    }

    public static ItemStack setInteger(ItemStack item, String key, int value) {
        net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();

        tagCompound.setInt(key, value);

        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    public static int getInteger(ItemStack item, String key) {
        if (item != null) {
            net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

            if (nmsItem.hasTag())
                return nmsItem.getTag().getInt(key);
        }

        return 0;
    }

    public static ItemStack setBoolean(ItemStack item, String key, boolean value) {
        net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();

        tagCompound.setBoolean(key, value);

        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    public static boolean getBoolean(ItemStack item, String key) {
        if (item != null) {
            net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

            if (nmsItem.hasTag())
                return nmsItem.getTag().getBoolean(key);
        }

        return false;
    }
}
