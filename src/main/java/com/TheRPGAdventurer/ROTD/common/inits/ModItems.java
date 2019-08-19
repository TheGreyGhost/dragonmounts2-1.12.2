package com.TheRPGAdventurer.ROTD.common.inits;

import com.TheRPGAdventurer.ROTD.common.items.ItemDragonHatchableEgg;
import com.TheRPGAdventurer.ROTD.common.items.ItemDragonOrb;
import com.TheRPGAdventurer.ROTD.common.items.ItemDragonWhistle;
import com.TheRPGAdventurer.ROTD.common.items.ItemTestRunner;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
  public static final List<Item> ITEMS = new ArrayList<Item>();

  //Other Start
  public static final Item dragon_whistle = new ItemDragonWhistle();
  public static final ItemDragonOrb dragon_orb = new ItemDragonOrb();
  public static final ItemTestRunner test_runner = new ItemTestRunner();
  public static final ItemDragonHatchableEgg DRAGON_HATCHABLE_EGG = new ItemDragonHatchableEgg();


  //Other End

}
