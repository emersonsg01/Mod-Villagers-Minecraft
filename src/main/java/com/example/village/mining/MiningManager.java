package com.example.village.mining;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gerencia as atividades de mineração dos villagers
 * Coordena a busca por minérios, escavação e coleta de recursos minerais
 */
public class MiningManager {
    private final Map<UUID, MiningTask> activeMiningTasks = new HashMap<>();
    private final Random random = new Random();
    
    // Contador para limitar a frequência de mineração
    private int miningTickCounter = 0;
    private static final int MINING_TICK_INTERVAL = 100; // A cada 5 segundos (20 ticks/segundo)
    
    /**
     * Processa as tarefas de mineração
     */
    public void processMiningTasks(ServerWorld world) {
        miningTickCounter++;
        
        // Limita a frequência de mineração para não sobrecarregar o servidor
        if (miningTickCounter % MINING_TICK_INTERVAL != 0) {
            return;
        }
        
        // Para cada vila, verifica se é necessário iniciar novas minerações
        for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
            checkVillageMining(village, world);
        }
        
        // Processa tarefas de mineração em andamento
        processActiveTasks(world);
    }
    
    /**
     * Verifica se uma vila precisa iniciar novas minerações
     */
    private void checkVillageMining(VillageData village, ServerWorld world) {
        // Verifica se a vila tem villagers disponíveis para mineração
        // Idealmente, apenas uma pequena porcentagem dos villagers deve minerar
        int availableMiners = Math.max(1, village.getPopulation() / 6); // ~16% dos villagers
        
        // Conta quantos mineradores já estão ativos para esta vila
        long activeMiners = activeMiningTasks.values().stream()
                .filter(task -> task.getVillageId().equals(village.getVillageId()))
                .count();
        
        // Se já temos mineradores suficientes, não inicia novas minerações
        if (activeMiners >= availableMiners) {
            return;
        }
        
        // Chance de iniciar uma nova mineração
        if (world.getRandom().nextFloat() < 0.25f) { // 25% de chance
            // Procura por locais de mineração conhecidos (cavernas descobertas)
            BlockPos miningLocation = findMiningLocation(village, world);
            if (miningLocation != null) {
                // Inicia uma nova tarefa de mineração
                startMiningTask(village, world, miningLocation);
            }
        }
    }
    
    /**
     * Encontra um local para mineração
     */
    private BlockPos findMiningLocation(VillageData village, ServerWorld world) {
        // Primeiro, verifica se a vila já descobriu alguma caverna
        for (UUID locationId : village.getDiscoveredLocations()) {
            if ("cave_entrance".equals(village.getDiscoveredLocationType(locationId))) {
                return village.getDiscoveredLocationPosition(locationId);
            }
        }
        
        // Se não encontrou cavernas conhecidas, procura por locais potenciais de mineração
        BlockPos center = village.getCenter();
        int searchRadius = 48; // Raio de busca em blocos
        
        // Procura em espiral a partir do centro
        for (int radius = 16; radius <= searchRadius; radius += 8) {
            for (int x = -radius; x <= radius; x += 8) {
                for (int z = -radius; z <= radius; z += 8) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue; // Apenas o perímetro
                    
                    BlockPos pos = center.add(x, 0, z);
                    // Ajusta para a superfície
                    int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ());
                    pos = new BlockPos(pos.getX(), y, pos.getZ());
                    
                    // Verifica se há uma caverna ou minérios próximos
                    if (hasMiningPotential(pos, world)) {
                        return pos;
                    }
                }
            }
        }
        
        return null; // Não encontrou local adequado
    }
    
    /**
     * Verifica se uma localização tem potencial para mineração
     */
    private boolean hasMiningPotential(BlockPos pos, ServerWorld world) {
        // Verifica se há uma caverna próxima
        if (hasCaveNearby(world, pos)) {
            return true;
        }
        
        // Verifica se há minérios expostos próximos
        return hasExposedOres(world, pos);
    }
    
    /**
     * Verifica se há uma caverna próxima
     */
    private boolean hasCaveNearby(ServerWorld world, BlockPos pos) {
        // Verifica se há espaços vazios abaixo da superfície
        int airCount = 0;
        int minAirCount = 10;
        
        // Verifica blocos abaixo da superfície
        for (int y = 5; y <= 30; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = pos.add(x, -y, z);
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
     * Verifica se há minérios expostos próximos
     */
    private boolean hasExposedOres(ServerWorld world, BlockPos pos) {
        // Blocos de minério para procurar
        Block[] oreBlocks = new Block[]{
            Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, 
            Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.COPPER_ORE,
            Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_COPPER_ORE
        };
        
        // Verifica em um raio ao redor da posição
        for (int y = 1; y <= 20; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = pos.add(x, -y, z);
                    Block block = world.getBlockState(checkPos).getBlock();
                    
                    for (Block oreBlock : oreBlocks) {
                        if (block == oreBlock) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Inicia uma tarefa de mineração
     */
    private void startMiningTask(VillageData village, ServerWorld world, BlockPos location) {
        // Cria uma nova tarefa de mineração
        MiningTask task = new MiningTask(village.getVillageId(), location);
        UUID taskId = UUID.randomUUID();
        activeMiningTasks.put(taskId, task);
        
        VillagerExpansionMod.LOGGER.info("Iniciando mineração para a vila " + village.getVillageId() + 
                                       " em " + location);
    }
    
    /**
     * Processa as tarefas de mineração ativas
     */
    private void processActiveTasks(ServerWorld world) {
        // Lista para armazenar tarefas concluídas
        Map<UUID, MiningTask> completedTasks = new HashMap<>();
        
        // Processa cada tarefa de mineração
        for (Map.Entry<UUID, MiningTask> entry : activeMiningTasks.entrySet()) {
            UUID taskId = entry.getKey();
            MiningTask task = entry.getValue();
            
            // Incrementa o progresso da mineração
            int progressAmount = random.nextInt(8) + 3; // 3-10 de progresso por tick
            boolean completed = task.incrementProgress(progressAmount);
            
            if (completed) {
                completedTasks.put(taskId, task);
            }
        }
        
        // Finaliza as tarefas concluídas
        for (Map.Entry<UUID, MiningTask> entry : completedTasks.entrySet()) {
            UUID taskId = entry.getKey();
            MiningTask task = entry.getValue();
            
            completeMiningTask(taskId, task, world);
            activeMiningTasks.remove(taskId);
        }
    }
    
    /**
     * Completa uma tarefa de mineração
     */
    private void completeMiningTask(UUID taskId, MiningTask task, ServerWorld world) {
        // Obtém a vila associada à tarefa
        UUID villageId = task.getVillageId();
        VillageData village = VillagerExpansionMod.getExpansionManager().getVillage(villageId);
        
        if (village == null) {
            VillagerExpansionMod.LOGGER.warn("Vila não encontrada ao completar mineração: " + villageId);
            return;
        }
        
        // Determina os recursos obtidos na mineração
        int coal = 0, iron = 0, gold = 0, diamond = 0;
        int stone = random.nextInt(10) + 5; // 5-15 pedras sempre
        
        // Chance de encontrar carvão (alta)
        if (random.nextFloat() < 0.8f) {
            coal = random.nextInt(5) + 1; // 1-5 carvão
        }
        
        // Chance de encontrar ferro (média)
        if (random.nextFloat() < 0.5f) {
            iron = random.nextInt(3) + 1; // 1-3 ferro
        }
        
        // Chance de encontrar ouro (baixa)
        if (random.nextFloat() < 0.2f) {
            gold = random.nextInt(2) + 1; // 1-2 ouro
        }
        
        // Chance de encontrar diamante (muito baixa)
        if (random.nextFloat() < 0.05f) {
            diamond = 1; // 1 diamante
        }
        
        // Adiciona os recursos à vila
        village.addResources(0, stone, 0); // Adiciona pedra
        village.addMineralResources(coal, iron, gold, diamond); // Adiciona minérios
        
        VillagerExpansionMod.LOGGER.info("Mineração concluída! Obtido: " + 
                                       coal + " carvão, " + 
                                       iron + " ferro, " + 
                                       gold + " ouro, " + 
                                       diamond + " diamante, " + 
                                       stone + " pedra");
        
        // Chance de descobrir uma nova caverna durante a mineração
        if (random.nextFloat() < 0.1f) { // 10% de chance
            // Cria uma nova localização descoberta para a caverna
            UUID locationId = UUID.randomUUID();
            BlockPos cavePos = task.getMiningPosition().add(
                    random.nextInt(16) - 8,
                    -random.nextInt(5) - 3,
                    random.nextInt(16) - 8
            );
            
            village.addDiscoveredLocation(locationId, cavePos, "deep_cave");
            VillagerExpansionMod.LOGGER.info("Descoberta nova caverna profunda durante mineração em " + cavePos);
        }
    }
    
    /**
     * Obtém uma tarefa de mineração ativa
     */
    public MiningTask getMiningTask(UUID taskId) {
        return activeMiningTasks.get(taskId);
    }
    
    /**
     * Obtém todas as tarefas de mineração ativas
     */
    public Map<UUID, MiningTask> getActiveMiningTasks() {
        return new HashMap<>(activeMiningTasks);
    }
}