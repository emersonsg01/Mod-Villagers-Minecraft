package com.example.village.profession;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import com.example.village.mining.MiningTask;
import com.example.village.resources.ResourceType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    
    // Inventário de minérios coletados
    private final Map<Item, Integer> collectedOres = new HashMap<>();
    
    // Estado de busca de ferramentas
    private boolean isSearchingTools = false;
    private int toolSearchCooldown = 0;
    private static final int TOOL_SEARCH_INTERVAL = 200; // 10 segundos
    
    // Ferramentas disponíveis para o minerador
    private boolean hasPickaxe = false;
    private Item currentPickaxeType = null;
    
    @Override
    public String getName() {
        return "Minerador";
    }
    
    @Override
    public void onTick(VillagerEntity villager, ServerWorld world) {
        // Equipa o villager com ferramentas básicas se ainda não estiver equipado
        if (!hasEquipment) {
            equipMiner(villager);
            hasEquipment = true;
        }
        
        // Se está procurando ferramentas, continua essa tarefa
        if (isSearchingTools) {
            searchForTools(villager, world);
            return;
        }
        
        // Se já está minerando, verifica o progresso
        if (isMining) {
            checkMiningProgress(villager, world);
            return;
        }
        
        // Verifica se precisa buscar ferramentas
        toolSearchCooldown--;
        if (toolSearchCooldown <= 0) {
            if (!hasPickaxe) {
                isSearchingTools = true;
                VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + " está procurando ferramentas");
                return;
            }
            toolSearchCooldown = TOOL_SEARCH_INTERVAL;
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
                        StringBuilder storedItems = new StringBuilder();
                        
                        // Registra os minérios armazenados
                        for (Map.Entry<Item, Integer> entry : collectedOres.entrySet()) {
                            storedItems.append(entry.getValue()).append("x ").append(entry.getKey().getName().getString()).append(", ");
                        }
                        
                        if (storedItems.length() > 0) {
                            storedItems.setLength(storedItems.length() - 2); // Remove a última vírgula e espaço
                            VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + " armazenou minérios em um baú em " + pos + ": " + storedItems);
                            
                            // Atualiza os recursos da vila
                            updateVillageResources(villager, world);
                            
                            // Limpa o inventário do minerador
                            collectedOres.clear();
                            return true;
                        }
                    }
                }
            }
        }
        
        return false; // Não encontrou baús próximos ou não tinha itens para armazenar
    }
    
    /**
     * Equipa o villager com ferramentas básicas de mineração
     * @param villager O villager a ser equipado
     */
    private void equipMiner(VillagerEntity villager) {
        // Equipa o villager com uma tocha inicialmente
        villager.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
        
        // Chance de equipar com capacete para proteção
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
     * Procura por ferramentas em baús próximos
     * @param villager O villager minerador
     * @param world O mundo do servidor
     */
    private void searchForTools(VillagerEntity villager, ServerWorld world) {
        BlockPos villagerPos = villager.getBlockPos();
        int searchRadius = 16;
        
        // Procura por baús em um raio ao redor do villager
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = villagerPos.add(x, y, z);
                    
                    // Verifica se o bloco é um baú
                    if (world.getBlockState(pos).getBlock().equals(net.minecraft.block.Blocks.CHEST)) {
                        // Simula a busca por ferramentas no baú
                        // Em uma implementação completa, verificaríamos o inventário real do baú
                        
                        // Tenta encontrar uma picareta no baú (simulação)
                        List<Item> possibleTools = new ArrayList<>();
                        possibleTools.add(Items.IRON_PICKAXE);
                        possibleTools.add(Items.STONE_PICKAXE);
                        possibleTools.add(Items.WOODEN_PICKAXE);
                        
                        // Simula uma chance de encontrar uma ferramenta
                        if (random.nextFloat() < 0.7f) {
                            // Encontrou uma ferramenta
                            int toolIndex = random.nextInt(possibleTools.size());
                            Item foundTool = possibleTools.get(toolIndex);
                            
                            // Equipa a ferramenta
                            villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(foundTool));
                            hasPickaxe = true;
                            currentPickaxeType = foundTool;
                            
                            VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + " encontrou e equipou " + 
                                                          foundTool.getName().getString() + " de um baú em " + pos);
                            
                            isSearchingTools = false;
                            return;
                        }
                    }
                }
            }
        }
        
        // Se chegou aqui, não encontrou ferramentas
        isSearchingTools = false;
        toolSearchCooldown = TOOL_SEARCH_INTERVAL / 2; // Tenta novamente em metade do tempo normal
        VillagerExpansionMod.LOGGER.info("Minerador " + villager.getUuid() + " não encontrou ferramentas disponíveis");
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
            
            // Adiciona minérios ao inventário do villager baseado na ferramenta usada
            collectOres(villager);
            
            // Chance de quebrar a ferramenta
            if (hasPickaxe && random.nextFloat() < 0.2f) {
                VillagerExpansionMod.LOGGER.info("A picareta do Minerador " + villager.getUuid() + " quebrou durante a mineração");
                hasPickaxe = false;
                currentPickaxeType = null;
                villager.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }
    
    /**
     * Coleta minérios baseado na ferramenta usada
     * @param villager O villager minerador
     */
    private void collectOres(VillagerEntity villager) {
        if (!hasPickaxe) {
            // Sem picareta, coleta apenas pedra e carvão em pequenas quantidades
            addOreToInventory(Items.COBBLESTONE, 1 + random.nextInt(3));
            if (random.nextFloat() < 0.3f) {
                addOreToInventory(Items.COAL, 1);
            }
            return;
        }
        
        // Com picareta, coleta baseado no tipo de picareta
        // Sempre coleta pedra e carvão
        addOreToInventory(Items.COBBLESTONE, 3 + random.nextInt(5));
        addOreToInventory(Items.COAL, 1 + random.nextInt(3));
        
        if (currentPickaxeType == Items.WOODEN_PICKAXE || currentPickaxeType == Items.STONE_PICKAXE) {
            // Picaretas de madeira e pedra: ferro e ouro em pequenas quantidades
            if (random.nextFloat() < 0.5f) {
                addOreToInventory(Items.IRON_ORE, 1 + random.nextInt(2));
            }
        } else if (currentPickaxeType == Items.IRON_PICKAXE) {
            // Picareta de ferro: ferro, ouro e diamante
            addOreToInventory(Items.IRON_ORE, 2 + random.nextInt(3));
            
            if (random.nextFloat() < 0.4f) {
                addOreToInventory(Items.GOLD_ORE, 1 + random.nextInt(2));
            }
            
            if (random.nextFloat() < 0.2f) {
                addOreToInventory(Items.DIAMOND, 1);
            }
        }
        
        // Log dos minérios coletados
        StringBuilder oresLog = new StringBuilder("Minerador " + villager.getUuid() + " coletou: ");
        for (Map.Entry<Item, Integer> entry : collectedOres.entrySet()) {
            oresLog.append(entry.getValue()).append("x ").append(entry.getKey().getName().getString()).append(", ");
        }
        
        if (oresLog.length() > ("Minerador " + villager.getUuid() + " coletou: ").length()) {
            oresLog.setLength(oresLog.length() - 2); // Remove a última vírgula e espaço
            VillagerExpansionMod.LOGGER.info(oresLog.toString());
        }
    }
    
    /**
     * Adiciona um minério ao inventário do minerador
     * @param ore O minério a ser adicionado
     * @param amount A quantidade a ser adicionada
     */
    private void addOreToInventory(Item ore, int amount) {
        int currentAmount = collectedOres.getOrDefault(ore, 0);
        collectedOres.put(ore, currentAmount + amount);
    }
    
    /**
     * Atualiza os recursos da vila com os minérios coletados
     * @param villager O villager minerador
     * @param world O mundo do servidor
     */
    private void updateVillageResources(VillagerEntity villager, ServerWorld world) {
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
        
        // Calcula a quantidade de cada tipo de recurso
        int stoneAmount = 0;
        int ironAmount = 0;
        int goldAmount = 0;
        
        for (Map.Entry<Item, Integer> entry : collectedOres.entrySet()) {
            Item ore = entry.getKey();
            int amount = entry.getValue();
            
            if (ore == Items.COBBLESTONE || ore == Items.STONE) {
                stoneAmount += amount;
            } else if (ore == Items.IRON_ORE || ore == Items.IRON_INGOT) {
                ironAmount += amount;
            } else if (ore == Items.GOLD_ORE || ore == Items.GOLD_INGOT) {
                goldAmount += amount;
            }
            // Outros recursos como diamante podem ser adicionados ao sistema de recursos da vila se necessário
        }
        
        // Atualiza os recursos da vila
        villagerVillage.addResources(stoneAmount, 0, 0);
        
        // Registra a atualização de recursos
        VillagerExpansionMod.LOGGER.info("Vila " + villagerVillage.getVillageId() + " recebeu " + 
                                      stoneAmount + " de pedra, " + ironAmount + " de ferro e " + 
                                      goldAmount + " de ouro dos mineradores");
    }
}