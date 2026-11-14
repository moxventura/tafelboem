package nl.jeeninga.tafelboem.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.BombTier;

/**
 * A bomb Graaf Fout threw and missed, caught by a kid (R1.4). Not a block item on purpose: it can't be placed, so it
 * never starts a free-play question, and a kid can't leave it lying about. Right-clicking it throws it back at him
 * (Task 8 adds that); outside a running fight the game takes it away again.
 */
public class TerugbomItem extends Item {
	private final BombTier tier;

	public TerugbomItem(BombTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public BombTier tier() {
		return tier;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel && player instanceof ServerPlayer kid) {
			TafelBoem.fights().throwBack(kid, hand);
		}
		return InteractionResult.SUCCESS;
	}
}
