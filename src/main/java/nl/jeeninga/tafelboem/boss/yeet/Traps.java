package nl.jeeninga.tafelboem.boss.yeet;

import java.util.List;

import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.core.TrapPick;

/** Where a yeet finds traps to land in (R1.3): the fight knows its arena and its lying bombs. */
@FunctionalInterface
public interface Traps {
	/** A yeet without traps: on cue, with the switch off, or outside an arena. */
	Traps NONE = at -> List.of();

	/** The trap blocks he could land in or on, around this spot. */
	List<TrapPick.Candidate> around(Vec3 at);
}
