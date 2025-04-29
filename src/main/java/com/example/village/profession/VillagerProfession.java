package com.example.village.profession;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Interface base para as profissões personalizadas de villagers
 * Define os métodos que todas as profissões devem implementar
 */
public interface VillagerProfession {
    
    /**
     * Obtém o nome da profissão
     * @return Nome da profissão
     */
    String getName();
    
    /**
     * Executa as ações específicas da profissão a cada tick
     * @param villager O villager que possui esta profissão
     * @param world O mundo do servidor
     */
    void onTick(VillagerEntity villager, ServerWorld world);
    
    /**
     * Verifica se o villager pode armazenar itens em baús
     * @param villager O villager que possui esta profissão
     * @param world O mundo do servidor
     * @return true se o villager pode armazenar itens, false caso contrário
     */
    boolean canStoreItems(VillagerEntity villager, ServerWorld world);
    
    /**
     * Tenta armazenar os itens coletados em baús próximos
     * @param villager O villager que possui esta profissão
     * @param world O mundo do servidor
     * @return true se os itens foram armazenados com sucesso, false caso contrário
     */
    boolean storeItems(VillagerEntity villager, ServerWorld world);
}