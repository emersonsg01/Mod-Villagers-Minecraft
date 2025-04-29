package com.example.village.profession;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import com.example.village.mining.MiningTask;
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
 * Implementação da profissão de Minerador para villagers
 * Os mineradores escavam em busca de minérios e recursos subterrâneos
 */
public class MinerProfession implements VillagerProfession {
    
    private final Random random = new Random();
    private boolean hasEquipment = false;
    private UUID currentTaskId = null;
    private boolean isMining = false;
    private int miningCooldown = 0;
    private static final int MINING_INTERVAL = 300; // 15 segundos
    
    @Override
    public String getName() {
        return "Minerador";
    }
    
    @Override
    public void onTick(VillagerEntity villager, ServerWorld world) {
        // Equipa o villager com ferramentas de mineração se ainda não estiver equipado
        if (!hasEquipment) {
            equipMiner(villager);
            hasEquipment = true;
        }
        
        // Se já está minerando, verifica o progresso
        if (isMining) {
            checkMiningProgress(villager, world);
            return;
        }
        
        // Tenta iniciar uma nova mineração
        miningCooldown--;
        if (miningCooldown <= 0) {
            tryStartMining(villager, world);
            miningCooldown = MINING_INTERVAL;
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
                        VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + " armazenou minérios em um baú em " + pos);
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
     * Equipa o villager com ferramentas de mineração
     * @param villager O villager a ser equipado
     */
    private void equipMiner(VillagerEntity villager) {
        // Equipa o villager com uma picareta
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        
        // Chance de equipar com outros itens úteis
        if (random.nextFloat() < 0.5f) {
            villager.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
        }
        
        if (random.nextFloat() < 0.3f) {
            villager.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
        
        // Impede que os itens caiam quando o villager morre
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0f);
        
        VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " equipado como Minerador");
    }
    
    /**
     * Tenta iniciar uma tarefa de mineração
     * @param villager O villager minerador
     * @param world O mundo do servidor
     */
    private void tryStartMining(VillagerEntity villager, ServerWorld world) {
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
        
        // Verifica se há tarefas de mineração disponíveis
        Map<UUID, MiningTask> tasks = VillagerExpansionMod.getExpansionManager()
                .getMiningManager().getActiveMiningTasks();
        
        // Procura por uma tarefa para a vila deste villager
        for (Map.Entry<UUID, MiningTask> entry : tasks.entrySet()) {
            MiningTask task = entry.getValue();
            if (task.getVillageId().equals(villagerVillage.getVillageId()) && !task.isCompleted()) {
                // Encontrou uma tarefa, atribui ao villager
                isMining = true;
                currentTaskId = entry.getKey();
                VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + 
                                              " iniciou mineração em " + task.getMiningPosition());
                return;
            }
        }
    }
    
    /**
     * Verifica o progresso de uma tarefa de mineração
     * @param villager O villager minerador
     * @param world O mundo do servidor
     */
    private void checkMiningProgress(VillagerEntity villager, ServerWorld world) {
        if (currentTaskId == null) {
            isMining = false;
            return;
        }
        
        // Verifica se a tarefa ainda existe
        MiningTask task = VillagerExpansionMod.getExpansionManager()
                .getMiningManager().getMiningTask(currentTaskId);
        
        if (task == null || task.isCompleted()) {
            // Tarefa concluída ou removida
            isMining = false;
            currentTaskId = null;
            VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + " concluiu tarefa de mineração");
            
            // Adiciona minérios ao inventário do villager (simulação de coleta de recursos)
            // Em uma implementação completa, adicionaríamos itens baseados no que foi minerado
            if (random.nextFloat() < 0.8f) {
                // Simula a coleta de minérios
                VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + " coletou minérios durante a mineração");
            }
        }
    }
}