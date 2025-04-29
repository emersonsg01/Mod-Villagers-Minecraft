package com.example.village.profession;

import com.example.VillagerExpansionMod;
import com.example.village.BuildingData;
import com.example.village.BuildingType;
import com.example.village.VillageData;
import com.example.village.builder.BuildTask;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.UUID;

/**
 * Implementação da profissão de Construtor para villagers
 * Os construtores são responsáveis por expandir a vila, construindo novas casas
 * e fabricando/colocando camas para permitir o crescimento populacional
 */
public class BuilderProfession implements VillagerProfession {
    
    private final Random random = Random.create();
    private boolean hasEquipment = false;
    private int buildCooldown = 0;
    private static final int BUILD_INTERVAL = 300; // 15 segundos
    
    // Estado de construção atual
    private boolean isBuilding = false;
    private BlockPos currentBuildPos = null;
    private int buildProgress = 0;
    
    // Estado de colocação de cama
    private boolean isPlacingBed = false;
    private BlockPos targetHousePos = null;
    
    @Override
    public String getName() {
        return "Construtor";
    }
    
    @Override
    public void onTick(VillagerEntity villager, ServerWorld world) {
        // Equipa o villager com ferramentas de construção se ainda não estiver equipado
        if (!hasEquipment) {
            equipBuilder(villager);
            hasEquipment = true;
        }
        
        // Se já está construindo ou colocando cama, continua a tarefa
        if (isBuilding) {
            continueBuildingTask(villager, world);
            return;
        }
        
        if (isPlacingBed) {
            continuePlacingBed(villager, world);
            return;
        }
        
        // Tenta iniciar uma nova tarefa de construção ou colocação de cama
        buildCooldown--;
        if (buildCooldown <= 0) {
            if (!tryPlaceBed(villager, world)) {
                tryStartBuilding(villager, world);
            }
            buildCooldown = BUILD_INTERVAL;
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
                        VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " armazenou materiais em um baú em " + pos);
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
     * Equipa o villager com ferramentas de construção
     * @param villager O villager a ser equipado
     */
    private void equipBuilder(VillagerEntity villager) {
        // Equipa o villager com um machado
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        
        // Chance de equipar com outros itens úteis
        if (random.nextFloat() < 0.5f) {
            villager.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.STONE_PICKAXE));
        }
        
        // Impede que os itens caiam quando o villager morre
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " equipado como Construtor");
    }
    
    /**
     * Tenta iniciar uma tarefa de construção
     * @param villager O villager construtor
     * @param world O mundo do servidor
     */
    private void tryStartBuilding(VillagerEntity villager, ServerWorld world) {
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
        
        // Verifica se a vila precisa de mais casas
        if (villagerVillage.needsMoreHouses()) {
            // Encontra um local adequado para construção
            BlockPos buildLocation = findBuildLocation(villagerVillage, world);
            if (buildLocation != null) {
                // Inicia a construção de uma nova casa
                isBuilding = true;
                currentBuildPos = buildLocation;
                buildProgress = 0;
                VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " iniciou construção de casa em " + buildLocation);
            }
        }
    }
    
    /**
     * Continua uma tarefa de construção em andamento
     * @param villager O villager construtor
     * @param world O mundo do servidor
     */
    private void continueBuildingTask(VillagerEntity villager, ServerWorld world) {
        if (currentBuildPos == null) {
            isBuilding = false;
            return;
        }
        
        // Verifica se o villager está próximo do local de construção
        double distanceToBuild = villager.getBlockPos().getSquaredDistance(currentBuildPos);
        if (distanceToBuild > 100) { // Mais de 10 blocos de distância
            // Villager está longe do local de construção, tenta se aproximar
            // Em uma implementação completa, usaríamos pathfinding para mover o villager
            VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " se aproximando do local de construção em " + currentBuildPos);
            return;
        }
        
        // Simula o progresso da construção
        buildProgress += 5; // Incrementa o progresso em 5%
        
        if (buildProgress >= 100) {
            // Construção concluída
            finishBuilding(villager, world);
        } else {
            // Ainda construindo
            VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " progresso da construção: " + buildProgress + "%");
        }
    }
    
    /**
     * Finaliza a construção de uma casa
     * @param villager O villager construtor
     * @param world O mundo do servidor
     */
    private void finishBuilding(VillagerEntity villager, ServerWorld world) {
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
            isBuilding = false;
            currentBuildPos = null;
            return;
        }
        
        // Cria um objeto BuildingData para a nova casa
        BuildingData building = new BuildingData(
            BuildingType.HOUSE,
            currentBuildPos,
            5, // sizeX
            4, // sizeY
            5  // sizeZ
        );
        
        // Marca a construção como concluída, mas sem camas ainda
        building.setCompleted(true);
        building.setBedCount(0); // Inicialmente sem camas
        
        // Adiciona a construção à vila
        villagerVillage.addBuilding(building);
        
        VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " concluiu construção de casa em " + currentBuildPos);
        
        // Prepara para colocar camas na nova casa
        isBuilding = false;
        isPlacingBed = true;
        targetHousePos = currentBuildPos;
        currentBuildPos = null;
    }
    
    /**
     * Tenta colocar uma cama em uma casa existente
     * @param villager O villager construtor
     * @param world O mundo do servidor
     * @return true se iniciou a colocação de cama, false caso contrário
     */
    private boolean tryPlaceBed(VillagerEntity villager, ServerWorld world) {
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
            return false;
        }
        
        // Procura por casas sem camas suficientes
        for (BuildingData building : villagerVillage.getBuildings()) {
            if (building.getType() == BuildingType.HOUSE && building.isCompleted() && building.getBedCount() < 2) {
                // Encontrou uma casa que precisa de camas
                isPlacingBed = true;
                targetHousePos = building.getPosition();
                VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " vai colocar cama em casa em " + targetHousePos);
                return true;
            }
        }
        
        return false; // Não encontrou casas que precisam de camas
    }
    
    /**
     * Continua a tarefa de colocar cama em uma casa
     * @param villager O villager construtor
     * @param world O mundo do servidor
     */
    private void continuePlacingBed(VillagerEntity villager, ServerWorld world) {
        if (targetHousePos == null) {
            isPlacingBed = false;
            return;
        }
        
        // Verifica se o villager está próximo da casa
        double distanceToHouse = villager.getBlockPos().getSquaredDistance(targetHousePos);
        if (distanceToHouse > 100) { // Mais de 10 blocos de distância
            // Villager está longe da casa, tenta se aproximar
            // Em uma implementação completa, usaríamos pathfinding para mover o villager
            VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " se aproximando da casa em " + targetHousePos);
            return;
        }
        
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
            isPlacingBed = false;
            targetHousePos = null;
            return;
        }
        
        // Encontra a casa alvo
        BuildingData targetHouse = null;
        for (BuildingData building : villagerVillage.getBuildings()) {
            if (building.getPosition().equals(targetHousePos)) {
                targetHouse = building;
                break;
            }
        }
        
        if (targetHouse == null || targetHouse.getBedCount() >= 2) {
            // Casa não encontrada ou já tem camas suficientes
            isPlacingBed = false;
            targetHousePos = null;
            return;
        }
        
        // Simula a colocação de uma cama
        // Em uma implementação completa, colocaríamos um bloco de cama real no mundo
        BlockPos bedPos = findBedPosition(targetHousePos, world);
        if (bedPos != null) {
            // Coloca a cama (simulação)
            placeBed(bedPos, world);
            
            // Atualiza o contador de camas da casa
            targetHouse.setBedCount(targetHouse.getBedCount() + 1);
            
            VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " colocou cama em " + bedPos);
            
            // Verifica se a casa já tem camas suficientes
            if (targetHouse.getBedCount() >= 2) {
                isPlacingBed = false;
                targetHousePos = null;
            }
        } else {
            // Não encontrou um local adequado para a cama
            VillagerExpansionMod.LOGGER.info("Construtor " + villager.getUuid() + " não encontrou local para cama na casa em " + targetHousePos);
            isPlacingBed = false;
            targetHousePos = null;
        }
    }
    
    /**
     * Encontra um local adequado para construção
     * @param village A vila
     * @param world O mundo do servidor
     * @return A posição para construção, ou null se não encontrar
     */
    private BlockPos findBuildLocation(VillageData village, ServerWorld world) {
        // Implementação básica: procura por um espaço plano próximo ao centro da vila
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
     * Encontra o nível do solo adequado para uma posição
     * @param pos A posição base
     * @param world O mundo do servidor
     * @return A posição ajustada, ou null se não encontrar
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
     * @param pos A posição a verificar
     * @param world O mundo do servidor
     * @return true se a localização é adequada, false caso contrário
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
     * Encontra uma posição adequada para colocar uma cama dentro de uma casa
     * @param housePos A posição da casa
     * @param world O mundo do servidor
     * @return A posição para a cama, ou null se não encontrar
     */
    private BlockPos findBedPosition(BlockPos housePos, ServerWorld world) {
        // Procura por um espaço adequado dentro da casa
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Evita as bordas da casa
                if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
                
                BlockPos pos = housePos.add(x, 0, z);
                
                // Verifica se o espaço está livre e tem um bloco sólido abaixo
                if (!world.getBlockState(pos).isSolid() && 
                    !world.getBlockState(pos.up()).isSolid() && 
                    world.getBlockState(pos.down()).isSolid()) {
                    
                    // Verifica se já existe uma cama nesta posição
                    if (!(world.getBlockState(pos).getBlock() instanceof BedBlock)) {
                        // Verifica se há espaço para a cama (2 blocos)
                        for (Direction dir : Direction.Type.HORIZONTAL) {
                            BlockPos secondPos = pos.offset(dir);
                            if (!world.getBlockState(secondPos).isSolid() && 
                                !world.getBlockState(secondPos.up()).isSolid() && 
                                world.getBlockState(secondPos.down()).isSolid() && 
                                !(world.getBlockState(secondPos).getBlock() instanceof BedBlock)) {
                                
                                return pos; // Encontrou um local adequado
                            }
                        }
                    }
                }
            }
        }
        
        return null; // Não encontrou local adequado
    }
    
    /**
     * Coloca uma cama no mundo
     * @param pos A posição da cama
     * @param world O mundo do servidor
     */
    private void placeBed(BlockPos pos, ServerWorld world) {
        // Em uma implementação completa, colocaríamos um bloco de cama real
        // Aqui apenas simulamos a colocação
        
        // Escolhe uma direção aleatória para a cama
        Direction direction = Direction.Type.HORIZONTAL.random(random);
        BlockPos headPos = pos.offset(direction);
        
        // Verifica se o espaço para a cabeça da cama está livre
        if (!world.getBlockState(headPos).isSolid() && 
            !world.getBlockState(headPos.up()).isSolid() && 
            world.getBlockState(headPos.down()).isSolid()) {
            
            // Simula a colocação da cama
            // Em uma implementação completa, usaríamos:
            // BlockState bedFoot = Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, direction);
            // BlockState bedHead = Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, direction);
            // world.setBlockState(pos, bedFoot);
            // world.setBlockState(headPos, bedHead);
            
            VillagerExpansionMod.LOGGER.info("Cama colocada em " + pos + " com direção " + direction);
        }
    }
}