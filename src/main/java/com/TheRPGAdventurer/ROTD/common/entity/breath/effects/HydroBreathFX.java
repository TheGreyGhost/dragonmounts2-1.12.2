package com.TheRPGAdventurer.ROTD.common.entity.breath.effects;

import com.TheRPGAdventurer.ROTD.common.entity.breath.DragonBreathMode;
import com.TheRPGAdventurer.ROTD.common.entity.breath.IEntityParticle;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeLegacy;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.helper.util.EntityMoveAndResizeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Created by TGG on 21/06/2015.
 * EntityFX that makes up the flame breath weapon; client side.
 * <p>
 * Usage:
 * (1) create a new FlameBreathFX using createFlameBreathFX
 * (2) spawn it as per normal
 */
@Deprecated
public class HydroBreathFX extends Entity implements IEntityParticle {

  public float scale;

  /**
   * creates a single EntityFX from the given parameters.  Applies some random spread to direction.
   *
   * @param world
   * @param x                     world [x,y,z] to spawn at (inates are the centre point of the fireball)
   * @param y
   * @param z
   * @param directionX            initial world direction [x,y,z] - will be normalised.
   * @param directionY
   * @param directionZ
   * @param power                 the power of the ball
   * @param partialTicksHeadStart if spawning multiple EntityFX per tick, use this parameter to spread the starting
   *                              location in the direction
   * @return the new FlameBreathFX
   */
  public static HydroBreathFX createHydroBreathFX(World world, double x, double y, double z,
                                                  double directionX, double directionY, double directionZ,
                                                  BreathNodeP.Power power,
                                                  float partialTicksHeadStart, EntityTameableDragon dragon) {
    Vec3d direction = new Vec3d(directionX, directionY, directionZ).normalize();

    Random rand = new Random();
    BreathNodeP breathNode = new BreathNodeLegacy(power, DragonBreathMode.DEFAULT);
    breathNode.randomiseProperties(rand);
    Vec3d actualMotion = breathNode.getRandomisedStartingMotion(direction, rand);

    x += actualMotion.x * partialTicksHeadStart;
    y += actualMotion.y * partialTicksHeadStart;
    z += actualMotion.z * partialTicksHeadStart;
    HydroBreathFX newIceBreathFX = new HydroBreathFX(world, x, y, z, actualMotion, breathNode);
    return newIceBreathFX;
  }

  /**
   * call once per tick to update the EntityFX size, position, collisions, etc
   */
  @Override
  public void onUpdate() {
    final float YOUNG_AGE = 0.25F;
    final float OLD_AGE = 0.75F;

    float lifetimeFraction = breathNode.getLifetimeFraction();

    final float ENTITY_SCALE_RELATIVE_TO_SIZE = 5.0F; // factor to convert from particleSize to particleScale
    float currentEntitySize = breathNode.getCurrentRenderDiameter();
    scale = ENTITY_SCALE_RELATIVE_TO_SIZE * currentEntitySize;

    // spawn a smoke trail after some time
    if (PARTICLE_CHANCE != 0 && rand.nextFloat() < lifetimeFraction && rand.nextFloat() <= PARTICLE_CHANCE) {
      float f1 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;
      float f2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width * 0.5F;
      this.world.spawnParticle(EnumParticleTypes.WATER_SPLASH, this.posX + (double) f1, (double) (0.8F), this.posZ + (double) f2, this.motionX, this.motionY, this.motionZ);
    }

    float newAABBDiameter = breathNode.getCurrentAABBcollisionSize();

    prevPosX = posX;
    prevPosY = posY;
    prevPosZ = posZ;
    entityMoveAndResizeHelper.moveAndResizeEntity(motionX, motionY, motionZ, newAABBDiameter, newAABBDiameter);

    // // if (isCollided && onGround) {
    //       motionY -= 0.01F;         // ensure that we hit the ground next time too
    //   }
    breathNode.updateAge(this);
    if (breathNode.isDead()) {
      setDead();
    }
  }

  /**
   * Vanilla moveEntity does a pile of unneeded calculations, and also doesn't handle resize around the centre properly,
   * so replace with a custom one
   *
   * @param dx the amount to move the entity in world inates [dx, dy, dz]
   * @param dy
   * @param dz
   */
  @Override
  public void move(MoverType mover, double dx, double dy, double dz) {
    entityMoveAndResizeHelper.moveAndResizeEntity(dx, dy, dz, this.width, this.height);
  }

  @Override
  public double getMotionX() {
    return motionX;
  }

  @Override
  public double getMotionY() {
    return motionY;
  }

  @Override
  public double getMotionZ() {
    return motionZ;
  }

  @Override
  public double getSpeedSQ() {
    return motionX * motionX + motionY * motionY + motionZ * motionZ;
  }

  //  public boolean isInWater() {return isInWater();}
  @Override
  public boolean isCollided() {
    return collided;
  }

  @Override
  public boolean isOnGround() {
    return onGround;
  }

  @Override
  public void setMotion(Vec3d newMotion) {
    motionX = newMotion.x;
    motionY = newMotion.y;
    motionZ = newMotion.z;
  }

  protected EnumParticleTypes getWaterParticleID() {
    if (LARGE_PARTICLE_CHANCE != 0 && rand.nextFloat() <= LARGE_PARTICLE_CHANCE) {
      return EnumParticleTypes.WATER_DROP;
    } else {
      return EnumParticleTypes.WATER_DROP;
    }
  }

  @Override
  protected void entityInit() {
  }

  @Override
  protected void readEntityFromNBT(NBTTagCompound compound) {
  }

  @Override
  protected void writeEntityToNBT(NBTTagCompound compound) {
  }

  private HydroBreathFX(World world, double x, double y, double z, Vec3d motion,
                        BreathNodeP i_breathNode) {
    super(world);

    breathNode = i_breathNode;
    this.setPosition(x, y, z);
    this.scale = (this.rand.nextFloat() * 0.7F + 0.7F) * 4.0F;

    //undo random velocity variation of vanilla EntityFX constructor
    motionX = motion.x;
    motionY = motion.y;
    motionZ = motion.z;
    dragon = new EntityTameableDragon(world);

    entityMoveAndResizeHelper = new EntityMoveAndResizeHelper(this);
  }
  private static final float MAX_ALPHA = 0.99F;
  private final float PARTICLE_CHANCE = 0.1f;
  private final float LARGE_PARTICLE_CHANCE = 0.3f;
  private BreathNodeP breathNode;
  private EntityTameableDragon dragon;
  private EntityMoveAndResizeHelper entityMoveAndResizeHelper;
}
