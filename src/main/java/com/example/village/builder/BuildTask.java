package com.example.village.builder;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Representa uma tarefa de construção em andamento
 */
public class BuildTask {
    private final BuildingTemplate template;
    private final UUID villageId;
    private final BlockPos position;
    private int currentBlockIndex = 0;
    
    public BuildTask(BuildingTemplate template, UUID villageId, BlockPos position) {
        this.template = template;
        this.villageId = villageId;
        this.position = position;
    }
    
    /**
     * Obtém o próximo bloco a ser colocado
     */
    public BuildingBlock getNextBlock() {
        List<BuildingBlock> blocks = template.getBlocks();
        if (currentBlockIndex >= blocks.size()) {
            return null; // Não há mais blocos para colocar
        }
        
        return blocks.get(currentBlockIndex);
    }
    
    /**
     * Marca o bloco atual como colocado e avança para o próximo
     */
    public void markBlockPlaced() {
        currentBlockIndex++;
    }
    
    /**
     * Verifica se a construção está concluída
     */
    public boolean isCompleted() {
        return currentBlockIndex >= template.getBlocks().size();
    }
    
    /**
     * Obtém o modelo de construção
     */
    public BuildingTemplate getTemplate() {
        return template;
    }
    
    /**
     * Obtém o ID da vila
     */
    public UUID getVillageId() {
        return villageId;
    }
    
    /**
     * Obtém a posição da construção
     */
    public BlockPos getPosition() {
        return position;
    }
    
    /**
     * Obtém o progresso atual da construção (0-100%)
     */
    public float getProgress() {
        int totalBlocks = template.getBlocks().size();
        if (totalBlocks == 0) return 100.0f;
        
        return (float) currentBlockIndex / totalBlocks * 100.0f;
    }
}