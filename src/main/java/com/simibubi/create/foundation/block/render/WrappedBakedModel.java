package com.simibubi.create.foundation.block.render;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public class WrappedBakedModel implements BakedModel {
	protected BakedModel template;

	public WrappedBakedModel(BakedModel template) {
		this.template = template;
	}
	
	public BakedModel getBakedModel() {
		return template;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return template.useAmbientOcclusion();
	}

	@Override
	public boolean hasDepth() {
		return template.hasDepth();
	}

	@Override
	public boolean isBuiltin() {
		return template.isBuiltin();
	}

	@Override
	public ModelOverrideList getOverrides() {
		return template.getOverrides();
	}

	@Override
	public ModelTransformation getTransformation() {
		return template.getTransformation();
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
		return template.getQuads(state, side, rand);
	}

	@Override
	public Sprite getSprite() {
		return template.getSprite();
	}

	@Override
	public boolean isSideLit() {
		return template.isSideLit();
	}
}
