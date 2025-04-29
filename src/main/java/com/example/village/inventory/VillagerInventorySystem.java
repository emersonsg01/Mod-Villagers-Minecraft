package com.example.village.inventory;

import com.example.VillagerExpansionMod;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de inventário para villagers
 * Permite que os villagers tenham um inventário similar ao dos jogadores,
 * com slots para itens e equipamentos, incluindo armaduras
 */
public class VillagerInventorySystem {
    
    // Mapa que associa o UUID do villager ao seu inventário
    private static final Map<UUID, SimpleInventory> villagerInventories = new HashMap<>();
    
    // Tamanho do inventário do villager (menor que o do jogador)
    private static final int INVENTORY_SIZE = 15;
    
    /**
     * Obtém o inventário de um villager, criando-o se não existir
     * @param villager O villager
     * @return O inventário do villager
     */
    public static SimpleInventory getInventory(VillagerEntity villager) {
        UUID villagerId = villager.getUuid();
        
        // Cria um novo inventário se não existir
        if (!villagerInventories.containsKey(villagerId)) {
            SimpleInventory inventory = new SimpleInventory(INVENTORY_SIZE);
            villagerInventories.put(villagerId, inventory);
            return inventory;
        }
        
        return villagerInventories.get(villagerId);
    }
    
    /**
     * Adiciona um item ao inventário do villager
     * @param villager O villager
     * @param item O item a ser adicionado
     * @param count A quantidade do item
     * @return true se o item foi adicionado com sucesso, false caso contrário
     */
    public static boolean addItemToInventory(VillagerEntity villager, Item item, int count) {
        SimpleInventory inventory = getInventory(villager);
        ItemStack stack = new ItemStack(item, count);
        
        // Tenta adicionar o item ao inventário
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);
            
            if (slotStack.isEmpty()) {
                // Slot vazio, adiciona o item
                inventory.setStack(i, stack);
                return true;
            } else if (slotStack.getItem() == item && slotStack.getCount() < slotStack.getMaxCount()) {
                // Mesmo item, tenta empilhar
                int spaceLeft = slotStack.getMaxCount() - slotStack.getCount();
                if (count <= spaceLeft) {
                    // Há espaço suficiente para todo o stack
                    slotStack.increment(count);
                    inventory.setStack(i, slotStack);
                    return true;
                } else {
                    // Adiciona o que couber e continua procurando slots para o restante
                    slotStack.increment(spaceLeft);
                    inventory.setStack(i, slotStack);
                    count -= spaceLeft;
                    stack = new ItemStack(item, count);
                }
            }
        }
        
        // Se chegou aqui e ainda tem itens para adicionar, o inventário está cheio
        return count <= 0;
    }
    
    /**
     * Verifica se o villager tem um item específico em seu inventário
     * @param villager O villager
     * @param item O item a ser verificado
     * @param count A quantidade mínima do item
     * @return true se o villager tem o item na quantidade especificada, false caso contrário
     */
    public static boolean hasItem(VillagerEntity villager, Item item, int count) {
        SimpleInventory inventory = getInventory(villager);
        int totalCount = 0;
        
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item) {
                totalCount += stack.getCount();
                if (totalCount >= count) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Remove uma quantidade específica de um item do inventário do villager
     * @param villager O villager
     * @param item O item a ser removido
     * @param count A quantidade a ser removida
     * @return true se os itens foram removidos com sucesso, false caso contrário
     */
    public static boolean removeItem(VillagerEntity villager, Item item, int count) {
        if (!hasItem(villager, item, count)) {
            return false;
        }
        
        SimpleInventory inventory = getInventory(villager);
        int remainingToRemove = count;
        
        for (int i = 0; i < inventory.size() && remainingToRemove > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item) {
                if (stack.getCount() <= remainingToRemove) {
                    // Remove o stack inteiro
                    remainingToRemove -= stack.getCount();
                    inventory.setStack(i, ItemStack.EMPTY);
                } else {
                    // Remove apenas parte do stack
                    stack.decrement(remainingToRemove);
                    remainingToRemove = 0;
                }
            }
        }
        
        return remainingToRemove == 0;
    }
    
    /**
     * Equipa o villager com uma armadura do inventário
     * @param villager O villager
     * @return true se alguma armadura foi equipada, false caso contrário
     */
    public static boolean equipArmorFromInventory(VillagerEntity villager) {
        SimpleInventory inventory = getInventory(villager);
        boolean equipped = false;
        
        // Verifica cada slot do inventário
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof ArmorItem) {
                ArmorItem armorItem = (ArmorItem) stack.getItem();
                EquipmentSlot slot = armorItem.getSlotType();
                
                // Verifica se o slot de equipamento está vazio
                if (villager.getEquippedStack(slot).isEmpty()) {
                    // Equipa a armadura
                    villager.equipStack(slot, stack.copy());
                    // Remove do inventário
                    inventory.removeStack(i);
                    equipped = true;
                    
                    // Impede que a armadura caia quando o villager morre
                    villager.setEquipmentDropChance(slot, 0.0f);
                    
                    VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " equipou " + 
                                                  armorItem.getName().getString());
                }
            }
        }
        
        return equipped;
    }
    
    /**
     * Armazena itens do inventário do villager em um baú próximo
     * @param villager O villager
     * @param world O mundo do servidor
     * @return true se os itens foram armazenados com sucesso, false caso contrário
     */
    public static boolean storeItemsInChest(VillagerEntity villager, ServerWorld world) {
        SimpleInventory inventory = getInventory(villager);
        
        // Verifica se o inventário está vazio
        boolean hasItems = false;
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getStack(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        
        if (!hasItems) {
            return false; // Inventário vazio, não há nada para armazenar
        }
        
        // Procura por baús próximos
        BlockPos villagerPos = villager.getBlockPos();
        int searchRadius = 16;
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = villagerPos.add(x, y, z);
                    
                    // Verifica se o bloco é um baú
                    if (world.getBlockState(pos).getBlock().equals(net.minecraft.block.Blocks.CHEST)) {
                        // Simula o armazenamento de itens (em uma implementação completa, usaríamos o inventário do baú)
                        StringBuilder storedItems = new StringBuilder();
                        
                        // Lista os itens armazenados
                        for (int i = 0; i < inventory.size(); i++) {
                            ItemStack stack = inventory.getStack(i);
                            if (!stack.isEmpty()) {
                                storedItems.append(stack.getCount())
                                          .append("x ")
                                          .append(stack.getItem().getName().getString())
                                          .append(", ");
                            }
                        }
                        
                        if (storedItems.length() > 0) {
                            storedItems.setLength(storedItems.length() - 2); // Remove a última vírgula e espaço
                            VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + 
                                                          " armazenou itens em um baú em " + pos + ": " + 
                                                          storedItems);
                            
                            // Limpa o inventário do villager
                            for (int i = 0; i < inventory.size(); i++) {
                                inventory.setStack(i, ItemStack.EMPTY);
                            }
                            
                            return true;
                        }
                    }
                }
            }
        }
        
        return false; // Não encontrou baús próximos
    }
    
    /**
     * Procura por itens específicos em baús próximos e os adiciona ao inventário do villager
     * @param villager O villager
     * @param world O mundo do servidor
     * @param targetItem O item que o villager está procurando
     * @return true se o item foi encontrado e adicionado ao inventário, false caso contrário
     */
    public static boolean findItemInChests(VillagerEntity villager, ServerWorld world, Item targetItem) {
        // Procura por baús próximos
        BlockPos villagerPos = villager.getBlockPos();
        int searchRadius = 16;
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = villagerPos.add(x, y, z);
                    
                    // Verifica se o bloco é um baú
                    if (world.getBlockState(pos).getBlock().equals(net.minecraft.block.Blocks.CHEST)) {
                        // Simula a busca pelo item no baú (em uma implementação completa, verificaríamos o inventário do baú)
                        // Chance de encontrar o item (simulação)
                        if (world.getRandom().nextFloat() < 0.4f) {
                            // Encontrou o item, adiciona ao inventário do villager
                            int count = 1 + world.getRandom().nextInt(3); // 1-3 itens
                            addItemToInventory(villager, targetItem, count);
                            
                            VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + 
                                                          " encontrou " + count + "x " + 
                                                          targetItem.getName().getString() + 
                                                          " em um baú em " + pos);
                            
                            return true;
                        }
                    }
                }
            }
        }
        
        return false; // Não encontrou o item em nenhum baú próximo
    }
}