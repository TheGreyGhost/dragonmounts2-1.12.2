package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;

/**
 * Created by TGG on 19/10/2019.
 * Used when the dragon rides on the player's shoulder
 */
public class DragonShoulderRidingHelper extends DragonHelper {
  public DragonShoulderRidingHelper(EntityTameableDragon dragon) {
    super(dragon);
  }

  /**
   * Initialise all the configuration tags used by this helper
   */
  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
//    DragonVariants.addVariantTagValidator(new DragonReproductionValidator());
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

  /**
   * Attempt to place the dragon onto one of the player's shoulders
   * @param entityPlayer
   * @return
   */
  public boolean attemptToPlaceOnShoulder(EntityPlayer entityPlayer) {
    if (!dragon.isTamedFor(entityPlayer)) return false;
    if (dragon.getRidingEntity() == entityPlayer) return false;
    if (true != (boolean)dragon.configuration().getVariantTagValue(DragonVariants.Category.BEHAVIOUR, WILL_RIDE_SHOULDER)) return false;
    entityPlayer.set

    if (!this.isSittingOnShoulder && !this.entity.isSitting() && !this.entity.getLeashed())
    {
      if (this.entity.getEntityBoundingBox().intersects(this.owner.getEntityBoundingBox()))
      {
        this.isSittingOnShoulder = this.entity.setEntityOnShoulder(this.owner);
      }
    }
  }

  private static final DragonVariantTag WILL_RIDE_SHOULDER = DragonVariantTag.addTag("willrideshoulder", true,
          "will the dragon ride on the player's shoulder?").categories(DragonVariants.Category.BEHAVIOUR);


}
