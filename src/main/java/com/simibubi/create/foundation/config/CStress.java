package com.simibubi.create.foundation.config;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.lib.utility.ConfigValue;

import dev.inkwell.conrad.api.value.data.SaveType;
import dev.inkwell.conrad.api.value.serialization.ConfigSerializer;
import dev.inkwell.conrad.api.value.serialization.FlatOwenSerializer;
import dev.inkwell.owen.OwenElement;
import dev.inkwell.vivian.api.builders.CategoryBuilder;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public class CStress extends ConfigBase {

	@Override
	public @NotNull SaveType getSaveType() {
		return SaveType.LEVEL;
	}

	@Override
	public @NotNull ConfigSerializer<OwenElement> getSerializer() {
		return FlatOwenSerializer.INSTANCE;
	}

	private Map<ResourceLocation, ConfigValue<Double>> capacities = new HashMap<>();
	private Map<ResourceLocation, ConfigValue<Double>> impacts = new HashMap<>();

	@Override
	protected void registerAll() {
//		builder.comment("", Comments.su, Comments.impact)
//			.push("impact");
		StressConfigDefaults.registeredDefaultImpacts
			.forEach((r, i) -> getImpacts().put(r, d(i.floatValue(), 0f, "impact", null, Comments.su, Comments.impact)));
//		builder.pop();

//		builder.comment("", Comments.su, Comments.capacity)
//			.push("capacity");
		StressConfigDefaults.registeredDefaultCapacities
			.forEach((r, i) -> getCapacities().put(r, d(i.floatValue(), 0f, "capacity", (CategoryBuilder) null, Comments.su, Comments.capacity)));
//		builder.pop();
	}

	public double getImpactOf(Block block) {
		ResourceLocation key = Registry.BLOCK.getKey(block);
		return getImpacts().containsKey(key) ? getImpacts().get(key)
			.get() : 0;
	}

	public double getCapacityOf(Block block) {
		ResourceLocation key = Registry.BLOCK.getKey(block);

		return getCapacities().containsKey(key) ? getCapacities().get(key)
			.get() : 0;
	}

	@Override
	public String getName() {
		return "stressValues.v" + StressConfigDefaults.forcedUpdateVersion;
	}

	public Map<ResourceLocation, ConfigValue<Double>> getImpacts() {
		return impacts;
	}

	public Map<ResourceLocation, ConfigValue<Double>> getCapacities() {
		return capacities;
	}

	private static class Comments {
		static String su = "[in Stress Units]";
		static String impact =
			"Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives.";
		static String capacity = "Configure how much stress a source can accommodate for.";
	}

}
