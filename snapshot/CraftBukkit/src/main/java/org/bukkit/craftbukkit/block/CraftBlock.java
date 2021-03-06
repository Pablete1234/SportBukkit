package org.bukkit.craftbukkit.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.server.*;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.geometry.Vec3;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import org.bukkit.util.RayBlockIntersection;
import org.bukkit.util.Vector;

public class CraftBlock implements Block {
    private final UUID worldId;
    private final BlockPosition position;

    public CraftBlock(CraftChunk chunk, int x, int y, int z) {
        this(chunk, BlockPosition.of(x, y, z));
    }

    public CraftBlock(CraftChunk chunk, Vec3 position) {
        this.worldId = chunk.getWorldId();
        this.position = BlockPosition.copyOf(position);
    }

    private net.minecraft.server.Block getNMSBlock() {
        return CraftMagicNumbers.getBlock(this); // TODO: UPDATE THIS
    }

    private static net.minecraft.server.Block getNMSBlock(int type) {
        return CraftMagicNumbers.getBlock(type);
    }

    @Override
    public UUID getWorldId() {
        return worldId;
    }

    @Override
    public CraftWorld getWorld() {
        return (CraftWorld) Bukkit.world(getWorldId());
    }

    @Override
    public BlockPosition getPosition() {
        return position;
    }

    public Location getLocation() {
        return new Location(getWorldId(), position);
    }

    public Location getLocation(Location loc) {
        if (loc != null) {
            loc.setWorldId(getWorldId());
            loc.setPosition(position);
            loc.setYaw(0);
            loc.setPitch(0);
        }

        return loc;
    }

    public BlockVector getVector() {
        return new BlockVector(position);
    }

    public int getX() {
        return position.coarseX();
    }

    public int getY() {
        return position.coarseY();
    }

    public int getZ() {
        return position.coarseZ();
    }

    public Chunk getChunk() {
        return getWorld().getChunkAt(this);
    }

    public void setData(final byte data) {
        setData(data, 3);
    }

    public void setData(final byte data, boolean applyPhysics) {
        if (applyPhysics) {
            setData(data, 3);
        } else {
            setData(data, 2);
        }
    }

    private void setData(final byte data, int flag) {
        net.minecraft.server.World world = nmsWorld();
        IBlockData blockData = world.getType(position);
        world.setTypeAndData(position, blockData.getBlock().fromLegacyData(data), flag);
    }

    private IBlockData getData0() {
        return nmsWorld().getType(position);
    }

    public byte getData() {
        IBlockData blockData = nmsWorld().getType(position);
        return (byte) blockData.getBlock().toLegacyData(blockData);
    }

    public void setType(final Material type) {
        setType(type, true);
    }

    @Override
    public void setType(Material type, boolean applyPhysics) {
        setTypeId(type.getId(), applyPhysics);
    }

    public boolean setTypeId(final int type) {
        return setTypeId(type, true);
    }

    public boolean setTypeId(final int type, final boolean applyPhysics) {
        net.minecraft.server.Block block = getNMSBlock(type);
        return setTypeIdAndData(type, (byte) block.toLegacyData(block.getBlockData()), applyPhysics);
    }

    public boolean setTypeIdAndData(final int type, final byte data, final boolean applyPhysics) {
        IBlockData blockData = getNMSBlock(type).fromLegacyData(data);

        // SPIGOT-611: need to do this to prevent glitchiness. Easier to handle this here (like /setblock) than to fix weirdness in tile entity cleanup
        if (type != 0 && blockData.getBlock() instanceof BlockTileEntity && type != getTypeId()) {
            nmsWorld().setTypeAndData(position, Blocks.AIR.getBlockData(), 0);
        }

        if (applyPhysics) {
            return nmsWorld().setTypeAndData(position, blockData, 3);
        } else {
            IBlockData old = nmsWorld().getType(position);
            boolean success = nmsWorld().setTypeAndData(position, blockData, 18); // NOTIFY | NO_OBSERVER
            if (success) {
                nmsWorld().notify(
                        position,
                        old,
                        blockData,
                        3
                );
            }
            return success;
        }
    }

    public Material getType() {
        return Material.getMaterial(getTypeId());
    }

    @Deprecated
    @Override
    public int getTypeId() {
        return CraftMagicNumbers.getId(getData0().getBlock());
    }

    public byte getLightLevel() {
        return (byte) nmsWorld().getLightLevel(position);
    }

    public byte getLightFromSky() {
        return (byte) nmsWorld().getBrightness(EnumSkyBlock.SKY, position);
    }

    public byte getLightFromBlocks() {
        return (byte) nmsWorld().getBrightness(EnumSkyBlock.BLOCK, position);
    }


    public Block getFace(final BlockFace face) {
        return getRelative(face, 1);
    }

    public Block getFace(final BlockFace face, final int distance) {
        return getRelative(face, distance);
    }

    public Block getRelative(final int modX, final int modY, final int modZ) {
        return getWorld().getBlockAt(getX() + modX, getY() + modY, getZ() + modZ);
    }

    public Block getRelative(BlockFace face) {
        return getRelative(face, 1);
    }

    public Block getRelative(BlockFace face, int distance) {
        return getRelative(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    public BlockFace getFace(final Block block) {
        BlockFace[] values = BlockFace.values();

        for (BlockFace face : values) {
            if ((this.getX() + face.getModX() == block.getX()) &&
                (this.getY() + face.getModY() == block.getY()) &&
                (this.getZ() + face.getModZ() == block.getZ())
            ) {
                return face;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "CraftBlock{" + "world=" + worldId + ",position=" + position + ",type=" + getType() + ",data=" + getData() + '}';
    }

    public static BlockFace notchToBlockFace(EnumDirection notch) {
        if (notch == null) return BlockFace.SELF;
        switch (notch) {
        case DOWN:
            return BlockFace.DOWN;
        case UP:
            return BlockFace.UP;
        case NORTH:
            return BlockFace.NORTH;
        case SOUTH:
            return BlockFace.SOUTH;
        case WEST:
            return BlockFace.WEST;
        case EAST:
            return BlockFace.EAST;
        default:
            return BlockFace.SELF;
        }
    }

    public static EnumDirection blockFaceToNotch(BlockFace face) {
        switch (face) {
        case DOWN:
            return EnumDirection.DOWN;
        case UP:
            return EnumDirection.UP;
        case NORTH:
            return EnumDirection.NORTH;
        case SOUTH:
            return EnumDirection.SOUTH;
        case WEST:
            return EnumDirection.WEST;
        case EAST:
            return EnumDirection.EAST;
        default:
            return null;
        }
    }

    public BlockState getState() {
        Material material = getType();

        switch (material) {
        case SIGN:
        case SIGN_POST:
        case WALL_SIGN:
            return new CraftSign(this);
        case CHEST:
        case TRAPPED_CHEST:
            return new CraftChest(this);
        case BURNING_FURNACE:
        case FURNACE:
            return new CraftFurnace(this);
        case DISPENSER:
            return new CraftDispenser(this);
        case DROPPER:
            return new CraftDropper(this);
        case END_GATEWAY:
            return new CraftEndGateway(this);
        case HOPPER:
            return new CraftHopper(this);
        case MOB_SPAWNER:
            return new CraftCreatureSpawner(this);
        case NOTE_BLOCK:
            return new CraftNoteBlock(this);
        case JUKEBOX:
            return new CraftJukebox(this);
        case BREWING_STAND:
            return new CraftBrewingStand(this);
        case SKULL:
            return new CraftSkull(this);
        case COMMAND:
        case COMMAND_CHAIN:
        case COMMAND_REPEATING:
            return new CraftCommandBlock(this);
        case BEACON:
            return new CraftBeacon(this);
        case BANNER:
        case WALL_BANNER:
        case STANDING_BANNER:
            return new CraftBanner(this);
        case FLOWER_POT:
            return new CraftFlowerPot(this);
        case STRUCTURE_BLOCK:
            return new CraftStructureBlock(this);
        case WHITE_SHULKER_BOX:
        case ORANGE_SHULKER_BOX:
        case MAGENTA_SHULKER_BOX:
        case LIGHT_BLUE_SHULKER_BOX:
        case YELLOW_SHULKER_BOX:
        case LIME_SHULKER_BOX:
        case PINK_SHULKER_BOX:
        case GRAY_SHULKER_BOX:
        case SILVER_SHULKER_BOX:
        case CYAN_SHULKER_BOX:
        case PURPLE_SHULKER_BOX:
        case BLUE_SHULKER_BOX:
        case BROWN_SHULKER_BOX:
        case GREEN_SHULKER_BOX:
        case RED_SHULKER_BOX:
        case BLACK_SHULKER_BOX:
            return new CraftShulkerBox(this);
        case ENCHANTMENT_TABLE:
            return new CraftEnchantingTable(this);
        case ENDER_CHEST:
            return new CraftEnderChest(this);
        case DAYLIGHT_DETECTOR:
        case DAYLIGHT_DETECTOR_INVERTED:
            return new CraftDaylightDetector(this);
        case REDSTONE_COMPARATOR_OFF:
        case REDSTONE_COMPARATOR_ON:
            return new CraftComparator(this);
        case BED_BLOCK:
            return new CraftBed(this);
        default:
            TileEntity tileEntity = getWorld().getTileEntityAt(position.coarseX(), position.coarseY(), position.coarseZ());
            if (tileEntity != null) {
                // block with unhandled TileEntity:
                return new CraftBlockEntityState<TileEntity>(this, (Class<TileEntity>) tileEntity.getClass());
            } else {
                // Block without TileEntity:
                return new CraftBlockState(this);
            }
        }
    }

    public Biome getBiome() {
        return getWorld().getBiome(position);
    }

    public void setBiome(Biome bio) {
        getWorld().setBiome(position, bio);
    }

    public static Biome biomeBaseToBiome(BiomeBase base) {
        if (base == null) {
            return null;
        }

        return Biome.valueOf(BiomeBase.REGISTRY_ID.b(base).getKey().toUpperCase(java.util.Locale.ENGLISH));
    }

    public static BiomeBase biomeToBiomeBase(Biome bio) {
        if (bio == null) {
            return null;
        }

        return BiomeBase.REGISTRY_ID.get(new MinecraftKey(bio.name().toLowerCase(java.util.Locale.ENGLISH)));
    }

    public double getTemperature() {
        return getWorld().getTemperature(position);
    }

    public double getHumidity() {
        return getWorld().getHumidity(position);
    }

    public boolean isBlockPowered() {
        return nmsWorld().getBlockPower(position) > 0;
    }

    public boolean isBlockIndirectlyPowered() {
        return nmsWorld().isBlockIndirectlyPowered(position);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CraftBlock)) return false;
        CraftBlock other = (CraftBlock) o;

        return this.getPosition().equals(other.getPosition()) && this.getWorld().equals(other.getWorld());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPosition(), getWorld());
    }

    public boolean isBlockFacePowered(BlockFace face) {
        return nmsWorld().isBlockFacePowered(position, blockFaceToNotch(face));
    }

    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        int power = nmsWorld().getBlockFacePower(position, blockFaceToNotch(face));

        Block relative = getRelative(face);
        if (relative.getType() == Material.REDSTONE_WIRE) {
            return Math.max(power, relative.getData()) > 0;
        }

        return power > 0;
    }

    public int getBlockPower(BlockFace face) {
        int power = 0;
        BlockRedstoneWire wire = Blocks.REDSTONE_WIRE;
        net.minecraft.server.World world = nmsWorld();
        if ((face == BlockFace.DOWN || face == BlockFace.SELF) && world.isBlockFacePowered(position.down(), EnumDirection.DOWN)) power = wire.getPower(world, position.down(), power);
        if ((face == BlockFace.UP || face == BlockFace.SELF) && world.isBlockFacePowered(position.up(), EnumDirection.UP)) power = wire.getPower(world, position.up(), power);
        if ((face == BlockFace.EAST || face == BlockFace.SELF) && world.isBlockFacePowered(position.east(), EnumDirection.EAST)) power = wire.getPower(world, position.east(), power);
        if ((face == BlockFace.WEST || face == BlockFace.SELF) && world.isBlockFacePowered(position.west(), EnumDirection.WEST)) power = wire.getPower(world, position.west(), power);
        if ((face == BlockFace.NORTH || face == BlockFace.SELF) && world.isBlockFacePowered(position.north(), EnumDirection.NORTH)) power = wire.getPower(world, position.north(), power);
        if ((face == BlockFace.SOUTH || face == BlockFace.SELF) && world.isBlockFacePowered(position.south(), EnumDirection.SOUTH)) power = wire.getPower(world, position.south(), power);
        return power > 0 ? power : (face == BlockFace.SELF ? isBlockIndirectlyPowered() : isBlockFaceIndirectlyPowered(face)) ? 15 : 0;
    }

    public int getBlockPower() {
        return getBlockPower(BlockFace.SELF);
    }

    public boolean isEmpty() {
        return getType() == Material.AIR;
    }

    public boolean isLiquid() {
        return (getType() == Material.WATER) || (getType() == Material.STATIONARY_WATER) || (getType() == Material.LAVA) || (getType() == Material.STATIONARY_LAVA);
    }

    public PistonMoveReaction getPistonMoveReaction() {
        return PistonMoveReaction.getById(getNMSBlock().h(getNMSBlock().fromLegacyData(getData())).ordinal());
    }

    private boolean itemCausesDrops(ItemStack item) {
        net.minecraft.server.Block block = this.getNMSBlock();
        net.minecraft.server.Item itemType = item != null ? net.minecraft.server.Item.getById(item.getTypeId()) : null;
        return block != null && (block.getBlockData().getMaterial().isAlwaysDestroyable() || (itemType != null && itemType.canDestroySpecialBlock(block.getBlockData())));
    }

    public boolean breakNaturally() {
        // Order matters here, need to drop before setting to air so skulls can get their data
        net.minecraft.server.Block block = this.getNMSBlock();
        byte data = getData();
        boolean result = false;

        if (block != null && block != Blocks.AIR) {
            block.dropNaturally(nmsWorld(), position, block.fromLegacyData(data), 1.0F, 0);
            result = true;
        }

        setTypeId(Material.AIR.getId());
        return result;
    }

    public boolean breakNaturally(ItemStack item) {
        if (itemCausesDrops(item)) {
            return breakNaturally();
        } else {
            return setTypeId(Material.AIR.getId());
        }
    }

    public Collection<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList<ItemStack>();

        net.minecraft.server.Block block = this.getNMSBlock();
        if (block != Blocks.AIR) {
            IBlockData data = getData0();
            // based on nms.Block.dropNaturally
            int count = block.getDropCount(0, nmsWorld().random);
            for (int i = 0; i < count; ++i) {
                Item item = block.getDropType(data, nmsWorld().random, 0);
                if (item != Items.a) {
                    // Skulls are special, their data is based on the tile entity
                    if (Blocks.SKULL == block) {
                        net.minecraft.server.ItemStack nmsStack = new net.minecraft.server.ItemStack(item, 1, block.getDropData(data));
                        TileEntitySkull tileentityskull = (TileEntitySkull) nmsWorld().getTileEntity(position);
                        if (tileentityskull.getSkullType() == 3 && tileentityskull.getGameProfile() != null) {
                            nmsStack.setTag(new NBTTagCompound());
                            NBTTagCompound nbttagcompound = new NBTTagCompound();

                            GameProfileSerializer.serialize(nbttagcompound, tileentityskull.getGameProfile());
                            nmsStack.getTag().set("SkullOwner", nbttagcompound);
                        }

                        drops.add(CraftItemStack.asBukkitCopy(nmsStack));
                        // We don't want to drop cocoa blocks, we want to drop cocoa beans.
                    } else if (Blocks.COCOA == block) {
                        int age = (Integer) data.get(BlockCocoa.AGE);
                        int dropAmount = (age >= 2 ? 3 : 1);
                        for (int j = 0; j < dropAmount; ++j) {
                            drops.add(new ItemStack(Material.INK_SACK, 1, (short) 3));
                        }
                    } else {
                        drops.add(new ItemStack(org.bukkit.craftbukkit.util.CraftMagicNumbers.getMaterial(item), 1, (short) block.getDropData(data)));
                    }
                }
            }
        }
        return drops;
    }

    public Collection<ItemStack> getDrops(ItemStack item) {
        if (itemCausesDrops(item)) {
            return getDrops();
        } else {
            return Collections.emptyList();
        }
    }

    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        getWorld().getBlockMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public MetadataValue getMetadata(String metadataKey, Plugin owningPlugin) {
        return getWorld().getBlockMetadata().getMetadata(this, metadataKey, owningPlugin);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return getWorld().getBlockMetadata().getMetadata(this, metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return getWorld().getBlockMetadata().hasMetadata(this, metadataKey);
    }

    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        getWorld().getBlockMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

    @Override
    public RayBlockIntersection rayTrace(Vector start, Vector end) {
        net.minecraft.server.Block nms = CraftMagicNumbers.getBlock(getType());
        if(nms == null) return null;

        MovingObjectPosition hit = nms.a(getData0(),
                                         getWorld().getHandle(),
                                         position,
                                         new Vec3D(start.getX(), start.getY(), start.getZ()),
                                         new Vec3D(end.getX(), end.getY(), end.getZ()));

        if(hit != null && hit.type == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            return new RayBlockIntersection(this,
                                            CraftBlock.notchToBlockFace(hit.direction),
                                            new Vector(hit.pos.x, hit.pos.y, hit.pos.z));
        } else {
            return null;
        }
    }

    @Override
    public RayBlockIntersection rayTrace(Location start, double distance) {
        return rayTrace(start.toVector(), start.toVector().add(start.getDirection().multiply(distance)));
    }

    public net.minecraft.server.Chunk nmsChunk() {
        return nmsWorld().getChunkAtWorldCoords(getPosition());
    }

    public WorldServer nmsWorld() {
        return getWorld().getHandle();
    }

    public BlockPosition nmsPosition() {
        return position;
    }

    @Override
    public void simulateNeighborChange(BlockFace neighbor, MaterialData before, MaterialData after) {
        final net.minecraft.server.World world = nmsWorld();
        final BlockPosition pos = nmsPosition();
        final BlockPosition neighborPos = pos.plus(neighbor.normal());

        // This is supposed to be functionally equivalent to updatePhysicsAndRedstone,
        // which uses the before state as the source rather than the after state
        // (for unknown reasons), so we do the same here.
        world.applySelfPhysics(pos, CraftMagicNumbers.getBlock(before.getItemType()), neighborPos);

        final net.minecraft.server.Block afterBlock = CraftMagicNumbers.getBlock(after.getItemType());
        if(afterBlock.isComplexRedstone(CraftMagicNumbers.getBlockData(after))) { // Avoided IBlockProperties.n() due to renaming risk
            world.updateComparator(pos, afterBlock, blockFaceToNotch(neighbor.getOppositeFace()), neighborPos);
        }
    }

    @Override
    public void simulateChangeForNeighbors(MaterialData before, MaterialData after) {
        nmsWorld().updatePhysicsAndRedstone(nmsPosition(), CraftMagicNumbers.getBlockData(before), CraftMagicNumbers.getBlockData(after));
    }
}
