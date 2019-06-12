package com.TheRPGAdventurer.ROTD.objects.items.gemset.armorset;

import com.TheRPGAdventurer.ROTD.inits.ModArmour;
import com.TheRPGAdventurer.ROTD.objects.items.EnumItemBreedTypes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class DragonArmourForest extends DragonArmourBase {

	private int effectCooldown;
	
	public DragonArmourForest(ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn, String unlocalizedName) {
		super(materialIn, renderIndexIn, equipmentSlotIn, unlocalizedName, EnumItemBreedTypes.FOREST);
	}
	
	@Override
	public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
		if (player.getCooldownTracker().getCooldown(this, 0) > 0) return;
		super.onArmorTick(world, player, itemStack);
		if (!(head == ModArmour.forestDragonScaleCap && chest == ModArmour.forestDragonScaleTunic && legs == ModArmour.forestDragonScaleLeggings && feet == ModArmour.forestDragonScaleBoots))
			return;
		player.addPotionEffect(new PotionEffect(MobEffects.LUCK, 210, 0, false, false));
		if (!(effectCooldown <= 0) && !(player.getHealth() < 10f)) return; // check this after because luck is a perma effect
		
		player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 200, 1, false, true));
		player.getCooldownTracker().setCooldown(this, 3260); //Relatively high because this is op af
	}
	
}
