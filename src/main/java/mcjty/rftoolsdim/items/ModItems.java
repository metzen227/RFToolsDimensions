package mcjty.rftoolsdim.items;

import mcjty.rftoolsdim.items.parts.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ModItems {

    public static DimletBaseItem dimletBaseItem;
    public static DimletControlCircuitItem dimletControlCircuitItem;
    public static DimletEnergyModuleItem dimletEnergyModuleItem;
    public static DimletMemoryUnitItem dimletMemoryUnitItem;
    public static DimletTypeControllerItem dimletTypeControllerItem;
    public static StructureEssenceItem structureEssenceItem;
    public static DimensionalShardItem dimensionalShardItem;
    public static KnownDimlet knownDimletItem;
    public static EmptyDimensionTab emptyDimensionTabItem;
    public static RealizedDimensionTab realizedDimensionTabItem;
    public static DimletTemplate dimletTemplateItem;

    public static void init() {
        dimletBaseItem = new DimletBaseItem();
        dimletControlCircuitItem = new DimletControlCircuitItem();
        dimletEnergyModuleItem = new DimletEnergyModuleItem();
        dimletMemoryUnitItem = new DimletMemoryUnitItem();
        dimletTypeControllerItem = new DimletTypeControllerItem();
        structureEssenceItem = new StructureEssenceItem();
        dimensionalShardItem = new DimensionalShardItem();
        knownDimletItem = new KnownDimlet();
        emptyDimensionTabItem = new EmptyDimensionTab();
        realizedDimensionTabItem = new RealizedDimensionTab();
        dimletTemplateItem = new DimletTemplate();
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        dimletBaseItem.initModel();
        dimletControlCircuitItem.initModel();
        dimletEnergyModuleItem.initModel();
        dimletMemoryUnitItem.initModel();
        dimletTypeControllerItem.initModel();
        structureEssenceItem.initModel();
        dimensionalShardItem.initModel();
        knownDimletItem.initModel();
        emptyDimensionTabItem.initModel();
        realizedDimensionTabItem.initModel();
        dimletTemplateItem.initModel();
    }
}