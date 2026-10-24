package nl.jeeninga.tafelboem.client;

import net.minecraft.client.renderer.entity.state.EvokerRenderState;

import nl.jeeninga.tafelboem.core.YeetPose;

/**
 * The evoker's render state plus how far Graaf Fout is rolled, flipped, squashed or sunk right now.
 */
public class GraafFoutRenderState extends EvokerRenderState {
	public YeetPose pose = YeetPose.NONE;
	public YeetPose.Transform transform = YeetPose.Transform.UPRIGHT;
}
