package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.inventory.ContainerDragon;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nullable;

/**
 * Created by TGG on 20/10/2019.
 */
public class DragonInventoryHelper extends DragonHelper {
  public DragonInventoryHelper(EntityTameableDragon dragon) {
    super(dragon);
    setCompleted(FunctionTag.CONSTRUCTOR);
  }

  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
    //    DragonVariants.addVariantTagValidator(new DragonReproductionValidator());
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    nbt.setBoolean(NBT_SADDLED, isSaddled());
    nbt.setInteger(NBT_ARMOR, this.getArmor());
    nbt.setBoolean(NBT_CHESTED, this.isChested());
    writeDragonInventory(nbt);
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    this.setSaddled(nbt.getBoolean(NBT_SADDLED));
    this.setChested(nbt.getBoolean(NBT_CHESTED));
    this.setArmor(nbt.getInteger(NBT_ARMOR));
    readDragonInventory(nbt);
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    dataManager.register(ARMOR, 0);
    dataManager.register(BANNER1, ItemStack.EMPTY);
    dataManager.register(BANNER2, ItemStack.EMPTY);
    dataManager.register(BANNER3, ItemStack.EMPTY);
    dataManager.register(BANNER4, ItemStack.EMPTY);
    dataManager.register(DATA_SADDLED, false);
    dataManager.register(CHESTED, false);
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

  @Override
  public void onConfigurationChange() {
    throw new NotImplementedException("onConfigurationChange()");
  }

  @Override
  public void onDeath(DamageSource src) {
    checkPreConditions(FunctionTag.VANILLA);
    if (dragonInv != null && !this.world.isRemote && !isTamed()) {
      for (int i = 0; i < dragonInv.getSizeInventory(); ++i) {
        ItemStack itemstack = dragonInv.getStackInSlot(i);
        if (!itemstack.isEmpty()) {
          this.entityDropItem(itemstack, 0.0F);
        }
      }
    }

  }
  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
    if (hasChestVarChanged && dragonInv != null && !this.isChested()) {
      for (int i = ContainerDragon.chestStartIndex; i < 30; i++) {
        if (!dragonInv.getStackInSlot(i).isEmpty()) {
          if (!world.isRemote) {
            this.entityDropItem(dragonInv.getStackInSlot(i), 1);
          }
          dragonInv.removeStackFromSlot(i);
        }
      }
      hasChestVarChanged = false;
    }
  }

  // is the dragon big enough to have an inventory?
  public boolean isLargeEnoughForInventory() {
    return true;
  }

  // used to be called isChestedLeft
  public boolean isChested() {
    return dataManager.get(CHESTED);
  }

  public void setChested(boolean chested) {
    dataManager.set(CHESTED, chested);
    hasChestVarChanged = true;
  }

  public boolean isSaddled() {return entityDataManager.get(DATA_SADDLED);}

  /**
   * Sets the saddle to the given item
   * @param itemStack the saddle; or empty to remove saddle
   * @return true for success
   */
  public boolean setSaddleItem(ItemStack itemStack) {
    if (itemStack.isEmpty()) {
      entityDataManager.set(DATA_SADDLED, false);
      return true; //todo spawn the existing saddle as an item
    }
    if (!dragon.riding().isASaddle(itemStack)) return false;
    entityDataManager.set(DATA_SADDLED, true);
    return true;
  }

  public ItemStack getBanner1() {
    return dataManager.get(BANNER1);
  }

  public void setBanner1(ItemStack bannered) {
    dataManager.set(BANNER1, bannered);
  }

  public ItemStack getBanner2() {
    return dataManager.get(BANNER2);
  }

  public void setBanner2(ItemStack male) {
    dataManager.set(BANNER2, male);
  }

  public ItemStack getBanner3() {
    return dataManager.get(BANNER3);
  }

  public void setBanner3(ItemStack male) {
    dataManager.set(BANNER3, male);
  }

  public ItemStack getBanner4() {
    return dataManager.get(BANNER4);
  }

  public void setBanner4(ItemStack male) {
    dataManager.set(BANNER4, male);
  }

  /**
   * 1 equals iron 2 equals gold 3 equals diamond 4 equals emerald
   *
   * @return 0 no armor
   */
  public int getArmor() {
    return this.dataManager.get(ARMOR);
  }

  public void setArmor(int armorType) {
    this.dataManager.set(ARMOR, armorType);
  }

  /**
   * The player just interacted with the dragon, should we attempt to mount it, or open the inventory GUI?
   * @return
   */
  public boolean isOpenGUIkeyPressed(EntityPlayer player) {
    return player.isSneaking();
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void openGUI(EntityPlayer playerEntity, int guiId) {
    if (!this.world.isRemote && (!this.isPassenger(playerEntity))) {
      playerEntity.openGui(DragonMounts.instance, guiId, this.world, this.getEntityId(), 0, 0);
    }
  }

  public boolean replaceItemInInventory(int inventorySlot, @Nullable ItemStack itemStackIn) {
    int j = inventorySlot - 500 + 2;
    if (j >= 0 && j < this.dragonInv.getSizeInventory()) {
      this.dragonInv.setInventorySlotContents(j, itemStackIn);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void readDragonInventory(NBTTagCompound nbt) {
    if (dragonInv != null) {
      NBTTagList nbttaglist = nbt.getTagList("Items", 10);
      InitializeDragonInventory();
      for (int i = 0; i < nbttaglist.tagCount(); ++i) {
        NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
        int j = nbttagcompound.getByte("Slot") & 255;
        this.dragonInv.setInventorySlotContents(j, new ItemStack(nbttagcompound));
      }
    } else {
      NBTTagList nbttaglist = nbt.getTagList("Items", 10);
      InitializeDragonInventory();
      for (int i = 0; i < nbttaglist.tagCount(); ++i) {
        NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
        int j = nbttagcompound.getByte("Slot") & 255;
        this.InitializeDragonInventory();
        this.dragonInv.setInventorySlotContents(j, new ItemStack(nbttagcompound));

        ItemStack saddle = dragonInv.getStackInSlot(0);
        ItemStack chest = dragonInv.getStackInSlot(1);
        ItemStack banner1 = dragonInv.getStackInSlot(31);
        ItemStack banner2 = dragonInv.getStackInSlot(32);
        ItemStack banner3 = dragonInv.getStackInSlot(33);
        ItemStack banner4 = dragonInv.getStackInSlot(34);

        if (world.isRemote) {
          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 0, saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 1, chest != null && chest.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !chest.isEmpty() ? 1 : 0));

          // maybe later we reintroduce armour?
//          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 2, this.getIntFromArmor(dragonInv.getStackInSlot(2))));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner1 != null && banner1.getItem() == Items.BANNER && !banner1.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner2 != null && banner2.getItem() == Items.BANNER && !banner2.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 33, banner3 != null && banner3.getItem() == Items.BANNER && !banner3.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 34, banner4 != null && banner4.getItem() == Items.BANNER && !banner4.isEmpty() ? 1 : 0));
        }
      }
    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void refreshInventory() {
    ItemStack saddle = this.dragonInv.getStackInSlot(0);
    ItemStack leftChestforInv = this.dragonInv.getStackInSlot(1);
    ItemStack banner1 = this.dragonInv.getStackInSlot(31);
    ItemStack banner2 = this.dragonInv.getStackInSlot(32);
    ItemStack banner3 = this.dragonInv.getStackInSlot(33);
    ItemStack banner4 = this.dragonInv.getStackInSlot(34);

    this.setSaddled(saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty());
    this.setChested(leftChestforInv != null && leftChestforInv.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !leftChestforInv.isEmpty());

    this.setBanner1(banner1);
    this.setBanner2(banner2);
    this.setBanner3(banner3);
    this.setBanner4(banner4);
//    this.setArmor(getIntFromArmor(this.dragonInv.getStackInSlot(2)));

    if (this.world.isRemote) {
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 0, saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 1, leftChestforInv != null && leftChestforInv.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !leftChestforInv.isEmpty() ? 1 : 0));
//      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 2, this.getIntFromArmor(this.dragonInv.getStackInSlot(2))));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner1 != null && banner1.getItem() == Items.BANNER && !banner1.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner2 != null && banner2.getItem() == Items.BANNER && !banner2.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner3 != null && banner3.getItem() == Items.BANNER && !banner3.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner4 != null && banner4.getItem() == Items.BANNER && !banner4.isEmpty() ? 1 : 0));


    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void writeDragonInventory(NBTTagCompound nbt) {
    if (dragonInv != null) {
      NBTTagList nbttaglist = new NBTTagList();
      for (int i = 0; i < this.dragonInv.getSizeInventory(); ++i) {
        ItemStack itemstack = this.dragonInv.getStackInSlot(i);
        if (!itemstack.isEmpty()) {
          NBTTagCompound nbttagcompound = new NBTTagCompound();
          nbttagcompound.setByte("Slot", (byte) i);
          itemstack.writeToNBT(nbttagcompound);
          nbttaglist.appendTag(nbttagcompound);
        }
      }
      nbt.setTag("Items", nbttaglist);
    }
    if (this.getCustomNameTag() != null && !this.getCustomNameTag().isEmpty()) {
      nbt.setString("CustomName", this.getCustomNameTag());
    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public class DragonInventory extends ContainerHorseChest {

    public DragonInventory(String inventoryTitle, int slotCount, EntityTameableDragon dragon) {
      super(inventoryTitle, slotCount);
      this.addInventoryChangeListener(new DragonInventoryListener(dragon));
    }
  }

  public class DragonInventoryListener implements IInventoryChangedListener {

    public DragonInventoryListener(EntityTameableDragon dragon) {
      this.dragon = dragon;
    }

    @Override
    public void onInventoryChanged(IInventory invBasic) {
      refreshInventory();
    }
    EntityTameableDragon dragon;

  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  private void InitializeDragonInventory() {
    int numberOfInventoryforChest = 27;
    int numberOfPlayerArmor = 5;
    DragonInventory dragonInv = this.dragonInv;
    this.dragonInv = new DragonInventory("dragonInv", 6 + numberOfInventoryforChest + 6 + numberOfPlayerArmor, this);
    this.dragonInv.setCustomName(this.getName());
    if (dragonInv != null) {
      int i = Math.min(dragonInv.getSizeInventory(), this.dragonInv.getSizeInventory());
      for (int j = 0; j < i; ++j) {
        ItemStack itemstack = dragonInv.getStackInSlot(j);
        if (!itemstack.isEmpty()) {
          this.dragonInv.setInventorySlotContents(j, itemstack.copy());
        }
      }

      if (world.isRemote) {
        ItemStack saddle = dragonInv.getStackInSlot(0);
        ItemStack chest_left = dragonInv.getStackInSlot(1);
        ItemStack banner1 = this.dragonInv.getStackInSlot(31);
        ItemStack banner2 = this.dragonInv.getStackInSlot(32);
        ItemStack banner3 = this.dragonInv.getStackInSlot(33);
        ItemStack banner4 = this.dragonInv.getStackInSlot(34);

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 0, saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 1, chest_left != null && chest_left.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !chest_left.isEmpty() ? 1 : 0));

//        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 2, this.getIntFromArmor(dragonInv.getStackInSlot(2))));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner1 != null && banner1.getItem() == Items.BANNER && !banner1.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner2 != null && banner2.getItem() == Items.BANNER && !banner2.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 33, banner3 != null && banner3.getItem() == Items.BANNER && !banner3.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 34, banner4 != null && banner4.getItem() == Items.BANNER && !banner4.isEmpty() ? 1 : 0));

      }
    }
  }

  private static final String NBT_ARMOR = "Armor";
  private static final String NBT_SADDLED = "Saddle";
  //  private static final String NBT_SHEARED = "Sheared";
  private static final String NBT_CHESTED = "Chested";

  private boolean hasChestVarChanged = false;

  private static final DataParameter<ItemStack> BANNER1 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<ItemStack> BANNER2 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<ItemStack> BANNER3 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<ItemStack> BANNER4 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);

  private static final DataParameter<Integer> ARMOR = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
  private static final DataParameter<Boolean> DATA_SADDLED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> CHESTED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);

  private DragonInventory dragonInv;

}
