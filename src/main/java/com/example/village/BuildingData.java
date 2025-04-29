package com.example.village;

import net.minecraft.util.math.BlockPos;

/**
 * Classe que armazena dados sobre uma construção na vila
 */
public class BuildingData {
    private final BuildingType type;
    private final BlockPos position;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private int bedCount; // Relevante apenas para casas
    private boolean completed;
    
    public BuildingData(BuildingType type, BlockPos position, int sizeX, int sizeY, int sizeZ) {
        this.type = type;
        this.position = position;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.completed = false;
        
        // Define o número de camas com base no tipo e tamanho da construção
        if (type == BuildingType.HOUSE) {
            // Uma casa pequena tem 1-2 camas, uma média tem 2-3, e uma grande tem 3-4
            int size = sizeX * sizeZ;
            if (size <= 25) { // Casa pequena (5x5 ou menor)
                this.bedCount = 1 + (size > 16 ? 1 : 0);
            } else if (size <= 49) { // Casa média (7x7 ou menor)
                this.bedCount = 2 + (size > 36 ? 1 : 0);
            } else { // Casa grande
                this.bedCount = 3 + (size > 64 ? 1 : 0);
            }
        } else {
            this.bedCount = 0;
        }
    }
    
    public BuildingType getType() {
        return type;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public int getSizeX() {
        return sizeX;
    }
    
    public int getSizeY() {
        return sizeY;
    }
    
    public int getSizeZ() {
        return sizeZ;
    }
    
    public int getBedCount() {
        return bedCount;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Define o número de camas na construção
     * @param bedCount O número de camas
     */
    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }
    
    /**
     * Verifica se uma posição está dentro desta construção
     */
    public boolean containsPosition(BlockPos pos) {
        int minX = position.getX() - sizeX / 2;
        int maxX = position.getX() + sizeX / 2;
        int minY = position.getY();
        int maxY = position.getY() + sizeY;
        int minZ = position.getZ() - sizeZ / 2;
        int maxZ = position.getZ() + sizeZ / 2;
        
        return pos.getX() >= minX && pos.getX() <= maxX &&
               pos.getY() >= minY && pos.getY() <= maxY &&
               pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }
}