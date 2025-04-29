package com.example.village.exploration;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gerencia a exploração do mundo pelos villagers
 * Coordena a descoberta de novas áreas, pontos de interesse e recursos
 */
public class ExplorationManager {
    private final Map<UUID, ExplorationTask> activeExplorationTasks = new HashMap<>();
    private final Random random = new Random();
    
    // Contador para limitar a frequência de exploração
    private int explorationTickCounter = 0;
    private static final int EXPLORATION_TICK_INTERVAL = 200; // A cada 10 segundos (20 ticks/segundo)
    
    /**
     * Processa as tarefas de exploração
     */
    public void processExplorationTasks(ServerWorld world) {
        explorationTickCounter++;
        
        // Limita a frequência de exploração para não sobrecarregar o servidor
        if (explorationTickCounter % EXPLORATION_TICK_INTERVAL != 0) {
            return;
        }
        
        // Para cada vila, verifica se é necessário iniciar novas explorações
        for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
            checkVillageExploration(village, world);
        }
        
        // Processa tarefas de exploração em andamento
        processActiveTasks(world);
    }
    
    /**
     * Verifica se uma vila precisa iniciar novas explorações
     */
    private void checkVillageExploration(VillageData village, ServerWorld world) {
        // Verifica se a vila tem villagers disponíveis para exploração
        // Idealmente, apenas uma pequena porcentagem dos villagers deve explorar
        int availableExplorers = Math.max(1, village.getPopulation() / 5); // 20% dos villagers
        
        // Conta quantos exploradores já estão ativos para esta vila
        long activeExplorers = activeExplorationTasks.values().stream()
                .filter(task -> task.getVillageId().equals(village.getVillageId()))
                .count();
        
        // Se já temos exploradores suficientes, não inicia novas explorações
        if (activeExplorers >= availableExplorers) {
            return;
        }
        
        // Chance de iniciar uma nova exploração
        if (world.getRandom().nextFloat() < 0.3f) { // 30% de chance
            // Encontra uma direção para explorar
            BlockPos explorationTarget = findExplorationTarget(village, world);
            if (explorationTarget != null) {
                // Inicia uma nova tarefa de exploração
                startExplorationTask(village, world, explorationTarget);
            }
        }
    }
    
    /**
     * Encontra um alvo para exploração
     */
    private BlockPos findExplorationTarget(VillageData village, ServerWorld world) {
        BlockPos center = village.getCenter();
        int minDistance = 64; // Distância mínima da vila
        int maxDistance = 256; // Distância máxima da vila
        
        // Escolhe uma direção aleatória
        double angle = random.nextDouble() * Math.PI * 2;
        int distance = minDistance + random.nextInt(maxDistance - minDistance);
        
        int x = center.getX() + (int)(Math.cos(angle) * distance);
        int z = center.getZ() + (int)(Math.sin(angle) * distance);
        
        // Encontra a altura do terreno nesta posição
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos target = new BlockPos(x, y, z);
        
        // Verifica se o alvo não está muito próximo de uma localização já descoberta
        if (village.isNearDiscoveredLocation(target, 32)) {
            return null; // Muito próximo de algo já descoberto
        }
        
        return target;
    }
    
    /**
     * Inicia uma tarefa de exploração
     */
    private void startExplorationTask(VillageData village, ServerWorld world, BlockPos target) {
        // Cria uma nova tarefa de exploração
        ExplorationTask task = new ExplorationTask(village.getVillageId(), target);
        UUID taskId = UUID.randomUUID();
        activeExplorationTasks.put(taskId, task);
        
        VillagerExpansionMod.LOGGER.info("Iniciando exploração para a vila " + village.getVillageId() + 
                                       " em direção a " + target);
    }
    
    /**
     * Processa as tarefas de exploração ativas
     */
    private void processActiveTasks(ServerWorld world) {
        // Lista para armazenar tarefas concluídas
        Map<UUID, ExplorationTask> completedTasks = new HashMap<>();
        
        // Processa cada tarefa de exploração
        for (Map.Entry<UUID, ExplorationTask> entry : activeExplorationTasks.entrySet()) {
            UUID taskId = entry.getKey();
            ExplorationTask task = entry.getValue();
            
            // Incrementa o progresso da exploração
            int progressAmount = random.nextInt(10) + 5; // 5-15 de progresso por tick
            boolean completed = task.incrementProgress(progressAmount);
            
            if (completed) {
                completedTasks.put(taskId, task);
            }
        }
        
        // Finaliza as tarefas concluídas
        for (Map.Entry<UUID, ExplorationTask> entry : completedTasks.entrySet()) {
            UUID taskId = entry.getKey();
            ExplorationTask task = entry.getValue();
            
            completeExplorationTask(taskId, task, world);
            activeExplorationTasks.remove(taskId);
        }
    }
    
    /**
     * Completa uma tarefa de exploração
     */
    private void completeExplorationTask(UUID taskId, ExplorationTask task, ServerWorld world) {
        // Obtém a vila associada à tarefa
        UUID villageId = task.getVillageId();
        VillageData village = VillagerExpansionMod.getExpansionManager().getVillage(villageId);
        
        if (village == null) {
            VillagerExpansionMod.LOGGER.warn("Vila não encontrada ao completar exploração: " + villageId);
            return;
        }
        
        // Verifica o que foi encontrado na exploração
        BlockPos target = task.getTargetPosition();
        String discoveryType = determineDiscoveryType(target, world);
        
        // Registra a descoberta na vila
        if (discoveryType != null) {
            village.addDiscoveredLocation(taskId, target, discoveryType);
            VillagerExpansionMod.LOGGER.info("Exploração concluída! Descoberto: " + discoveryType + " em " + target);
            
            // Adiciona recursos baseados no que foi descoberto
            addResourcesBasedOnDiscovery(village, discoveryType);
        } else {
            VillagerExpansionMod.LOGGER.info("Exploração concluída, mas nada interessante foi encontrado em " + target);
        }
    }
    
    /**
     * Determina o tipo de descoberta baseado na localização
     */
    private String determineDiscoveryType(BlockPos pos, ServerWorld world) {
        // Verifica um raio ao redor da posição alvo
        int radius = 16;
        
        // Estruturas naturais
        if (hasBlocksInArea(world, pos, radius, Blocks.WATER, 20)) {
            return "lake";
        }
        
        if (hasBlocksInArea(world, pos, radius, Blocks.LAVA, 5)) {
            return "lava_pool";
        }
        
        if (hasBlocksInArea(world, pos, radius, new Block[]{Blocks.OAK_LOG, Blocks.BIRCH_LOG, 
                Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG}, 15)) {
            return "forest";
        }
        
        if (hasBlocksInArea(world, pos, radius, Blocks.SAND, 30)) {
            return "desert";
        }
        
        if (hasBlocksInArea(world, pos, radius, Blocks.SNOW_BLOCK, 20)) {
            return "snow_biome";
        }
        
        if (world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()) > 100) {
            return "mountain";
        }
        
        // Verifica se há cavernas próximas
        if (hasCaveNearby(world, pos)) {
            return "cave_entrance";
        }
        
        // Não encontrou nada interessante
        return null;
    }
    
    /**
     * Verifica se há uma quantidade mínima de um tipo de bloco em uma área
     */
    private boolean hasBlocksInArea(ServerWorld world, BlockPos center, int radius, Block block, int minCount) {
        return hasBlocksInArea(world, center, radius, new Block[]{block}, minCount);
    }
    
    /**
     * Verifica se há uma quantidade mínima de blocos de um conjunto em uma área
     */
    private boolean hasBlocksInArea(ServerWorld world, BlockPos center, int radius, Block[] blocks, int minCount) {
        int count = 0;
        
        for (int x = -radius; x <= radius; x += 4) { // Amostragem a cada 4 blocos para performance
            for (int z = -radius; z <= radius; z += 4) {
                BlockPos checkPos = center.add(x, 0, z);
                // Ajusta para a superfície
                checkPos = new BlockPos(checkPos.getX(), 
                                       world.getTopY(Heightmap.Type.WORLD_SURFACE, checkPos.getX(), checkPos.getZ()), 
                                       checkPos.getZ());
                
                Block block = world.getBlockState(checkPos).getBlock();
                for (Block targetBlock : blocks) {
                    if (block == targetBlock) {
                        count++;
                        break;
                    }
                }
                
                if (count >= minCount) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Verifica se há uma caverna próxima
     */
    private boolean hasCaveNearby(ServerWorld world, BlockPos pos) {
        // Verifica se há espaços vazios abaixo da superfície
        int airCount = 0;
        int minAirCount = 10;
        
        BlockPos surfacePos = new BlockPos(pos.getX(), 
                                          world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()), 
                                          pos.getZ());
        
        // Verifica blocos abaixo da superfície
        for (int y = 1; y <= 20; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = surfacePos.add(x, -y, z);
                    if (world.isAir(checkPos)) {
                        airCount++;
                        
                        if (airCount >= minAirCount) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Adiciona recursos à vila baseado no tipo de descoberta
     */
    private void addResourcesBasedOnDiscovery(VillageData village, String discoveryType) {
        switch (discoveryType) {
            case "forest":
                village.addResources(10, 0, 5); // Madeira e alguma comida
                break;
            case "lake":
                village.addResources(0, 0, 8); // Comida (peixe)
                break;
            case "mountain":
                village.addResources(0, 8, 0); // Pedra
                break;
            case "cave_entrance":
                // Chance de encontrar minérios
                if (random.nextFloat() < 0.3f) {
                    village.addMineralResources(2, 1, 0, 0); // Carvão e ferro
                }
                break;
        }
    }
    
    /**
     * Obtém uma tarefa de exploração ativa
     */
    public ExplorationTask getExplorationTask(UUID taskId) {
        return activeExplorationTasks.get(taskId);
    }
    
    /**
     * Obtém todas as tarefas de exploração ativas
     */
    public Map<UUID, ExplorationTask> getActiveExplorationTasks() {
        return new HashMap<>(activeExplorationTasks);
    }
}