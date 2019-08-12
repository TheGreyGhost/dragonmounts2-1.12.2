package com.TheRPGAdventurer.ROTD.common.entity.breeds;

import com.TheRPGAdventurer.ROTD.common.entity.EntityDragonEgg;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.world.World;

/**
 * Created by TGG on 9/08/2019.
 * Used to generate dragons and eggs
 */
public class DragonFactory {

  public DragonFactory() {

  }

  public static DragonFactory getDefaultDragonFactory() {return defaultDragonFactory;}

  public EntityTameableDragon createDragon(World world, DragonBreedNew dragonBreed) {
    EntityTameableDragon newDragon = new EntityTameableDragon(world, dragonBreed, dragonBreed.getDragonVariants());
    return newDragon;
  }

  public EntityDragonEgg createEgg(World world, DragonBreedNew dragonBreed) {
    EntityDragonEgg newDragonEgg = new EntityDragonEgg(world, dragonBreed, dragonBreed.getDragonVariants());
    return newDragonEgg;
  }



  private static DragonFactory defaultDragonFactory = new DragonFactory();

}
