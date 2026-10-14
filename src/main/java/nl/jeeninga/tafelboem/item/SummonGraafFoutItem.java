package nl.jeeninga.tafelboem.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * "Roep Graaf Fout op!": summons the villain a few blocks in front of the player. In a TafelBoem
 * world the doors are the way to fight him, so there it only says so.
 */
public class SummonGraafFoutItem extends Item {
	public SummonGraafFoutItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel) {
			if (TafelBoem.world().filter(world -> world.level() == serverLevel).isPresent()) {
				// Without an arena a yeet over the void could carry him out of reach for good.
				player.sendSystemMessage(Component.translatable("tafelboem.message.use_the_doors"));
				return InteractionResult.SUCCESS;
			}
			Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
			Vec3 spot = player.position().add(look.scale(8)).add(0, 1.5, 0);
			if (!TafelBoem.fights().summon(serverLevel, spot)) {
				player.sendSystemMessage(Component.translatable("tafelboem.message.boss_already_here"));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
