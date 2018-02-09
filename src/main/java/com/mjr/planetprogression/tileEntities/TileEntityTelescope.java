package com.mjr.planetprogression.tileEntities;

import java.util.EnumSet;

import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Moon;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.core.energy.item.ItemElectricBase;
import micdoodle8.mods.galacticraft.core.energy.tile.TileBaseElectricBlock;
import micdoodle8.mods.galacticraft.core.inventory.IInventoryDefaults;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.miccore.Annotations.NetworkedField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;

import com.mjr.mjrlegendslib.util.PlayerUtilties;
import com.mjr.planetprogression.Config;
import com.mjr.planetprogression.handlers.capabilities.CapabilityStatsHandler;
import com.mjr.planetprogression.handlers.capabilities.IStatsCapability;
import com.mjr.planetprogression.item.ResearchPaper;

public class TileEntityTelescope extends TileBaseElectricBlock implements IInventoryDefaults, ISidedInventory {

	public static final int PROCESS_TIME_REQUIRED_BASE = 200;
	@NetworkedField(targetSide = Side.CLIENT)
	public int processTimeRequired = PROCESS_TIME_REQUIRED_BASE;
	@NetworkedField(targetSide = Side.CLIENT)
	public int processTicks = 0;
	@NetworkedField(targetSide = Side.CLIENT)
	public String owner = "";
	
	@NetworkedField(targetSide = Side.CLIENT)
	public float currentRotation;

	private ItemStack[] containingItems = new ItemStack[2];

	public TileEntityTelescope() {
		super();
		this.storage.setMaxExtract(300);
	}

	@Override
	public void update() {
		if (!this.worldObj.isRemote) {
			if (this.hasEnoughEnergyToRun) {
				if (this.canResearch()) {
					++this.processTicks;

					this.processTimeRequired = TileEntityTelescope.PROCESS_TIME_REQUIRED_BASE * 2 / (1 + this.poweredByTierGC);

					if (this.processTicks >= this.processTimeRequired) {
						this.worldObj.playSound(null, this.getPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.3F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
						this.processTicks = 0;
						this.doResearch();
					}
				} else {
					this.processTicks = 0;
				}
			} else {
				this.processTicks = 0;
			}
		}
		super.update();
	}

	private void doResearch() {
		IStatsCapability stats = null;

		EntityPlayerMP player = PlayerUtilties.getPlayerFromUUID(this.owner);
		if (player != null) {
			stats = player.getCapability(CapabilityStatsHandler.PP_STATS_CAPABILITY, null);
		}

		if (Config.researchMode == 0 || Config.researchMode == 1 || Config.researchMode == 2 || Config.researchMode == 3) {
			boolean found = false;
			for (Planet planet : GalaxyRegistry.getRegisteredPlanets().values()) {
				if (((ResearchPaper) this.containingItems[1].getItem()).getPlanet().equalsIgnoreCase(planet.getLocalizedName())) {
					if (!stats.getUnlockedPlanets().contains(planet)) {
						stats.addUnlockedPlanets(planet);
						player.addChatMessage(new TextComponentString("Research Completed! You have unlocked " + planet.getLocalizedName()));
						this.containingItems[1] = null;
						found = true;
						break;
					}
				}
			}
			if (found == false) {
				for (Moon moon : GalaxyRegistry.getRegisteredMoons().values()) {
					if (((ResearchPaper) this.containingItems[1].getItem()).getPlanet().equalsIgnoreCase(moon.getLocalizedName())) {
						if (!stats.getUnlockedPlanets().contains(moon)) {
							stats.addUnlockedPlanets(moon);
							player.addChatMessage(new TextComponentString("Research Completed! You have discovered " + moon.getLocalizedName()));
							this.containingItems[1] = null;
							break;
						}
					}
				}
			}
		}
	}

	private boolean canResearch() {
		IStatsCapability stats = null;

		EntityPlayerMP player = PlayerUtilties.getPlayerFromUUID(this.owner);

		if (this.containingItems[1] != null && this.containingItems[1].getItem() instanceof ResearchPaper) {
			if (player != null) {
				stats = player.getCapability(CapabilityStatsHandler.PP_STATS_CAPABILITY, null);
			}

			if (stats != null)
				if (stats.getUnlockedPlanets().size() != GalaxyRegistry.getRegisteredPlanets().size())
					return true;
		}

		// stats.setUnlockedPlanets(new ArrayList<Planet>()); // DEBUG Tool

		return false;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList var2 = nbt.getTagList("Items", 10);
		this.containingItems = new ItemStack[this.getSizeInventory()];

		for (int var3 = 0; var3 < var2.tagCount(); ++var3) {
			NBTTagCompound var4 = var2.getCompoundTagAt(var3);
			int var5 = var4.getByte("Slot") & 255;

			if (var5 < this.containingItems.length) {
				this.containingItems[var5] = ItemStack.loadItemStackFromNBT(var4);
			}
		}
		this.setOwner(nbt.getString("Owner"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		NBTTagList var2 = new NBTTagList();

		for (int var3 = 0; var3 < this.containingItems.length; ++var3) {
			if (this.containingItems[var3] != null) {
				NBTTagCompound var4 = new NBTTagCompound();
				var4.setByte("Slot", (byte) var3);
				this.containingItems[var3].writeToNBT(var4);
				var2.appendTag(var4);
			}
		}

		nbt.setTag("Items", var2);
		nbt.setString("Owner", this.getOwner());

		return nbt;
	}

	@Override
	public String getName() {
		return GCCoreUtil.translate("container.telescope.name");
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		return new int[] { 0 };
	}

	@Override
	public boolean hasCustomName() {
		return true;
	}

	@Override
	public ITextComponent getDisplayName() {
		return (this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName(), new Object[0]));
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer) {
		return this.worldObj.getTileEntity(getPos()) == this && par1EntityPlayer.getDistanceSq(this.getPos().getX() + 0.5D, this.getPos().getY() + 0.5D, this.getPos().getZ() + 0.5D) <= 64.0D;
	}

	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemStack) {
		return slotID == 0 && ItemElectricBase.isElectricItem(itemStack.getItem());
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
		return index == 0;
	}

	@Override
	public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
		return this.isItemValidForSlot(index, itemStackIn);
	}

	@Override
	public EnumSet<EnumFacing> getElectricalOutputDirections() {
		return EnumSet.noneOf(EnumFacing.class);
	}

	@Override
	public boolean shouldUseEnergy() {
		return !this.getDisabled(0);
	}

	@Override
	public EnumFacing getElectricInputDirection() {
		return EnumFacing.getFront((this.getBlockMetadata() & 3) + 2);
	}

	@Override
	public ItemStack getBatteryInSlot() {
		return this.getStackInSlot(0);
	}

	@Override
	public void setDisabled(int index, boolean disabled) {
		if (this.disableCooldown == 0) {
			switch (index) {
			case 0:
				this.disabled = disabled;
				this.disableCooldown = 10;
				break;
			default:
				break;
			}
		}
	}

	@Override
	public boolean getDisabled(int index) {
		switch (index) {
		case 0:
			return this.disabled;
		default:
			break;
		}

		return true;
	}

	@Override
	public int getSizeInventory() {
		return this.containingItems.length;
	}

	@Override
	public ItemStack getStackInSlot(int par1) {
		return this.containingItems[par1];
	}

	@Override
	public ItemStack decrStackSize(int par1, int par2) {
		if (this.containingItems[par1] != null) {
			ItemStack var3;

			if (this.containingItems[par1].stackSize <= par2) {
				var3 = this.containingItems[par1];
				this.containingItems[par1] = null;
				return var3;
			} else {
				var3 = this.containingItems[par1].splitStack(par2);

				if (this.containingItems[par1].stackSize == 0) {
					this.containingItems[par1] = null;
				}

				return var3;
			}
		} else {
			return null;
		}
	}

	@Override
	public ItemStack removeStackFromSlot(int par1) {
		if (this.containingItems[par1] != null) {
			ItemStack var2 = this.containingItems[par1];
			this.containingItems[par1] = null;
			return var2;
		} else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int par1, ItemStack par2ItemStack) {
		this.containingItems[par1] = par2ItemStack;

		if (par2ItemStack != null && par2ItemStack.stackSize > this.getInventoryStackLimit()) {
			par2ItemStack.stackSize = this.getInventoryStackLimit();
		}
	}

	@Override
	public EnumFacing getFront() {
		return EnumFacing.NORTH;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getOwner() {
		return this.owner;
	}
}