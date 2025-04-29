package com.example.village.builder;

import com.example.VillagerExpansionMod;
import com.example.village.BuildingData;
import com.example.village.BuildingType;
import com.example.village.VillageData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gerencia as tarefas de construção dos villagers
 */
public class BuildingManager {
    private final Map<BlockPos, BuildTask> activeBuildTasks = new HashMap<>();
    private final Random random = new Random();
    
    // Contador para limitar a frequência de construção
    private int buildTickCounter = 0;
    private static final int BUILD_TICK_INTERVAL = 10; // A cada 0.5 segundos (20 ticks/segundo)
    
    /**
     * Agenda uma nova tarefa de construção
     */
    public void scheduleBuildTask(ServerWorld world, BlockPos position, BuildingType type, VillageData village) {
        // Verifica se já existe uma tarefa de construção nesta posição
        if (activeBuildTasks.containsKey(position)) {
            VillagerExpansionMod.LOGGER.info("Já existe uma tarefa de construção nesta posição: " + position);
            return;
        }
        
        // Verifica se a vila tem recursos suficientes para a construção
        if (!village.consumeResourcesForBuilding(type)) {
            VillagerExpansionMod.LOGGER.info("Recursos insuficientes para construir: " + type);
            return;
        }
        
        // Cria um modelo de construção baseado no tipo
        BuildingTemplate template = createBuildingTemplate(type, position, world);
        
        // Cria uma nova tarefa de construção
        BuildTask task = new BuildTask(template, village.getVillageId(), position);
        activeBuildTasks.put(position, task);
        
        VillagerExpansionMod.LOGGER.info("Nova tarefa de construção agendada: " + type + " em " + position);
    }
    
    /**
     * Processa as tarefas de construção ativas
     */
    public void processBuildingTasks(ServerWorld world) {
        buildTickCounter++;
        
        // Limita a frequência de construção para não sobrecarregar o servidor
        if (buildTickCounter % BUILD_TICK_INTERVAL != 0) {
            return;
        }
        
        // Lista para armazenar tarefas concluídas
        List<BlockPos> completedTasks = new ArrayList<>();
        
        // Processa cada tarefa de construção
        for (Map.Entry<BlockPos, BuildTask> entry : activeBuildTasks.entrySet()) {
            BlockPos pos = entry.getKey();
            BuildTask task = entry.getValue();
            
            // Verifica se a tarefa está concluída
            if (task.isCompleted()) {
                completedTasks.add(pos);
                finalizeBuildTask(world, task);
                continue;
            }
            
            // Processa a próxima etapa da construção
            if (processBuildStep(world, task)) {
                // Se retornou true, a tarefa foi concluída nesta etapa
                completedTasks.add(pos);
                finalizeBuildTask(world, task);
            }
        }
        
        // Remove as tarefas concluídas
        for (BlockPos pos : completedTasks) {
            activeBuildTasks.remove(pos);
        }
    }
    
    /**
     * Processa uma etapa de construção
     * @return true se a construção foi concluída
     */
    private boolean processBuildStep(ServerWorld world, BuildTask task) {
        // Obtém o próximo bloco a ser colocado
        BuildingBlock nextBlock = task.getNextBlock();
        if (nextBlock == null) {
            // Não há mais blocos para colocar, a construção está concluída
            return true;
        }
        
        // Coloca o bloco no mundo
        world.setBlockState(nextBlock.getPosition(), nextBlock.getBlockState());
        
        // Marca o bloco como colocado
        task.markBlockPlaced();
        
        // Verifica se todos os blocos foram colocados
        return task.isCompleted();
    }
    
    /**
     * Finaliza uma tarefa de construção
     */
    private void finalizeBuildTask(ServerWorld world, BuildTask task) {
        // Obtém a vila associada à tarefa
        UUID villageId = task.getVillageId();
        VillageData village = VillagerExpansionMod.getExpansionManager().getVillage(villageId);
        
        if (village != null) {
            // Cria um objeto BuildingData para a nova construção
            BuildingTemplate template = task.getTemplate();
            BuildingData building = new BuildingData(
                template.getType(),
                task.getPosition(),
                template.getSizeX(),
                template.getSizeY(),
                template.getSizeZ()
            );
            
            // Marca a construção como concluída
            building.setCompleted(true);
            
            // Adiciona a construção à vila
            village.addBuilding(building);
            
            VillagerExpansionMod.LOGGER.info("Construção concluída: " + template.getType() + " em " + task.getPosition());
        } else {
            VillagerExpansionMod.LOGGER.warn("Vila não encontrada ao finalizar construção: " + villageId);
        }
    }
    
    /**
     * Cria um modelo de construção baseado no tipo
     */
    private BuildingTemplate createBuildingTemplate(BuildingType type, BlockPos position, World world) {
        switch (type) {
            case HOUSE:
                return createHouseTemplate(position, world);
            case FARM:
                return createFarmTemplate(position, world);
            case STORAGE:
                return createStorageTemplate(position, world);
            default:
                throw new IllegalArgumentException("Tipo de construção desconhecido: " + type);
        }
    }
    
    /**
     * Cria um modelo de casa
     */
    private BuildingTemplate createHouseTemplate(BlockPos position, World world) {
        // Determina o tamanho da casa (variação aleatória)
        int sizeX = 5 + random.nextInt(2) * 2; // 5 ou 7
        int sizeY = 4 + random.nextInt(2);     // 4 ou 5
        int sizeZ = 5 + random.nextInt(2) * 2; // 5 ou 7
        
        // Cria o modelo de construção
        BuildingTemplate template = new BuildingTemplate(BuildingType.HOUSE, position, sizeX, sizeY, sizeZ);
        
        // Determina os materiais baseados no bioma
        Block wallMaterial = Blocks.OAK_PLANKS; // Padrão
        Block floorMaterial = Blocks.OAK_PLANKS;
        Block roofMaterial = Blocks.OAK_STAIRS;
        
        // Ajusta materiais baseado no bioma (simplificado)
        if (world.getBiome(position).value().getTemperature() < 0.3f) {
            // Bioma frio (taiga, montanhas)
            wallMaterial = Blocks.SPRUCE_PLANKS;
            floorMaterial = Blocks.SPRUCE_PLANKS;
            roofMaterial = Blocks.SPRUCE_STAIRS;
        } else if (world.getBiome(position).value().getTemperature() > 0.9f) {
            // Bioma quente (deserto, savana)
            wallMaterial = Blocks.SANDSTONE;
            floorMaterial = Blocks.SMOOTH_SANDSTONE;
            roofMaterial = Blocks.SANDSTONE_STAIRS;
        }
        
        // Adiciona os blocos ao modelo
        addHouseBlocks(template, wallMaterial, floorMaterial, roofMaterial);
        
        return template;
    }
    
    /**
     * Adiciona os blocos para um modelo de casa
     */
    private void addHouseBlocks(BuildingTemplate template, Block wallMaterial, Block floorMaterial, Block roofMaterial) {
        int sizeX = template.getSizeX();
        int sizeY = template.getSizeY();
        int sizeZ = template.getSizeZ();
        BlockPos origin = template.getPosition();
        
        // Ajusta a origem para o canto inferior esquerdo da construção
        BlockPos corner = origin.add(-sizeX/2, 0, -sizeZ/2);
        
        // Adiciona o piso
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos pos = corner.add(x, 0, z);
                template.addBlock(new BuildingBlock(pos, floorMaterial.getDefaultState()));
            }
        }
        
        // Adiciona as paredes
        for (int y = 1; y < sizeY - 1; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    // Apenas as bordas (paredes)
                    if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                        BlockPos pos = corner.add(x, y, z);
                        
                        // Adiciona janelas em posições específicas
                        if (y == 2 && ((x == 0 || x == sizeX - 1) && z > 0 && z < sizeZ - 1 && z % 2 == 1) ||
                                      ((z == 0 || z == sizeZ - 1) && x > 0 && x < sizeX - 1 && x % 2 == 1)) {
                            template.addBlock(new BuildingBlock(pos, Blocks.GLASS_PANE.getDefaultState()));
                        } else if (y == 1 && x == sizeX / 2 && z == 0) {
                            // Porta na frente, no meio
                            template.addBlock(new BuildingBlock(pos, Blocks.AIR.getDefaultState())); // Espaço para a porta
                            template.addBlock(new BuildingBlock(pos, Blocks.OAK_DOOR.getDefaultState())); // Porta
                        } else {
                            template.addBlock(new BuildingBlock(pos, wallMaterial.getDefaultState()));
                        }
                    }
                }
            }
        }
        
        // Adiciona o telhado
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos pos = corner.add(x, sizeY - 1, z);
                template.addBlock(new BuildingBlock(pos, roofMaterial.getDefaultState()));
            }
        }
        
        // Adiciona camas
        int bedCount = 0;
        int maxBeds = (sizeX <= 5) ? 2 : 4; // Casas pequenas têm 2 camas, casas grandes têm 4
        
        for (int x = 1; x < sizeX - 1; x++) {
            for (int z = 1; z < sizeZ - 1; z++) {
                // Coloca camas apenas nas bordas internas
                if ((x == 1 || x == sizeX - 2) && z > 1 && z < sizeZ - 2) {
                    if (bedCount < maxBeds) {
                        BlockPos bedPos = corner.add(x, 1, z);
                        template.addBlock(new BuildingBlock(bedPos, Blocks.RED_BED.getDefaultState()));
                        bedCount++;
                    }
                }
            }
        }
    }
    
    /**
     * Cria um modelo de fazenda
     */
    private BuildingTemplate createFarmTemplate(BlockPos position, World world) {
        // Tamanho da fazenda
        int sizeX = 7;
        int sizeY = 1;
        int sizeZ = 7;
        
        // Cria o modelo de construção
        BuildingTemplate template = new BuildingTemplate(BuildingType.FARM, position, sizeX, sizeY, sizeZ);
        
        // Ajusta a origem para o canto inferior esquerdo da construção
        BlockPos corner = position.add(-sizeX/2, 0, -sizeZ/2);
        
        // Adiciona terra cultivada e água
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos pos = corner.add(x, 0, z);
                
                if (x == sizeX/2 && z == sizeZ/2) {
                    // Água no centro
                    template.addBlock(new BuildingBlock(pos, Blocks.WATER.getDefaultState()));
                } else if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                    // Cerca nas bordas
                    template.addBlock(new BuildingBlock(pos, Blocks.OAK_FENCE.getDefaultState()));
                } else {
                    // Terra cultivada no resto
                    template.addBlock(new BuildingBlock(pos, Blocks.FARMLAND.getDefaultState()));
                    
                    // Adiciona cultivos aleatórios
                    BlockPos cropPos = pos.up();
                    if (random.nextFloat() < 0.7f) { // 70% de chance de ter um cultivo
                        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
                        Block crop = crops[random.nextInt(crops.length)];
                        template.addBlock(new BuildingBlock(cropPos, crop.getDefaultState()));
                    }
                }
            }
        }
        
        // Adiciona um portão
        BlockPos gatePos = corner.add(sizeX/2, 0, 0);
        template.addBlock(new BuildingBlock(gatePos, Blocks.OAK_FENCE_GATE.getDefaultState()));
        
        return template;
    }
    
    /**
     * Cria um modelo de armazém
     */
    private BuildingTemplate createStorageTemplate(BlockPos position, World world) {
        // Tamanho do armazém
        int sizeX = 5;
        int sizeY = 4;
        int sizeZ = 5;
        
        // Cria o modelo de construção
        BuildingTemplate template = new BuildingTemplate(BuildingType.STORAGE, position, sizeX, sizeY, sizeZ);
        
        // Determina os materiais baseados no bioma (similar à casa)
        Block wallMaterial = Blocks.OAK_PLANKS; // Padrão
        Block floorMaterial = Blocks.OAK_PLANKS;
        Block roofMaterial = Blocks.OAK_STAIRS;
        
        // Ajusta materiais baseado no bioma (simplificado)
        if (world.getBiome(position).value().getTemperature() < 0.3f) {
            // Bioma frio (taiga, montanhas)
            wallMaterial = Blocks.SPRUCE_PLANKS;
            floorMaterial = Blocks.SPRUCE_PLANKS;
            roofMaterial = Blocks.SPRUCE_STAIRS;
        } else if (world.getBiome(position).value().getTemperature() > 0.9f) {
            // Bioma quente (deserto, savana)
            wallMaterial = Blocks.SANDSTONE;
            floorMaterial = Blocks.SMOOTH_SANDSTONE;
            roofMaterial = Blocks.SANDSTONE_STAIRS;
        }
        
        // Adiciona os blocos ao modelo (similar à casa, mas com baús)
        addStorageBlocks(template, wallMaterial, floorMaterial, roofMaterial);
        
        return template;
    }
    
    /**
     * Adiciona os blocos para um modelo de armazém
     */
    private void addStorageBlocks(BuildingTemplate template, Block wallMaterial, Block floorMaterial, Block roofMaterial) {
        int sizeX = template.getSizeX();
        int sizeY = template.getSizeY();
        int sizeZ = template.getSizeZ();
        BlockPos origin = template.getPosition();
        
        // Ajusta a origem para o canto inferior esquerdo da construção
        BlockPos corner = origin.add(-sizeX/2, 0, -sizeZ/2);
        
        // Adiciona o piso
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos pos = corner.add(x, 0, z);
                template.addBlock(new BuildingBlock(pos, floorMaterial.getDefaultState()));
            }
        }
        
        // Adiciona as paredes
        for (int y = 1; y < sizeY - 1; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    // Apenas as bordas (paredes)
                    if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) {
                        BlockPos pos = corner.add(x, y, z);
                        
                        // Adiciona janelas em posições específicas
                        if (y == 2 && ((x == 0 || x == sizeX - 1) && z > 0 && z < sizeZ - 1 && z % 2 == 1) ||
                                      ((z == 0 || z == sizeZ - 1) && x > 0 && x < sizeX - 1 && x % 2 == 1)) {
                            template.addBlock(new BuildingBlock(pos, Blocks.GLASS_PANE.getDefaultState()));
                        } else if (y == 1 && x == sizeX / 2 && z == 0) {
                            // Porta na frente, no meio
                            template.addBlock(new BuildingBlock(pos, Blocks.AIR.getDefaultState())); // Espaço para a porta
                            template.addBlock(new BuildingBlock(pos, Blocks.OAK_DOOR.getDefaultState())); // Porta
                        } else {
                            template.addBlock(new BuildingBlock(pos, wallMaterial.getDefaultState()));
                        }
                    }
                }
            }
        }
        
        // Adiciona o telhado
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos pos = corner.add(x, sizeY - 1, z);
                template.addBlock(new BuildingBlock(pos, roofMaterial.getDefaultState()));
            }
        }
        
        // Adiciona baús
        for (int x = 1; x < sizeX - 1; x++) {
            for (int z = 1; z < sizeZ - 1; z++) {
                // Coloca baús apenas nas bordas internas
                if ((x == 1 || x == sizeX - 2) && (z == 1 || z == sizeZ - 2)) {
                    BlockPos chestPos = corner.add(x, 1, z);
                    
                    // Determina a direção do baú para que fique virado para o centro
                    Direction direction = Direction.NORTH; // Padrão
                    if (x == 1 && z == 1) direction = Direction.SOUTH;
                    else if (x == 1 && z == sizeZ - 2) direction = Direction.NORTH;
                    else if (x == sizeX - 2 && z == 1) direction = Direction.EAST;
                    else if (x == sizeX - 2 && z == sizeZ - 2) direction = Direction.WEST;
                    
                    BlockState chestState = Blocks.CHEST.getDefaultState();
                    template.addBlock(new BuildingBlock(chestPos, chestState));
                }
            }
        }
    }
}