package org.bukkit.util;

import java.util.Map;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.geometry.Vec3;

/**
 * A vector with a hash function that floors the X, Y, Z components, a la
 * BlockVector in WorldEdit. BlockVectors can be used in hash sets and
 * hash maps. Be aware that BlockVectors are mutable, but it is important
 * that BlockVectors are never changed once put into a hash set or hash map.
 */
@SerializableAs("BlockVector")
public class BlockVector extends Vector {

    /**
     * Construct the vector with all components as 0.
     */
    public BlockVector() {
        super();
    }

    /**
     * Construct the vector with another vector.
     *
     * @param vec The other vector.
     */
    public BlockVector(Vector vec) {
        super(vec);
    }

    /**
     * Construct the vector with provided integer components.
     *
     * @param x X component
     * @param y Y component
     * @param z Z component
     */
    public BlockVector(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Construct the vector with provided double components.
     *
     * @param x X component
     * @param y Y component
     * @param z Z component
     */
    public BlockVector(double x, double y, double z) {
        super(x, y, z);
    }

    /**
     * Construct the vector with provided float components.
     *
     * @param x X component
     * @param y Y component
     * @param z Z component
     */
    public BlockVector(float x, float y, float z) {
        super(x, y, z);
    }

    public BlockVector(Vec3 v) {
        super(v);
    }

    @Override
    public boolean isFine() {
        return false;
    }

    @Override
    public boolean isCoarse() {
        return true;
    }

    @Override
    public boolean equals(Vec3 v) {
        return v != null && v.isCoarse() && coarseEquals(v);
    }


    /**
     * Checks if another object is equivalent.
     *
     * @param obj The other object
     * @return whether the other object is equivalent
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Vec3 && equals((Vec3) obj));
    }

    /**
     * Returns a hash code for this vector.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return coarseHashCode();
    }

    /**
     * Get a new block vector.
     *
     * @return vector
     */
    @Override
    public BlockVector clone() {
        return (BlockVector) super.clone();
    }

    public static BlockVector deserialize(Map<String, Object> args) {
        double x = 0;
        double y = 0;
        double z = 0;

        if (args.containsKey("x")) {
            x = (Double) args.get("x");
        }
        if (args.containsKey("y")) {
            y = (Double) args.get("y");
        }
        if (args.containsKey("z")) {
            z = (Double) args.get("z");
        }

        return new BlockVector(x, y, z);
    }
}
