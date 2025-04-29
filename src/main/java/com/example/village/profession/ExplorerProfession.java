package com.example.village.profession;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import com.example.village.exploration.ExplorationTask;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Implementação da profissão de Explorador para villagers
 * Os exploradores descobrem novas áreas, coletam recursos e encontram locais de interesse
 */
public class ExplorerProfession implements VillagerProfession {
    
    private final Random random = new Random();
    private boolean hasEquipment = false;
    private UUID currentTaskId = null;
    private boolean isExploring = false;
    private int explorationCooldown = 0;
    private static final int EXPLORATION_INTERVAL = 400; // 20 segundos
    
    @Override
    public String getName() {
        return "Explorador";
    }
    
    @Override
    public void onTick(VillagerEntity villager, ServerWorld world) {
        // Equipa o villager com itens de exploração se ainda não estiver equipado
        if (!hasEquipment) {
            equipExplorer(villager);
            hasEquipment = true;
        }
        
        // Se já está explorando, verifica o progresso
        if (isExploring) {
            checkExplorationProgress(villager, world);
            return;
        }
        
        // Tenta iniciar uma nova exploração
        explorationCooldown--;
        if (explorationCooldown <= 0) {
            tryStartExploration(villager, world);
            explorationCooldown = EXPLORATION_INTERVAL;
        }
    }
    
    @Override
    public boolean canStoreItems(VillagerEntity villager, ServerWorld world) {
        // Verifica se o villager tem itens para armazenar
        return !villager.getInventory().isEmpty();
    }
    
    @Override
    public boolean storeItems(VillagerEntity villager, ServerWorld world) {
        // Procura por baús próximos para armazenar itens
        BlockPos villagerPos = villager.getBlockPos();
        int searchRadius = 16;
        
        // Procura por baús em um raio ao redor do villager
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = villagerPos.add(x, y, z);
                    
                    // Verifica se o bloco é um baú
                    if (world.getBlockState(pos).getBlock().equals(net.minecraft.block.Blocks.CHEST)) {
                        // Simula o armazenamento de itens (em uma implementação completa, usaríamos o inventário do baú)
                        VillagerExpansionMod.LOGGER.info("Explorador " + villager.getUuid() + " armazenou itens em um baú em " + pos);
                        // Limpa o inventário do villager (simulação)
                        // Em uma implementação completa, transferiríamos os itens para o baú
                        return true;
                    }
                }
            }
        }
        
        return false; // Não encontrou baús próximos
    }
    
    /**
     * Equipa o villager com itens de exploração
     * @param villager O villager a ser equipado
     */
    private void equipExplorer(VillagerEntity villager) {
        // Equipa o villager com uma tocha
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TORCH));
        
        // Chance de equipar com outros itens úteis
        if (random.nextFloat() < 0.5f) {
            villager.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.MAP));
        }
        
        if (random.nextFloat() < 0.3f) {
            villager.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        }
        
        // Impede que os itens caiam quando o villager morre
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0f);
        
        VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " equipado como Explorador");
    }
    
    /**
     * Tenta iniciar uma tarefa de exploração
     * @param villager O villager explorador
     * @param world O mundo do servidor
     */
    private void tryStartExploration(VillagerEntity villager, ServerWorld world) {
        // Encontra a vila do villager
        VillageData villagerVillage = null;
        UUID villagerId = villager.getUuid();
        
        for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
            if (village.getVillagers().contains(villagerId)) {
                villagerVillage = village;
                break;
            }
        }
        
        if (villagerVillage == null) {
            return; // Villager não pertence a nenhuma vila
        }
        
        // Verifica se há tarefas de exploração disponíveis
        Map<UUID, ExplorationTask> tasks = VillagerExpansionMod.getExpansionManager()
                .getExplorationManager().getActiveExplorationTasks();
        
        // Procura por uma tarefa para a vila deste villager
        for (Map.Entry<UUID, ExplorationTask> entry : tasks.entrySet()) {
            ExplorationTask task = entry.getValue();
            if (task.getVillageId().equals(villagerVillage.getVillageId()) && !task.isCompleted()) {
                // Encontrou uma tarefa, atribui ao villager
                isExploring = true;
                currentTaskId = entry.getKey();
                VillagerExpansionMod.LOGGER.info("Explorador " + villager.getUuid() + 
                                              " iniciou exploração para " + task.getTargetPosition());
                return;
            }
        }
    }
    
    /**
     * Verifica o progresso de uma tarefa de exploração
     * @param villager O villager explorador
     * @param world O mundo do servidor
     */
    private void checkExplorationProgress(VillagerEntity villager, ServerWorld world) {
        if (currentTaskId == null) {
            isExploring = false;
            return;
        }
        
        // Verifica se a tarefa ainda existe
        ExplorationTask task = VillagerExpansionMod.getExpansionManager()
                .getExplorationManager().getExplorationTask(currentTaskId);
        
        if (task == null || task.isCompleted()) {
            // Tarefa concluída ou removida
            isExploring = false;
            currentTaskId = null;
            VillagerExpansionMod.LOGGER.info("Explorador " + villager.getUuid() + " concluiu tarefa de exploração");
            
            // Adiciona itens ao inventário do villager (simulação de coleta de recursos)
            // Em uma implementação completa, adicionaríamos itens baseados no que foi descoberto
            if (random.nextFloat() < 0.7f) {
                // Simula a coleta de recursos
                VillagerExpansionMod.LOGGER.info("Explorador " + villager.getUuid() + " coletou recursos durante a exploração");
            }
        }
    }
}