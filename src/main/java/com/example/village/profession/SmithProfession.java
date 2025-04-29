package com.example.village.profession;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementação da profissão de Ferreiro para villagers
 * Os ferreiros fabricam ferramentas e as armazenam em baús para uso por outros villagers
 */
public class SmithProfession implements VillagerProfession {
    
    private final Random random = Random.create();
    private boolean hasEquipment = false;
    private int craftCooldown = 0;
    private static final int CRAFT_INTERVAL = 400; // 20 segundos
    
    // Estado de fabricação atual
    private boolean isCrafting = false;
    private Item currentCraftingItem = null;
    private int craftingProgress = 0;
    
    // Mapa para rastrear ferramentas fabricadas e armazenadas
    private final Map<Item, Integer> toolsInStorage = new HashMap<>();
    
    // Lista de ferramentas que o ferreiro pode fabricar
    private final List<Item> availableTools = new ArrayList<>();
    
    public SmithProfession() {
        // Inicializa a lista de ferramentas disponíveis
        availableTools.add(Items.WOODEN_PICKAXE);
        availableTools.add(Items.STONE_PICKAXE);
        availableTools.add(Items.IRON_PICKAXE);
        availableTools.add(Items.WOODEN_AXE);
        availableTools.add(Items.STONE_AXE);
        availableTools.add(Items.IRON_AXE);
        availableTools.add(Items.WOODEN_SHOVEL);
        availableTools.add(Items.STONE_SHOVEL);
        availableTools.add(Items.IRON_SHOVEL);
        availableTools.add(Items.WOODEN_HOE);
        availableTools.add(Items.STONE_HOE);
        availableTools.add(Items.IRON_HOE);
        availableTools.add(Items.WOODEN_SWORD);
        availableTools.add(Items.STONE_SWORD);
        availableTools.add(Items.IRON_SWORD);
        
        // Inicializa o mapa de ferramentas em armazenamento
        for (Item tool : availableTools) {
            toolsInStorage.put(tool, 0);
        }
    }
    
    @Override
    public String getName() {
        return "Ferreiro";
    }
    
    @Override
    public void onTick(VillagerEntity villager, ServerWorld world) {
        // Equipa o villager com ferramentas de ferreiro se ainda não estiver equipado
        if (!hasEquipment) {
            equipSmith(villager);
            hasEquipment = true;
        }
        
        // Se já está fabricando, continua a tarefa
        if (isCrafting) {
            continueCraftingTask(villager, world);
            return;
        }
        
        // Tenta iniciar uma nova tarefa de fabricação
        craftCooldown--;
        if (craftCooldown <= 0) {
            tryStartCrafting(villager, world);
            craftCooldown = CRAFT_INTERVAL;
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
                        VillagerExpansionMod.LOGGER.info("Ferreiro " + villager.getUuid() + " armazenou ferramentas em um baú em " + pos);
                        
                        // Atualiza o registro de ferramentas armazenadas
                        if (currentCraftingItem != null) {
                            int currentAmount = toolsInStorage.getOrDefault(currentCraftingItem, 0);
                            toolsInStorage.put(currentCraftingItem, currentAmount + 1);
                            VillagerExpansionMod.LOGGER.info("Ferreiro armazenou " + currentCraftingItem.getName().getString() + ". Total em estoque: " + (currentAmount + 1));
                        }
                        
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
     * Equipa o villager com ferramentas de ferreiro
     * @param villager O villager a ser equipado
     */
    private void equipSmith(VillagerEntity villager) {
        // Equipa o villager com um martelo (representado por uma picareta de ferro)
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        
        // Chance de equipar com outros itens úteis
        if (random.nextFloat() < 0.5f) {
            villager.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_INGOT));
        }
        
        // Impede que os itens caiam quando o villager morre
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " equipado como Ferreiro");
    }
    
    /**
     * Tenta iniciar uma tarefa de fabricação
     * @param villager O villager ferreiro
     * @param world O mundo do servidor
     */
    private void tryStartCrafting(VillagerEntity villager, ServerWorld world) {
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
        
        // Verifica quais ferramentas estão em falta
        List<Item> toolsNeeded = new ArrayList<>();
        for (Item tool : availableTools) {
            int currentAmount = toolsInStorage.getOrDefault(tool, 0);
            if (currentAmount < 2) { // Mantém pelo menos 2 de cada ferramenta
                toolsNeeded.add(tool);
            }
        }
        
        if (toolsNeeded.isEmpty()) {
            return; // Não há necessidade de fabricar novas ferramentas
        }
        
        // Escolhe uma ferramenta aleatória para fabricar
        currentCraftingItem = toolsNeeded.get(random.nextInt(toolsNeeded.size()));
        
        // Inicia a fabricação
        isCrafting = true;
        craftingProgress = 0;
        VillagerExpansionMod.LOGGER.info("Ferreiro " + villager.getUuid() + " iniciou fabricação de " + currentCraftingItem.getName().getString());
    }
    
    /**
     * Continua uma tarefa de fabricação em andamento
     * @param villager O villager ferreiro
     * @param world O mundo do servidor
     */
    private void continueCraftingTask(VillagerEntity villager, ServerWorld world) {
        if (currentCraftingItem == null) {
            isCrafting = false;
            return;
        }
        
        // Simula o progresso da fabricação
        craftingProgress += 10; // Incrementa o progresso em 10%
        
        if (craftingProgress >= 100) {
            // Fabricação concluída
            finishCrafting(villager, world);
        } else {
            // Ainda fabricando
            VillagerExpansionMod.LOGGER.info("Ferreiro " + villager.getUuid() + " progresso da fabricação: " + craftingProgress + "%");
        }
    }
    
    /**
     * Finaliza a fabricação de uma ferramenta
     * @param villager O villager ferreiro
     * @param world O mundo do servidor
     */
    private void finishCrafting(VillagerEntity villager, ServerWorld world) {
        // Adiciona a ferramenta ao inventário do villager
        villager.getInventory().insertStack(new ItemStack(currentCraftingItem));
        
        VillagerExpansionMod.LOGGER.info("Ferreiro " + villager.getUuid() + " concluiu fabricação de " + currentCraftingItem.getName().getString());
        
        // Tenta armazenar a ferramenta em um baú
        if (storeItems(villager, world)) {
            VillagerExpansionMod.LOGGER.info("Ferreiro armazenou a ferramenta fabricada em um baú");
        } else {
            VillagerExpansionMod.LOGGER.info("Ferreiro não encontrou um baú para armazenar a ferramenta");
        }
        
        // Reseta o estado de fabricação
        isCrafting = false;
        currentCraftingItem = null;
        craftingProgress = 0;
    }
    
    /**
     * Verifica se há ferramentas disponíveis para um tipo específico
     * @param toolType O tipo de ferramenta desejada
     * @return true se há ferramentas disponíveis, false caso contrário
     */
    public boolean hasToolAvailable(Item toolType) {
        return toolsInStorage.getOrDefault(toolType, 0) > 0;
    }
    
    /**
     * Obtém uma ferramenta do armazenamento
     * @param toolType O tipo de ferramenta desejada
     * @return true se a ferramenta foi obtida com sucesso, false caso contrário
     */
    public boolean retrieveTool(Item toolType) {
        int currentAmount = toolsInStorage.getOrDefault(toolType, 0);
        if (currentAmount > 0) {
            toolsInStorage.put(toolType, currentAmount - 1);
            VillagerExpansionMod.LOGGER.info("Ferramenta " + toolType.getName().getString() + " retirada do armazenamento. Restantes: " + (currentAmount - 1));
            return true;
        }
        return false;
    }
}