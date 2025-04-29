package com.example.village.builder;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * Representa um bloco individual em uma construção
 */
public class BuildingBlock {
    private final BlockPos position;
    private final BlockState blockState;
    
    public BuildingBlock(BlockPos position, BlockState blockState) {
        this.position = position;
        this.blockState = blockState;
    }
    
    /**
     * Obtém a posição do bloco
     */
    public BlockPos getPosition() {
        return position;
    }
    
    /**
     * Obtém o estado do bloco
     */
    public BlockState getBlockState() {
        return blockState;
    }
}