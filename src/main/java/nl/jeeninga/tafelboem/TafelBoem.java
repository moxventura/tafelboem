package nl.jeeninga.tafelboem;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.command.TafelBoemCommand;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.QuizClient;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.ModNetworking;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.registry.ModCreativeTab;
import nl.jeeninga.tafelboem.registry.ModEntities;
import nl.jeeninga.tafelboem.registry.ModItems;

public class TafelBoem implements ModInitializer {
	public static final String MOD_ID = "tafelboem";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static @Nullable SessionManager sessions;
	private static @Nullable BossFights fights;

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModItems.register();
		ModEntities.register();
		ModCreativeTab.register();
		ModNetworking.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> TafelBoemCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			AnswerLog log = new AnswerLog(server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID));
			sessions = new SessionManager(server, new Learners(log), QuizClient.NETWORK);
			fights = new BossFights(sessions);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (fights != null) {
				fights.endAll();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			sessions = null;
			fights = null;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (sessions != null) {
				sessions.tick();
			}
			if (fights != null) {
				fights.tick();
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (sessions != null) {
				sessions.onDisconnect(handler.player);
			}
		});
		// Spectacle animals saved by a previous session (e.g. after a crash) poof instead of piling up.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.isLoadedFromDisk() && entity.entityTags().contains(BombEffects.SPECTACLE_TAG)) {
				entity.discard();
			}
		});

		LOGGER.info("TafelBoem loaded - every fuse is a sum!");
	}

	public static SessionManager sessions() {
		if (sessions == null) {
			throw new IllegalStateException("TafelBoem sessions are only available while a server is running");
		}
		return sessions;
	}

	public static BossFights fights() {
		if (fights == null) {
			throw new IllegalStateException("TafelBoem fights are only available while a server is running");
		}
		return fights;
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
