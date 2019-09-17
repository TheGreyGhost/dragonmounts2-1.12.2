package com.TheRPGAdventurer.ROTD.util;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class DMUtils {

  public static Logger getLogger() {
    if (logger == null) {
      logger = LogManager.getFormatterLogger(DragonMounts.MODID);
    }
    return logger;
  }

  public static String translateToLocal(String s) {
    return I18n.format(s);
  }

  /**
   * Consumes the currently equipped item of a player if it matches the item
   * type in the parameters. The stack will be decreased or removed only if
   * the player is not in creative mode.
   *
   * @param player player to check
   * @param items  one or more types of items that should be consumed. Only the
   *               first match will be consumed.
   * @return the consumed item type or null if no matching item was equipped.
   */
  public static Item consumeEquipped(EntityPlayer player, Item... items) {
    ItemStack itemStack = player.getHeldItemMainhand();

    if (itemStack == null) {
      return null;
    }

    Item equippedItem = itemStack.getItem();

    for (Item item : items) {
      if (item == equippedItem) {
        // don't reduce stack in creative mode
        if (!player.capabilities.isCreativeMode) {
          itemStack.shrink(1);
        }

        // required because the stack isn't reduced in onItemRightClick()
        if (itemStack.getMaxStackSize() <= 0) {
          player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }

        return item;
      }
    }

    return null;
  }

  public static boolean consumeEquipped(EntityPlayer player, Item item) {
    return consumeEquipped(player, new Item[]{item}) != null;
  }

  public static boolean consumeEquippedArray(EntityPlayer player, Item[] foodItems) {
    return consumeEquipped(player, foodItems) != null;
  }

  public static int getFoodPoints(EntityPlayer player) {
    Item item = player.getHeldItemMainhand().getItem();
    if (item != null && item instanceof ItemFood) {
      int points = ((ItemFood) item).getHealAmount(new ItemStack(item)) * 2;
      return points;
    }
    return 0;
  }

  /**
   * @return True if fish is found in player hand
   * @WolfShotz Checks if held item is any kind of Fish (Registered under listAllfishraw in OreDict)
   * This allows other mods' fishes to be used with dragon taming
   */
  public static boolean consumeFish(EntityPlayer player) {
    Set<Item> consumeFish = OreDictionary.getOres("listAllfishraw").stream().map(ItemStack::getItem).collect(Collectors.toSet());
    ItemStack itemstack = player.getHeldItemMainhand();
    if (itemstack.getItem() != null) {
      if (consumeFish.contains(itemstack.getItem())) return true;
    }
    return false;
  }

  /**
   * Checks if a player has food equipped.
   *
   * @param player player to check
   * @return true if the player has a food item selected
   */
  public static boolean hasEquippedFood(EntityPlayer player) {
    ItemStack itemStack = player.getHeldItemMainhand();

    if (itemStack == null) {
      return false;
    }

    return itemStack.getItem() instanceof ItemFood;
  }

  /**
   * Checks if a player has items equipped that can be used with a right-click.
   * Typically applies for weapons, food and tools.
   *
   * @param player player to check
   * @return true if the player has an usable item equipped
   */
  public static boolean hasEquippedUsable(EntityPlayer player) {
    ItemStack itemStack = player.getHeldItemMainhand();

    if (itemStack == null) {
      return false;
    }

    return itemStack.getItemUseAction() != EnumAction.NONE;
  }

  /**
   * Checks if a player has a specific item equipped.
   *
   * @param player player to check
   * @param item   required item type
   * @return true if the player has the given item equipped
   */
  public static boolean hasEquipped(EntityPlayer player, Item item) {
    ItemStack itemStack = player.getHeldItemMainhand();
    if (itemStack == null) return false;
    //found item in mainHand, check if its specified item
    return itemStack.getItem() == item;
  }

  /**
   * taken from stackoverflow
   *
   * @param rnd
   * @param start
   * @param end
   * @param exclude
   * @return
   */
  public static int getRandomWithExclusionstatic(Random rnd, int start, int end, int... exclude) {
    int random = start + rnd.nextInt(end - start + 1 - exclude.length);
    for (int ex : exclude) {
      if (random < ex) {
        break;
      }
      random++;
    }
    return random;
  }

  /**
   * taken from stackoverflow
   *
   * @param rnd
   * @param start
   * @param end
   * @param exclude
   * @return
   */
  public int getRandomWithExclusion(Random rnd, int start, int end, int... exclude) {
    int random = start + rnd.nextInt(end - start + 1 - exclude.length);
    for (int ex : exclude) {
      if (random < ex) {
        break;
      }
      random++;
    }
    return random;
  }

  /** list all the files in a particular resource location
   *  Looks in assets/dragonmounts/{pathToFolder} and returns a list of all the filenames it finds
   * **/
  public static List<String> listAssetsFolderContents(String pathToFolder) throws IOException {
    final int MAX_LINES = 1000;       // just an arbitrary limit to stop silliness
    final int MAX_LINE_LENGTH = 1000; // just an arbitrary limit to stop silliness
    List<String> retval = new ArrayList<>();
    InputStream stream = DragonMounts.class.getClassLoader().getResourceAsStream("assets/"+ DragonMounts.MODID + "/" + pathToFolder);
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    int linecount = 0;
    while (reader.ready()) {
      String nextLine = reader.readLine();
      if (++linecount > MAX_LINES) {
        throw new IOException("Folder " + pathToFolder + " contained too many entries (more than " + MAX_LINES + ")");
      }
      if (nextLine.length() > MAX_LINE_LENGTH) {
        throw new IOException("One of the filenames (" + nextLine.substring(0, 20) + "...) in folder " + pathToFolder
                + " was too long (more than " + MAX_LINE_LENGTH + " characters)");
      }

      retval.add(nextLine);
    }
    return retval;
  }



  private static Logger logger;
}