package nl.jeeninga.tafelboem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.state.EvokerRenderState;
import net.minecraft.resources.Identifier;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetPose;

/**
 * The vanilla evoker model in Graaf Fout's purple cape with a big red ✗. On top of the spin the
 * server sends, it draws the tumble (rolling and flipping around his middle) and the faceplant
 * (tipping forward around his feet).
 */
public class GraafFoutRenderer extends EvokerRenderer<GraafFout> {
	private static final Identifier TEXTURE = TafelBoem.id("textures/entity/graaf_fout.png");

	public GraafFoutRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(EvokerRenderState state) {
		return TEXTURE;
	}

	@Override
	public EvokerRenderState createRenderState() {
		return new GraafFoutRenderState();
	}

	@Override
	public void extractRenderState(GraafFout entity, EvokerRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		if (state instanceof GraafFoutRenderState yeet) {
			yeet.pose = entity.renderPose();
			yeet.angles = entity.renderAngles(partialTicks);
		}
	}

	@Override
	protected void setupRotations(EvokerRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		if (!(state instanceof GraafFoutRenderState yeet) || yeet.angles.equals(YeetPose.Angles.UPRIGHT)) {
			return;
		}
		float pivot = yeet.pose == YeetPose.TUMBLE ? state.boundingBoxHeight / entityScale / 2F : 0F;
		poseStack.translate(0F, pivot, 0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(yeet.angles.roll()));
		poseStack.mulPose(Axis.XP.rotationDegrees(yeet.angles.pitch()));
		poseStack.translate(0F, -pivot, 0F);
	}
}
