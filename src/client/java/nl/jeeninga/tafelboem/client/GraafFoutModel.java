package nl.jeeninga.tafelboem.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.state.EvokerRenderState;

import nl.jeeninga.tafelboem.core.YeetPose;

/**
 * The vanilla evoker model with one pose of Graaf Fout's own: for the Knalletje's signature he
 * stands on one leg and holds his sore toe with both hands.
 */
public class GraafFoutModel extends IllagerModel<EvokerRenderState> {
	private final ModelPart arms;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;

	public GraafFoutModel(ModelPart root) {
		super(root);
		this.arms = root.getChild("arms");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
	}

	@Override
	public void setupAnim(EvokerRenderState state) {
		super.setupAnim(state);
		if (state instanceof GraafFoutRenderState yeet && yeet.shown == YeetPose.TOE_HOP) {
			rightLeg.xRot = -1.1F;
			// Both hands reach for the raised foot instead of the folded arms.
			arms.visible = false;
			rightArm.visible = true;
			leftArm.visible = true;
			rightArm.xRot = -0.9F;
			rightArm.zRot = 0.25F;
			leftArm.xRot = -0.9F;
			leftArm.zRot = 0.45F;
		}
	}
}
