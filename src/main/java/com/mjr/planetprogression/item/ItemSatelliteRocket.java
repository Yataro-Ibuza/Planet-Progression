package com.mjr.planetprogression.item;

import java.util.List;

import com.mjr.mjrlegendslib.util.TranslateUtilities;
import com.mjr.planetprogression.PlanetProgression;
import com.mjr.planetprogression.blocks.PlanetProgression_Blocks;
import com.mjr.planetprogression.entities.EntitySatelliteRocket;
import com.mjr.planetprogression.tileEntities.TileEntitySatelliteLandingPad;

import micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType;
import micdoodle8.mods.galacticraft.api.item.IHoldableItem;
import micdoodle8.mods.galacticraft.core.GCFluids;
import micdoodle8.mods.galacticraft.core.util.EnumColor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSatelliteRocket extends Item implements IHoldableItem {
	public ItemSatelliteRocket(String assetName) {
		super();
		this.setMaxDamage(0);
		this.setHasSubtypes(true);
		this.setMaxStackSize(1);
		this.setUnlocalizedName(assetName);
		this.setCreativeTab(PlanetProgression.tab);
	}

	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		boolean padFound = false;
		TileEntity tile = null;

		if (worldIn.isRemote) {
			return EnumActionResult.PASS;
		} else {
			float centerX = -1;
			float centerY = -1;
			float centerZ = -1;

			for (int i = -1; i < 2; i++) {
				for (int j = -1; j < 2; j++) {
					BlockPos pos1 = pos.add(i, 0, j);
					IBlockState state = worldIn.getBlockState(pos1);
					final Block id = state.getBlock();
					int meta = id.getMetaFromState(state);

					if (id == PlanetProgression_Blocks.ADVANCED_LAUCHPAD_FULL && meta == 0) {
						padFound = true;
						tile = worldIn.getTileEntity(pos.add(i, 0, j));

						centerX = pos.getX() + i + 0.5F;
						centerY = pos.getY() + 0.4F;
						centerZ = pos.getZ() + j + 0.5F;

						break;
					}
				}

				if (padFound) {
					break;
				}
			}

			if (padFound) {
				if (!placeRocketOnPad(stack, worldIn, tile, centerX, centerY, centerZ, playerIn)) {
					return EnumActionResult.FAIL;
				}

				if (!playerIn.capabilities.isCreativeMode) {
					stack.stackSize--;

					if (stack.stackSize <= 0) {
						stack = null;
					}
				}
				return EnumActionResult.SUCCESS;
			} else {
				return EnumActionResult.PASS;
			}
		}
	}

	@Override
	public void getSubItems(Item par1, CreativeTabs par2CreativeTabs, List<ItemStack> par3List) {
		for (int i = 1; i < 2; i++) {
			par3List.add(new ItemStack(par1, 1, i));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack par1ItemStack, EntityPlayer player, List<String> par2List, boolean b) {
		par2List.add(EnumColor.DARK_AQUA + "\u00a7o" + TranslateUtilities.translate("gui.satellite_pad_use.desc"));
		par2List.add(EnumColor.ORANGE + "\u00a7o" + TranslateUtilities.translate("gui.satellite_pad_extras.1.desc"));
		par2List.add(EnumColor.ORANGE + "\u00a7o" + TranslateUtilities.translate("gui.satellite_pad_extras.2.desc"));
		EnumRocketType type;

		if (par1ItemStack.getItemDamage() < 10) {
			type = EnumRocketType.values()[par1ItemStack.getItemDamage()];
		} else {
			type = EnumRocketType.values()[par1ItemStack.getItemDamage() - 10];
		}

		if (!type.getTooltip().isEmpty()) {
			par2List.add(type.getTooltip());
		}

		if (type.getPreFueled()) {
			par2List.add(EnumColor.RED + "\u00a7o" + TranslateUtilities.translate("gui.creative_only.desc"));
		}

		if (par1ItemStack.hasTagCompound() && par1ItemStack.getTagCompound().hasKey("RocketFuel")) {
			EntitySatelliteRocket rocket = new EntitySatelliteRocket(FMLClientHandler.instance().getWorldClient(), 0, 0, 0, EnumRocketType.values()[par1ItemStack.getItemDamage()]);
			par2List.add(TranslateUtilities.translate("gui.message.fuel.name") + ": " + par1ItemStack.getTagCompound().getInteger("RocketFuel") + " / " + rocket.fuelTank.getCapacity());
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack par1ItemStack) {
		return super.getUnlocalizedName(par1ItemStack) + ".rocket";
	}

	@Override
	public boolean shouldHoldLeftHandUp(EntityPlayer player) {
		return true;
	}

	@Override
	public boolean shouldHoldRightHandUp(EntityPlayer player) {
		return true;
	}

	@Override
	public boolean shouldCrouch(EntityPlayer player) {
		return true;
	}

	public static boolean placeRocketOnPad(ItemStack stack, World worldIn, TileEntity tile, float centerX, float centerY, float centerZ, EntityPlayer playerIn) {
		// Check whether there is already a rocket on the pad
		if (tile instanceof TileEntitySatelliteLandingPad) {
			if (((TileEntitySatelliteLandingPad) tile).getDockedEntity() != null) {
				return false;
			}
		} else {
			return false;
		}

		EntitySatelliteRocket rocket = new EntitySatelliteRocket(worldIn, centerX, centerY, centerZ, EnumRocketType.values()[stack.getItemDamage()], playerIn);

		rocket.rotationYaw += 45;
		rocket.setPosition(rocket.posX, rocket.posY + rocket.getOnPadYOffset(), rocket.posZ);
		worldIn.spawnEntityInWorld(rocket);

		if (rocket.getType().getPreFueled()) {
			rocket.fuelTank.fill(new FluidStack(GCFluids.fluidFuel, rocket.getMaxFuel()), true);
		} else if (stack.hasTagCompound() && stack.getTagCompound().hasKey("RocketFuel")) {
			rocket.fuelTank.fill(new FluidStack(GCFluids.fluidFuel, stack.getTagCompound().getInteger("RocketFuel")), true);
		}

		return true;
	}
}