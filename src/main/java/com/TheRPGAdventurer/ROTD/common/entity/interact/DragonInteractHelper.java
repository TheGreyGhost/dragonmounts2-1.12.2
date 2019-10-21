/*
** 2016 April 24
**
** The author disclaims copyright to this source code. In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity.interact;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.GuiHandler;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonHelper;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 *   Handles interaction between the player and the dragon (i.e. the player clicks on the dragon-
 *   eg to feed it, tame it, mount it, open its inventory, etc.
 *
 *   Was originally structured as Interaction base class with subclasses (one per type of action) but this is probably overkill
 *     so I just combined them all into one class.
 *
 *   deals with tamed
 *
 */
public class DragonInteractHelper extends DragonHelper {

  public DragonInteractHelper(EntityTameableDragon dragon) {
    super(dragon);
//    actions.add(new DragonInteract(dragon));
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {

  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {

  }

  @Override
  public void registerDataParameters() {

  }

  @Override
  public void initialiseServerSide() {

  }

  @Override
  public void initialiseClientSide() {

  }

  @Override
  public void notifyDataManagerChange(DataParameter<?> key) {

  }

  @Override
  public void onLivingUpdate() {
    // set home position near owner when tamed
    if (isTamed()) {
      Entity owner = getOwner();
      if (owner != null) {
        setHomePosAndDistance(owner.getPosition(), HOME_RADIUS);
      }
    }
  }


  public static void registerConfigurationTags() { //todo initialise tags here
  }

  public boolean interact(EntityPlayer player, ItemStack item) {
    if (dragon.isServer()) {
      if (attemptBreedingInteraction(player, item)) return true;


      if (isAllowed(player)) {
                /*
                 * Riding
                 */
        if (dragon.canFitPassenger(player) && dragon.isTamed() && dragon.isSaddled() && !dragon.isBaby() && !player.isSneaking() && !hasInteractItemsEquipped(player)) {
          dragon.setRidingPlayer(player);
          return true;
        }

                /*
                 * GUI
                 */
        if (player.isSneaking() && dragon.isTamedFor(player) && !hasInteractItemsEquipped(player)) {
          // Dragon Inventory
          dragon.openGUI(player, GuiHandler.GUI_DRAGON);
          return true;
        }
      }

            /*
             * Sit
             */
      if (dragon.isTamed() && (DMUtils.hasEquipped(player, Items.STICK) || DMUtils.hasEquipped(player, Items.BONE)) && dragon.onGround) {
        dragon.getAISit().setSitting(!dragon.isSitting());
        dragon.getNavigator().clearPath();
        return true;
      }

            /*
             * Consume
             */
      if (DMUtils.hasEquippedFood(player)) {
        if (DMUtils.consumeFish(player) || DMUtils.consumeEquippedArray(player, DragonBreed.getFoodItems())) {
          // Taming
          if (!dragon.isTamed()) {
            dragon.tamedFor(player, dragon.getRNG().nextInt(5) == 0);
            eatEvent(player);
            return true;
          }

          // heal
          if (DragonMounts.instance.getConfig().hungerDecrement == 0) {
            eatEvent(player);
            dragon.heal(50);
            return true;
            //  hunger
          } else if (dragon.getHunger() < 100) {
            eatEvent(player);
            dragon.setHunger(dragon.getHunger() + (DMUtils.getFoodPoints(player)));
            return true;
          }

          // breed
          if (dragon.isBreedingItem(item) && dragon.isAdult() && !dragon.isInLove()) {
            eatEvent(player);
            dragon.setInLove(player);
            return true;
          }
          return true;
        }

        // Stop Growth
        ItemFood shrinking = (ItemFood) DMUtils.consumeEquipped(player, dragon.getBreed().getShrinkingFood());
        if (shrinking != null) {
          dragon.setGrowthPaused(true);
          eatEvent(player);
          player.sendStatusMessage(new TextComponentTranslation("dragon.growth.paused"), true);
          return true;
        }
        // Continue growth
        ItemFood growing = (ItemFood) DMUtils.consumeEquipped(player, dragon.getBreed().getGrowingFood());
        if (growing != null) {
          dragon.setGrowthPaused(false);
          eatEvent(player);
          return true;
        }
      }
    }
    return false;



  }

  @Nullable
  public EntityPlayer getControllingPlayer() {
    Entity entity = this.getPassengers().isEmpty() ? null : getPassengers().get(0);
    if (entity instanceof EntityPlayer) {
      return (EntityPlayer) entity;
    } else {
      return null;
    }
  }

  /**
   * attempt to use this item to put the dragon into love mode
   * @param player
   * @param itemstack
   * @return true if success
   */
  private boolean attemptBreedingInteraction(EntityPlayer player, ItemStack itemstack) {
    if (!itemstack.isEmpty()) {
      if (dragon.isBreedingItem(itemstack) && dragon.reproduction().canReproduce() && !dragon.isInLove()) {
        itemstack.shrink(1);
        dragon.setInLove(player);
        return true;
      }
    }
    return false;
  }

  /**
   * attempt to put the dragon on the player's shoulder (like a parrot)
   * @param player
   * @param itemStack
   * @return
   */
  private boolean attemptRideOnShoulder(EntityPlayer player, ItemStack itemStack) {
    // if the dragon is small enough, put it on the player's shoulder
    if (!dragon.isTamedFor(player) || player.isSneaking()) return false;



      dragon.setSitting(false);
      dragon.startRiding(player, true);
      return true;
    }

    if (player.isPassenger(this)) {
      return false;
    }

  }

  public boolean canBeLeashedTo(EntityPlayer player) {
    return true;
  }


  public boolean processInteract(EntityPlayer player, EnumHand hand) {
    ItemStack item = player.getHeldItem(hand);

    ItemStack itemstack = player.getHeldItem(hand);

    if (itemstack.getItem() == Items.BUCKET && !player.capabilities.isCreativeMode && !this.isChild() && DragonMounts.instance.getConfig().canMilk) {
      player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
      itemstack.shrink(1);

      if (itemstack.isEmpty()) {
        player.setHeldItem(hand, new ItemStack(Items.MILK_BUCKET));
      } else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.MILK_BUCKET))) {
        player.dropItem(new ItemStack(Items.MILK_BUCKET), false);
      }

      return true;
    }

    if (getHealth() <= 0) return false;


    return getInteractHelper().interact(player, item);
  }

  @SideOnly(Side.CLIENT)
  private void eatEvent(EntityPlayer player) {
    dragon.playSound(dragon.getEatSound(), 0.6f, 0.75f);
    spawnItemCrackParticles(DMUtils.consumeEquipped(player, DragonBreed.getFoodItems()));
  }

  private void spawnItemCrackParticles(Item item) {
    for (int i = 0; i < 15; i++) {
      double motionX = dragon.getRNG().nextGaussian() * 0.07D;
      double motionY = dragon.getRNG().nextGaussian() * 0.07D;
      double motionZ = dragon.getRNG().nextGaussian() * 0.07D;
      Vec3d pos = dragon.getAnimator().getThroatPosition();
      double hx = pos.x;
      double hy = pos.y;
      double hz = pos.z;
      // Spawn calculated particles
      dragon.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, hx, hy, hz, motionX, motionY, motionZ, Item.getIdFromItem(item));
    }
  }

  protected boolean isAllowed(EntityPlayer player) {
    boolean hasFood = DMUtils.consumeEquippedArray(player, DragonBreed.getFoodItems()) || DMUtils.consumeFish(player);

    if (!dragon.isTamed() && !hasFood) {
      player.sendStatusMessage(new TextComponentTranslation("dragon.notTamed"), true);
      return dragon.isTamedFor(player);
    } else if (!dragon.allowedOtherPlayers() && !dragon.isTamedFor(player) && dragon.isTamed() && !(dragon.getHealthRelative() < 1 && hasFood)) {
      player.sendStatusMessage(new TextComponentTranslation("dragon.locked"), true);
      return dragon.isTamedFor(player);
    } else return true;
  }

  public boolean allowedOtherPlayers() {
    return this.dataManager.get(ALLOW_OTHERPLAYERS);
  }

  public void setToAllowedOtherPlayers(boolean allow) {
    dataManager.set(ALLOW_OTHERPLAYERS, allow);
  }

  public void tamedFor(EntityPlayer player, boolean successful) {
    if (successful) {
      dragon.setTamed(true);
      navigator.clearPath(); // replacement for setPathToEntity(null);
      setAttackTarget(null);
      setOwnerId(player.getUniqueID());
      playTameEffect(true);
      world.setEntityState(this, (byte) 7);
    } else {
      playTameEffect(false);
      world.setEntityState(this, (byte) 6);
    }
  }

  public boolean isTamedFor(EntityPlayer player) {
    return isTamed() && isOwner(player);
  }


private static final String NBT_ALLOWOTHERPLAYERS = "AllowOtherPlayers";

//    private final List<DragonInteractBase> actions = new ArrayList<>();
}
