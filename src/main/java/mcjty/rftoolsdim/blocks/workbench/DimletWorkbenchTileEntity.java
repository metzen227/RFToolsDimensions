package mcjty.rftoolsdim.blocks.workbench;

import mcjty.lib.container.DefaultSidedInventory;
import mcjty.lib.container.InventoryHelper;
import mcjty.lib.entity.GenericEnergyReceiverTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.network.PacketRequestIntegerFromServer;
import mcjty.lib.varia.BlockTools;
import mcjty.rftoolsdim.RFToolsDim;
import mcjty.rftoolsdim.config.MachineConfiguration;
import mcjty.rftoolsdim.config.Settings;
import mcjty.rftoolsdim.dimensions.dimlets.DimletKey;
import mcjty.rftoolsdim.dimensions.dimlets.KnownDimletConfiguration;
import mcjty.rftoolsdim.dimensions.dimlets.types.DimletCraftingTools;
import mcjty.rftoolsdim.dimensions.dimlets.types.DimletType;
import mcjty.rftoolsdim.dimensions.dimlets.types.IDimletType;
import mcjty.rftoolsdim.items.ModItems;
import mcjty.rftoolsdim.network.RFToolsDimMessages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.util.Map;

public class DimletWorkbenchTileEntity extends GenericEnergyReceiverTileEntity implements ITickable, DefaultSidedInventory {
    public static final String CMD_STARTEXTRACT = "startExtract";
    public static final String CMD_GETEXTRACTING = "getExtracting";
    public static final String CLIENTCMD_GETEXTRACTING = "getExtracting";
    public static final String CMD_SETAUTOEXTRACT = "setAutoExtract";

    private InventoryHelper inventoryHelper = new InventoryHelper(this, DimletWorkbenchContainer.factory, DimletWorkbenchContainer.SIZE_BUFFER + 9);

    private int extracting = 0;
    private DimletKey idToExtract = null;
    private int inhibitCrafting = 0;
    private boolean autoExtract = false;

    public int getExtracting() {
        return extracting;
    }

    public boolean isAutoExtract() {return autoExtract;
    }

    public DimletWorkbenchTileEntity() {
        super(MachineConfiguration.WORKBENCH_MAXENERGY, MachineConfiguration.WORKBENCH_RECEIVEPERTICK);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return DimletWorkbenchContainer.factory.getAccessibleSlots();
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == DimletWorkbenchContainer.SLOT_OUTPUT;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return index == DimletWorkbenchContainer.SLOT_INPUT;
    }

    @Override
    public InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
        if (index < DimletWorkbenchContainer.SLOT_BASE || index > DimletWorkbenchContainer.SLOT_ESSENCE) {
            return;
        }

        checkCrafting();
    }

    private void checkCrafting() {
        if (inhibitCrafting == 0) {
            if (!checkDimletCrafting()) {
                if (inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_OUTPUT) != null) {
                    inventoryHelper.setInventorySlotContents(0, DimletWorkbenchContainer.SLOT_OUTPUT, null);
                }
            }
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            checkStateServer();;
        }
    }

    private void checkStateServer() {
        if (extracting > 0) {
            extracting--;
            if (extracting == 0) {
                if (!doExtract()) {
                    // We failed due to not enough power. Try again later.
                    extracting = 10;
                }
            }
            markDirty();
        } else if (autoExtract) {
            startExtracting();
        }
    }

    private boolean checkDimletCrafting() {
        ItemStack stackBase = inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_BASE);
        if (stackBase == null) {
            return false;
        }
        ItemStack stackController = inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_CONTROLLER);
        if (stackController == null) {
            return false;
        }
        ItemStack stackTypeController = inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_TYPE_CONTROLLER);
        if (stackTypeController == null) {
            return false;
        }
        ItemStack stackMemory = inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_MEMORY);
        if (stackMemory == null) {
            return false;
        }
        ItemStack stackEnergy = inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_ENERGY);
        if (stackEnergy == null) {
            return false;
        }
        ItemStack stackEssence = inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_ESSENCE);
        if (stackEssence == null) {
            return false;
        }

        DimletType type = DimletType.values()[stackTypeController.getItemDamage()];
        IDimletType itype = type.dimletType;
        DimletKey key = itype.attemptDimletCrafting(stackController, stackMemory, stackEnergy, stackEssence);
        if (key != null) {
            inventoryHelper.setInventorySlotContents(1, DimletWorkbenchContainer.SLOT_OUTPUT, KnownDimletConfiguration.getDimletStack(key));
            return true;
        }
        return false;
    }

    public void craftDimlet() {
        inhibitCrafting++;
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_BASE, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_CONTROLLER, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_TYPE_CONTROLLER, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_ENERGY, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_MEMORY, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_ESSENCE, 1);
        inhibitCrafting--;
        checkCrafting();
    }

    private void startExtracting() {
        if (extracting > 0) {
            // Already extracting
            return;
        }
        ItemStack stack = inventoryHelper.getStackInSlot(DimletWorkbenchContainer.SLOT_INPUT);
        if (stack != null) {
            if (ModItems.knownDimletItem.equals(stack.getItem())) {
                DimletKey key = KnownDimletConfiguration.getDimletKey(stack);
                Settings settings = KnownDimletConfiguration.getSettings(key);
                if (settings != null && settings.getDimlet()) {
                    if (!KnownDimletConfiguration.isCraftable(key)) {
                        extracting = 64;
                        idToExtract = key;
                        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_INPUT, 1);
                        markDirty();
                    }
                }
            }
        }
    }

    private boolean doExtract() {
        int rf = MachineConfiguration.workbenchRfPerDimlet;
        if (getEnergyStored(EnumFacing.DOWN) < rf) {
            // Not enough energy.
            return false;
        }
        consumeEnergy(rf);

        float factor = getInfusedFactor();

        Settings entry = KnownDimletConfiguration.getSettings(idToExtract);

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletBaseItem));
        }

        int rarity = entry.getRarity();

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletTypeControllerItem, 1, idToExtract.getType().ordinal()));
        }

        int level = DimletCraftingTools.calculateItemLevelFromRarity(rarity);
        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletMemoryUnitItem, 1, level));
        } else {
            factor += 0.1f;     // If this failed we increase our chances a bit
        }

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletEnergyModuleItem, 1, level));
        } else {
            factor += 0.1f;     // If this failed we increase our chances a bit
        }

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletControlCircuitItem, 1, rarity));
        }

        idToExtract = null;
        markDirty();

        return true;
    }

    private boolean extractSuccess(float factor) {
        return worldObj.rand.nextFloat() <= (0.61f + factor * 0.4f);
    }

    private void mergeItemOrThrowInWorld(ItemStack stack) {
        int notInserted = inventoryHelper.mergeItemStack(this, false, stack, DimletWorkbenchContainer.SLOT_BUFFER, DimletWorkbenchContainer.SLOT_BUFFER + DimletWorkbenchContainer.SIZE_BUFFER, null);
        if (notInserted > 0) {
            BlockTools.spawnItemStack(worldObj, getPos().getX(), getPos().getY(), getPos().getZ(), stack);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound, inventoryHelper);
        extracting = tagCompound.getInteger("extracting");
        idToExtract = null;
        if (tagCompound.hasKey("extKtype")) {
            DimletType type = DimletType.getTypeByOpcode(tagCompound.getString("extKtype"));
            idToExtract = new DimletKey(type, tagCompound.getString("extDkey"));
        } else {
            idToExtract = null;
        }
        autoExtract = tagCompound.getBoolean("autoExtract");
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, inventoryHelper);
        tagCompound.setInteger("extracting", extracting);
        if (idToExtract != null) {
            tagCompound.setString("extKtype", idToExtract.getType().dimletType.getOpcode());
            tagCompound.setString("extDkey", idToExtract.getId());
        }
        tagCompound.setBoolean("autoExtract", autoExtract);
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, Map<String, Argument> args) {
        boolean rc = super.execute(playerMP, command, args);
        if (rc) {
            return true;
        }
        if (CMD_STARTEXTRACT.equals(command)) {
            startExtracting();
            return true;
        } else if (CMD_SETAUTOEXTRACT.equals(command)) {
            autoExtract = args.get("auto").getBoolean();
            markDirty();
            return true;
        }
        return false;
    }

    // Request the extracting amount from the server. This has to be called on the client side.
    public void requestExtractingFromServer() {
        RFToolsDimMessages.INSTANCE.sendToServer(new PacketRequestIntegerFromServer(RFToolsDim.MODID, getPos(),
                CMD_GETEXTRACTING,
                CLIENTCMD_GETEXTRACTING));
    }

    @Override
    public Integer executeWithResultInteger(String command, Map<String, Argument> args) {
        Integer rc = super.executeWithResultInteger(command, args);
        if (rc != null) {
            return rc;
        }
        if (CMD_GETEXTRACTING.equals(command)) {
            return extracting;
        }
        return null;
    }

    @Override
    public boolean execute(String command, Integer result) {
        boolean rc = super.execute(command, result);
        if (rc) {
            return true;
        }
        if (CLIENTCMD_GETEXTRACTING.equals(command)) {
            extracting = result;
            return true;
        }
        return false;
    }

}