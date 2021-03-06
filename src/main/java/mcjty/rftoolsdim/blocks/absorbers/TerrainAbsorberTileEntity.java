package mcjty.rftoolsdim.blocks.absorbers;

import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.rftoolsdim.config.DimletConstructionConfiguration;
import mcjty.rftoolsdim.dimensions.DimensionInformation;
import mcjty.rftoolsdim.dimensions.RfToolsDimensionManager;
import mcjty.rftoolsdim.dimensions.types.TerrainType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;

import java.util.Random;

public class TerrainAbsorberTileEntity extends GenericTileEntity implements ITickable {

    private int absorbing = 0;
    private String terrainName = null;

    @Override
    public void update() {
        if (getWorld().isRemote) {
            checkStateClient();
        } else {
            checkStateServer();
        }
    }


    protected void checkStateClient() {
        if (absorbing > 0) {
            Random rand = getWorld().rand;

            double u = rand.nextFloat() * 2.0f - 1.0f;
            double v = (float) (rand.nextFloat() * 2.0f * Math.PI);
            double x = Math.sqrt(1 - u * u) * Math.cos(v);
            double y = Math.sqrt(1 - u * u) * Math.sin(v);
            double z = u;
            double r = 1.0f;

            getWorld().spawnParticle(EnumParticleTypes.PORTAL, getPos().getX() + 0.5f + x * r, getPos().getY() + 0.5f + y * r, getPos().getZ() + 0.5f + z * r, -x, -y, -z);
        }
    }

    protected void checkStateServer() {
        if (absorbing > 0) {
            int dim = getWorld().provider.getDimension();
            String terrain = getCurrentTerrain(dim);
            if (!terrain.equals(terrainName)) {
                return;
            }

            absorbing--;
            markDirtyClient();
        }
    }

    private String getCurrentTerrain(int dim) {
        String terrain;
        if (dim == 0) {
            terrain = TerrainType.TERRAIN_NORMAL.getId();
        } else if (dim == -1) {
            terrain = TerrainType.TERRAIN_CAVERN.getId();
        } else if (dim == 1) {
            terrain = TerrainType.TERRAIN_ISLAND.getId();
        } else {
            DimensionInformation dimensionInformation = RfToolsDimensionManager.getDimensionManager(getWorld()).getDimensionInformation(dim);
            if (dimensionInformation != null) {
                terrain = dimensionInformation.getTerrainType().getId();
            } else {
                terrain = TerrainType.TERRAIN_NORMAL.getId();
            }
        }
        return terrain;
    }

    public int getAbsorbing() {
        return absorbing;
    }

    public String getTerrainName() {
        return terrainName;
    }

    public void placeDown() {
        if (terrainName == null) {
            int dim = getWorld().provider.getDimension();
            String terrain = getCurrentTerrain(dim);
            if (terrain == null) {
                terrainName = null;
                absorbing = 0;
            } else if (!terrain.equals(terrainName)) {
                terrainName = terrain;
                absorbing = DimletConstructionConfiguration.maxTerrainAbsorbtion;
            }
            markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        return tagCompound;
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        tagCompound.setInteger("absorbing", absorbing);
        if (terrainName != null) {
            tagCompound.setString("terrain", terrainName);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        absorbing = tagCompound.getInteger("absorbing");
        if (tagCompound.hasKey("terrain")) {
            terrainName = tagCompound.getString("terrain");
        } else {
            terrainName = null;
        }
    }


}

