package nl.jeeninga.tafelboem.client;

import net.minecraft.client.renderer.entity.state.EvokerRenderState;

import nl.jeeninga.tafelboem.core.YeetPose;

/**
 * The evoker's render state plus how far Graaf Fout is rolled, flipped or face down right now.
 */
public class GraafFoutRenderState extends EvokerRenderState {
	public YeetPose pose = YeetPose.NONE;
	public YeetPose.Angles angles = YeetPose.Angles.UPRIGHT;
}
