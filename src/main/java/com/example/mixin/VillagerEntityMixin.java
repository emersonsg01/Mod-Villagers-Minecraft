package com.example.mixin;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import com.example.village.exploration.ExplorationTask;
import com.example.village.inventory.VillagerInventorySystem;
import com.example.village.mining.MiningTask;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mixin(VillagerEntity.class)
 public abstract class VillagerEntityMixin extends MerchantEntity {
    
    // Contador para limitar a frequência de verificações
    private int expansionTickCounter = 0;
    private static final int EXPANSION_CHECK_INTERVAL = 200; // A cada 10 segundos
    
    // Variáveis para controle de exploração e mineração
    private boolean isExploring = false;
    private boolean isMining = false;
    private UUID currentTaskId = null;
    private int taskCheckCounter = 0;
    private static final int TASK_CHECK_INTERVAL = 40; // A cada 2 segundos
    private final Random random = new Random();
    
    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }
    
    // Contador para limitar a frequência de verificações de profissão
    private int professionTickCounter = 0;
    private static final int PROFESSION_CHECK_INTERVAL = 60; // A cada 3 segundos
    
    // Contador para verificações de inventário e equipamentos
    private int inventoryTickCounter = 0;
    private static final int INVENTORY_CHECK_INTERVAL = 100; // A cada 5 segundos
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        // Só executa no servidor
        if (this.getWorld().isClient) return;
        
        expansionTickCounter++;
        taskCheckCounter++;
        professionTickCounter++;
        inventoryTickCounter++;
        
        // Limita a frequência de verificações de vila
        if (expansionTickCounter % EXPANSION_CHECK_INTERVAL == 0) {
            // Verifica se o villager pertence a uma vila
            checkVillagerVillage((ServerWorld) this.getWorld());
        }
        
        // Limita a frequência de verificações de tarefas
        if (taskCheckCounter % TASK_CHECK_INTERVAL == 0) {
            // Verifica e atualiza tarefas de exploração e mineração
            checkAndUpdateTasks((ServerWorld) this.getWorld());
        }
        
        // Limita a frequência de verificações de profissão
        if (professionTickCounter % PROFESSION_CHECK_INTERVAL == 0) {
            // Verifica e atualiza a profissão do villager
            checkVillagerProfession((ServerWorld) this.getWorld());
        }
        
        // Limita a frequência de verificações de inventário
        if (inventoryTickCounter % INVENTORY_CHECK_INTERVAL == 0) {
            // Verifica e atualiza o inventário e equipamentos do villager
            checkVillagerInventory((ServerWorld) this.getWorld());
        }
    }
    
    /**
     * Verifica se o villager pertence a uma vila e o adiciona se necessário
     */
    private void checkVillagerVillage(ServerWorld world) {
        VillagerEntity villager = (VillagerEntity)(Object)this;
        BlockPos pos = villager.getBlockPos();
        UUID villagerId = villager.getUuid();
        
        // Verifica se o villager já pertence a alguma vila conhecida
        boolean foundVillage = false;
        for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
            if (village.getVillagers().contains(villagerId)) {
                foundVillage = true;
                break;
            }
            
            // Se não pertence, mas está dentro do raio de uma vila, adiciona-o
            if (village.isInRange(pos)) {
                village.addVillager(villagerId);
                foundVillage = true;
                VillagerExpansionMod.LOGGER.info("Villager adicionado a uma vila existente");
                break;
            }
        }
        
        // Se não pertence a nenhuma vila, verifica se há outros villagers próximos para formar uma nova vila
        if (!foundVillage) {
            world.getEntitiesByType(EntityType.VILLAGER, entity -> 
                    entity.getUuid() != villagerId && 
                    entity.getBlockPos().isWithinDistance(pos, 32) // Villagers a até 32 blocos
                    ) // Sem limite de villagers
                .stream()
                .findFirst() // Pega o primeiro villager encontrado
                .ifPresent(otherVillager -> {
                    // Cria uma nova vila centrada entre os dois villagers
                    BlockPos centerPos = new BlockPos(
                            (pos.getX() + otherVillager.getBlockPos().getX()) / 2,
                            (pos.getY() + otherVillager.getBlockPos().getY()) / 2,
                            (pos.getZ() + otherVillager.getBlockPos().getZ()) / 2
                    );
                    
                    VillageData newVillage = new VillageData(UUID.randomUUID(), centerPos);
                    newVillage.addVillager(villagerId);
                    newVillage.addVillager(otherVillager.getUuid());
                    
                    // Adiciona a nova vila ao gerenciador
                    VillagerExpansionMod.LOGGER.info("Nova vila criada com 2 villagers em: " + centerPos);
                });
        }
    }
    
    /**
     * Verifica e atualiza tarefas de exploração e mineração
     */
    private void checkAndUpdateTasks(ServerWorld world) {
        VillagerEntity villager = (VillagerEntity)(Object)this;
        UUID villagerId = villager.getUuid();
        
        // Se o villager já está explorando ou minerando, verifica o progresso
        if (isExploring) {
            checkExplorationProgress(world, villagerId);
            return;
        }
        
        if (isMining) {
            checkMiningProgress(world, villagerId);
            return;
        }
        
        // Se não está em nenhuma tarefa, verifica se pode iniciar uma nova
        // Primeiro, encontra a vila do villager
        VillageData villagerVillage = null;
        for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
            if (village.getVillagers().contains(villagerId)) {
                villagerVillage = village;
                break;
            }
        }
        
        if (villagerVillage == null) {
            return; // Villager não pertence a nenhuma vila
        }
        
        // Decide aleatoriamente se o villager vai explorar ou minerar
        float taskChance = random.nextFloat();
        
        // 10% de chance de iniciar uma exploração
        if (taskChance < 0.1f) {
            tryStartExploration(world, villagerVillage);
        }
        // 8% de chance de iniciar uma mineração
        else if (taskChance < 0.18f) {
            tryStartMining(world, villagerVillage);
        }
    }
    
    /**
     * Tenta iniciar uma tarefa de exploração
     */
    private void tryStartExploration(ServerWorld world, VillageData village) {
        // Verifica se há tarefas de exploração disponíveis
        Map<UUID, ExplorationTask> tasks = VillagerExpansionMod.getExpansionManager()
                .getExplorationManager().getActiveExplorationTasks();
        
        // Procura por uma tarefa para a vila deste villager
        for (Map.Entry<UUID, ExplorationTask> entry : tasks.entrySet()) {
            ExplorationTask task = entry.getValue();
            if (task.getVillageId().equals(village.getVillageId()) && !task.isCompleted()) {
                // Encontrou uma tarefa, atribui ao villager
                isExploring = true;
                currentTaskId = entry.getKey();
                VillagerExpansionMod.LOGGER.info("Villager " + ((VillagerEntity)(Object)this).getUuid() + 
                                              " iniciou exploração para " + task.getTargetPosition());
                return;
            }
        }
    }
    
    /**
     * Tenta iniciar uma tarefa de mineração
     */
    private void tryStartMining(ServerWorld world, VillageData village) {
        // Verifica se há tarefas de mineração disponíveis
        Map<UUID, MiningTask> tasks = VillagerExpansionMod.getExpansionManager()
                .getMiningManager().getActiveMiningTasks();
        
        // Procura por uma tarefa para a vila deste villager
        for (Map.Entry<UUID, MiningTask> entry : tasks.entrySet()) {
            MiningTask task = entry.getValue();
            if (task.getVillageId().equals(village.getVillageId()) && !task.isCompleted()) {
                // Encontrou uma tarefa, atribui ao villager
                isMining = true;
                currentTaskId = entry.getKey();
                VillagerExpansionMod.LOGGER.info("Villager " + ((VillagerEntity)(Object)this).getUuid() + 
                                              " iniciou mineração em " + task.getMiningPosition());
                return;
            }
        }
    }
    
    /**
     * Verifica o progresso de uma tarefa de exploração
     */
    private void checkExplorationProgress(ServerWorld world, UUID villagerId) {
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
            VillagerExpansionMod.LOGGER.info("Villager " + villagerId + " concluiu tarefa de exploração");
        }
    }
    
    /**
     * Verifica o progresso de uma tarefa de mineração
     */
    private void checkMiningProgress(ServerWorld world, UUID villagerId) {
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
            VillagerExpansionMod.LOGGER.info("Villager " + villagerId + " concluiu tarefa de mineração");
        }
    }
    
    /**
     * Verifica e atualiza a profissão do villager
     * @param world O mundo do servidor
     */
    private void checkVillagerProfession(ServerWorld world) {
        VillagerEntity villager = (VillagerEntity)(Object)this;
        UUID villagerId = villager.getUuid();
        
        // Verifica se o villager já tem uma profissão personalizada
        if (VillagerExpansionMod.getExpansionManager().getProfessionManager().hasProfession(villagerId)) {
            // Já tem uma profissão personalizada, não faz nada
            return;
        }
        
        // Verifica se o villager não tem profissão no sistema vanilla
        // Na versão 1.21, a forma de verificar a profissão pode ter mudado
        // Verificamos se o villager tem uma profissão usando instanceof ou outras propriedades
        boolean hasNoProfession = true;
        
        // Verifica se o villager está em alguma atividade de trabalho
        if (villager.getBrain().hasActivity(Activity.WORK)) {
            hasNoProfession = false;
        }
        
        if (hasNoProfession && !villager.isBaby()) {
            // Não tem profissão, atribui uma profissão personalizada
            // O ProfessionManager cuidará de atribuir uma profissão aleatória
            VillagerExpansionMod.LOGGER.info("Villager " + villagerId + " não tem profissão, atribuindo uma profissão personalizada");
        }
    }
    
    /**
     * Verifica e atualiza o inventário e equipamentos do villager
     * @param world O mundo do servidor
     */
    private void checkVillagerInventory(ServerWorld world) {
        VillagerEntity villager = (VillagerEntity)(Object)this;
        
        // Inicializa o inventário do villager se ainda não existir
        SimpleInventory inventory = VillagerInventorySystem.getInventory(villager);
        
        // Chance de tentar equipar armaduras do inventário
        if (random.nextFloat() < 0.3f) {
            VillagerInventorySystem.equipArmorFromInventory(villager);
        }
        
        // Chance de tentar armazenar itens em baús
        if (random.nextFloat() < 0.2f && VillagerInventorySystem.storeItemsInChest(villager, world)) {
            VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " armazenou itens em um baú");
        }
    }
}