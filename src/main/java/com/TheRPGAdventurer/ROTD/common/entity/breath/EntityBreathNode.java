//package com.TheRPGAdventurer.ROTD.common.entity.breath;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Random;
//
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeLegacy;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.helper.util.EntityMoveAndResizeHelper;
//import com.TheRPGAdventurer.ROTD.common.entity.helper.util.Pair;
//
//import net.minecraft.entity.Entity;
//import net.minecraft.nbt.NBTTagCompound;
//import net.minecraft.util.EnumFacing;
//import net.minecraft.util.math.AxisAlignedBB;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
///**
// * Created by TGG on 31/07/2015.
// * Server side; tracks the position, motion, and collision detection of a breath node in a breath weapon stream,
// * Used with an associated BreathNode to track age, size and initial speed
// * <p>
// * Usage:
// * 1) construct using createEntityBreathNodeServer
// * 2) call onUpdate() every tick to move and collide
// * 3) various getters for intensity, radius, and recent collisions.
// */
//class EntityBreathNode extends Entity implements IEntityParticle {
//    public static EntityBreathNode createEntityBreathNodeServer(World world, double x, double y, double z, double directionX, double directionY, double directionZ, BreathNodeP.Power power) {
//        Vec3d direction=new Vec3d(directionX, directionY, directionZ).normalize();
//
//        Random rand=new Random();
//        BreathNodeP breathNode =new BreathNodeLegacy(power, DragonBreathMode.DEFAULT);
//        Vec3d actualMotion= breathNode.getRandomisedStartingMotion(direction, rand);
//        // don't randomise the other properties (size, age) on the server.
//
//        EntityBreathNode newEntity=new EntityBreathNode(world, x, y, z, actualMotion, breathNode);
//        return newEntity;
//    }
//
//    private EntityBreathNode(World world, double x, double y, double z, Vec3d motion,
//                             BreathNodeP i_breathNode) {
//        super(world);
//        breathNode = i_breathNode;
//
//        final float ARBITRARY_START_SIZE=0.2F;
//        this.setSize(ARBITRARY_START_SIZE, ARBITRARY_START_SIZE);
//        this.setPosition(x, y, z);
//        lastTickPosX=x;
//        lastTickPosY=y;
//        lastTickPosZ=z;
//
//        motionX=motion.x;
//        motionY=motion.y;
//        motionZ=motion.z;
//        entityMoveAndResizeHelper=new EntityMoveAndResizeHelper(this);
//    }
//
//    @Override
//    public void onUpdate() {
//
//        handleWaterMovement();
//
//        float newAABBDiameter= breathNode.getCurrentAABBcollisionSize();
//
//        prevPosX=posX;
//        prevPosY=posY;
//        prevPosZ=posZ;
//        collisions=entityMoveAndResizeHelper.moveAndResizeEntity(motionX, motionY, motionZ, newAABBDiameter, newAABBDiameter);
//        intensityAtCollision=getCurrentIntensity();
//
//        if (collided && onGround) {
//            motionY-=0.01F;         // ensure that we hit the ground next time too
//        }
//        breathNode.updateAge(this);
//        if (breathNode.isDead()) {
//            setDead();
//        }
//    }
//
//    public float getCurrentRadius() {
//        return breathNode.getCurrentDiameterOfEffect() / 2.0F;
//    }
//
//    public float getCurrentIntensity() {
//        return breathNode.getCurrentIntensity();
//    }
//
//    /**
//     * Get a collection of the collisions that occurred during the last tick update
//     *
//     * @return returns a collection showing which parts of the entity collided with an object- eg
//     * (WEST, [3,2,6]-->[3.5, 2, 6] means the west face of the entity collided; the entity tried to move to
//     * x = 3, but got pushed back to x=3.5
//     */
//    public Collection<Pair<EnumFacing, AxisAlignedBB>> getRecentCollisions() {
//        if (collisions==null) collisions=new ArrayList<Pair<EnumFacing, AxisAlignedBB>>();
//
//        return collisions;
//    }
//
//    /**
//     * The intensity of the node at the time the last collision occurred
//     *
//     * @return snapshot of getCurrentIntensity at the last collision.  Meaningless if getRecentCollisions() empty.
//     */
//    public float getIntensityAtCollision() {
//        return intensityAtCollision;
//    }
//
//  @Override
//  public void setMotion(Vec3d newMotion)
//  {
//    motionX = newMotion.x;
//    motionY = newMotion.y;
//    motionZ = newMotion.z;
//  }
//
//  @Override
//  public double getMotionX() {return motionX;}
//  @Override
//  public double getMotionY() {return motionY;}
//  @Override
//  public double getMotionZ() {return motionZ;}
//  @Override
//  public double getSpeedSQ() {return motionX*motionX + motionY*motionY + motionZ*motionZ;}
//  //  public boolean isInWater() {return isInWater();}
//  @Override
//  public boolean isCollided() {return collided;}
//  @Override
//  public boolean isOnGround() {return onGround;}
//
//  private BreathNodeP breathNode;
//    private EntityMoveAndResizeHelper entityMoveAndResizeHelper;
//
//    @Override
//    protected void registerDataParameters() {}
//
//    @Override
//    protected void readEntityFromNBT(NBTTagCompound tagCompund) {}
//
//    @Override
//    protected void writeEntityToNBT(NBTTagCompound tagCompound) {}
//
//    private Collection<Pair<EnumFacing, AxisAlignedBB>> collisions;
//    private float intensityAtCollision;
//
//}
