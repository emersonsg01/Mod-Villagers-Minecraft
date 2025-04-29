package com.example.village;

import com.example.VillagerExpansionMod;
import com.example.village.builder.BuildingManager;
import com.example.village.exploration.ExplorationManager;
import com.example.village.mining.MiningManager;
import com.example.village.resources.ResourceManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerGossipType;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Classe principal que gerencia a expansão das vilas
 * Coordena a detecção de vilas, o comportamento dos villagers e a construção de novas estruturas
 */
public class VillageExpansionManager {
    
    private final Map<UUID, VillageData> villages = new HashMap<>();
    private final BuildingManager buildingManager;
    private final ResourceManager resourceManager;
    private final ExplorationManager explorationManager;
    private final MiningManager miningManager;
    private final com.example.village.profession.ProfessionManager professionManager;
    private final com.example.village.relation.VillageRelationManager villageRelationManager;
    
    // Contador para limitar a frequência de verificações
    private int tickCounter = 0;
    private static final int VILLAGE_CHECK_INTERVAL = 200; // A cada 10 segundos (20 ticks/segundo)
    
    public VillageExpansionManager() {
        this.buildingManager = new BuildingManager();
        this.resourceManager = new ResourceManager();
        this.explorationManager = new ExplorationManager();
        this.miningManager = new MiningManager();
        this.professionManager = new com.example.village.profession.ProfessionManager();
        this.villageRelationManager = new com.example.village.relation.VillageRelationManager();
        VillagerExpansionMod.LOGGER.info("VillageExpansionManager inicializado");
    }
    
    /**
     * Chamado quando um mundo é carregado
     */
    public void onWorldLoad(ServerWorld world) {
        // Carregar dados de vilas salvos ou inicializar novos
        VillagerExpansionMod.LOGGER.info("Carregando dados de vilas para o mundo");
        detectVillages(world);
    }
    
    /**
     * Chamado a cada tick do mundo
     */
    public void onWorldTick(ServerWorld world) {
        tickCounter++;
        
        // Limita a frequência de verificações para não sobrecarregar o servidor
        if (tickCounter % VILLAGE_CHECK_INTERVAL == 0) {
            updateVillages(world);
        }
        
        // Processa ações de construção em andamento
        buildingManager.processBuildingTasks(world);
        
        // Processa coleta de recursos
        resourceManager.processResourceCollection(world);
        
        // Processa exploração do mundo
        explorationManager.processExplorationTasks(world);
        
        // Processa atividades de mineração
        miningManager.processMiningTasks(world);
        
        // Processa profissões dos villagers
        professionManager.processProfessions(world);
        
        // Processa relações entre vilas e conflitos
        villageRelationManager.processVillageRelations(world);
    }
    
    /**
     * Detecta vilas existentes no mundo
     */
    private void detectVillages(ServerWorld world) {
        // Implementação básica: procura por villagers e agrupa-os em vilas
        // Em uma implementação completa, usaríamos o sistema de POI (Points of Interest) do Minecraft
        world.getEntitiesByType(net.minecraft.entity.EntityType.VILLAGER, entity -> true)
                .forEach(villager -> {
                    VillagerEntity villagerEntity = (VillagerEntity) villager;
                    BlockPos villagerPos = villagerEntity.getBlockPos();
                    
                    // Verifica se o villager já pertence a uma vila conhecida
                    boolean foundVillage = false;
                    for (VillageData village : villages.values()) {
                        if (village.isInRange(villagerPos)) {
                            village.addVillager(villagerEntity.getUuid());
                            foundVillage = true;
                            break;
                        }
                    }
                    
                    // Se não pertence a nenhuma vila, cria uma nova
                    if (!foundVillage) {
                        VillageData newVillage = new VillageData(UUID.randomUUID(), villagerPos);
                        newVillage.addVillager(villagerEntity.getUuid());
                        villages.put(newVillage.getVillageId(), newVillage);
                        VillagerExpansionMod.LOGGER.info("Nova vila detectada em: " + villagerPos);
                    }
                });
    }
    
    /**
     * Atualiza o estado das vilas
     */
    private void updateVillages(ServerWorld world) {
        // Atualiza cada vila
        villages.values().forEach(village -> {
            // Verifica necessidades da vila
            checkVillageNeeds(village, world);
            
            // Atualiza população
            updateVillagePopulation(village, world);
        });
    }
    
    /**
     * Verifica as necessidades da vila (novas casas, recursos, etc)
     */
    private void checkVillageNeeds(VillageData village, ServerWorld world) {
        // Verifica se a vila precisa de mais casas
        if (village.needsMoreHouses()) {
            BlockPos buildLocation = findBuildLocation(village, world);
            if (buildLocation != null) {
                // Inicia a construção de uma nova casa
                buildingManager.scheduleBuildTask(world, buildLocation, BuildingType.HOUSE, village);
                VillagerExpansionMod.LOGGER.info("Agendando construção de nova casa em: " + buildLocation);
            }
        }
        
        // Verifica se a vila precisa de mais fazendas
        if (village.needsMoreFarms()) {
            BlockPos farmLocation = findFarmLocation(village, world);
            if (farmLocation != null) {
                // Inicia a construção de uma nova fazenda
                buildingManager.scheduleBuildTask(world, farmLocation, BuildingType.FARM, village);
                VillagerExpansionMod.LOGGER.info("Agendando construção de nova fazenda em: " + farmLocation);
            }
        }
        
        // Verifica se a vila precisa de mais armazéns
        if (village.needsMoreStorage()) {
            BlockPos storageLocation = findStorageLocation(village, world);
            if (storageLocation != null) {
                // Inicia a construção de um novo armazém
                buildingManager.scheduleBuildTask(world, storageLocation, BuildingType.STORAGE, village);
                VillagerExpansionMod.LOGGER.info("Agendando construção de novo armazém em: " + storageLocation);
            }
        }
    }
    
    /**
     * Encontra um local adequado para construção
     */
    private BlockPos findBuildLocation(VillageData village, ServerWorld world) {
        // Implementação básica: procura por um espaço plano próximo ao centro da vila
        // Em uma implementação completa, usaríamos algoritmos mais sofisticados
        BlockPos center = village.getCenter();
        
        // Procura em espiral a partir do centro
        for (int radius = 5; radius <= 20; radius += 5) {
            for (int x = -radius; x <= radius; x += 5) {
                for (int z = -radius; z <= radius; z += 5) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue; // Apenas o perímetro
                    
                    BlockPos pos = center.add(x, 0, z);
                    // Ajusta a altura para encontrar o solo
                    pos = findSuitableGroundLevel(pos, world);
                    
                    if (pos != null && isSuitableBuildingLocation(pos, world)) {
                        return pos;
                    }
                }
            }
        }
        
        return null; // Não encontrou local adequado
    }
    
    /**
     * Encontra um local adequado para fazenda
     */
    private BlockPos findFarmLocation(VillageData village, ServerWorld world) {
        // Similar ao findBuildLocation, mas com critérios específicos para fazendas
        // (proximidade à água, terreno plano, etc.)
        return findBuildLocation(village, world); // Simplificado para este exemplo
    }
    
    /**
     * Encontra um local adequado para armazém
     */
    private BlockPos findStorageLocation(VillageData village, ServerWorld world) {
        // Similar ao findBuildLocation, mas com critérios específicos para armazéns
        // (proximidade ao centro da vila, etc.)
        return findBuildLocation(village, world); // Simplificado para este exemplo
    }
    
    /**
     * Encontra o nível do solo adequado para uma posição
     */
    private BlockPos findSuitableGroundLevel(BlockPos pos, ServerWorld world) {
        // Procura do topo para baixo por um bloco sólido com espaço livre acima
        BlockPos.Mutable mutablePos = new BlockPos.Mutable(pos.getX(), world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()), pos.getZ());
        
        while (mutablePos.getY() > world.getBottomY()) {
            mutablePos.move(0, -1, 0);
            if (world.getBlockState(mutablePos).isSolid() && 
                !world.getBlockState(mutablePos.up()).isSolid() && 
                !world.getBlockState(mutablePos.up(2)).isSolid()) {
                return mutablePos.up();
            }
        }
        
        return null; // Não encontrou um nível adequado
    }
    
    /**
     * Verifica se uma localização é adequada para construção
     */
    private boolean isSuitableBuildingLocation(BlockPos pos, ServerWorld world) {
        // Verifica se há espaço suficiente e se o terreno é adequado
        // Implementação básica: verifica se há um espaço 5x5x5 livre
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (world.getBlockState(checkPos).isSolid() && y > 0) {
                        return false; // Há um bloco sólido no caminho
                    }
                }
            }
        }
        
        // Verifica se o terreno abaixo é sólido
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = pos.add(x, -1, z);
                if (!world.getBlockState(checkPos).isSolid()) {
                    return false; // O terreno não é sólido
                }
            }
        }
        
        return true;
    }
    
    /**
     * Atualiza a população da vila
     */
    private void updateVillagePopulation(VillageData village, ServerWorld world) {
        // Verifica apenas se há recursos para novos villagers, sem limite de camas
        if (village.canGrow()) {
            // Chance aumentada de criar um novo villager
            if (world.getRandom().nextFloat() < 0.2f) { // 20% de chance a cada verificação (aumentado de 10%)
                // Em uma implementação completa, usaríamos o sistema de reprodução do Minecraft
                VillagerExpansionMod.LOGGER.info("Vila crescendo! Novo villager será adicionado.");
                village.incrementPopulation();
                
                // Verifica se precisa construir mais casas após o crescimento
                if (village.needsMoreHouses()) {
                    VillagerExpansionMod.LOGGER.info("População crescendo, necessário construir mais casas!");
                }
            }
        }
    }
    
    /**
     * Obtém os dados de uma vila pelo ID
     */
    public VillageData getVillage(UUID villageId) {
        return villages.get(villageId);
    }
    
    /**
     * Obtém todas as vilas gerenciadas
     */
    public Iterable<VillageData> getVillages() {
        return villages.values();
    }
    
    /**
     * Obtém o gerenciador de construção
     */
    public BuildingManager getBuildingManager() {
        return buildingManager;
    }
    
    /**
     * Obtém o gerenciador de recursos
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }
    
    /**
     * Obtém o gerenciador de exploração
     */
    public ExplorationManager getExplorationManager() {
        return explorationManager;
    }
    
    /**
     * Obtém o gerenciador de mineração
     */
    public MiningManager getMiningManager() {
        return miningManager;
    }
    
    /**
     * Obtém o gerenciador de profissões
     * @return O gerenciador de profissões
     */
    public com.example.village.profession.ProfessionManager getProfessionManager() {
        return professionManager;
    }
    
    /**
     * Obtém o gerenciador de relações entre vilas
     * @return O gerenciador de relações entre vilas
     */
    public com.example.village.relation.VillageRelationManager getVillageRelationManager() {
        return villageRelationManager;
    }
}