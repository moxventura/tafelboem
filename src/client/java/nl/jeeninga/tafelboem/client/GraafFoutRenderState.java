package nl.jeeninga.tafelboem.client;

import net.minecraft.client.renderer.entity.state.EvokerRenderState;

import nl.jeeninga.tafelboem.core.YeetPose;

/**
 * The evoker's render state plus how far Graaf Fout is rolled, flipped, squashed or sunk right now.
 */
public class GraafFoutRenderState extends EvokerRenderState {
	/** The pose to turn round (it stays on while he settles back up). */
	public YeetPose pose = YeetPose.NONE;
	/** The pose he is in right now (none while he settles back up): what the texture and the model look at. */
	public YeetPose shown = YeetPose.NONE;
	public YeetPose.Transform transform = YeetPose.Transform.UPRIGHT;
}
