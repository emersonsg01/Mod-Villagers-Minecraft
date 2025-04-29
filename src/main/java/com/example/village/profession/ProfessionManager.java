package com.example.village.profession;

import com.example.VillagerExpansionMod;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gerencia as profissões personalizadas dos villagers
 * Atribui profissões aos villagers sem profissão e gerencia as profissões existentes
 */
public class ProfessionManager {
    
    // Mapa que associa o UUID do villager à sua profissão personalizada
    private final Map<UUID, com.example.village.profession.VillagerProfession> villagerProfessions = new HashMap<>();
    
    // Contador para limitar a frequência de verificações
    private int professionTickCounter = 0;
    private static final int PROFESSION_CHECK_INTERVAL = 100; // A cada 5 segundos
    
    private final Random random = new Random();
    
    /**
     * Processa as profissões dos villagers
     * @param world O mundo do servidor
     */
    public void processProfessions(ServerWorld world) {
        professionTickCounter++;
        
        // Limita a frequência de verificações para não sobrecarregar o servidor
        if (professionTickCounter % PROFESSION_CHECK_INTERVAL != 0) {
            return;
        }
        
        // Processa as profissões existentes
        processExistingProfessions(world);
        
        // Atribui profissões aos villagers sem profissão
        assignProfessionsToUnemployed(world);
    }
    
    /**
     * Processa as profissões existentes
     * @param world O mundo do servidor
     */
    private void processExistingProfessions(ServerWorld world) {
        // Cria uma cópia do mapa para evitar ConcurrentModificationException
        Map<UUID, com.example.village.profession.VillagerProfession> professionsCopy = new HashMap<>(villagerProfessions);
        
        // Processa cada profissão
        for (Map.Entry<UUID, com.example.village.profession.VillagerProfession> entry : professionsCopy.entrySet()) {
            UUID villagerId = entry.getKey();
            com.example.village.profession.VillagerProfession profession = entry.getValue();
            
            // Encontra o villager pelo UUID
            world.getEntity(villagerId);
            if (world.getEntity(villagerId) instanceof VillagerEntity villager) {
                // Executa as ações da profissão
                profession.onTick(villager, world);
                
                // Verifica se o villager pode armazenar itens
                if (profession.canStoreItems(villager, world)) {
                    profession.storeItems(villager, world);
                }
            } else {
                // Se o villager não existe mais, remove a profissão
                villagerProfessions.remove(villagerId);
            }
        }
    }
    
    /**
     * Atribui profissões aos villagers sem profissão
     * @param world O mundo do servidor
     */
    private void assignProfessionsToUnemployed(ServerWorld world) {
        // Encontra todos os villagers sem profissão
        world.getEntitiesByType(net.minecraft.entity.EntityType.VILLAGER, entity -> {
            VillagerEntity villager = (VillagerEntity) entity;
            // Verifica se o villager não tem profissão e não é uma criança
            // Na versão 1.21, verificamos se o villager tem uma atividade de trabalho
            boolean hasNoProfession = !villager.getBrain().hasActivity(Activity.WORK);
            return hasNoProfession && !villager.isBaby();
        }).forEach(entity -> {
            VillagerEntity villager = (VillagerEntity) entity;
            UUID villagerId = villager.getUuid();
            
            // Verifica se o villager já tem uma profissão personalizada
            if (!villagerProfessions.containsKey(villagerId)) {
                // Atribui uma profissão aleatória
                assignRandomProfession(villager, world);
            }
        });
    }
    
    /**
     * Atribui uma profissão aleatória a um villager
     * @param villager O villager que receberá a profissão
     * @param world O mundo do servidor
     */
    private void assignRandomProfession(VillagerEntity villager, ServerWorld world) {
        // Escolhe uma profissão aleatória
        int professionChoice = random.nextInt(4); // 0: Guerreiro, 1: Explorador, 2: Minerador, 3: Construtor
        
        com.example.village.profession.VillagerProfession profession;
        switch (professionChoice) {
            case 0:
                profession = new WarriorProfession();
                break;
            case 1:
                profession = new ExplorerProfession();
                break;
            case 2:
                profession = new MinerProfession();
                break;
            case 3:
                profession = new BuilderProfession();
                break;
            default:
                profession = new WarriorProfession(); // Padrão: Guerreiro
        }
        
        // Associa a profissão ao villager
        villagerProfessions.put(villager.getUuid(), profession);
        
        VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " recebeu a profissão de " + profession.getName());
    }
    
    /**
     * Obtém a profissão de um villager
     * @param villagerId UUID do villager
     * @return A profissão do villager, ou null se não tiver uma profissão personalizada
     */
    public com.example.village.profession.VillagerProfession getProfession(UUID villagerId) {
        return villagerProfessions.get(villagerId);
    }
    
    /**
     * Verifica se um villager tem uma profissão personalizada
     * @param villagerId UUID do villager
     * @return true se o villager tem uma profissão personalizada, false caso contrário
     */
    public boolean hasProfession(UUID villagerId) {
        return villagerProfessions.containsKey(villagerId);
    }
    
    /**
     * Remove a profissão de um villager
     * @param villagerId UUID do villager
     */
    public void removeProfession(UUID villagerId) {
        villagerProfessions.remove(villagerId);
    }
}