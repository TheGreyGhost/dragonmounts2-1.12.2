package com.TheRPGAdventurer.ROTD.common.entity.breath;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeFactory;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.EntityBreathNodeP;
import com.TheRPGAdventurer.ROTD.common.entity.breath.weapons.BreathWeapon;
import com.TheRPGAdventurer.ROTD.common.entity.breath.weapons.BreathWeaponP;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by TGG on 30/07/2015.
 * BreathAffectedArea base class
 * Represents the area of the world (blocks, entities) affected by the breathweapon.
 * Usage:
 * (1) Construct from a BreathWeapon
 * (2) continueBreathing() once per tick whenever the dragon is breathing
 * (3) updateTick() every tick to update the area of effect, and implement breathweapon effects on the blocks &
 * entities within the area of effect
 */
public class BreathAffectedArea {

  public BreathWeapon breathWeapon;
  @Deprecated
  public BreathAffectedArea(BreathWeapon i_breathWeapon) {
    breathWeapon = i_breathWeapon;
  }
  public BreathAffectedArea(BreathWeaponP i_breathWeapon) {
    breathWeaponP = i_breathWeapon;
  }

  /**
   * Tell BreathAffectedArea that breathing is ongoing.  Call once per tick before updateTick()
   *
   * @param world
   * @param origin      the origin of the beam
   * @param destination the destination of the beam, used to calculate direction
   * @param power
   */
  @Deprecated
  public void continueBreathingLegacy(World world, Vec3d origin, Vec3d destination, BreathNodeP.Power power, EntityTameableDragon dragon) {
    Vec3d direction = destination.subtract(origin).normalize();

    EntityBreathNodeP newNode = EntityBreathNodeP.createEntityBreathNodeServerLegacy(world, origin.x, origin.y, origin.z,
            direction.x, direction.y, direction.z, power);

    entityBreathNodes.add(newNode);
  }

  /**
   * Tell BreathAffectedArea that breathing is ongoing.  Call once per tick before updateTick()
   *
   * @param world
   * @param origin      the origin of the beam
   * @param destination the destination of the beam, used to calculate direction
   * @param power
   */
  public void continueBreathing(World world, Vec3d origin, Vec3d destination,
                                BreathNodeFactory breathNodeFactory, BreathNodeP.Power power, DragonBreathMode breathMode) {
    Vec3d direction = destination.subtract(origin).normalize();

    EntityBreathNodeP newNode = EntityBreathNodeP.createEntityBreathNodeServer(
            world, origin.x, origin.y, origin.z, direction.x, direction.y, direction.z,
            breathNodeFactory, power, breathMode);

    entityBreathNodes.add(newNode);
  }

  /**
   * updates the BreathAffectedArea, called once per tick
   */
  @Deprecated
  public void updateTickLegacy(World world) {
    ArrayList<NodeLineSegment> segments = new ArrayList<>();

    // create a list of NodeLineSegments from the motion path of the BreathNodes
    Iterator<EntityBreathNodeP> it = entityBreathNodes.iterator();
    while (it.hasNext()) {
      EntityBreathNodeP entity = it.next();
      if (entity.isDead) {
        it.remove();
      } else {
        float radius = entity.getCurrentRadius();
        Vec3d initialPosition = entity.getPositionVector();
        entity.onUpdate();
        Collection<Pair<EnumFacing, AxisAlignedBB>> recentCollisions = entity.getRecentCollisions();
        Vec3d finalPosition = entity.getPositionVector();
        segments.add(new NodeLineSegment(initialPosition, finalPosition, radius, recentCollisions));
      }
    }

    updateBlockAndEntityHitDensities(world, segments, entityBreathNodes, blocksAffectedByBeam, entitiesAffectedByBeam);
    implementEffectsOnBlocksTickLegacy(world, blocksAffectedByBeam);
    implementEffectsOnEntitiesTickLegacy(world, entitiesAffectedByBeam);
    decayBlockAndEntityHitDensities(blocksAffectedByBeam, entitiesAffectedByBeam);
  }

  /**
   * updates the BreathAffectedArea, called once per tick
   */
  public void updateTick(World world, DragonBreathMode new_dragonBreathMode) {
    if (!new_dragonBreathMode.equals(dragonBreathMode)) {
      dragonBreathMode = new_dragonBreathMode;
      if (breathWeaponP.shouldResetOnBreathModeChange(dragonBreathMode)) {
        entityBreathNodes.clear();
        blocksAffectedByBeam.clear();
        entitiesAffectedByBeam.clear();
      }
    }

    ArrayList<NodeLineSegment> segments = new ArrayList<>();

    // create a list of NodeLineSegments from the motion path of the BreathNodes
    Iterator<EntityBreathNodeP> it = entityBreathNodes.iterator();
    while (it.hasNext()) {
      EntityBreathNodeP entity = it.next();
      if (entity.isDead) {
        it.remove();
      } else {
        float radius = entity.getCurrentRadius();
        Vec3d initialPosition = entity.getPositionVector();
        entity.updateBreathMode(dragonBreathMode);
        entity.onUpdate();
        Collection<Pair<EnumFacing, AxisAlignedBB>> recentCollisions = entity.getRecentCollisions();
        Vec3d finalPosition = entity.getPositionVector();
        segments.add(new NodeLineSegment(initialPosition, finalPosition, radius, recentCollisions));
      }
    }

    updateBlockAndEntityHitDensities(world, segments, entityBreathNodes, blocksAffectedByBeam, entitiesAffectedByBeam);

    implementEffectsOnBlocksTick(world, blocksAffectedByBeam);
    implementEffectsOnEntitiesTick(world, entitiesAffectedByBeam);

    decayBlockAndEntityHitDensities(blocksAffectedByBeam, entitiesAffectedByBeam);
  }

  private void implementEffectsOnBlocksTick(World world, HashMap<Vec3i, BreathAffectedBlock> affectedBlocks) {
    if (!DragonMounts.instance.getConfig().doBreathweaponsAffectBlocks()) {
      return;
    }
    for (Map.Entry<Vec3i, BreathAffectedBlock> blockInfo : affectedBlocks.entrySet()) {
      BreathAffectedBlock newHitDensity = breathWeaponP.affectBlock(world, blockInfo.getKey(), blockInfo.getValue());
      blockInfo.setValue(newHitDensity);
    }
  }

  private void implementEffectsOnEntitiesTick(World world, HashMap<Integer, BreathAffectedEntity> affectedEntities) {
    Iterator<Map.Entry<Integer, BreathAffectedEntity>> itAffectedEntities = affectedEntities.entrySet().iterator();
    while (itAffectedEntities.hasNext()) {
      Map.Entry<Integer, BreathAffectedEntity> affectedEntity = itAffectedEntities.next();
      BreathAffectedEntity newHitDensity = breathWeaponP.affectEntity(world, affectedEntity.getKey(), affectedEntity.getValue());
      if (newHitDensity == null) {
        itAffectedEntities.remove();
      } else {
        affectedEntity.setValue(newHitDensity);
      }
    }
  }

  private void implementEffectsOnBlocksTickLegacy(World world, HashMap<Vec3i, BreathAffectedBlock> affectedBlocks) {
    if (!DragonMounts.instance.getConfig().doBreathweaponsAffectBlocks()) {
      return;
    }
    for (Map.Entry<Vec3i, BreathAffectedBlock> blockInfo : affectedBlocks.entrySet()) {
      BreathAffectedBlock newHitDensity = breathWeapon.affectBlock(world, blockInfo.getKey(), blockInfo.getValue());
      blockInfo.setValue(newHitDensity);
    }
  }

  private void implementEffectsOnEntitiesTickLegacy(World world, HashMap<Integer, BreathAffectedEntity> affectedEntities) {
    Iterator<Map.Entry<Integer, BreathAffectedEntity>> itAffectedEntities = affectedEntities.entrySet().iterator();
    while (itAffectedEntities.hasNext()) {
      Map.Entry<Integer, BreathAffectedEntity> affectedEntity = itAffectedEntities.next();
      BreathAffectedEntity newHitDensity = breathWeapon.affectEntity(world, affectedEntity.getKey(), affectedEntity.getValue());
      if (newHitDensity == null) {
        itAffectedEntities.remove();
      } else {
        affectedEntity.setValue(newHitDensity);
      }
    }
  }

  /**
   * decay the hit densities of the affected blocks and entities (eg for flame weapon - cools down)
   */
  private void decayBlockAndEntityHitDensities(HashMap<Vec3i, BreathAffectedBlock> affectedBlocks,
                                               HashMap<Integer, BreathAffectedEntity> affectedEntities) {
    Iterator<Map.Entry<Vec3i, BreathAffectedBlock>> itAffectedBlocks = affectedBlocks.entrySet().iterator();
    while (itAffectedBlocks.hasNext()) {
      Map.Entry<Vec3i, BreathAffectedBlock> affectedBlock = itAffectedBlocks.next();
      BreathAffectedBlock carryover = affectedBlock.getValue();
      carryover.decayBlockEffectTick();
      if (carryover.isUnaffected()) {
        itAffectedBlocks.remove();
      }
    }

    Iterator<Map.Entry<Integer, BreathAffectedEntity>> itAffectedEntities = affectedEntities.entrySet().iterator();
    while (itAffectedEntities.hasNext()) {
      Map.Entry<Integer, BreathAffectedEntity> affectedEntity = itAffectedEntities.next();
      BreathAffectedEntity carryover = affectedEntity.getValue();
      carryover.decayEntityEffectTick();
      if (carryover.isUnaffected()) {
        itAffectedEntities.remove();
      }
    }
  }

  /**
   * Models the collision of the breath nodes on the world blocks and entities:
   * Each breathnode which contacts a world block will increase the corresponding 'hit density' by an amount proportional
   * to the intensity of the node and the degree of overlap between the node and the block.
   * Likewise for the entities contacted by the breathnode
   *
   * @param world
   * @param nodeLineSegments  the nodeLineSegments in the breath weapon beam
   * @param entityBreathNodes the breathnodes in the breath weapon beam  - parallel to nodeLineSegments, must correspond 1:1
   * @param affectedBlocks    each block touched by the beam has an entry in this map.  The hitDensity (float) is increased
   *                          every time a node touches it.  blocks without an entry haven't been touched.
   * @param affectedEntities  every entity touched by the beam has an entry in this map (entityID).  The hitDensity (float)
   *                          for an entity is increased every time a node touches it.  entities without an entry haven't
   *                          been touched.
   */
  private void updateBlockAndEntityHitDensities(World world,
                                                ArrayList<NodeLineSegment> nodeLineSegments,
                                                ArrayList<EntityBreathNodeP> entityBreathNodes,
                                                HashMap<Vec3i, BreathAffectedBlock> affectedBlocks,
                                                HashMap<Integer, BreathAffectedEntity> affectedEntities) {
    checkNotNull(nodeLineSegments);
    checkNotNull(entityBreathNodes);
    checkNotNull(affectedBlocks);
    checkNotNull(affectedEntities);
    if (nodeLineSegments.size() != entityBreathNodes.size()) { //todo debug
      int i = 1;
    }
    checkArgument(nodeLineSegments.size() == entityBreathNodes.size());

    if (entityBreathNodes.isEmpty()) return;

    final int NUMBER_OF_CLOUD_POINTS = 10;
    for (int i = 0; i < nodeLineSegments.size(); ++i) {
      float intensity = entityBreathNodes.get(i).getIntensityAtCollision();
      nodeLineSegments.get(i).addStochasticCloud(affectedBlocks, intensity, NUMBER_OF_CLOUD_POINTS);
      nodeLineSegments.get(i).addBlockCollisions(affectedBlocks, intensity);
    }

    AxisAlignedBB allAABB = NodeLineSegment.getAxisAlignedBoundingBoxForAll(nodeLineSegments);
    List<EntityLivingBase> allEntities = world.getEntitiesWithinAABB(EntityLivingBase.class, allAABB);

    Multimap<Vec3i, Integer> occupiedByEntities = ArrayListMultimap.create();
    Map<Integer, AxisAlignedBB> entityHitBoxes = new HashMap<Integer, AxisAlignedBB>();
    for (EntityLivingBase entityLivingBase : allEntities) {
      AxisAlignedBB aabb = entityLivingBase.getEntityBoundingBox();
      entityHitBoxes.put(entityLivingBase.getEntityId(), aabb);
      for (int x = (int) aabb.minX; x <= (int) aabb.maxX; ++x) {
        for (int y = (int) aabb.minY; y <= (int) aabb.maxY; ++y) {
          for (int z = (int) aabb.minZ; z <= (int) aabb.maxZ; ++z) {
            Vec3i pos = new Vec3i(x, y, z);
            occupiedByEntities.put(pos, entityLivingBase.getEntityId());
          }
        }
      }
      //      }
    }

    final int NUMBER_OF_ENTITY_CLOUD_POINTS = 10;
    for (int i = 0; i < nodeLineSegments.size(); ++i) {
      Set<Integer> checkedEntities = new HashSet<Integer>();
      AxisAlignedBB aabb = nodeLineSegments.get(i).getAxisAlignedBoundingBox();
      for (int x = (int) aabb.minX; x <= (int) aabb.maxX; ++x) {
        for (int y = (int) aabb.minY; y <= (int) aabb.maxY; ++y) {
          for (int z = (int) aabb.minZ; z <= (int) aabb.maxZ; ++z) {
            Vec3i pos = new Vec3i(x, y, z);
            Collection<Integer> entitiesHere = occupiedByEntities.get(pos);

            if (entitiesHere != null) {
              for (Integer entityID : entitiesHere) {
                if (!checkedEntities.contains(entityID)) {
                  checkedEntities.add(entityID);
                  float intensity = entityBreathNodes.get(i).getCurrentIntensity();
                  Entity entityToCheck = world.getEntityByID(entityID);

                  if (entityToCheck != null) {
                    AxisAlignedBB entityAABB = entityToCheck.getEntityBoundingBox();
                    float hitDensity = nodeLineSegments.get(i).collisionCheckAABB(entityAABB, intensity, NUMBER_OF_ENTITY_CLOUD_POINTS);

                    if (hitDensity > 0.0) {
                      BreathAffectedEntity currentDensity = affectedEntities.get(entityID);
                      if (currentDensity == null) {
                        currentDensity = new BreathAffectedEntity();
                      }
                      currentDensity.addHitDensity(nodeLineSegments.get(i).getSegmentDirection(), hitDensity);
                      affectedEntities.put(entityID, currentDensity);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  //  private ArrayList<EntityBreathNodeP> entityBreathNodes = new ArrayList<>();
  private HashMap<Vec3i, BreathAffectedBlock> blocksAffectedByBeam = new HashMap<Vec3i, BreathAffectedBlock>();
  private HashMap<Integer, BreathAffectedEntity> entitiesAffectedByBeam = new HashMap<Integer, BreathAffectedEntity>();
  private ArrayList<EntityBreathNodeP> entityBreathNodes = new ArrayList<>();
  private BreathWeaponP breathWeaponP;
  private DragonBreathMode dragonBreathMode;


}