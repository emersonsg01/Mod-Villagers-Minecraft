package com.example.village.builder;

import com.example.village.BuildingType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um modelo de construção com todos os blocos que a compõem
 */
public class BuildingTemplate {
    private final BuildingType type;
    private final BlockPos position;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<BuildingBlock> blocks = new ArrayList<>();
    
    public BuildingTemplate(BuildingType type, BlockPos position, int sizeX, int sizeY, int sizeZ) {
        this.type = type;
        this.position = position;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }
    
    /**
     * Adiciona um bloco ao modelo
     */
    public void addBlock(BuildingBlock block) {
        blocks.add(block);
    }
    
    /**
     * Obtém a lista de blocos do modelo
     */
    public List<BuildingBlock> getBlocks() {
        return blocks;
    }
    
    /**
     * Obtém o tipo de construção
     */
    public BuildingType getType() {
        return type;
    }
    
    /**
     * Obtém a posição central da construção
     */
    public BlockPos getPosition() {
        return position;
    }
    
    /**
     * Obtém o tamanho X da construção
     */
    public int getSizeX() {
        return sizeX;
    }
    
    /**
     * Obtém o tamanho Y da construção
     */
    public int getSizeY() {
        return sizeY;
    }
    
    /**
     * Obtém o tamanho Z da construção
     */
    public int getSizeZ() {
        return sizeZ;
    }
}