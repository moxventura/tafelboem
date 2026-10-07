package nl.jeeninga.tafelboem.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.state.EvokerRenderState;
import net.minecraft.resources.Identifier;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.GraafFout;

/**
 * The vanilla evoker model in Graaf Fout's purple cape with a big red ✗.
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
}
