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

import com.TheRPGAdventurer.ROTD.client.gui.GuiHandler;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.util.EntityState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.commons.lang3.NotImplementedException;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 *   Handles interaction between the player and the dragon (i.e. the player clicks on the dragon-
 *   eg to feed it, tame it, mount it, open its inventory, etc.
 *
 *   Was originally structured as Interaction base class with subclasses (one per type of action) but this is probably overkill
 *     so I just combined them all into one class.
 *
 *   deals with
 *   - tamed
 *   - interaction between a player and a dragon that is tamed by someone else
 *   - home position - previously used to follow the owner but that's not what it's intended for in vanilla.  Vanilla uses it for (eg) villagers returning to their village
 *      AI task is used to follow owner instead.
 *     Currently - home isn't used for anything.  Maybe future use = to stay near a nest or an egg?
 *
 *  Interact is responsible for identifying which interaction is appropriate, and consuming the item if appropriate.
 *    The other helpers are responsible for change the dragon state in response to the interaction.
 *
 */
public class DragonInteractHelper extends DragonHelper {

  public DragonInteractHelper(EntityTameableDragon dragon) {
    super(dragon);
    setCompleted(FunctionTag.CONSTRUCTOR);
//    actions.add(new DragonInteract(dragon));
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    nbt.setBoolean(NBT_ALLOW_OTHER_PLAYERS, this.allowedOtherPlayers());
    //        nbt.setBoolean("sleeping", this.isSleeping()); //unused as of now
    nbt.setBoolean(NBT_HAS_HOME_POSITION, this.hasHomePosition);
    if (homePos != null && this.hasHomePosition) {
      nbt.setTag(NBT_HOME_POSITION, NBTUtil.createPosTag(homePos));
    }
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    this.setToAllowedOtherPlayers(nbt.getBoolean(NBT_ALLOW_OTHER_PLAYERS));
    this.hasHomePosition = nbt.getBoolean(NBT_HAS_HOME_POSITION);
    if (nbt.hasKey(NBT_HOME_POSITION, 10)) {
      this.homePos = NBTUtil.getPosFromTag(nbt.getCompoundTag(NBT_HOME_POSITION));
    } else {
      this.homePos = new BlockPos(0,0,0);
    }
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    entityDataManager.register(DATAPARAM_ALLOW_OTHER_PLAYERS, false);
//    dataManager.register(GROWTH_PAUSED, false);
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
  }

  @Override
  public void initialiseServerSide() {
    checkPreConditions(FunctionTag.INITIALISE_SERVER);
    setCompleted(FunctionTag.INITIALISE_SERVER);
  }

  @Override
  public void initialiseClientSide() {
    checkPreConditions(FunctionTag.INITIALISE_CLIENT);
    setCompleted(FunctionTag.INITIALISE_CLIENT);
  }

  public void onConfigurationChange() {
    throw new NotImplementedException("onConfigurationChange()");
  }

  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
//    // set home position near owner when tamed      /
//    if (dragon.isTamed()) {
//      Entity owner = dragon.getOwner();
//      if (owner != null) {
//        dragon.setHomePosAndDistance(owner.getPosition(), HOME_RADIUS);
//      }
//    }
  }

  public static void registerConfigurationTags() { // static initialisers register the tags
  }

  /**
   * I think this is called on both server and client
   * @param player
   * @param itemStack
   * @return
   */
  public boolean interact(EntityPlayer player, ItemStack itemStack) {

    // interactions:
    // see featureLocations.txt for logic
    boolean foodOrBreedItem = false;
    if (dragon.combat().considersThisItemEdible(itemStack.getItem())) {
      foodOrBreedItem = true;
      if (!dragon.isTamed()) {
        attemptToTame(player, itemStack);
        return true;
      } else {
        if (dragon.combat().feed(itemStack)) {
          consumeItem(player, itemStack);
          return true;
        }
      }
    }

    if (dragon.reproduction().isBreedingItem(itemStack)) {
      foodOrBreedItem = true;
      if (attemptBreedingInteraction(player, itemStack)) return true;
      return false;
    }

    if (foodOrBreedItem) return false;

//   Not sure how the whistle interaction was supposed to work

//
//          /*
//           * Sit
//           */
//    if (dragon.isTamed() && (DMUtils.hasEquipped(player, Items.STICK) || DMUtils.hasEquipped(player, Items.BONE)) && dragon.onGround) {
//      dragon.getAISit().setSitting(!dragon.isSitting());
//      dragon.getNavigator().clearPath();
//      return true;
//    }

    if (!dragon.isTamed()) {
      sendDragonNotTamedMessage(player);
      return false;
    }
    if (!isTamedFor(player) && !allowedOtherPlayers()) {
      sendDragonLockedMessage(player);
      return false;
    }

    if (dragon.riding().isASaddle(itemStack)) {
      boolean success = dragon.riding().attemptToSaddle(player, itemStack);
      if (!success) return false;
      consumeItem(player, itemStack);
      return true;
    }

    if (dragon.inventory().isOpenGUIkeyPressed(player)) {
      // Dragon Inventory
      dragon.inventory().openGUI(player, GuiHandler.GUI_DRAGON);
      return true;
    }

    if ()

    // attempt to mount the dragon
    if (!dragon.riding().isSaddled()) {
      player.sendStatusMessage(new TextComponentTranslation("dragon.msg.isnotsaddled"), true);
      return false;
    }
              /*
               * Riding
               */
      if (dragon.canFitPassenger(player) && dragon.isTamed() && dragon.isSaddled()  && !player.isSneaking() && !hasInteractItemsEquipped(player)) {
        dragon.setRidingPlayer(player);
        return true;
      }

              /*
               * GUI
               */
      if (player.isSneaking() && dragon.isTamedFor(player) && !hasInteractItemsEquipped(player)) {
      }
    }
    return false;
          /*/

//      // Stop Growth
//      ItemFood shrinking = (ItemFood) DMUtils.consumeEquipped(player, dragon.getBreed().getShrinkingFood());
//      if (shrinking != null) {
//        dragon.setGrowthPaused(true);
//        eatEvent(player);
//        player.sendStatusMessage(new TextComponentTranslation("dragon.growth.paused"), true);
//        return true;
//      }
//      // Continue growth
//      ItemFood growing = (ItemFood) DMUtils.consumeEquipped(player, dragon.getBreed().getGrowingFood());
//      if (growing != null) {
//        dragon.setGrowthPaused(false);
//        eatEvent(player);
//        return true;
//      }
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
   * if successful, dragon eats it
   * @param player
   * @param itemstack
   * @return true if success
   */
  private boolean attemptBreedingInteraction(EntityPlayer player, ItemStack itemstack) {
    if (dragon.isBreedingItem(itemstack) && dragon.reproduction().canReproduce() && !dragon.isInLove()) {
      dragon.setInLove(player);
      consumeItem(player, itemstack);
      return true;
    }
    return false;

//    // breed
//    if (dragon.isBreedingItem(item)
//            && dragon.reproduction().hasReachedReproductiveAge() && dragon.reproduction().isFertile()
//            && !dragon.isInLove()) {
//      eatEvent(player);
//      dragon.setInLove(player);
//      return true;
//    }
//    return true;

  }

  /**
   * Attempts to tame the dragon.  Consumes the item even if unsuccessful.
   * @param player
   * @return true for successful taming
   */
  private boolean attemptToTame(EntityPlayer player, ItemStack itemStack) {
    double tameChance = (double)dragon.configuration().getVariantTagValue(DragonVariants.Category.BEHAVIOUR, CHANCE_OF_TAMING_SUCCESS);
    boolean successfulTaming = (dragon.getRNG().nextDouble() < tameChance / 100.0);
    onTameAttempt(player, successfulTaming);
    consumeItem(player, itemStack);
    return successfulTaming;
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

  /** I think this is called on both server and client
   *
   * @param player
   * @param hand
   * @return
   */
  public boolean processInteract(EntityPlayer player, EnumHand hand) {
    ItemStack item = player.getHeldItem(hand);
//    ItemStack itemstack = player.getHeldItem(hand);
//
//   remove milking dragon for now
//    if (itemstack.getItem() == Items.BUCKET && !player.capabilities.isCreativeMode && !this.isChild() && DragonMounts.instance.getConfig().canMilk) {
//      player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
//      itemstack.shrink(1);
//
//      if (itemstack.isEmpty()) {
//        player.setHeldItem(hand, new ItemStack(Items.MILK_BUCKET));
//      } else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.MILK_BUCKET))) {
//        player.dropItem(new ItemStack(Items.MILK_BUCKET), false);
//      }
//
//      return true;
//    }
    if (dragon.getHealth() <= 0) return false;

    return interact(player, item);
  }

  private void consumeItem(EntityPlayer player, ItemStack itemStack) {
    dragon.sounds().playEatSound();
//    dragon.playSound(dragon.getEatSound(), 0.6f, 0.75f);
    spawnMouthChewingItemParticles(itemStack.getItem());
    itemStack.shrink(1);
  }

  private void spawnMouthChewingItemParticles(Item item) {
    if (!dragon.isClient()) return;
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

  private void sendDragonNotTamedMessage(EntityPlayer player) {
    player.sendStatusMessage(new TextComponentTranslation("dragon.msg.notTamed"), true);
  }

  private void sendDragonLockedMessage(EntityPlayer player) {
    player.sendStatusMessage(new TextComponentTranslation("dragon.msg.locked"), true);
  }


  public boolean allowedOtherPlayers() {
    return entityDataManager.get(DATAPARAM_ALLOW_OTHER_PLAYERS);
  }

  public void setToAllowedOtherPlayers(boolean allow) {
    entityDataManager.set(DATAPARAM_ALLOW_OTHER_PLAYERS, allow);
  }

  /**
   * Implements the outcome of a tame attempt (successful or failed)
   * Based on code from EntityWolf.processInteract()
   * @param player
   * @param successful
   */
  public void onTameAttempt(EntityPlayer player, boolean successful) {
    if (!dragon.isServer()) return;
    if (successful) {
      dragon.setTamed(true);
      dragon.getNavigator().clearPath(); // replacement for setPathToEntity(null);
      dragon.setAttackTarget(null);
      dragon.setOwnerId(player.getUniqueID());
//      dragon.playTameEffect(true);  this effect is played by the setEntityState sent to the client
     dragon.world.setEntityState(dragon, EntityState.TAME_ATTEMPT_SUCCEEDED.getMagicNumber());
    } else {
//      playTameEffect(false);
      dragon.world.setEntityState(dragon, EntityState.TAME_ATTEMPT_FAILED.getMagicNumber());
    }
  }

  public boolean isTamedFor(EntityPlayer player) {
    return dragon.isTamed() && dragon.isOwner(player);
  }

  private static final DataParameter<Boolean> DATAPARAM_ALLOW_OTHER_PLAYERS = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);

  private static final String NBT_ALLOW_OTHER_PLAYERS = "AllowOtherPlayers";
  private static final String NBT_HAS_HOME_POSITION = "HasHomePosition";
  private static final String NBT_HOME_POSITION = "HomePosition";
  //    private final List<DragonInteractBase> actions = new ArrayList<>();

  public static final double BASE_FOLLOW_RANGE = 70;
  public static final double BASE_FOLLOW_RANGE_FLYING = BASE_FOLLOW_RANGE * 2;
  public static final int HOME_RADIUS = 64;

  // server side only
  private boolean hasHomePosition = false;
  private BlockPos homePos;

  private static final DragonVariantTag CHANCE_OF_TAMING_SUCCESS = DragonVariantTag.addTag("chanceoftamingsuccess", 25, 0, 100,
          "the chance that feeding the dragon will tame it (%)").categories(DragonVariants.Category.BEHAVIOUR);


}
