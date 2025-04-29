package com.example.village.profession;

import com.example.VillagerExpansionMod;
import com.example.village.VillageData;
import com.example.village.relation.VillageRelationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Implementação da profissão de Guerreiro para villagers
 * Os guerreiros defendem a vila contra ameaças e coletam itens dos inimigos derrotados
 * Também participam de conflitos entre vilas
 */
public class WarriorProfession implements VillagerProfession {
    
    private final Random random = new Random();
    private boolean hasEquipment = false;
    private int patrolCooldown = 0;
    private static final int PATROL_INTERVAL = 200; // 10 segundos
    
    // Rastreamento de alvo para conflitos entre vilas
    private UUID targetVillageId = null;
    private boolean isAttacking = false;
    private int conflictCooldown = 0;
    private static final int CONFLICT_CHECK_INTERVAL = 400; // 20 segundos
    
    @Override
    public String getName() {
        return "Guerreiro";
    }
    
    @Override
    public void onTick(VillagerEntity villager, ServerWorld world) {
        // Equipa o villager com armas e armaduras se ainda não estiver equipado
        if (!hasEquipment) {
            equipWarrior(villager);
            hasEquipment = true;
        }
        
        // Verifica se deve participar de conflitos entre vilas
        conflictCooldown--;
        if (conflictCooldown <= 0) {
            checkVillageConflicts(villager, world);
            conflictCooldown = CONFLICT_CHECK_INTERVAL;
        }
        
        // Se estiver atacando outra vila, foca nessa tarefa
        if (isAttacking && targetVillageId != null) {
            attackEnemyVillage(villager, world);
            return;
        }
        
        // Patrulha a área em busca de ameaças normais
        patrolCooldown--;
        if (patrolCooldown <= 0) {
            patrolArea(villager, world);
            patrolCooldown = PATROL_INTERVAL;
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
                        VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + " armazenou itens em um baú em " + pos);
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
     * Equipa o villager com armas e armaduras
     * @param villager O villager a ser equipado
     */
    private void equipWarrior(VillagerEntity villager) {
        // Equipa o villager com uma espada de ferro
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        
        // Chance de equipar com armadura
        if (random.nextFloat() < 0.5f) {
            villager.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
        
        if (random.nextFloat() < 0.3f) {
            villager.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        }
        
        // Impede que os itens caiam quando o villager morre
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0f);
        villager.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0f);
        
        VillagerExpansionMod.LOGGER.info("Villager " + villager.getUuid() + " equipado como Guerreiro");
    }
    
    /**
     * Patrulha a área em busca de ameaças
     * @param villager O villager guerreiro
     * @param world O mundo do servidor
     */
    private void patrolArea(VillagerEntity villager, ServerWorld world) {
        BlockPos villagerPos = villager.getBlockPos();
        int searchRadius = 32;
        
        // Cria uma caixa de colisão para buscar entidades hostis
        Box searchBox = new Box(
                villagerPos.getX() - searchRadius, villagerPos.getY() - 8, villagerPos.getZ() - searchRadius,
                villagerPos.getX() + searchRadius, villagerPos.getY() + 8, villagerPos.getZ() + searchRadius
        );
        
        // Busca por entidades hostis próximas
        List<Entity> nearbyEntities = world.getOtherEntities(villager, searchBox, entity -> entity instanceof HostileEntity);
        
        if (!nearbyEntities.isEmpty()) {
            // Encontrou uma ameaça, ataca a entidade mais próxima
            Entity target = nearbyEntities.get(0);
            double closestDistance = villager.squaredDistanceTo(target);
            
            for (Entity entity : nearbyEntities) {
                double distance = villager.squaredDistanceTo(entity);
                if (distance < closestDistance) {
                    target = entity;
                    closestDistance = distance;
                }
            }
            
            // Ataca a entidade mais próxima
            // Nota: Em uma implementação completa, adicionaríamos goals ao villager para atacar entidades hostis
            VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + " encontrou uma ameaça: " + target.getType().getName().getString());
        }
    }
    
    /**
     * Verifica se há conflitos entre vilas e se este guerreiro deve participar
     * @param villager O villager guerreiro
     * @param world O mundo do servidor
     */
    private void checkVillageConflicts(VillagerEntity villager, ServerWorld world) {
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
        
        // Obtém o gerenciador de relações entre vilas
        VillageRelationManager relationManager = VillagerExpansionMod.getExpansionManager().getVillageRelationManager();
        
        // Verifica se há conflitos ativos envolvendo a vila deste villager
        for (VillageData otherVillage : VillagerExpansionMod.getExpansionManager().getVillages()) {
            // Não verifica a própria vila
            if (otherVillage.getVillageId().equals(villagerVillage.getVillageId())) {
                continue;
            }
            
            // Verifica se há um conflito ativo entre as vilas
            if (relationManager.hasActiveConflict(villagerVillage.getVillageId(), otherVillage.getVillageId())) {
                // Chance de participar do conflito (nem todos os guerreiros participam ao mesmo tempo)
                if (random.nextFloat() < 0.7f) { // 70% de chance
                    targetVillageId = otherVillage.getVillageId();
                    isAttacking = true;
                    VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + 
                                                  " mobilizado para conflito contra vila " + 
                                                  targetVillageId);
                    return;
                }
            }
        }
        
        // Se chegou aqui, não há conflitos ativos ou o guerreiro não foi selecionado para participar
        // Reseta o estado de ataque se estava atacando anteriormente
        if (isAttacking) {
            isAttacking = false;
            targetVillageId = null;
            VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + " retornou de conflito");
        }
    }
    
    /**
     * Ataca uma vila inimiga durante um conflito
     * @param villager O villager guerreiro
     * @param world O mundo do servidor
     */
    private void attackEnemyVillage(VillagerEntity villager, ServerWorld world) {
        if (targetVillageId == null) {
            isAttacking = false;
            return;
        }
        
        // Encontra a vila alvo
        final VillageData targetVillage;
        {
            VillageData tempVillage = null;
            for (VillageData village : VillagerExpansionMod.getExpansionManager().getVillages()) {
                if (village.getVillageId().equals(targetVillageId)) {
                    tempVillage = village;
                    break;
                }
            }
            targetVillage = tempVillage;
        }
        
        if (targetVillage == null) {
            // Vila alvo não existe mais
            isAttacking = false;
            targetVillageId = null;
            return;
        }
        
        // Obtém a posição central da vila alvo
        BlockPos targetPos = targetVillage.getCenter();
        
        // Verifica se o guerreiro está próximo da vila alvo
        double distanceToTarget = villager.getBlockPos().getSquaredDistance(targetPos);
        
        if (distanceToTarget > 100 * 100) { // Mais de 100 blocos de distância
            // Guerreiro está longe da vila alvo, tenta se aproximar
            // Em uma implementação completa, usaríamos pathfinding para mover o villager
            VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + 
                                          " se aproximando da vila alvo em " + targetPos);
            return;
        }
        
        // Guerreiro está próximo da vila alvo, procura por villagers inimigos
        int searchRadius = 32;
        Box searchBox = new Box(
                villager.getX() - searchRadius, villager.getY() - 8, villager.getZ() - searchRadius,
                villager.getX() + searchRadius, villager.getY() + 8, villager.getZ() + searchRadius
        );
        
        // Busca por villagers da vila inimiga
        List<Entity> nearbyEntities = world.getOtherEntities(villager, searchBox, entity -> {
            if (entity instanceof VillagerEntity) {
                VillagerEntity enemyVillager = (VillagerEntity) entity;
                // Verifica se o villager pertence à vila alvo
                return targetVillage.getVillagers().contains(enemyVillager.getUuid());
            }
            return false;
        });
        
        if (!nearbyEntities.isEmpty()) {
            // Encontrou um villager inimigo, ataca o mais próximo
            Entity target = nearbyEntities.get(0);
            double closestDistance = villager.squaredDistanceTo(target);
            
            for (Entity entity : nearbyEntities) {
                double distance = villager.squaredDistanceTo(entity);
                if (distance < closestDistance) {
                    target = entity;
                    closestDistance = distance;
                }
            }
            
            // Ataca o villager inimigo
            // Em uma implementação completa, adicionaríamos goals ao villager para atacar
            VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + 
                                          " atacando villager inimigo " + target.getUuid() + 
                                          " da vila " + targetVillageId);
            
            // Chance de causar dano ao villager inimigo
            if (random.nextFloat() < 0.3f && closestDistance < 4) { // 30% de chance se estiver próximo
                // Simula um ataque bem-sucedido
                VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + 
                                              " causou dano ao villager inimigo " + target.getUuid());
                
                // Em uma implementação completa, causaríamos dano real à entidade
                // ((VillagerEntity)target).damage(DamageSource.mob(villager), 2.0f);
            }
        } else {
            // Não encontrou villagers inimigos, procura por estruturas para danificar
            // Em uma implementação completa, poderíamos danificar construções da vila inimiga
            VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + 
                                          " procurando por estruturas para danificar na vila " + 
                                          targetVillageId);
        }
        
        // Chance de encerrar o ataque e retornar à vila
        if (random.nextFloat() < 0.05f) { // 5% de chance a cada verificação
            isAttacking = false;
            targetVillageId = null;
            VillagerExpansionMod.LOGGER.info("Guerreiro " + villager.getUuid() + " retornando à vila após ataque");
        }
    }
    
    /**
     * Define a vila alvo para ataque durante um conflito
     * @param targetId ID da vila alvo
     */
    public void setTargetVillage(UUID targetId) {
        this.targetVillageId = targetId;
        this.isAttacking = targetId != null;
    }
    
    /**
     * Verifica se o guerreiro está atacando uma vila
     * @return true se está atacando, false caso contrário
     */
    public boolean isAttacking() {
        return isAttacking;
    }
    
    /**
     * Obtém o ID da vila alvo
     * @return UUID da vila alvo, ou null se não estiver atacando
     */
    public UUID getTargetVillageId() {
        return targetVillageId;
    }
}