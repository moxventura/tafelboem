package nl.jeeninga.tafelboem.game;

import java.util.OptionalLong;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.core.AnswerTimes;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;

/**
 * One question for one rooted player.
 */
public final class QuizSession {
	public enum State {
		ASKING,
		CORRECTING
	}

	private final int id;
	private final UUID playerId;
	private final Fact fact;
	private final BombTier tier;
	private final BlockPos bombPos;
	private final Vec3 anchor;
	private final String context;
	private final @Nullable QuizListener listener;
	private final long startedAtMillis;
	private State state = State.ASKING;
	private int ageTicks;
	private long answerMillis = -1;
	private OptionalLong usualMillis = OptionalLong.empty();

	QuizSession(int id, UUID playerId, Fact fact, BombTier tier, BlockPos bombPos, Vec3 anchor, String context,
			@Nullable QuizListener listener) {
		this.id = id;
		this.playerId = playerId;
		this.fact = fact;
		this.tier = tier;
		this.bombPos = bombPos;
		this.anchor = anchor;
		this.context = context;
		this.listener = listener;
		this.startedAtMillis = System.currentTimeMillis();
	}

	public int id() {
		return id;
	}

	public UUID playerId() {
		return playerId;
	}

	public Fact fact() {
		return fact;
	}

	public BombTier tier() {
		return tier;
	}

	public BlockPos bombPos() {
		return bombPos;
	}

	public Vec3 anchor() {
		return anchor;
	}

	public String context() {
		return context;
	}

	public @Nullable QuizListener listener() {
		return listener;
	}

	public long elapsedMillis() {
		return System.currentTimeMillis() - startedAtMillis;
	}

	/** The right answer came after {@code millis}; {@code usual} is the kid's usual time from before it. */
	void answered(long millis, OptionalLong usual) {
		this.answerMillis = millis;
		this.usualMillis = usual;
	}

	/** How long the right answer took, in milliseconds; -1 before it was given. */
	public long answerMillis() {
		return answerMillis;
	}

	/** The kid's usual time for this sum from before this answer; empty with too little history. */
	public OptionalLong usualMillis() {
		return usualMillis;
	}

	public boolean wasFast() {
		return AnswerTimes.isFast(usualMillis, answerMillis);
	}

	public State state() {
		return state;
	}

	void startCorrecting() {
		state = State.CORRECTING;
		ageTicks = 0;
	}

	int tickAge() {
		return ++ageTicks;
	}
}
