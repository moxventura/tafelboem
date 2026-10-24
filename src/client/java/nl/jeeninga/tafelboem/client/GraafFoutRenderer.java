package nl.jeeninga.tafelboem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.state.EvokerRenderState;
import net.minecraft.resources.Identifier;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetPose;

/**
 * The vanilla evoker model in Graaf Fout's purple cape with a big red ✗. On top of the spin the
 * server sends, it draws the tumble (rolling and flipping around his middle), the faceplant
 * (tipping forward around his feet), head-first (upside down, sunk, legs wiggling) and the
 * pancake (squashed flat), the soot face (a second texture) and the sore toe (a model of his own).
 */
public class GraafFoutRenderer extends EvokerRenderer<GraafFout> {
	private static final Identifier TEXTURE = TafelBoem.id("textures/entity/graaf_fout.png");
	private static final Identifier SOOT_TEXTURE = TafelBoem.id("textures/entity/graaf_fout_soot.png");
	/** Squashed flat he spreads out a little, like dough. */
	private static final float SPREAD = 0.6F;

	public GraafFoutRenderer(EntityRendererProvider.Context context) {
		super(context);
		// The evoker model plus a pose of his own: holding his sore toe.
		this.model = new GraafFoutModel(context.bakeLayer(ModelLayers.EVOKER));
	}

	@Override
	public Identifier getTextureLocation(EvokerRenderState state) {
		return state instanceof GraafFoutRenderState yeet && yeet.shown == YeetPose.SOOT ? SOOT_TEXTURE : TEXTURE;
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
			yeet.shown = entity.shownPose();
			yeet.transform = entity.renderTransform(partialTicks);
			if (entity.shownPose().upsideDown()) {
				state.isUpsideDown = true;
				// Legs wiggling in the air: the walk animation, running on the spot.
				state.walkAnimationPos = state.ageInTicks * 1.6F;
				state.walkAnimationSpeed = 1F;
			}
		}
	}

	@Override
	protected void setupRotations(EvokerRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		if (state instanceof GraafFoutRenderState sunk && sunk.transform.sink() != 0F) {
			poseStack.translate(0F, -sunk.transform.sink() / entityScale, 0F);
		}
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		if (!(state instanceof GraafFoutRenderState yeet) || yeet.transform.roll() == 0F && yeet.transform.pitch() == 0F) {
			return;
		}
		float pivot = state.boundingBoxHeight / entityScale * yeet.pose.pivotShare();
		poseStack.translate(0F, pivot, 0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(yeet.transform.roll()));
		poseStack.mulPose(Axis.XP.rotationDegrees(yeet.transform.pitch()));
		poseStack.translate(0F, -pivot, 0F);
	}

	@Override
	protected void scale(EvokerRenderState state, PoseStack poseStack) {
		// Runs after the model is flipped and before it is lowered onto his feet, so he squashes towards the ground.
		if (state instanceof GraafFoutRenderState yeet && (yeet.transform.height() != 1F || yeet.transform.depth() != 1F)) {
			float width = 1F + (1F - yeet.transform.height()) * SPREAD;
			poseStack.scale(width, yeet.transform.height(), yeet.transform.depth());
		}
	}
}
