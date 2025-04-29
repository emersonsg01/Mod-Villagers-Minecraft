package com.example.village.exploration;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Representa uma tarefa de exploração em andamento
 */
public class ExplorationTask {
    private final UUID villageId;
    private final BlockPos targetPosition;
    private int progress = 0;
    private static final int MAX_PROGRESS = 100;
    
    public ExplorationTask(UUID villageId, BlockPos targetPosition) {
        this.villageId = villageId;
        this.targetPosition = targetPosition;
    }
    
    /**
     * Incrementa o progresso da exploração
     * @return true se a exploração foi concluída
     */
    public boolean incrementProgress(int amount) {
        progress += amount;
        return progress >= MAX_PROGRESS;
    }
    
    /**
     * Verifica se a exploração está concluída
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
     * Obtém a posição alvo da exploração
     */
    public BlockPos getTargetPosition() {
        return targetPosition;
    }
    
    /**
     * Obtém o progresso atual da exploração (0-100%)
     */
    public float getProgressPercentage() {
        return (float) progress / MAX_PROGRESS * 100.0f;
    }
}