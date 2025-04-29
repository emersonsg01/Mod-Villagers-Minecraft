package com.example.village;

import com.example.VillagerExpansionMod;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Classe que armazena dados sobre uma vila
 * Inclui informações sobre localização, população, estruturas e recursos
 */
public class VillageData {
    private final UUID villageId;
    private BlockPos center;
    private final Set<UUID> villagers = new HashSet<>();
    private final List<BuildingData> buildings = new ArrayList<>();
    
    // Estatísticas da vila
    private int population = 0;
    private int bedCount = 0;
    private int farmCount = 0;
    private int storageCount = 0;
    
    // Limites e raios
    private static final int VILLAGE_RADIUS = 64;
    private static final int MAX_POPULATION_PER_BED = 2;
    
    // Recursos da vila
    private int woodResource = 0;
    private int stoneResource = 0;
    private int foodResource = 0;
    private int coalResource = 0;
    private int ironResource = 0;
    private int goldResource = 0;
    private int diamondResource = 0;
    
    // Localizações descobertas pelos villagers exploradores
    private final Set<UUID> discoveredLocations = new HashSet<>();
    private final Map<UUID, BlockPos> discoveredLocationPositions = new HashMap<>();
    private final Map<UUID, String> discoveredLocationTypes = new HashMap<>();
    
    public VillageData(UUID villageId, BlockPos center) {
        this.villageId = villageId;
        this.center = center;
        VillagerExpansionMod.LOGGER.info("Nova vila criada com ID: " + villageId + " no centro: " + center);
    }
    
    /**
     * Verifica se uma posição está dentro do raio da vila
     */
    public boolean isInRange(BlockPos pos) {
        return pos.isWithinDistance(center, VILLAGE_RADIUS);
    }
    
    /**
     * Adiciona um villager à vila
     */
    public void addVillager(UUID villagerId) {
        if (!villagers.contains(villagerId)) {
            villagers.add(villagerId);
            population++;
            VillagerExpansionMod.LOGGER.info("Villager adicionado à vila. População atual: " + population);
        }
    }
    
    /**
     * Remove um villager da vila
     */
    public void removeVillager(UUID villagerId) {
        if (villagers.remove(villagerId)) {
            population--;
            VillagerExpansionMod.LOGGER.info("Villager removido da vila. População atual: " + population);
        }
    }
    
    /**
     * Adiciona uma nova construção à vila
     */
    public void addBuilding(BuildingData building) {
        buildings.add(building);
        
        // Atualiza contadores baseados no tipo de construção
        switch (building.getType()) {
            case HOUSE:
                bedCount += building.getBedCount();
                break;
            case FARM:
                farmCount++;
                break;
            case STORAGE:
                storageCount++;
                break;
        }
        
        VillagerExpansionMod.LOGGER.info("Nova construção adicionada à vila: " + building.getType());
    }
    
    /**
     * Verifica se a vila precisa de mais casas
     */
    public boolean needsMoreHouses() {
        // Sempre precisa de mais casas para expansão contínua
        // Prioriza construção quando a população está próxima da capacidade atual
        return population >= bedCount * 0.5; // Reduzido para 50% para iniciar construção mais cedo
    }
    
    /**
     * Verifica se a vila precisa de mais fazendas
     */
    public boolean needsMoreFarms() {
        // Uma fazenda para cada 4 villagers
        return farmCount < Math.ceil(population / 4.0);
    }
    
    /**
     * Verifica se a vila precisa de mais armazéns
     */
    public boolean needsMoreStorage() {
        // Um armazém para cada 6 villagers
        return storageCount < Math.ceil(population / 6.0);
    }
    
    /**
     * Verifica se a vila pode crescer (tem recursos)
     */
    public boolean canGrow() {
        // Pode crescer se há comida suficiente, sem limite de camas
        // Apenas verifica se há recursos alimentícios disponíveis
        return foodResource >= 10;
    }
    
    /**
     * Obtém o número de camas disponíveis
     * Modificado para sempre retornar um valor positivo, permitindo crescimento ilimitado
     */
    public int getAvailableBeds() {
        // Retorna um valor positivo mesmo quando a população excede o número de camas
        // Isso permite que a vila continue crescendo sem limite
        int normalBeds = bedCount * MAX_POPULATION_PER_BED - population;
        return normalBeds > 0 ? normalBeds : 1; // Sempre permite pelo menos um novo villager
    }
    
    /**
     * Incrementa a população da vila
     */
    public void incrementPopulation() {
        population++;
        // Consome recursos para o novo villager
        foodResource -= 10;
    }
    
    /**
     * Adiciona recursos à vila
     */
    public void addResources(int wood, int stone, int food) {
        woodResource += wood;
        stoneResource += stone;
        foodResource += food;
    }
    
    /**
     * Adiciona recursos minerais à vila
     */
    public void addMineralResources(int coal, int iron, int gold, int diamond) {
        coalResource += coal;
        ironResource += iron;
        goldResource += gold;
        diamondResource += diamond;
        VillagerExpansionMod.LOGGER.info("Recursos minerais adicionados à vila: " + coal + " carvão, " + 
                                        iron + " ferro, " + gold + " ouro, " + diamond + " diamante");
    }
    
    /**
     * Consome recursos da vila para construção
     */
    public boolean consumeResourcesForBuilding(BuildingType type) {
        int woodNeeded = 0;
        int stoneNeeded = 0;
        
        switch (type) {
            case HOUSE:
                woodNeeded = 20;
                stoneNeeded = 10;
                break;
            case FARM:
                woodNeeded = 10;
                stoneNeeded = 5;
                break;
            case STORAGE:
                woodNeeded = 15;
                stoneNeeded = 8;
                break;
        }
        
        // Verifica se há recursos suficientes
        if (woodResource >= woodNeeded && stoneResource >= stoneNeeded) {
            woodResource -= woodNeeded;
            stoneResource -= stoneNeeded;
            return true;
        }
        
        return false;
    }
    
    /**
     * Consome recursos da vila (usado durante conflitos ou outros eventos)
     * @param wood Quantidade de madeira a ser consumida
     * @param stone Quantidade de pedra a ser consumida
     * @param food Quantidade de comida a ser consumida
     */
    public void consumeResources(int wood, int stone, int food) {
        woodResource = Math.max(0, woodResource - wood);
        stoneResource = Math.max(0, stoneResource - stone);
        foodResource = Math.max(0, foodResource - food);
        VillagerExpansionMod.LOGGER.info("Vila " + villageId + " perdeu recursos: " + 
                                      wood + " madeira, " + stone + " pedra, " + food + " comida");
    }
    
    /**
     * Adiciona uma localização descoberta à vila
     */
    public void addDiscoveredLocation(UUID locationId, BlockPos position, String locationType) {
        discoveredLocations.add(locationId);
        discoveredLocationPositions.put(locationId, position);
        discoveredLocationTypes.put(locationId, locationType);
        VillagerExpansionMod.LOGGER.info("Nova localização descoberta adicionada à vila: " + locationType + " em " + position);
    }
    
    /**
     * Verifica se uma localização já foi descoberta pela vila
     */
    public boolean hasDiscoveredLocation(UUID locationId) {
        return discoveredLocations.contains(locationId);
    }
    
    /**
     * Verifica se uma posição está próxima a alguma localização já descoberta
     */
    public boolean isNearDiscoveredLocation(BlockPos pos, int radius) {
        for (BlockPos discoveredPos : discoveredLocationPositions.values()) {
            if (discoveredPos.isWithinDistance(pos, radius)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtém todas as localizações descobertas
     */
    public Set<UUID> getDiscoveredLocations() {
        return new HashSet<>(discoveredLocations);
    }
    
    /**
     * Obtém a posição de uma localização descoberta
     */
    public BlockPos getDiscoveredLocationPosition(UUID locationId) {
        return discoveredLocationPositions.get(locationId);
    }
    
    /**
     * Obtém o tipo de uma localização descoberta
     */
    public String getDiscoveredLocationType(UUID locationId) {
        return discoveredLocationTypes.get(locationId);
    }
    
    // Getters
    public UUID getVillageId() {
        return villageId;
    }
    
    public BlockPos getCenter() {
        return center;
    }
    
    public void setCenter(BlockPos center) {
        this.center = center;
    }
    
    public int getPopulation() {
        return population;
    }
    
    public int getBedCount() {
        return bedCount;
    }
    
    public int getFarmCount() {
        return farmCount;
    }
    
    public int getStorageCount() {
        return storageCount;
    }
    
    public int getWoodResource() {
        return woodResource;
    }
    
    public int getStoneResource() {
        return stoneResource;
    }
    
    public int getFoodResource() {
        return foodResource;
    }
    
    public int getCoalResource() {
        return coalResource;
    }
    
    public int getIronResource() {
        return ironResource;
    }
    
    public int getGoldResource() {
        return goldResource;
    }
    
    public int getDiamondResource() {
        return diamondResource;
    }
    
    public Set<UUID> getVillagers() {
        return new HashSet<>(villagers);
    }
    
    public List<BuildingData> getBuildings() {
        return new ArrayList<>(buildings);
    }
}