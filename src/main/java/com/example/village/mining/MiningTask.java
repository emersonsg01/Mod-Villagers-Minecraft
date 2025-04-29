package com.example.village.mining;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Representa uma tarefa de mineração em andamento
 */
public class MiningTask {
    private final UUID villageId;
    private final BlockPos miningPosition;
    private int progress = 0;
    private static final int MAX_PROGRESS = 100;
    
    public MiningTask(UUID villageId, BlockPos miningPosition) {
        this.villageId = villageId;
        this.miningPosition = miningPosition;
    }
    
    /**
     * Incrementa o progresso da mineração
     * @return true se a mineração foi concluída
     */
    public boolean incrementProgress(int amount) {
        progress += amount;
        return progress >= MAX_PROGRESS;
    }
    
    /**
     * Verifica se a mineração está concluída
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
     * Obtém a posição de mineração
     */
    public BlockPos getMiningPosition() {
        return miningPosition;
    }
    
    /**
     * Obtém o progresso atual da mineração (0-100%)
     */
    public float getProgressPercentage() {
        return (float) progress / MAX_PROGRESS * 100.0f;
    }
}