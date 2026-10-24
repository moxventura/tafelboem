package nl.jeeninga.tafelboem.boss;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.yeet.Yeet;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.YeetReport;
import nl.jeeninga.tafelboem.game.QuizClient;

/**
 * The yeet-o-meter: after every yeet everyone in the fight sees how far Graaf Fout flew. The kid who
 * kicked also sees why he flew further (a fast answer, right answers in a row) and when it beat
 * their own record: personal bests only, never a ranking between siblings. Every kick also gets a
 * line in the game log, because the action bar never reaches it.
 */
final class YeetMeter {
	private final GraafFout boss;
	private final Participants participants;
	private final QuizClient client;
	private final TafelBoemConfig config;
	private @Nullable YeetRecords records;

	YeetMeter(GraafFout boss, Participants participants, QuizClient client, TafelBoemConfig config) {
		this.boss = boss;
		this.participants = participants;
		this.client = client;
		this.config = config;
	}

	void useRecords(@Nullable YeetRecords records) {
		this.records = records;
	}

	/**
	 * He is down after a yeet, or back after flying out of the arena.
	 *
	 * @param kick    the kick that sent him; null for a yeet on cue (a test)
	 * @param landing the landing's id, for the game log
	 * @param factor  how much further the kick sent him
	 */
	void landed(Yeet yeet, boolean flewOut, @Nullable Kick kick, String landing, boolean signature, double factor) {
		ServerLevel level = (ServerLevel) boss.level();
		ServerPlayer kicker = kick == null ? null : level.getServer().getPlayerList().getPlayer(kick.kicker());
		int blocks = yeet.blocks();
		int previousBest = 0;
		boolean record = false;
		if (kicker != null && records != null && !yeet.measuresHeight()) {
			previousBest = records.best(kicker);
			if (blocks > previousBest) {
				records.beaten(kicker, blocks);
				// The first yeet on file only sets the best: the first kick after the update is no record.
				record = previousBest > 0 && config.yeetRecords;
			}
		}

		if (flewOut || blocks >= 1) {
			// A helicopter says how high, even when it drops him out of the arena: a height is no distance.
			String key = yeet.measuresHeight() ? "tafelboem.message.yeet_height"
					: flewOut ? "tafelboem.message.yeet_out" : "tafelboem.message.yeet_distance";
			Component plain = Component.translatable(key, boss.getDisplayName(), blocks).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
			for (ServerPlayer player : flewOut ? participants.players() : participants.nearBoss()) {
				client.actionBar(player, player == kicker ? forKicker(plain, kick, record, blocks) : plain);
			}
		}
		if (record) {
			client.sound(kicker, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
		}
		if (kick != null) {
			String name = kicker != null ? kicker.getPlainTextName() : kick.kicker().toString();
			TafelBoem.LOGGER.info("TafelBoem yeet: {}", new YeetReport(name, kick.tier(), kick.fact(), kick.millis(), kick.usual(),
					kick.fast(), kick.streak(), factor, yeet.style(), landing, signature, blocks, yeet.measuresHeight(), flewOut,
					record, previousBest).describe());
		}
	}

	/** The kicker's own meter: a new record, or why he flew further. */
	private Component forKicker(Component plain, Kick kick, boolean record, int blocks) {
		if (record) {
			return Component.translatable("tafelboem.message.yeet_record", boss.getDisplayName(), blocks)
					.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		}
		Component why = why(kick);
		return why == null ? plain : plain.copy().append(" ").append(why);
	}

	private @Nullable Component why(Kick kick) {
		if (!config.yeetBoost) {
			return null;
		}
		boolean streak = kick.streak() >= 2;
		if (kick.fast() && streak) {
			return Component.translatable("tafelboem.message.yeet_why.both", kick.streak());
		}
		if (kick.fast()) {
			return Component.translatable("tafelboem.message.yeet_why.fast");
		}
		return streak ? Component.translatable("tafelboem.message.yeet_why.streak", kick.streak()) : null;
	}
}
