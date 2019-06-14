package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.helper;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.util.math.MathHelper;

/**
 * For debugging.  Just copied the base class (too many private fields!)
 * Created by TGG on 22/07/2015.
 */
public class DragonLookHelper extends EntityLookHelper {

    public DragonLookHelper(EntityTameableDragon i_dragon) {
        super(i_dragon);
        entity = i_dragon;
    }

    private EntityLiving entity;
    /**
     * The amount of change that is made each update for an entity facing a direction.
     */
    private float deltaLookYaw;
    /**
     * The amount of change that is made each update for an entity facing a direction.
     */
    private float deltaLookPitch;
    /**
     * Whether or not the entity is trying to look at something.
     */
    private boolean isLooking;
    private double posX;
    private double posY;
    private double posZ;

    /**
     * Sets position to look at using entity
     */
    @Override
    public void setLookPositionWithEntity(Entity dragon, float lookYaw, float lookPitch) {
        this.posX = dragon.posX;

        if (dragon instanceof EntityLivingBase) {
            this.posY = dragon.posY + (double) dragon.getEyeHeight();
        } else {
            this.posY = (dragon.getEntityBoundingBox().minY + dragon.getEntityBoundingBox().maxY) / 2.0D;
        }

        this.posZ = dragon.posZ;
        this.deltaLookYaw = lookYaw;
        this.deltaLookPitch = lookPitch;
        this.isLooking = true;
    }

    /**
     * Sets position to look at
     */
    @Override
    public void setLookPosition(double x, double y, double z, float lookYaw, float lookPitch) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.deltaLookYaw = lookYaw;
        this.deltaLookPitch = lookPitch;
        this.isLooking = true;
    }

    /**
     * Updates look
     */
    @Override
    public void onUpdateLook() {
        this.entity.rotationPitch = 0.0F;

        if (this.isLooking) {
            this.isLooking = false;
            double d0 = this.posX - this.entity.posX;
            double d1 = this.posY - (this.entity.posY + (double) this.entity.getEyeHeight());
            double d2 = this.posZ - this.entity.posZ;
            double d3 = (double) MathHelper.sqrt(d0 * d0 + d2 * d2);
            float f = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float f1 = (float) (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
            this.entity.rotationPitch = this.updateRotation(this.entity.rotationPitch, f1, this.deltaLookPitch);
            this.entity.rotationYawHead = this.updateRotation(this.entity.rotationYawHead, f, this.deltaLookYaw);
        } else {
            this.entity.rotationYawHead = this.updateRotation(this.entity.rotationYawHead, this.entity.renderYawOffset, 10.0F);
        }

        float f2 = MathX.normDeg(this.entity.rotationYawHead - this.entity.renderYawOffset);

        if (!this.entity.getNavigator().noPath()) {
            if (f2 < -75.0F) {
                this.entity.rotationYawHead = this.entity.renderYawOffset - 75.0F;
            }

            if (f2 > 75.0F) {
                this.entity.rotationYawHead = this.entity.renderYawOffset + 75.0F;
            }
        }
    }

    private float updateRotation(float p_75652_1_, float p_75652_2_, float p_75652_3_) {
        float f3 = MathX.normDeg(p_75652_2_ - p_75652_1_);

        if (f3 > p_75652_3_) {
            f3 = p_75652_3_;
        }

        if (f3 < -p_75652_3_) {
            f3 = -p_75652_3_;
        }

        return p_75652_1_ + f3;
    }

    @Override
    public boolean getIsLooking() {
        return this.isLooking;
    }

    @Override
    public double getLookPosX() {
        return this.posX;
    }

    @Override
    public double getLookPosY() {
        return this.posY;
    }

    @Override
    public double getLookPosZ() {
        return this.posZ;
    }

}