package com.TheRPGAdventurer.ROTD.common.entity.breath;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.helper.util.Base64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by TGG on 6/07/2015.
 * The target of the dragon's breath weapon
 * Can be a world location [x,y,z], a particular entity, or a direction [x,y,z]
 * Typical usage:
 * (1a) use targetLocation(), targetEntity(), targetEntityID(), or targetDirection() to create a target.
 * or
 * (1b) fromMovingObjectPosition to create a target from a raytraced MOP
 * or
 * (1c) fromBytes to create from a bytebuf, typically from network message
 * or
 * (1d) fromEncodedString to create from a printable string (Base64), typically from a datawatcher
 * (2) use getters to retrieve the target type and details.
 * (3) toBytes or toEncodedString to create a serialised representation (for network or datawatcher respectively)
 * (4) setEntityLook() to set the look of the entity based on the target
 * (5) setNavigationPath/Avoid() to set navigate towards or away from the target
 * (6) plus some utility methods to check if the target is near or far, if it has changed, etc
 */
public class BreathWeaponTarget {
  public enum TypeOfTarget {LOCATION, ENTITY, DIRECTION}

  public enum WeaponUsed {PRIMARY, SECONDARY, NONE}

  public static BreathWeaponTarget targetLocation(Vec3d location, WeaponUsed i_weaponUsed) {
    BreathWeaponTarget retval = new BreathWeaponTarget(TypeOfTarget.LOCATION, i_weaponUsed);
    retval.coordinates = location;
    return retval;
  }

  public static BreathWeaponTarget targetEntity(Entity entity, WeaponUsed i_weaponUsed) {
    BreathWeaponTarget retval = new BreathWeaponTarget(TypeOfTarget.ENTITY, i_weaponUsed);
    retval.entityID = entity.getEntityId();
    return retval;
  }

  public static BreathWeaponTarget targetEntityID(int i_entity, WeaponUsed i_weaponUsed) {
    BreathWeaponTarget retval = new BreathWeaponTarget(TypeOfTarget.ENTITY, i_weaponUsed);
    retval.entityID = i_entity;
    return retval;
  }

  public static BreathWeaponTarget targetDirection(Vec3d direction, WeaponUsed i_weaponUsed) {
    BreathWeaponTarget retval = new BreathWeaponTarget(TypeOfTarget.DIRECTION, i_weaponUsed);
    retval.coordinates = direction.normalize();
    return retval;
  }

  // create a BreathWeaponTarget from a ByteBuf
  public static BreathWeaponTarget fromBytes(ByteBuf buf) throws IndexOutOfBoundsException, IllegalArgumentException {
    int typeOfHitInt = buf.readInt();
    if (typeOfHitInt < 0 || typeOfHitInt >= TypeOfTarget.values().length) {
      throw new IllegalArgumentException("typeOfHitInt was " + typeOfHitInt);
    }
    TypeOfTarget typeOfHit = TypeOfTarget.values()[typeOfHitInt];

    int weaponUsedInt = buf.readInt();
    if (weaponUsedInt < 0 || weaponUsedInt >= WeaponUsed.values().length) {
      throw new IllegalArgumentException("weaponUsed was " + weaponUsedInt);
    }
    WeaponUsed weaponUsed = WeaponUsed.values()[weaponUsedInt];

    BreathWeaponTarget breathWeaponTarget;
    switch (typeOfHit) {
      case DIRECTION: {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        breathWeaponTarget = BreathWeaponTarget.targetDirection(new Vec3d(x, y, z), weaponUsed);
        break;
      }
      case LOCATION: {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        breathWeaponTarget = BreathWeaponTarget.targetLocation(new Vec3d(x, y, z), weaponUsed);
        break;
      }
      case ENTITY: {
        int rawEntityID = buf.readInt();
        breathWeaponTarget = BreathWeaponTarget.targetEntityID(rawEntityID, weaponUsed);
        break;
      }
      default: {
        throw new IllegalArgumentException("Invalid typeOfHit" + typeOfHit);
      }
    }
    return breathWeaponTarget;
  }

  /**
   * Create a target from a MovingObjectPosition
   *
   * @param movingObjectPosition can be null
   * @param entityPlayer         can be null
   * @return null if not possible
   */
  public static BreathWeaponTarget fromMovingObjectPosition(RayTraceResult movingObjectPosition,
                                                            EntityPlayer entityPlayer, WeaponUsed weaponUsed) {
    if (movingObjectPosition == null) {
      return (entityPlayer == null) ? null : targetDirection(entityPlayer.getLook(1.0F), weaponUsed);
    }
    switch (movingObjectPosition.typeOfHit) {
      case BLOCK: {
        // when the side of a block is hit, we can't tell which block it was, because the hitVec is on the boundary.
        //  to solve this, we push the hitVec back off the boundary if this is the zpos, ypos, or xpos face
        final double NUDGE = 0.001;
        switch (movingObjectPosition.sideHit) {
          case EAST:
            return targetLocation(movingObjectPosition.hitVec.subtract(NUDGE, 0, 0), weaponUsed);
          case UP:
            return targetLocation(movingObjectPosition.hitVec.subtract(0, NUDGE, 0), weaponUsed);
          case SOUTH:
            return targetLocation(movingObjectPosition.hitVec.subtract(0, 0, NUDGE), weaponUsed);
          default:
            return targetLocation(movingObjectPosition.hitVec, weaponUsed);
        }
      }
      case ENTITY: {
        return targetEntity(movingObjectPosition.entityHit, weaponUsed);
      }
      case MISS: {
        return (entityPlayer == null) ? null : targetDirection(entityPlayer.getLook(1.0F), weaponUsed);
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown typeOfHit:" + movingObjectPosition.typeOfHit);
        return null;
      }
    }
  }

  /**
   * create a BreathWeaponTarget from a string-encoded version
   *
   * @param targetString
   * @return the target; or null if no target
   */
  public static BreathWeaponTarget fromEncodedString(String targetString) throws IndexOutOfBoundsException, IllegalArgumentException {
    if (targetString.isEmpty()) return null;
    byte[] bytes = Base64.decode(targetString);
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    return fromBytes(byteBuf);
  }

  /**
   * Check if these two BreathWeaponTargets are significantly different from each other
   *
   * @param first
   * @param second
   * @return true if similar, false if not.
   */
  public static boolean approximatelyMatches(BreathWeaponTarget first, BreathWeaponTarget second) {
    if (first == null) {
      return (second == null);
    }
    return first.approximatelyMatches(second);
  }

  public TypeOfTarget getTypeOfTarget() {
    return typeOfTarget;
  }

  public WeaponUsed getWeaponUsed() {
    return weaponUsed;
  }

  /**
   * getChangeInValue the entity being targeted
   *
   * @param world
   * @return null if not found or not valid
   */
  public Entity getTargetEntity(World world) {
    return world.getEntityByID(entityID);
  }

  public Vec3d getTargetedLocation() {
    return new Vec3d(coordinates.x, coordinates.y, coordinates.z);
  }

  public Vec3d getTargetedDirection() {
    return new Vec3d(coordinates.x, coordinates.y, coordinates.z);
  }

  /**
   * Sets where the entity is looking, based on the target
   *
   * @param world
   * @param entityLookHelper
   * @param yawSpeed         speed of head yaw change
   * @param pitchSpeed       speed of head pitch change
   */
  public void setEntityLook(World world, EntityLookHelper entityLookHelper,
                            Vec3d origin, float yawSpeed, float pitchSpeed) {
    switch (typeOfTarget) {
      case LOCATION: {
        entityLookHelper.setLookPosition(coordinates.x, coordinates.y, coordinates.z,
                yawSpeed, pitchSpeed);
        break;
      }
      case ENTITY: {
        Entity targetEntity = world.getEntityByID(entityID);
        if (targetEntity != null) {
          entityLookHelper.setLookPositionWithEntity(targetEntity, yawSpeed, pitchSpeed);
        }
        break;
      }
      case DIRECTION: {  // simulate a look direction by choosing a very-far-away point
        final double FAR_DISTANCE = 1000;
        entityLookHelper.setLookPosition(origin.x + FAR_DISTANCE * coordinates.x,
                origin.y + FAR_DISTANCE * coordinates.y,
                origin.z + FAR_DISTANCE * coordinates.z,
                yawSpeed, pitchSpeed);
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown typeOfTarget:" + typeOfTarget);
        break;
      }
    }
  }

  /**
   * Set the path navigation to head towards the given target (no effect for DIRECTION target type)
   *
   * @param world
   * @param pathNavigate
   * @param moveSpeed
   */
  public void setNavigationPath(World world, PathNavigate pathNavigate, double moveSpeed) {
    switch (typeOfTarget) {
      case LOCATION: {
        pathNavigate.tryMoveToXYZ(coordinates.x, coordinates.y, coordinates.z, moveSpeed);
        break;
      }
      case ENTITY: {
        Entity targetEntity = world.getEntityByID(entityID);
        if (targetEntity != null) {
          pathNavigate.tryMoveToEntityLiving(targetEntity, moveSpeed);
        }
        break;
      }
      case DIRECTION: {  // no need to move
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown typeOfTarget:" + typeOfTarget);
        break;
      }
    }
  }

  /**
   * Set the path navigation to head away from the given target (no effect for DIRECTION target type)
   *
   * @param world
   * @param pathNavigate
   * @param moveSpeed
   */
  public void setNavigationPathAvoid(World world, PathNavigate pathNavigate, Vec3d currentPosition, double moveSpeed, double desiredDistance) {
    Vec3d target;

    switch (typeOfTarget) {
      case LOCATION: {
        target = coordinates;
        break;
      }
      case ENTITY: {
        Entity targetEntity = world.getEntityByID(entityID);
        if (targetEntity == null) return;
        target = targetEntity.getPositionVector().addVector(0, targetEntity.getEyeHeight(), 0);
        break;
      }
      case DIRECTION: {  // no need to move
        return;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown typeOfTarget:" + typeOfTarget);
        return;
      }
    }

    // choose a block at random at the desired radius from the target.  Initially try directly opposite, later on try
    //   from the entire radius around the target.

    Random random = new Random();

    final int RANDOM_TRIES = 10;
    int numberOfTries = 1;
    double deltaX = currentPosition.x - target.x;
    double deltaZ = currentPosition.z - target.z;
    double fleeAngle = Math.atan2(deltaZ, deltaX);
    do {
      double halfAngleOfSearch = Math.PI * (double) numberOfTries / (double) RANDOM_TRIES;
      double angle = fleeAngle + (random.nextFloat() * 2.0 - 1.0) * halfAngleOfSearch;
      double xDest = target.x + Math.cos(angle) * desiredDistance;
      double zDest = target.z + Math.sin(angle) * desiredDistance;

      int blockX = MathHelper.floor(xDest);
      int blockY = MathHelper.floor(target.y);
      int blockZ = MathHelper.floor(zDest);

      int initBlockY = blockY;
      if (world.isAirBlock(new BlockPos(blockX, blockY, blockZ))) {
        while (blockY > 0 && world.isAirBlock(new BlockPos(blockX, blockY - 1, blockZ))) {
          --blockY;
        }
      } else {
        final int MAX_BLOCK_Y = 255;
        while (blockY <= MAX_BLOCK_Y && !world.isAirBlock(new BlockPos(blockX, blockY + 1, blockZ))) {
          ++blockY;
        }
        ++blockY;
      }
      int changeInY = blockY - initBlockY;
      boolean success = pathNavigate.tryMoveToXYZ(xDest, target.y + changeInY, zDest, moveSpeed);
      if (success) return;
    } while (++numberOfTries <= RANDOM_TRIES);
  }

  /**
   * calculate the distance from the given point to the target
   *
   * @param world
   * @return distance squared to the target, or -ve number if not relevant (eg target type DIRECTION)
   */
  public double distanceSQtoTarget(World world, Vec3d startPoint) {
    switch (typeOfTarget) {
      case LOCATION: {
        return startPoint.squareDistanceTo(coordinates);
      }
      case ENTITY: {
        Entity targetEntity = world.getEntityByID(entityID);
        if (targetEntity != null) {
          return startPoint.squareDistanceTo(targetEntity.getPositionVector());
        } else {
          return -1;
        }
      }
      case DIRECTION: {  // no need to move
        return -1;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown typeOfTarget:" + typeOfTarget);
        return -1;
      }
    }
  }

  /**
   * getTargetedPoint the point being targeted in [x,y,z]
   *
   * @param world
   * @param origin the origin of the breath weapon (dragon's throat)
   * @return an [x,y,z] to fire the beam at; or null if none
   */
  public Vec3d getTargetedPoint(World world, Vec3d origin) {
    Vec3d destination = null;
    switch (typeOfTarget) {
      case LOCATION: {
        destination = getTargetedLocation();
        break;
      }
      case DIRECTION: {
        destination = origin.add(getTargetedDirection());
        break;
      }
      case ENTITY: {
        Entity entity = getTargetEntity(world);
        if (entity == null) {
          destination = null;
        } else {
          destination = entity.getPositionVector().addVector(0, entity.getEyeHeight() / 2.0, 0);
        }
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unexpected target type:" + typeOfTarget);
        destination = null;
        break;
      }
    }
    return destination;
  }

  /**
   * write the BreathWeaponTarget to a ByteBuf
   *
   * @param buf
   */
  public void toBytes(ByteBuf buf) {
    buf.writeInt(typeOfTarget.ordinal());
    buf.writeInt(weaponUsed.ordinal());
    switch (typeOfTarget) {
      case LOCATION:
      case DIRECTION: {
        buf.writeDouble(coordinates.x);
        buf.writeDouble(coordinates.y);
        buf.writeDouble(coordinates.z);
        break;
      }
      case ENTITY: {
        buf.writeInt(entityID);
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown type of hit:" + typeOfTarget);
        break;
      }
    }
  }

  /**
   * writes the BreathWeaponTarget to an encoded string
   *
   * @return the encoded string
   */
  public String toEncodedString() {
    final int INITIAL_CAPACITY = 256;
    ByteBuf byteBuf = Unpooled.buffer(INITIAL_CAPACITY);
    toBytes(byteBuf);
    byte[] messageonly = Arrays.copyOf(byteBuf.array(), byteBuf.readableBytes());
    return Base64.encodeToString(messageonly, true);
  }

  /**
   * Check if these two BreathWeaponTargets are significantly different from each other
   *
   * @param other
   * @return true if similar, false if not.
   */
  public boolean approximatelyMatches(BreathWeaponTarget other) {
    if (other == null) return false;
    if (other.typeOfTarget != this.typeOfTarget) return false;
    if (other.weaponUsed != this.weaponUsed) return false;

    switch (typeOfTarget) {
      case ENTITY: {
        return (this.entityID == other.entityID);
      }

      case LOCATION: {
        double squareDistance = this.coordinates.squareDistanceTo(other.coordinates);
        final double THRESHOLD_DISTANCE = 0.5;
        return squareDistance < THRESHOLD_DISTANCE * THRESHOLD_DISTANCE;
      }

      case DIRECTION: {
        final double THRESHOLD_CHANGE_IN_ANGLE = 1.0; // in degrees
        double cosAngle = this.coordinates.dotProduct(other.coordinates);  // coordinates are both always normalised
        return cosAngle > Math.cos(Math.toRadians(THRESHOLD_CHANGE_IN_ANGLE));
      }
      default: {
        DragonMounts.loggerLimit.error_once("invalid typeOfTarget:" + typeOfTarget);
        return false;
      }
    }
  }

  /**
   * Check if these two BreathWeaponTargets exactly match each other
   *
   * @param other
   * @return
   */
  public boolean exactlyMatches(BreathWeaponTarget other) {
    if (other.typeOfTarget != this.typeOfTarget) return false;
    if (other.weaponUsed != this.weaponUsed) return false;
    switch (typeOfTarget) {
      case ENTITY: {
        return (this.entityID == other.entityID);
      }

      case DIRECTION:
      case LOCATION: {
        return (this.coordinates.x == other.coordinates.x
                && this.coordinates.y == other.coordinates.y
                && this.coordinates.z == other.coordinates.z);
      }
      default: {
        DragonMounts.loggerLimit.error_once("invalid typeOfTarget:" + typeOfTarget);
        return false;
      }
    }
  }

//  public double getYawAngle(Vec3d dragonPosition)
//  {
//    double d0 = this.posX - this.entity.posX;
//    double d1 = this.posY - (this.entity.posY + (double)this.entity.getEyeHeight());
//    double d2 = this.posZ - this.entity.posZ;
//    double d3 = (double)MathHelper.sqrt_double(d0 * d0 + d2 * d2);
//    float f = (float)(Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
//    float f1 = (float)(-(Math.atan2(d1, d3) * 180.0D / Math.PI));
//    this.entity.rotationPitch = this.constrainAngle(this.entity.rotationPitch, f1, this.deltaLookPitch);
//    this.entity.rotationYawHead = this.constrainAngle(this.entity.rotationYawHead, f, this.deltaLookYaw);
//
//  }
//


  @Override
  public String toString() {
    String retval = "BreathWeaponTarget(" + weaponUsed + ", " + typeOfTarget + ") ";
    if (typeOfTarget == TypeOfTarget.ENTITY) {
      return retval + ":" + entityID;
    }
    return retval + String.format(":[%.2f, %.2f, %.2f]",
            coordinates.x, coordinates.y, coordinates.z);
  }

  private BreathWeaponTarget(TypeOfTarget i_typeOfTarget, WeaponUsed i_weaponUsed) {
    typeOfTarget = i_typeOfTarget;
    weaponUsed = i_weaponUsed;
  }

  private TypeOfTarget typeOfTarget;
  private Vec3d coordinates;
  private int entityID;
  private WeaponUsed weaponUsed;

}
