package com.simibubi.create.content.curiosities.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.WorldAttached;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.util.Hand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.EntityTeleportEvent;
import net.minecraftforge.registries.IRegistryDelegate;

public class PotatoCannonProjectileTypes {

	private static final GameProfile ZOMBIE_CONVERTER_NAME =
		new GameProfile(UUID.fromString("be12d3dc-27d3-4992-8c97-66be53fd49c5"), "Converter");
	private static final WorldAttached<FakePlayer> ZOMBIE_CONVERTERS =
		new WorldAttached<>(w -> new FakePlayer((ServerWorld) w, ZOMBIE_CONVERTER_NAME));

	public static final Map<ResourceLocation, PotatoCannonProjectileTypes> ALL = new HashMap<>();
	public static final Map<IRegistryDelegate<Item>, PotatoCannonProjectileTypes> ITEM_MAP = new HashMap<>();
	public static final PotatoCannonProjectileTypes

	FALLBACK = create("fallback").damage(0)
		.register(),

		POTATO = create("potato").damage(5)
			.reloadTicks(15)
			.velocity(1.25f)
			.knockback(1.5f)
			.renderTumbling()
			.onBlockHit(plantCrop(Blocks.POTATOES.delegate))
			.registerAndAssign(Items.POTATO),

		BAKED_POTATO = create("baked_potato").damage(5)
			.reloadTicks(15)
			.velocity(1.25f)
			.knockback(0.5f)
			.renderTumbling()
			.onEntityHit(ray -> ray.getEntity().setSecondsOnFire(3))
			.registerAndAssign(Items.BAKED_POTATO),

		CARROT = create("carrot").damage(4)
			.reloadTicks(12)
			.velocity(1.45f)
			.knockback(0.3f)
			.renderTowardMotion(140, 1)
			.soundPitch(1.5f)
			.onBlockHit(plantCrop(Blocks.CARROTS.delegate))
			.registerAndAssign(Items.CARROT),

		GOLDEN_CARROT = create("golden_carrot").damage(12)
			.reloadTicks(15)
			.velocity(1.45f)
			.knockback(0.5f)
			.renderTowardMotion(140, 2)
			.soundPitch(1.5f)
			.registerAndAssign(Items.GOLDEN_CARROT),

		SWEET_BERRIES = create("sweet_berry").damage(3)
			.reloadTicks(10)
			.knockback(0.1f)
			.velocity(1.05f)
			.renderTumbling()
			.splitInto(3)
			.soundPitch(1.25f)
			.registerAndAssign(Items.SWEET_BERRIES),

		CHOCOLATE_BERRIES = create("chocolate_berry").damage(4)
			.reloadTicks(10)
			.knockback(0.2f)
			.velocity(1.05f)
			.renderTumbling()
			.splitInto(3)
			.soundPitch(1.25f)
			.registerAndAssign(AllItems.CHOCOLATE_BERRIES.get()),

		POISON_POTATO = create("poison_potato").damage(5)
			.reloadTicks(15)
			.knockback(0.05f)
			.velocity(1.25f)
			.renderTumbling()
			.onEntityHit(potion(MobEffects.POISON, 1,160, true))
			.registerAndAssign(Items.POISONOUS_POTATO),

		CHORUS_FRUIT = create("chorus_fruit").damage(3)
			.reloadTicks(15)
			.velocity(1.20f)
			.knockback(0.05f)
			.renderTumbling()
			.onEntityHit(chorusTeleport(20))
			.registerAndAssign(Items.CHORUS_FRUIT),

		APPLE = create("apple").damage(5)
			.reloadTicks(10)
			.velocity(1.45f)
			.knockback(0.5f)
			.renderTumbling()
			.soundPitch(1.1f)
			.registerAndAssign(Items.APPLE),

		HONEYED_APPLE = create("honeyed_apple").damage(6)
			.reloadTicks(15)
			.velocity(1.35f)
			.knockback(0.1f)
			.renderTumbling()
			.soundPitch(1.1f)
			.onEntityHit(potion(MobEffects.MOVEMENT_SLOWDOWN, 2,160))
			.registerAndAssign(AllItems.HONEYED_APPLE.get()),

		GOLDEN_APPLE = create("golden_apple").damage(1)
			.reloadTicks(100)
			.velocity(1.45f)
			.knockback(0.05f)
			.renderTumbling()
			.soundPitch(1.1f)
			.onEntityHit(ray -> {
				Entity entity = ray.getEntity();
				World world = entity.level;

				if (!(entity instanceof ZombieVillagerEntity)
					|| !((ZombieVillagerEntity) entity).hasEffect(Effects.WEAKNESS))
					return foodEffects(Foods.GOLDEN_APPLE, false).test(ray);
				if (world.isClientSide)
					return false;

				FakePlayer dummy = ZOMBIE_CONVERTERS.get(world);
				dummy.setItemInHand(Hand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE, 1));
				((ZombieVillagerEntity) entity).mobInteract(dummy, Hand.MAIN_HAND);
				return true;
			})
			.registerAndAssign(Items.GOLDEN_APPLE),

		ENCHANTED_GOLDEN_APPLE = create("enchanted_golden_apple").damage(1)
			.reloadTicks(100)
			.velocity(1.45f)
			.knockback(0.05f)
			.renderTumbling()
			.soundPitch(1.1f)
			.onEntityHit(foodEffects(Foods.ENCHANTED_GOLDEN_APPLE, false))
			.registerAndAssign(Items.ENCHANTED_GOLDEN_APPLE),

		BEETROOT = create("beetroot").damage(2)
			.reloadTicks(5)
			.velocity(1.6f)
			.knockback(0.1f)
			.renderTowardMotion(140, 2)
			.soundPitch(1.6f)
			.registerAndAssign(Items.BEETROOT),

		MELON_SLICE = create("melon_slice").damage(3)
			.reloadTicks(8)
			.knockback(0.1f)
			.velocity(1.45f)
			.renderTumbling()
			.soundPitch(1.5f)
			.registerAndAssign(Items.MELON_SLICE),

		GLISTENING_MELON = create("glistening_melon").damage(5)
			.reloadTicks(8)
			.knockback(0.1f)
			.velocity(1.45f)
			.renderTumbling()
			.soundPitch(1.5f)
			.onEntityHit(potion(MobEffects.GLOWING, 1, 100, true))
			.registerAndAssign(Items.GLISTERING_MELON_SLICE),

		MELON_BLOCK = create("melon_block").damage(8)
			.reloadTicks(20)
			.knockback(2.0f)
			.velocity(0.95f)
			.renderTumbling()
			.soundPitch(0.9f)
			.onBlockHit(placeBlockOnGround(Blocks.MELON.delegate))
			.registerAndAssign(Blocks.MELON),

		PUMPKIN_BLOCK = create("pumpkin_block").damage(6)
			.reloadTicks(15)
			.knockback(2.0f)
			.velocity(0.95f)
			.renderTumbling()
			.soundPitch(0.9f)
			.onBlockHit(placeBlockOnGround(Blocks.PUMPKIN.delegate))
			.registerAndAssign(Blocks.PUMPKIN),

		PUMPKIN_PIE = create("pumpkin_pie").damage(7)
			.reloadTicks(15)
			.knockback(0.05f)
			.velocity(1.1f)
			.renderTumbling()
			.sticky()
			.soundPitch(1.1f)
			.registerAndAssign(Items.PUMPKIN_PIE),

		CAKE = create("cake").damage(8)
			.reloadTicks(15)
			.knockback(0.1f)
			.velocity(1.1f)
			.renderTumbling()
			.sticky()
			.soundPitch(1.0f)
			.registerAndAssign(Items.CAKE),

		BLAZE_CAKE = create("blaze_cake").damage(15)
			.reloadTicks(20)
			.knockback(0.3f)
			.velocity(1.1f)
			.renderTumbling()
			.sticky()
			.preEntityHit(setFire(12))
			.soundPitch(1.0f)
			.registerAndAssign(AllItems.BLAZE_CAKE.get())
	;

	public static void registerType(ResourceLocation resLoc, PotatoCannonProjectileTypes type) {
		synchronized (ALL) {
			ALL.put(resLoc, type);
		}
	}

	public static void assignType(IRegistryDelegate<Item> item, PotatoCannonProjectileTypes type) {
		synchronized (ITEM_MAP) {
			ITEM_MAP.put(item, type);
		}
	}

	public static Optional<PotatoCannonProjectileTypes> getProjectileTypeOf(ItemStack item) {
		if (item.isEmpty())
			return Optional.empty();
		return Optional.ofNullable(ITEM_MAP.get(item.getItem().delegate));
	}

	public static void register() {}

	private static PotatoCannonProjectileTypes.Builder create(String name) {
		return new PotatoCannonProjectileTypes.Builder(Create.asResource(name));
	}

	private float gravityMultiplier = 1;
	private float velocityMultiplier = 1;
	private float drag = 0.99f;
	private float knockback = 1;
	private int reloadTicks = 10;
	private int damage = 1;
	private int split = 1;
	private float fwoompPitch = 1;
	private boolean sticky = false;
	private PotatoProjectileRenderMode renderMode = new PotatoProjectileRenderMode.Billboard();
	private Predicate<EntityHitResult> preEntityHit = e -> false; // True if hit should be canceled
	private Predicate<EntityRayTraceResult> onEntityHit = e -> false; // True if shouldn't recover projectile
	private BiPredicate<LevelAccessor, BlockHitResult> onBlockHit = (w, ray) -> false;

	public float getGravityMultiplier() {
		return gravityMultiplier;
	}

	public float getDrag() {
		return drag;
	}

	public int getSplit() {
		return split;
	}

	public float getVelocityMultiplier() {
		return velocityMultiplier;
	}

	public float getKnockback() {
		return knockback;
	}

	public int getReloadTicks() {
		return reloadTicks;
	}

	public float getSoundPitch() {
		return fwoompPitch;
	}

	public PotatoProjectileRenderMode getRenderMode() {
		return renderMode;
	}

	public int getDamage() {
		return damage;
	}

	public boolean isSticky() { return sticky; }

	public boolean preEntityHit(EntityHitResult ray) {
		return preEntityHit.test(ray);
	}

	public void onBlockHit(LevelAccessor world, BlockHitResult ray) {
		onBlockHit.accept(world, ray);
	}

	public boolean onBlockHit(IWorld world, BlockRayTraceResult ray) {
		return onBlockHit.test(world, ray);
	}

	private static Predicate<EntityRayTraceResult> setFire(int seconds) {
		return ray -> {
			ray.getEntity().setSecondsOnFire(seconds);
			return false;
		};
	}

	private static Predicate<EntityHitResult> potion(MobEffect effect, int level, int ticks, boolean recoverable) {
		return ray -> {
			Entity entity = ray.getEntity();
			if (entity.level.isClientSide)
				return true;
			if (entity instanceof LivingEntity)
				applyEffect((LivingEntity) entity, new EffectInstance(effect, ticks, level - 1));
			return !recoverable;
		};
	}

	private static Predicate<EntityHitResult> foodEffects(FoodProperties food, boolean recoverable) {
		return ray -> {
			Entity entity = ray.getEntity();
			if (entity.level.isClientSide)
				return true;

			if (entity instanceof LivingEntity) {
				for (Pair<MobEffectInstance, Float> effect : food.getEffects()) {
					if (Create.RANDOM.nextFloat() < effect.getSecond())
						applyEffect((LivingEntity) entity, new EffectInstance(effect.getFirst()));
				}
			}
			return !recoverable;
		};
	}

	public static void applyEffect(LivingEntity entity, EffectInstance effect) {
		if (effect.getEffect().isInstantenous())
			effect.getEffect().applyInstantenousEffect(null, null, entity, effect.getDuration(), 1.0);
		else
			entity.addEffect(effect);
	}

	private static BiPredicate<LevelAccessor, BlockHitResult> plantCrop(IRegistryDelegate<? extends Block> cropBlock) {
		return (world, ray) -> {
			if (world.isClientSide())
				return true;

			BlockPos hitPos = ray.getBlockPos();
			if (!world.hasChunksAt(hitPos, 1))
				return true;
			Direction face = ray.getDirection();
			BlockPos placePos = hitPos.relative(face);
			if (!world.getBlockState(placePos)
				.getMaterial()
				.isReplaceable())
				return false;
			if (!(cropBlock.get() instanceof IPlantable))
				return false;
			BlockState blockState = world.getBlockState(hitPos);
			if (!blockState.canSustainPlant(world, hitPos, face, (IPlantable) cropBlock.get()))
				return false;
			world.setBlock(placePos, cropBlock.get().defaultBlockState(), 3);
			return true;
		};
	}

	private static BiPredicate<LevelAccessor, BlockHitResult> placeBlockOnGround(IRegistryDelegate<? extends Block> block) {
		return (world, ray) -> {
			if (world.isClientSide())
				return true;

			BlockPos hitPos = ray.getBlockPos();
			if (!world.hasChunksAt(hitPos, 1))
				return true;
			Direction face = ray.getDirection();
			BlockPos placePos = hitPos.relative(face);
			if (!world.getBlockState(placePos)
				.getMaterial()
				.isReplaceable())
				return false;

			if (face == Direction.UP) {
				world.setBlock(placePos, block.get().defaultBlockState(), 3);
			} else if (world instanceof Level) {
				double y = ray.getLocation().y - 0.5;
				if (!world.isEmptyBlock(placePos.above()))
					y = Math.min(y, placePos.getY());
				if (!world.isEmptyBlock(placePos.below()))
					y = Math.max(y, placePos.getY());

				FallingBlockEntity falling = new FallingBlockEntity((Level) world, placePos.getX() + 0.5, y,
					placePos.getZ() + 0.5, block.get().defaultBlockState());
				falling.time = 1;
				world.addFreshEntity(falling);
			}

			return true;
		};
	}

	private static Predicate<EntityHitResult> chorusTeleport(double teleportDiameter) {
		return ray -> {
			Entity entity = ray.getEntity();
			Level world = entity.getCommandSenderWorld();
			if (world.isClientSide)
				return true;
			if (!(entity instanceof LivingEntity))
				return false;
			LivingEntity livingEntity = (LivingEntity) entity;

			double entityX = livingEntity.getX();
			double entityY = livingEntity.getY();
			double entityZ = livingEntity.getZ();

			for (int teleportTry = 0; teleportTry < 16; ++teleportTry) {
				double teleportX = entityX + (livingEntity.getRandom().nextDouble() - 0.5D) * teleportDiameter;
				double teleportY = Mth.clamp(entityY + (livingEntity.getRandom().nextInt((int) teleportDiameter) - (int) (teleportDiameter / 2)), 0.0D, world.getHeight() - 1);
				double teleportZ = entityZ + (livingEntity.getRandom().nextDouble() - 0.5D) * teleportDiameter;

				EntityTeleportEvent.ChorusFruit event = ForgeEventFactory.onChorusFruitTeleport(livingEntity, teleportX, teleportY, teleportZ);
				if (event.isCanceled())
					return false;
				if (livingEntity.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
					if (livingEntity.isPassenger())
						livingEntity.stopRiding();

					SoundEvent soundevent = livingEntity instanceof Fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
					world.playSound(null, entityX, entityY, entityZ, soundevent, SoundSource.PLAYERS, 1.0F, 1.0F);
					livingEntity.playSound(soundevent, 1.0F, 1.0F);
					livingEntity.setDeltaMovement(Vector3d.ZERO);
					return true;
				}
			}

			return false;
		};
	}

	public static class Builder {

		protected ResourceLocation loc;
		protected PotatoCannonProjectileTypes result;

		public Builder(ResourceLocation loc) {
			this.result = new PotatoCannonProjectileTypes();
			this.loc = loc;
		}

		public Builder damage(int damage) {
			result.damage = damage;
			return this;
		}

		public Builder gravity(float modifier) {
			result.gravityMultiplier = modifier;
			return this;
		}

		public Builder knockback(float knockback) {
			result.knockback = knockback;
			return this;
		}

		public Builder drag(float drag) {
			result.drag = drag;
			return this;
		}

		public Builder reloadTicks(int reload) {
			result.reloadTicks = reload;
			return this;
		}

		public Builder splitInto(int split) {
			result.split = split;
			return this;
		}

		public Builder soundPitch(float pitch) {
			result.fwoompPitch = pitch;
			return this;
		}

		public Builder velocity(float velocity) {
			result.velocityMultiplier = velocity;
			return this;
		}

		public Builder renderTumbling() {
			result.renderMode = new PotatoProjectileRenderMode.Tumble();
			return this;
		}

		public Builder renderBillboard() {
			result.renderMode = new PotatoProjectileRenderMode.Billboard();
			return this;
		}

		public Builder renderTowardMotion(int spriteAngle, float spin) {
			result.renderMode = new PotatoProjectileRenderMode.TowardMotion(spriteAngle, spin);
			return this;
		}

		public Builder sticky() {
			result.sticky = true;
			return this;
		}

		public Builder preEntityHit(Predicate<EntityRayTraceResult> callback) {
			result.preEntityHit = callback;
			return this;
		}

		public Builder onEntityHit(Predicate<EntityHitResult> callback) {
			result.onEntityHit = callback;
			return this;
		}

		public Builder onBlockHit(BiPredicate<LevelAccessor, BlockHitResult> callback) {
			result.onBlockHit = callback;
			return this;
		}

		public PotatoCannonProjectileTypes register() {
			registerType(loc, result);
			return result;
		}

		public PotatoCannonProjectileTypes registerAndAssign(ItemLike... items) {
			registerType(loc, result);
			for (ItemLike provider : items)
				assignType(provider.asItem().delegate, result);
			return result;
		}

	}

}