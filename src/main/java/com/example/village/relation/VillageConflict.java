package com.example.village.relation;

import java.util.Random;
import java.util.UUID;

/**
 * Representa um conflito ativo entre duas vilas
 * Rastreia o estado do conflito, sua duração e resultado
 */
public class VillageConflict {
    
    private final UUID attackerVillageId;
    private final UUID defenderVillageId;
    private final long startTime;
    private int duration;
    private boolean resolved;
    private boolean attackerVictorious;
    
    // Duração do conflito em ticks (1-3 dias de jogo)
    private static final int MIN_CONFLICT_DURATION = 24000; // 20 min (1 dia de jogo)
    private static final int MAX_CONFLICT_DURATION = 72000; // 60 min (3 dias de jogo)
    
    private final Random random = new Random();
    
    /**
     * Cria um novo conflito entre duas vilas
     * @param attackerVillageId ID da vila atacante
     * @param defenderVillageId ID da vila defensora
     */
    public VillageConflict(UUID attackerVillageId, UUID defenderVillageId) {
        this.attackerVillageId = attackerVillageId;
        this.defenderVillageId = defenderVillageId;
        this.startTime = System.currentTimeMillis();
        this.resolved = false;
        
        // Define uma duração aleatória para o conflito
        this.duration = MIN_CONFLICT_DURATION + random.nextInt(MAX_CONFLICT_DURATION - MIN_CONFLICT_DURATION);
        
        // Define aleatoriamente quem será vitorioso (em uma implementação completa, isso seria baseado em força militar)
        this.attackerVictorious = random.nextFloat() < 0.6f; // 60% de chance do atacante vencer
    }
    
    /**
     * Atualiza o estado do conflito
     * @return true se o conflito foi resolvido nesta atualização, false caso contrário
     */
    public boolean update() {
        if (resolved) {
            return false;
        }
        
        // Verifica se o conflito já durou o suficiente para ser resolvido
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        
        // Converte o tempo real em ticks aproximados (20 ticks por segundo)
        long elapsedTicks = (elapsedTime / 50); // 50ms por tick
        
        if (elapsedTicks >= duration) {
            resolved = true;
            return true;
        }
        
        return false;
    }
    
    /**
     * Verifica se o conflito foi resolvido
     * @return true se o conflito foi resolvido, false caso contrário
     */
    public boolean isResolved() {
        return resolved;
    }
    
    /**
     * Verifica se o atacante foi vitorioso
     * @return true se o atacante venceu, false se o defensor venceu
     */
    public boolean isAttackerVictorious() {
        return attackerVictorious;
    }
    
    /**
     * Obtém o ID da vila atacante
     * @return UUID da vila atacante
     */
    public UUID getAttackerVillageId() {
        return attackerVillageId;
    }
    
    /**
     * Obtém o ID da vila defensora
     * @return UUID da vila defensora
     */
    public UUID getDefenderVillageId() {
        return defenderVillageId;
    }
    
    /**
     * Obtém o tempo de início do conflito
     * @return Timestamp de início do conflito
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Obtém a duração planejada do conflito em ticks
     * @return Duração do conflito em ticks
     */
    public int getDuration() {
        return duration;
    }
    
    /**
     * Define manualmente o resultado do conflito
     * Útil para intervenções de jogadores ou eventos especiais
     * @param attackerWins true se o atacante vence, false se o defensor vence
     */
    public void setResult(boolean attackerWins) {
        this.attackerVictorious = attackerWins;
    }
    
    /**
     * Resolve imediatamente o conflito
     * Útil para intervenções de jogadores ou eventos especiais
     */
    public void resolveImmediately() {
        this.resolved = true;
    }
}