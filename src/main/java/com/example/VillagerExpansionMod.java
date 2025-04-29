package com.example;

import com.example.village.VillageExpansionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerExpansionMod implements ModInitializer {
	public static final String MOD_ID = "villager_expansion";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static VillageExpansionManager expansionManager;
	
	@Override
	public void onInitialize() {
		LOGGER.info("Inicializando Villager Expansion Mod!");
		
		// Inicializa o gerenciador de expansão de vilas
		expansionManager = new VillageExpansionManager();
		
		// Registra eventos para o ciclo de vida do servidor
		ServerWorldEvents.LOAD.register((server, world) -> {
			LOGGER.info("Carregando dados de vilas para o mundo: " + world.getRegistryKey().getValue());
			expansionManager.onWorldLoad(world);
		});
		
		// Registra eventos de tick para processar a expansão da vila
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			expansionManager.onWorldTick(world);
		});
		
		LOGGER.info("Villager Expansion Mod inicializado com sucesso!");
	}
	
	public static VillageExpansionManager getExpansionManager() {
		return expansionManager;
	}
}