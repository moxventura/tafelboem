package nl.jeeninga.tafelboem.command;

import java.util.List;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.Stars;
import nl.jeeninga.tafelboem.core.StatsFormat;
import nl.jeeninga.tafelboem.core.StatsReport;

/**
 * {@code /tafelboem stats [player]}: a parent-friendly summary of the answer log and the fights log.
 * {@code /tafelboem arena}: make this world fit for TafelBoem (needs cheats, like {@code /gamerule}).
 */
public final class TafelBoemCommand {
	private TafelBoemCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tafelboem")
				.then(Commands.literal("arena")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> arena(context.getSource())))
				.then(Commands.literal("stats")
						.executes(context -> stats(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> stats(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
	}

	private static int arena(CommandSourceStack source) {
		BlockPos center = BlockPos.containing(source.getPosition());
		int removed = ArenaSetup.apply(source.getLevel(), center);
		source.sendSuccess(() -> Component.translatable("tafelboem.arena.done", removed).withStyle(ChatFormatting.GOLD), true);
		return 1;
	}

	private static int stats(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
		String name = player.getPlainTextName();
		StatsReport report = StatsReport.of(TafelBoem.sessions().learners().log().read(name));

		if (report.totalAnswers() == 0) {
			source.sendSuccess(() -> Component.translatable("tafelboem.stats.empty", name), false);
			return 0;
		}

		int percent = Math.round(100f * report.totalCorrect() / report.totalAnswers());
		source.sendSuccess(() -> Component.translatable("tafelboem.stats.header", name, report.totalAnswers(), percent)
				.withStyle(ChatFormatting.GOLD), false);

		List<FightRecord> fights = TafelBoem.fightLog().read(name);
		for (FightLevel fightLevel : FightLevel.DOORS) {
			List<FightRecord> onLevel = fights.stream().filter(fight -> fight.level() == fightLevel.number()).toList();
			if (onLevel.isEmpty()) {
				continue;
			}
			long won = onLevel.stream().filter(FightRecord::won).count();
			Component levelName = Component.translatable("tafelboem.level." + fightLevel.number());
			Component line = TafelBoem.config().stars
					? Component.translatable("tafelboem.stats.level", levelName, Stars.show(Stars.best(fights, fightLevel.number())), won, onLevel.size())
					: Component.translatable("tafelboem.stats.level_no_stars", levelName, won, onLevel.size());
			source.sendSuccess(() -> line, false);
		}

		String language = source.getPlayer() != null ? source.getPlayer().clientInformation().language() : null;
		for (int table = Fact.MIN_FACTOR; table <= Fact.MAX_FACTOR; table++) {
			StatsReport.TableStats stats = report.table(table);
			if (stats.attempts() == 0) {
				continue;
			}
			String seconds = StatsFormat.seconds(stats.medianCorrectMillis(), language);
			ChatFormatting color = stats.accuracyPercent() >= 90 ? ChatFormatting.GREEN
					: stats.accuracyPercent() >= 70 ? ChatFormatting.YELLOW : ChatFormatting.RED;
			source.sendSuccess(() -> Component.translatable("tafelboem.stats.table", stats.table(), stats.correct(),
					stats.attempts(), seconds).withStyle(color), false);
		}

		List<Fact> practising = report.stillPractising(5);
		if (practising.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("tafelboem.stats.hardest_none"), false);
		} else {
			String practisingText = practising.stream().map(fact -> fact.a() + "×" + fact.b()).collect(Collectors.joining(", "));
			source.sendSuccess(() -> Component.translatable("tafelboem.stats.hardest", practisingText), false);
		}

		StatsReport.ReaskStats reasks = report.reasks();
		if (reasks.asked() > 0) {
			source.sendSuccess(() -> Component.translatable("tafelboem.stats.reasks", reasks.correct(), reasks.asked(),
					reasks.percent()), false);
		}
		int misclicks = report.likelyMisclicks();
		if (misclicks > 0) {
			source.sendSuccess(() -> Component.translatable("tafelboem.stats.misclicks", misclicks), false);
		}
		return report.totalAnswers();
	}
}
