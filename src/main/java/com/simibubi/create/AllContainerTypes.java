package com.simibubi.create;

import com.simibubi.create.content.logistics.block.inventories.AdjustableCrateContainer;
import com.simibubi.create.content.logistics.block.inventories.AdjustableCrateScreen;
import com.simibubi.create.content.logistics.item.filter.AttributeFilterContainer;
import com.simibubi.create.content.logistics.item.filter.AttributeFilterScreen;
import com.simibubi.create.content.logistics.item.filter.FilterContainer;
import com.simibubi.create.content.logistics.item.filter.FilterScreen;
import com.simibubi.create.content.schematics.block.SchematicTableContainer;
import com.simibubi.create.content.schematics.block.SchematicTableScreen;
import com.simibubi.create.content.schematics.block.SchematicannonContainer;
import com.simibubi.create.content.schematics.block.SchematicannonScreen;
import com.tterrag.registrate.builders.ContainerBuilder.ForgeContainerFactory;
import com.tterrag.registrate.builders.ContainerBuilder.ScreenFactory;
import com.tterrag.registrate.util.entry.ContainerEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import com.simibubi.create.lib.utility.ContainerTypeFactoryWrapper;

import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.ContainerType.IFactory;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.network.IContainerFactory;

public class AllContainerTypes {

	public static final ContainerEntry<SchematicTableContainer> SCHEMATIC_TABLE =
		register("schematic_table", SchematicTableContainer::new, () -> SchematicTableScreen::new);

	public static final ContainerEntry<SchematicannonContainer> SCHEMATICANNON =
		register("schematicannon", SchematicannonContainer::new, () -> SchematicannonScreen::new);

	public static final ContainerEntry<AdjustableCrateContainer> FLEXCRATE =
		register("flexcrate", AdjustableCrateContainer::new, () -> AdjustableCrateScreen::new);

	public static final ContainerEntry<FilterContainer> FILTER =
		register("filter", FilterContainer::new, () -> FilterScreen::new);

	public static final ContainerEntry<AttributeFilterContainer> ATTRIBUTE_FILTER =
		register("attribute_filter", AttributeFilterContainer::new, () -> AttributeFilterScreen::new);

	private static <C extends Container, S extends Screen & IHasContainer<C>> ContainerEntry<C> register(String name, ForgeContainerFactory<C> factory, NonNullSupplier<ScreenFactory<C, S>> screenFactory) {
		return Create.registrate().container(name, factory, screenFactory).register();
	}

	public static void register() {}

}
