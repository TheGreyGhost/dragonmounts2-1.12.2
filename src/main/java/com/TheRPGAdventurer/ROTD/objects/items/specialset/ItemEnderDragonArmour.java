package com.TheRPGAdventurer.ROTD.objects.items.specialset;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.objects.items.EnumItemBreedTypes;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import com.TheRPGAdventurer.ROTD.util.IHasModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class ItemEnderDragonArmour extends ItemArmor implements IHasModel {

    private final PotionEffect potionEffect;
    public EnumItemBreedTypes type;

    public ItemEnderDragonArmour(ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn, String unlocalizedName, PotionEffect potionEffect2, EnumItemBreedTypes type) {
        super(materialIn, renderIndexIn, equipmentSlotIn);
        this.potionEffect=potionEffect2;
        this.setUnlocalizedName("dragonscale_" + equipmentSlotIn.toString().toLowerCase());
        this.setRegistryName(new ResourceLocation(DragonMounts.MODID, unlocalizedName));
        this.setCreativeTab(DragonMounts.armoryTab);
        this.type=type;

        setNameColor();
    }

    @SideOnly(Side.CLIENT)
    private void setNameColor() {
        new ItemStack(this).setStackDisplayName(type.color + new ItemStack(this).getDisplayName());
    }

    @Override
    public void onArmorTick(final World world, final EntityPlayer player, final ItemStack itemStack) {
        //		if (!player.isPotionActive(potionEffect.getPotion())) { // If the Potion isn't currently active,
        //			player.addPotionEffect(new PotionEffect(potionEffect)); // Apply a copy of the PotionEffect to the player
        //		}

        spawnParticles(player);
    }

    public void spawnParticles(EntityPlayer player) {
        // spawn generic particles
        Random rand=new Random();
        double px=player.posX + (rand.nextDouble() - 0.3);
        double py=player.posY + (rand.nextDouble() - 0.3);
        double pz=player.posZ + (rand.nextDouble() - 0.3);
        double ox=(rand.nextDouble() - 0.3) * 2;
        double oy=(rand.nextDouble() - 0.3) * 2;
        double oz=(rand.nextDouble() - 0.3) * 2;
        player.world.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, ox, oy, oz);

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(type.color + DMUtils.translateToLocal("dragon." + type.toString().toLowerCase()));
    }

    @Override
    public void RegisterModels() {
        DragonMounts.proxy.registerItemRenderer(this, 0, "inventory");
    }

}