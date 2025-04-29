package com.example.village.relation;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import com.example.village.profession.ProfessionManager;
import com.example.village.profession.WarriorProfession;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gerencia as relações entre vilas diferentes
 * Controla a reputação entre vilas e inicia conflitos quando a reputação cai abaixo de um limite
 */
public class VillageRelationManager {
    
    // Mapa que armazena a reputação entre pares de vilas (villageId1_villageId2 -> reputação)
    private final Map<String, Integer> villageRelations = new HashMap<>();
    
    // Mapa que armazena conflitos ativos entre vilas
    private final Map<String, VillageConflict> activeConflicts = new HashMap<>();
    
    // Constantes para gerenciamento de reputação
    private static final int MAX_REPUTATION = 100;
    private static final int INITIAL_REPUTATION = 50;
    private static final int CONFLICT_THRESHOLD = 20;
    private static final int PROXIMITY_PENALTY = 2;
    private static final int RESOURCE_COMPETITION_PENALTY = 3;
    private static final int RANDOM_EVENT_PENALTY = 5;
    private static final int SUCCESSFUL_RAID_PENALTY = 15;
    private static final int PEACE_NEGOTIATION_BONUS = 10;
    
    // Contador para limitar a frequência de verificações
    private int relationTickCounter = 0;
    private static final int RELATION_CHECK_INTERVAL = 200; // A cada 10 segundos
    
    private final Random random = new Random();
    
    /**
     * Processa as relações entre vilas
     * @param world O mundo do servidor
     */
    public void processVillageRelations(ServerWorld world) {
        relationTickCounter++;
        
        // Limita a frequência de verificações para não sobrecarregar o servidor
        if (relationTickCounter % RELATION_CHECK_INTERVAL != 0) {
            return;
        }
        
        // Obtém todas as vilas do gerenciador de expansão
        var villages = VillagerExpansionMod.getExpansionManager().getVillages();
        
        // Atualiza as relações entre todas as vilas
        updateVillageRelations(villages);
        
        // Processa conflitos existentes
        processExistingConflicts(world, villages);
        
        // Verifica se novas vilas entraram em conflito
        checkForNewConflicts(world, villages);
    }
    
    /**
     * Atualiza as relações entre todas as vilas
     * @param villages Lista de vilas para atualizar relações
     */
    private void updateVillageRelations(Iterable<VillageData> villages) {
        // Para cada par de vilas, atualiza a relação
        for (VillageData village1 : villages) {
            for (VillageData village2 : villages) {
                // Não processa a mesma vila
                if (village1.getVillageId().equals(village2.getVillageId())) {
                    continue;
                }
                
                // Obtém ou cria a chave de relação entre as vilas
                String relationKey = getRelationKey(village1.getVillageId(), village2.getVillageId());
                
                // Se não existe relação, inicializa
                if (!villageRelations.containsKey(relationKey)) {
                    villageRelations.put(relationKey, INITIAL_REPUTATION);
                    continue;
                }
                
                // Obtém a reputação atual
                int currentReputation = villageRelations.get(relationKey);
                
                // Fatores que afetam a reputação
                int reputationChange = 0;
                
                // 1. Proximidade: vilas muito próximas tendem a ter conflitos
                if (isVillagesClose(village1, village2)) {
                    reputationChange -= PROXIMITY_PENALTY;
                }
                
                // 2. Competição por recursos: vilas com recursos escassos tendem a ter conflitos
                if (hasResourceCompetition(village1, village2)) {
                    reputationChange -= RESOURCE_COMPETITION_PENALTY;
                }
                
                // 3. Eventos aleatórios: pequenos desentendimentos ou mal-entendidos
                if (random.nextFloat() < 0.1f) { // 10% de chance
                    reputationChange -= RANDOM_EVENT_PENALTY;
                    VillagerExpansionMod.LOGGER.info("Evento aleatório causou tensão entre as vilas " + 
                                                  village1.getVillageId() + " e " + village2.getVillageId());
                }
                
                // 4. Chance de melhorar relações naturalmente (recuperação gradual)
                if (random.nextFloat() < 0.05f && currentReputation < MAX_REPUTATION) { // 5% de chance
                    reputationChange += 1;
                }
                
                // Aplica a mudança de reputação
                currentReputation = Math.max(0, Math.min(MAX_REPUTATION, currentReputation + reputationChange));
                villageRelations.put(relationKey, currentReputation);
                
                // Registra mudanças significativas
                if (reputationChange < -3) {
                    VillagerExpansionMod.LOGGER.info("Relação entre vilas " + village1.getVillageId() + 
                                                  " e " + village2.getVillageId() + 
                                                  " deteriorou para " + currentReputation);
                }
            }
        }
    }
    
    /**
     * Processa conflitos existentes entre vilas
     * @param world O mundo do servidor
     * @param villages Lista de vilas para processar conflitos
     */
    private void processExistingConflicts(ServerWorld world, Iterable<VillageData> villages) {
        // Cria uma cópia para evitar ConcurrentModificationException
        Map<String, VillageConflict> conflictsCopy = new HashMap<>(activeConflicts);
        
        for (Map.Entry<String, VillageConflict> entry : conflictsCopy.entrySet()) {
            String relationKey = entry.getKey();
            VillageConflict conflict = entry.getValue();
            
            // Atualiza o estado do conflito
            conflict.update();
            
            // Verifica se o conflito terminou
            if (conflict.isResolved()) {
                // Remove o conflito da lista de ativos
                activeConflicts.remove(relationKey);
                
                // Aplica consequências do conflito
                applyConflictConsequences(world, conflict);
                
                VillagerExpansionMod.LOGGER.info("Conflito entre vilas " + conflict.getAttackerVillageId() + 
                                              " e " + conflict.getDefenderVillageId() + 
                                              " terminou. Resultado: " + 
                                              (conflict.isAttackerVictorious() ? "Atacante venceu" : "Defensor venceu"));
            }
        }
    }
    
    /**
     * Verifica se novas vilas entraram em conflito
     * @param world O mundo do servidor
     * @param villages Lista de vilas para verificar conflitos
     */
    private void checkForNewConflicts(ServerWorld world, Iterable<VillageData> villages) {
        for (VillageData village1 : villages) {
            for (VillageData village2 : villages) {
                // Não processa a mesma vila
                if (village1.getVillageId().equals(village2.getVillageId())) {
                    continue;
                }
                
                // Obtém a chave de relação
                String relationKey = getRelationKey(village1.getVillageId(), village2.getVillageId());
                
                // Verifica se já existe um conflito ativo
                if (activeConflicts.containsKey(relationKey)) {
                    continue;
                }
                
                // Verifica se a reputação está abaixo do limite
                int reputation = villageRelations.getOrDefault(relationKey, INITIAL_REPUTATION);
                if (reputation <= CONFLICT_THRESHOLD) {
                    // Inicia um novo conflito
                    startConflict(world, village1, village2, relationKey);
                }
            }
        }
    }
    
    /**
     * Inicia um conflito entre duas vilas
     * @param world O mundo do servidor
     * @param attacker Vila atacante
     * @param defender Vila defensora
     * @param relationKey Chave de relação entre as vilas
     */
    private void startConflict(ServerWorld world, VillageData attacker, VillageData defender, String relationKey) {
        // Cria um novo conflito
        VillageConflict conflict = new VillageConflict(attacker.getVillageId(), defender.getVillageId());
        activeConflicts.put(relationKey, conflict);
        
        // Mobiliza guerreiros para o ataque
        mobilizeWarriors(world, attacker, defender, true);
        
        // Mobiliza guerreiros para a defesa
        mobilizeWarriors(world, defender, attacker, false);
        
        VillagerExpansionMod.LOGGER.info("Conflito iniciado entre vilas " + attacker.getVillageId() + 
                                      " (atacante) e " + defender.getVillageId() + " (defensora)");
    }
    
    /**
     * Mobiliza guerreiros de uma vila para atacar ou defender
     * @param world O mundo do servidor
     * @param village Vila que mobilizará guerreiros
     * @param targetVillage Vila alvo (atacada ou atacante)
     * @param isAttacking true se estiver atacando, false se estiver defendendo
     */
    private void mobilizeWarriors(ServerWorld world, VillageData village, VillageData targetVillage, boolean isAttacking) {
        ProfessionManager professionManager = VillagerExpansionMod.getExpansionManager().getProfessionManager();
        
        // Encontra todos os villagers guerreiros da vila
        for (UUID villagerId : village.getVillagers()) {
            // Verifica se o villager é um guerreiro
            if (professionManager.hasProfession(villagerId)) {
                var profession = professionManager.getProfession(villagerId);
                if (profession instanceof WarriorProfession) {
                    // Encontra o villager no mundo
                    if (world.getEntity(villagerId) instanceof VillagerEntity villager) {
                        // Define o alvo do guerreiro
                        if (isAttacking) {
                            // Ataque: direciona para o centro da vila alvo
                            BlockPos targetPos = targetVillage.getCenter();
                            VillagerExpansionMod.LOGGER.info("Guerreiro " + villagerId + 
                                                          " mobilizado para atacar a vila " + 
                                                          targetVillage.getVillageId() + " em " + targetPos);
                            
                            // Em uma implementação completa, definiríamos metas de IA para o villager atacar
                        } else {
                            // Defesa: permanece próximo ao centro da própria vila
                            VillagerExpansionMod.LOGGER.info("Guerreiro " + villagerId + 
                                                          " mobilizado para defender a vila " + 
                                                          village.getVillageId());
                            
                            // Em uma implementação completa, definiríamos metas de IA para o villager defender
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Aplica as consequências de um conflito resolvido
     * @param world O mundo do servidor
     * @param conflict O conflito resolvido
     */
    private void applyConflictConsequences(ServerWorld world, VillageConflict conflict) {
        // Obtém as vilas envolvidas
        UUID attackerId = conflict.getAttackerVillageId();
        UUID defenderId = conflict.getDefenderVillageId();
        
        VillageData attacker = null;
        VillageData defender = null;
        
        for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
            if (village.getVillageId().equals(attackerId)) {
                attacker = village;
            } else if (village.getVillageId().equals(defenderId)) {
                defender = village;
            }
            
            if (attacker != null && defender != null) {
                break;
            }
        }
        
        if (attacker == null || defender == null) {
            return; // Uma das vilas não existe mais
        }
        
        // Obtém a chave de relação
        String relationKey = getRelationKey(attackerId, defenderId);
        
        // Aplica consequências baseadas no resultado
        if (conflict.isAttackerVictorious()) {
            // Atacante venceu: rouba recursos e deteriora ainda mais a relação
            int woodStolen = Math.min(20, defender.getWoodResource());
            int stoneStolen = Math.min(15, defender.getStoneResource());
            int foodStolen = Math.min(25, defender.getFoodResource());
            
            // Remove recursos da vila defensora
            defender.consumeResources(woodStolen, stoneStolen, foodStolen);
            
            // Adiciona recursos à vila atacante
            attacker.addResources(woodStolen, stoneStolen, foodStolen);
            
            // Deteriora ainda mais a relação
            int currentReputation = villageRelations.getOrDefault(relationKey, 0);
            villageRelations.put(relationKey, Math.max(0, currentReputation - SUCCESSFUL_RAID_PENALTY));
            
            VillagerExpansionMod.LOGGER.info("Vila " + attackerId + " saqueou recursos da vila " + 
                                          defenderId + ": " + woodStolen + " madeira, " + 
                                          stoneStolen + " pedra, " + foodStolen + " comida");
        } else {
            // Defensor venceu: chance de melhorar a relação através de negociações
            if (random.nextFloat() < 0.3f) { // 30% de chance de negociação de paz
                int currentReputation = villageRelations.getOrDefault(relationKey, 0);
                villageRelations.put(relationKey, Math.min(MAX_REPUTATION, currentReputation + PEACE_NEGOTIATION_BONUS));
                
                VillagerExpansionMod.LOGGER.info("Vilas " + attackerId + " e " + defenderId + 
                                              " negociaram paz após conflito. Reputação melhorou para " + 
                                              villageRelations.get(relationKey));
            }
        }
    }
    
    /**
     * Verifica se duas vilas estão próximas o suficiente para gerar tensão
     * @param village1 Primeira vila
     * @param village2 Segunda vila
     * @return true se as vilas estão próximas, false caso contrário
     */
    private boolean isVillagesClose(VillageData village1, VillageData village2) {
        BlockPos pos1 = village1.getCenter();
        BlockPos pos2 = village2.getCenter();
        
        // Considera vilas próximas se estiverem a menos de 200 blocos de distância
        double distanceSquared = pos1.getSquaredDistance(pos2);
        return distanceSquared < 200 * 200;
    }
    
    /**
     * Verifica se há competição por recursos entre duas vilas
     * @param village1 Primeira vila
     * @param village2 Segunda vila
     * @return true se há competição por recursos, false caso contrário
     */
    private boolean hasResourceCompetition(VillageData village1, VillageData village2) {
        // Verifica se ambas as vilas têm poucos recursos
        boolean village1LowResources = village1.getWoodResource() < 30 || 
                                     village1.getStoneResource() < 20 || 
                                     village1.getFoodResource() < 40;
        
        boolean village2LowResources = village2.getWoodResource() < 30 || 
                                     village2.getStoneResource() < 20 || 
                                     village2.getFoodResource() < 40;
        
        // Se ambas têm poucos recursos e estão próximas, há competição
        return village1LowResources && village2LowResources && isVillagesClose(village1, village2);
    }
    
    /**
     * Obtém a chave de relação entre duas vilas
     * @param villageId1 ID da primeira vila
     * @param villageId2 ID da segunda vila
     * @return Chave de relação no formato "menor_id_maior_id"
     */
    private String getRelationKey(UUID villageId1, UUID villageId2) {
        // Garante que a chave seja consistente independente da ordem dos IDs
        if (villageId1.compareTo(villageId2) < 0) {
            return villageId1.toString() + "_" + villageId2.toString();
        } else {
            return villageId2.toString() + "_" + villageId1.toString();
        }
    }
    
    /**
     * Obtém a reputação entre duas vilas
     * @param villageId1 ID da primeira vila
     * @param villageId2 ID da segunda vila
     * @return Valor da reputação entre as vilas
     */
    public int getReputation(UUID villageId1, UUID villageId2) {
        String relationKey = getRelationKey(villageId1, villageId2);
        return villageRelations.getOrDefault(relationKey, INITIAL_REPUTATION);
    }
    
    /**
     * Verifica se há um conflito ativo entre duas vilas
     * @param villageId1 ID da primeira vila
     * @param villageId2 ID da segunda vila
     * @return true se há um conflito ativo, false caso contrário
     */
    public boolean hasActiveConflict(UUID villageId1, UUID villageId2) {
        String relationKey = getRelationKey(villageId1, villageId2);
        return activeConflicts.containsKey(relationKey);
    }
    
    /**
     * Obtém todos os conflitos ativos
     * @return Mapa de conflitos ativos
     */
    public Map<String, VillageConflict> getActiveConflicts() {
        return new HashMap<>(activeConflicts);
    }
}