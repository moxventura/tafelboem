package nl.jeeninga.tafelboem.command;

import java.util.List;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.core.StatsReport;

/**
 * {@code /tafelboem stats [player]}: a parent-friendly summary of the answer log.
 */
public final class TafelBoemCommand {
	private TafelBoemCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tafelboem")
				.then(Commands.literal("stats")
						.executes(context -> stats(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> stats(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
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

		for (int table = Fact.MIN_FACTOR; table <= Fact.MAX_FACTOR; table++) {
			StatsReport.TableStats stats = report.table(table);
			if (stats.attempts() == 0) {
				continue;
			}
			String seconds = String.format("%.1f", stats.medianCorrectMillis() / 1000.0);
			ChatFormatting color = stats.accuracyPercent() >= 90 ? ChatFormatting.GREEN
					: stats.accuracyPercent() >= 70 ? ChatFormatting.YELLOW : ChatFormatting.RED;
			source.sendSuccess(() -> Component.translatable("tafelboem.stats.table", stats.table(), stats.correct(),
					stats.attempts(), seconds).withStyle(color), false);
		}

		List<Fact> hardest = report.hardestFacts(5);
		String hardestText = hardest.stream().map(fact -> fact.a() + "×" + fact.b()).collect(Collectors.joining(", "));
		source.sendSuccess(() -> Component.translatable("tafelboem.stats.hardest", hardestText), false);
		return report.totalAnswers();
	}
}
