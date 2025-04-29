package com.example.village.resources;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gerencia a coleta e distribuição de recursos para as vilas
 */
public class ResourceManager {
    private final Map<UUID, ResourceCollectionTask> activeCollectionTasks = new HashMap<>();
    private final Random random = new Random();
    
    // Contador para limitar a frequência de coleta
    private int resourceTickCounter = 0;
    private static final int RESOURCE_TICK_INTERVAL = 100; // A cada 5 segundos (20 ticks/segundo)
    
    /**
     * Processa a coleta de recursos
     */
    public void processResourceCollection(ServerWorld world) {
        resourceTickCounter++;
        
        // Limita a frequência de coleta para não sobrecarregar o servidor
        if (resourceTickCounter % RESOURCE_TICK_INTERVAL != 0) {
            return;
        }
        
        // Para cada vila, verifica se é necessário coletar recursos
        for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
            checkVillageResources(village, world);
        }
    }
    
    /**
     * Verifica os recursos de uma vila e inicia tarefas de coleta se necessário
     */
    private void checkVillageResources(VillageData village, ServerWorld world) {
        // Verifica se a vila precisa de madeira
        if (village.getWoodResource() < 20) {
            // Procura por árvores próximas
            BlockPos treeLocation = findNearbyResource(village, world, ResourceType.WOOD);
            if (treeLocation != null) {
                // Inicia uma tarefa de coleta de madeira
                startResourceCollection(village, world, treeLocation, ResourceType.WOOD);
            }
        }
        
        // Verifica se a vila precisa de pedra
        if (village.getStoneResource() < 15) {
            // Procura por pedras próximas
            BlockPos stoneLocation = findNearbyResource(village, world, ResourceType.STONE);
            if (stoneLocation != null) {
                // Inicia uma tarefa de coleta de pedra
                startResourceCollection(village, world, stoneLocation, ResourceType.STONE);
            }
        }
        
        // Verifica se a vila precisa de comida
        if (village.getFoodResource() < 30) {
            // Verifica se há fazendas na vila
            if (village.getFarmCount() > 0) {
                // Simula a coleta de alimentos das fazendas
                int foodCollected = random.nextInt(5) + 1; // 1-5 unidades de comida
                village.addResources(0, 0, foodCollected);
                VillagerExpansionMod.LOGGER.info("Vila coletou " + foodCollected + " unidades de comida das fazendas");
            }
        }
    }
    
    /**
     * Encontra recursos próximos à vila
     */
    private BlockPos findNearbyResource(VillageData village, ServerWorld world, ResourceType type) {
        BlockPos center = village.getCenter();
        int searchRadius = 32; // Raio de busca em blocos
        
        // Blocos a procurar baseados no tipo de recurso
        Block[] targetBlocks;
        switch (type) {
            case WOOD:
                targetBlocks = new Block[]{Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, 
                                          Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG};
                break;
            case STONE:
                targetBlocks = new Block[]{Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, 
                                          Blocks.DIORITE, Blocks.GRANITE};
                break;
            default:
                return null;
        }
        
        // Procura em espiral a partir do centro
        for (int radius = 5; radius <= searchRadius; radius += 5) {
            for (int x = -radius; x <= radius; x += 5) {
                for (int z = -radius; z <= radius; z += 5) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue; // Apenas o perímetro
                    
                    BlockPos checkPos = center.add(x, 0, z);
                    
                    // Ajusta a altura para encontrar o recurso
                    checkPos = findResourceAtPosition(checkPos, world, targetBlocks);
                    
                    if (checkPos != null) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null; // Não encontrou o recurso
    }
    
    /**
     * Procura por um recurso específico em uma coluna vertical
     */
    private BlockPos findResourceAtPosition(BlockPos pos, World world, Block[] targetBlocks) {
        // Procura do topo para baixo
        // Usa getTopY com Heightmap.Type.WORLD_SURFACE para obter a altura máxima do mundo nesta posição
        BlockPos.Mutable mutablePos = new BlockPos.Mutable(pos.getX(), world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()), pos.getZ());
        
        while (mutablePos.getY() > world.getBottomY()) {
            mutablePos.move(0, -1, 0);
            Block block = world.getBlockState(mutablePos).getBlock();
            
            for (Block targetBlock : targetBlocks) {
                if (block == targetBlock) {
                    return mutablePos.toImmutable();
                }
            }
        }
        
        return null; // Não encontrou o recurso
    }
    
    /**
     * Inicia uma tarefa de coleta de recursos
     */
    private void startResourceCollection(VillageData village, ServerWorld world, BlockPos resourcePos, ResourceType type) {
        // Verifica se já existe uma tarefa de coleta para este recurso
        for (ResourceCollectionTask task : activeCollectionTasks.values()) {
            if (task.getResourcePosition().equals(resourcePos)) {
                return; // Já existe uma tarefa para este recurso
            }
        }
        
        // Cria uma nova tarefa de coleta
        ResourceCollectionTask task = new ResourceCollectionTask(village.getVillageId(), resourcePos, type);
        activeCollectionTasks.put(UUID.randomUUID(), task);
        
        VillagerExpansionMod.LOGGER.info("Iniciando coleta de " + type + " em " + resourcePos);
        
        // Simula a coleta imediata (em uma implementação completa, isso seria feito ao longo do tempo)
        completeResourceCollection(village, world, task);
    }
    
    /**
     * Completa uma tarefa de coleta de recursos
     */
    private void completeResourceCollection(VillageData village, ServerWorld world, ResourceCollectionTask task) {
        // Quantidade de recursos coletados (variação aleatória)
        int amount = random.nextInt(5) + 3; // 3-7 unidades
        
        // Adiciona os recursos à vila
        switch (task.getResourceType()) {
            case WOOD:
                village.addResources(amount, 0, 0);
                VillagerExpansionMod.LOGGER.info("Vila coletou " + amount + " unidades de madeira");
                break;
            case STONE:
                village.addResources(0, amount, 0);
                VillagerExpansionMod.LOGGER.info("Vila coletou " + amount + " unidades de pedra");
                break;
        }
        
        // Em uma implementação completa, remover o bloco do mundo
        // world.setBlockState(task.getResourcePosition(), Blocks.AIR.getDefaultState());
        
        // Remove a tarefa da lista de tarefas ativas
        activeCollectionTasks.values().removeIf(t -> t.getResourcePosition().equals(task.getResourcePosition()));
    }
    
    /**
     * Verifica se um villager possui um item específico em seu inventário
     */
    public boolean hasItem(VillagerEntity villager, Item item) {
        // Verifica se o villager possui o item em seu inventário
        for (int i = 0; i < villager.getInventory().size(); i++) {
            ItemStack stack = villager.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Obtém a quantidade de um item específico no inventário de um villager
     */
    public int getItemCount(VillagerEntity villager, Item item) {
        int count = 0;
        
        // Conta a quantidade do item no inventário
        for (int i = 0; i < villager.getInventory().size(); i++) {
            ItemStack stack = villager.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        
        return count;
    }
}