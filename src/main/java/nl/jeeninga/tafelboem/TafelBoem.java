package nl.jeeninga.tafelboem;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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

import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.command.TafelBoemCommand;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.game.QuizClient;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.FightLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.log.PlaySummaries;
import nl.jeeninga.tafelboem.net.ModNetworking;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.registry.ModCreativeTab;
import nl.jeeninga.tafelboem.registry.ModEntities;
import nl.jeeninga.tafelboem.registry.ModItems;
import nl.jeeninga.tafelboem.world.ArenaCatalog;
import nl.jeeninga.tafelboem.world.Hub;
import nl.jeeninga.tafelboem.world.TafelBoemWorld;
import nl.jeeninga.tafelboem.world.TafelBoemWorlds;
import nl.jeeninga.tafelboem.world.WorldLayout;

public class TafelBoem implements ModInitializer {
	public static final String MOD_ID = "tafelboem";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static TafelBoemConfig config = new TafelBoemConfig();
	private static @Nullable SessionManager sessions;
	private static @Nullable BossFights fights;
	private static @Nullable FightLog fightLog;
	private static @Nullable PlaySummaries summaries;
	private static @Nullable TafelBoemWorld world;

	@Override
	public void onInitialize() {
		config = TafelBoemConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
		ModBlocks.register();
		ModItems.register();
		ModEntities.register();
		ModCreativeTab.register();
		ModNetworking.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> TafelBoemCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Path logDirectory = server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID);
			AnswerLog log = new AnswerLog(logDirectory);
			fightLog = new FightLog(logDirectory);
			// The playtest numbers go to the game log too, so the log the parent shares is the whole evaluation.
			PlaySummaries playSummaries = new PlaySummaries();
			summaries = playSummaries;
			log.listen(playSummaries::answered);
			fightLog.listen(fight -> {
				LOGGER.info("TafelBoem fight: {}", fight.describe());
				playSummaries.fought(fight);
			});
			sessions = new SessionManager(server, new Learners(log), QuizClient.NETWORK, config);
			fights = new BossFights(sessions);

			ServerLevel overworld = server.overworld();
			if (TafelBoemWorlds.isTafelBoemWorld(overworld)) {
				world = new TafelBoemWorld(overworld, WorldLayout.DEFAULT, fights, fightLog,
						new ArenaCatalog(server.getStructureManager(), new Random(), ArenaCatalog.DEFAULT_FOLDER), QuizClient.NETWORK, true);
				world.setUp();
				LevelDoorBlock.use(world::queueDoor);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (fights != null) {
				fights.endAll();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			sessions = null;
			fights = null;
			fightLog = null;
			summaries = null;
			world = null;
			LevelDoorBlock.use(LevelDoorBlock.NOBODY);
			ProtectedAreas.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (sessions != null) {
				sessions.tick();
			}
			if (fights != null) {
				fights.tick();
			}
			if (world != null) {
				world.tick();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (summaries != null) {
				summaries.joined(handler.player.getPlainTextName(), System.currentTimeMillis());
			}
			if (world != null) {
				world.onJoin(handler.player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (world != null) {
				world.onDisconnect(handler.player);
			}
			if (sessions != null) {
				sessions.onDisconnect(handler.player);
			}
			// Also runs for everyone when the world closes, after their last fight line is written.
			if (summaries != null) {
				summaries.left(handler.player.getPlainTextName(), System.currentTimeMillis())
						.ifPresent(line -> LOGGER.info("TafelBoem session: {}", line));
			}
		});
		// Spectacle animals saved by a previous session (e.g. after a crash) poof instead of piling up,
		// and so do hub frames of an earlier world load: every load places them again.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.isLoadedFromDisk()
					&& (entity.entityTags().contains(BombEffects.SPECTACLE_TAG) || Hub.isLeftOverFrame(entity, world == null ? null : world.hub()))) {
				entity.discard();
			}
		});

		LOGGER.info("TafelBoem loaded - every fuse is a sum!");
	}

	public static TafelBoemConfig config() {
		return config;
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

	public static FightLog fightLog() {
		if (fightLog == null) {
			throw new IllegalStateException("The TafelBoem fights log is only available while a server is running");
		}
		return fightLog;
	}

	/** Only there in a TafelBoem world; any other world plays as before. */
	public static Optional<TafelBoemWorld> world() {
		return Optional.ofNullable(world);
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
