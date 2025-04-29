package com.example.village.resources;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Representa uma tarefa de coleta de recursos em andamento
 */
public class ResourceCollectionTask {
    private final UUID villageId;
    private final BlockPos resourcePosition;
    private final ResourceType resourceType;
    private int progress = 0;
    private static final int MAX_PROGRESS = 100;
    
    public ResourceCollectionTask(UUID villageId, BlockPos resourcePosition, ResourceType resourceType) {
        this.villageId = villageId;
        this.resourcePosition = resourcePosition;
        this.resourceType = resourceType;
    }
    
    /**
     * Incrementa o progresso da coleta
     * @return true se a coleta foi concluída
     */
    public boolean incrementProgress(int amount) {
        progress += amount;
        return progress >= MAX_PROGRESS;
    }
    
    /**
     * Verifica se a coleta está concluída
     */
    public boolean isCompleted() {
        return progress >= MAX_PROGRESS;
    }
    
    /**
     * Obtém o ID da vila
     */
    public UUID getVillageId() {
        return villageId;
    }
    
    /**
     * Obtém a posição do recurso
     */
    public BlockPos getResourcePosition() {
        return resourcePosition;
    }
    
    /**
     * Obtém o tipo de recurso
     */
    public ResourceType getResourceType() {
        return resourceType;
    }
    
    /**
     * Obtém o progresso atual da coleta (0-100%)
     */
    public float getProgressPercentage() {
        return (float) progress / MAX_PROGRESS * 100.0f;
    }
}