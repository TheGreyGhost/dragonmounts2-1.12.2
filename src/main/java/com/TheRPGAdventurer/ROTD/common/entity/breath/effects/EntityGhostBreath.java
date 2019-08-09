//package com.TheRPGAdventurer.ROTD.common.entity.breath.effects;
//
//import com.TheRPGAdventurer.ROTD.common.entity.breath.IEntityParticle;
//import net.minecraft.entity.EntityLiving;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
//@Deprecated
//public class EntityGhostBreath extends EntityLiving implements IEntityParticle {
//
//  public EntityGhostBreath(World worldIn) {
//    super(worldIn);
//  }
//
//  @Override
//  public double getMotionX() {
//    return motionX;
//  }
//
//  @Override
//  public double getMotionY() {
//    return motionY;
//  }
//
//  @Override
//  public double getMotionZ() {
//    return motionZ;
//  }
//
//  @Override
//  public double getSpeedSQ() {
//    return motionX * motionX + motionY * motionY + motionZ * motionZ;
//  }
//
//  //  public boolean isInWater() {return isInWater();}
//  @Override
//  public boolean isCollided() {
//    return collided;
//  }
//
//  @Override
//  public boolean isOnGround() {
//    return onGround;
//  }
//
//  @Override
//  public void setMotion(Vec3d newMotion) {
//    motionX = newMotion.x;
//    motionY = newMotion.y;
//    motionZ = newMotion.z;
//  }
//
//  @Override
//  protected void initEntityAI() {
//  }
//}
