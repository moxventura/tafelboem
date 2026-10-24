package nl.jeeninga.tafelboem.boss;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.landing.Landing;
import nl.jeeninga.tafelboem.boss.yeet.Yeet;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;

/**
 * Graaf Fout (Count Wrong): an Evoker body with all its spells removed. He hovers, teleports, throws
 * bombs, and gets hilariously yeeted every time a kid kicks a bomb back at him.
 *
 * <p>He can't be hurt by anything else and is never saved with the world; a fight lives only as
 * long as the server session.
 */
public class GraafFout extends Evoker {
	private static final EntityDataAccessor<Byte> DATA_YEET_POSE = SynchedEntityData.defineId(GraafFout.class, EntityDataSerializers.BYTE);

	private static final double HOVER_HEIGHT = 1.5;

	private @Nullable BossFight fight;
	/** The flight he is on, while he is being yeeted. */
	private @Nullable Yeet yeet;
	/** What he does once a yeet has put him down. */
	private @Nullable Landing landing;
	private boolean finale;

	/** Client side: when the pose last changed and what came before, so the renderer can ease in and out. */
	private YeetPose shownPose = YeetPose.NONE;
	private YeetPose previousPose = YeetPose.NONE;
	private YeetPose.Transform previousTransform = YeetPose.Transform.UPRIGHT;
	private int poseChangedAt;

	public GraafFout(EntityType<? extends GraafFout> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Evoker.createAttributes();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32.0F, 1.0F));
	}

	void attach(BossFight fight) {
		this.fight = fight;
		setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_YEET_POSE, (byte) YeetPose.NONE.ordinal());
	}

	/** The extra roll or faceplant on top of his spin; the server decides, every client draws it. */
	public YeetPose yeetPose() {
		return YeetPose.byId(entityData.get(DATA_YEET_POSE));
	}

	public void setYeetPose(YeetPose pose) {
		entityData.set(DATA_YEET_POSE, (byte) pose.ordinal());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_YEET_POSE.equals(accessor) && level().isClientSide()) {
			previousTransform = renderTransform(0F);
			previousPose = shownPose;
			shownPose = yeetPose();
			poseChangedAt = tickCount;
		}
	}

	/** Client side: the pose he is in right now, none while he settles back up. */
	public YeetPose shownPose() {
		return shownPose;
	}

	/** Client side: the pose whose pivot the renderer should use, including while settling back up. */
	public YeetPose renderPose() {
		return shownPose != YeetPose.NONE ? shownPose : previousPose;
	}

	/** Client side: the extra roll, pitch, squash and sink to draw him with right now. */
	public YeetPose.Transform renderTransform(float partialTick) {
		float ticks = tickCount - poseChangedAt + partialTick;
		return shownPose != YeetPose.NONE ? shownPose.transform(ticks) : YeetPose.settle(previousTransform, ticks);
	}

	public boolean isReacting() {
		return yeet != null || landing != null;
	}

	@Override
	public void checkDespawn() {
		// Not even peaceful difficulty makes him disappear mid-fight.
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Only answers hurt Graaf Fout: swords, arrows and lava do nothing.
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
		// Running round after a landing, he would shove the kids about.
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level) {
			if (fight == null) {
				discard();
			} else if (finale) {
				flyAway(level);
			} else if (yeet != null) {
				if (yeet.tick(level)) {
					land(level);
				}
			} else if (landing != null) {
				if (landing.tick(level)) {
					landing.finish();
					landing = null;
				}
			} else {
				hover(level);
			}
		}
	}

	/**
	 * A kid kicked a bomb back: launch him away from the explosion, spinning and flailing. How he
	 * flies is up to the style's {@link Yeet}.
	 *
	 * @param kicker where the kid who kicked stands; the boomerang comes back to them
	 */
	public void yeet(Vec3 from, double power, YeetStyle style, Vec3 kicker) {
		// Before the flight starts: ending a landing puts him upright with gravity off.
		endLanding();
		Vec3 direction = position().subtract(from).multiply(1, 0, 1);
		if (direction.lengthSqr() < 1.0E-4) {
			direction = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
		}
		yeet = Yeet.start(style, this, direction.normalize(), power, kicker);
		setYeetPose(yeet.startPose());
		needsSync = true;
		armsUp(true);
		playSound(SoundEvents.EVOKER_HURT, 1.5F, 1.9F);
	}

	/**
	 * Defeated: one last enormous yeet, straight into the sky.
	 */
	public void finalYeet() {
		finale = true;
		yeet = null;
		endLanding();
		setYeetPose(YeetPose.NONE);
		setNoGravity(true);
		armsUp(true);
		playSound(SoundEvents.EVOKER_HURT, 2.0F, 0.6F);
	}

	/**
	 * He flew out of the arena (or fell through a gap): poof back near the kids, dizzy as after a
	 * normal landing when he was being yeeted.
	 *
	 * @return the yeet he was on, or null when he wasn't being yeeted
	 */
	public @Nullable Yeet comeBack(ServerLevel level, Vec3 nearKid, ArenaBox arena) {
		Yeet cut = yeet;
		yeet = null;
		endLanding();
		setYeetPose(YeetPose.NONE);
		setDeltaMovement(Vec3.ZERO);
		armsUp(false);
		teleportNear(level, nearKid, arena);
		if (cut != null) {
			startLanding(LandingStyle.DIZZY, Landing.Scene.NONE);
			// After the landing has begun: finish() of a cut-short landing switched gravity off.
			setNoGravity(false);
		} else {
			setNoGravity(true);
		}
		return cut;
	}

	public void teleportNear(ServerLevel level, Vec3 around) {
		teleportNear(level, around, null);
	}

	/**
	 * Pops up 7 to 10 blocks from {@code around}. In an arena only over its footprint, and only where
	 * there is ground (outside the arena the heightmap is the void's).
	 */
	public void teleportNear(ServerLevel level, Vec3 around, @Nullable ArenaBox arena) {
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 7 + random.nextDouble() * 3;
			double x = around.x + Math.cos(angle) * distance;
			double z = around.z + Math.sin(angle) * distance;
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
			if (arena == null || arena.containsColumn(x, z) && ground > arena.minY()) {
				appear(level, x, ground + HOVER_HEIGHT, z);
				return;
			}
		}
		// No ground around the kid (say, in a corner): the middle of the arena always has some.
		double x = arena.middleX();
		double z = arena.middleZ();
		appear(level, x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + HOVER_HEIGHT, z);
	}

	private void appear(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
		teleportTo(x, y, z);
		level.sendParticles(ParticleTypes.POOF, x, y + 1, z, 20, 0.3, 0.6, 0.3, 0.02);
		level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.4F);
	}

	private void hover(ServerLevel level) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
		double targetY = ground + HOVER_HEIGHT + Math.sin(tickCount * 0.1) * 0.25;
		double dy = Mth.clamp((targetY - getY()) * 0.1, -0.15, 0.15);
		setDeltaMovement(0, dy, 0);
	}

	/** Turns him to look the way of {@code direction}, without his usual spin. */
	public void face(Vec3 direction) {
		float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90F;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** Stops spinning, turns to face where he was flying and falls flat on his face. */
	public void faceplantTowards(Vec3 direction) {
		face(direction);
		setYeetPose(YeetPose.FACEPLANT);
	}

	private void land(ServerLevel level) {
		Yeet landed = yeet;
		yeet = null;
		if (yeetPose() == YeetPose.TUMBLE || yeetPose() == YeetPose.SPLAT) {
			setYeetPose(YeetPose.NONE);
		}
		setNoGravity(false);
		setDeltaMovement(Vec3.ZERO);
		armsUp(false);
		for (int i = 0; i < 24; i++) {
			double angle = Math.PI * 2 * i / 24;
			level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(angle) * 1.2, getY() + 0.1, getZ() + Math.sin(angle) * 1.2,
					1, 0, 0, 0, 0.05);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_BIG_FALL, SoundSource.HOSTILE, 1.5F, 0.8F);
		if (fight != null) {
			fight.onYeetLanded(landed);
		}
		if (landing == null) {
			// No fight to pick one (or it's over): the dizzy spell, as always.
			startLanding(LandingStyle.DIZZY, Landing.Scene.NONE);
		}
	}

	/** He is down after a yeet: the fight says how he lands. Any landing still going ends first. */
	public void startLanding(LandingStyle style, Landing.Scene scene) {
		endLanding();
		if (level() instanceof ServerLevel level) {
			landing = Landing.start(style, this, scene, level);
		}
	}

	/** The flight he is on, or null when he isn't being yeeted. */
	@Nullable YeetStyle yeetStyle() {
		return yeet == null ? null : yeet.style();
	}

	/** The landing he is in, or null when he isn't down. */
	public @Nullable LandingStyle landingStyle() {
		return landing == null ? null : landing.style();
	}

	void endLanding() {
		if (landing != null) {
			landing.finish();
			landing = null;
		}
	}

	private void flyAway(ServerLevel level) {
		spin(60F);
		setDeltaMovement(0, 1.2, 0);
		level.sendParticles(ParticleTypes.FIREWORK, getX(), getY(), getZ(), 4, 0.2, 0.2, 0.2, 0.05);
	}

	/** Turns him round on the spot; every yeet spins (playtest 1: the spin was the funniest thing). */
	public void spin(float degrees) {
		float yaw = getYRot() + degrees;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** Arms up (flailing, or waving while he runs) or down. */
	public void armsUp(boolean up) {
		spellCastingTickCount = up ? 200 : 0;
		setIsCastingSpell(up ? IllagerSpell.WOLOLO : IllagerSpell.NONE);
	}
}
