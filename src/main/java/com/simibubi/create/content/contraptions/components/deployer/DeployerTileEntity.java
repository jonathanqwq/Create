package com.simibubi.create.content.contraptions.components.deployer;

import static com.simibubi.create.content.contraptions.base.DirectionalKineticBlock.FACING;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.curiosities.tools.SandPaperItem;
import com.simibubi.create.content.curiosities.tools.SandPaperPolishingRecipe.SandPaperInv;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.render.backend.core.PartialModel;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.BeltProcessingBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.lib.lba.item.IItemHandlerModifiable;
import com.simibubi.create.lib.lba.item.ItemStackHandler;
import com.simibubi.create.lib.lba.item.RecipeWrapper;
import com.simibubi.create.lib.utility.Constants;
import com.simibubi.create.lib.utility.LazyOptional;

import com.simibubi.create.lib.utility.NBTSerializer;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;

public class DeployerTileEntity extends KineticTileEntity {

	protected State state;
	protected Mode mode;
	protected ItemStack heldItem = ItemStack.EMPTY;
	protected DeployerFakePlayer player;
	protected int timer;
	protected float reach;
	protected boolean boop = false;
	protected List<ItemStack> overflowItems = new ArrayList<>();
	protected FilteringBehaviour filtering;
	protected boolean redstoneLocked;
	private LazyOptional<IItemHandlerModifiable> invHandler;
	private ListNBT deferredInventoryList;

	private LerpedFloat animatedOffset;

	public BeltProcessingBehaviour processingBehaviour;

	enum State {
		WAITING, EXPANDING, RETRACTING, DUMPING;
	}

	enum Mode {
		PUNCH, USE
	}

	public DeployerTileEntity(TileEntityType<? extends DeployerTileEntity> type) {
		super(type);
		state = State.WAITING;
		mode = Mode.USE;
		heldItem = ItemStack.EMPTY;
		redstoneLocked = false;
		animatedOffset = LerpedFloat.linear()
			.startWithValue(0);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		filtering = new FilteringBehaviour(this, new DeployerFilterSlot());
		behaviours.add(filtering);
		processingBehaviour =
			new BeltProcessingBehaviour(this).whenItemEnters((s, i) -> BeltDeployerCallbacks.onItemReceived(s, i, this))
				.whileItemHeld((s, i) -> BeltDeployerCallbacks.whenItemHeld(s, i, this));
		behaviours.add(processingBehaviour);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!world.isRemote) {
			player = new DeployerFakePlayer((ServerWorld) world);
			if (deferredInventoryList != null) {
				player.inventory.read(deferredInventoryList);
				deferredInventoryList = null;
				heldItem = player.getHeldItemMainhand();
				sendData();
			}
			Vector3d initialPos = VecHelper.getCenterOf(pos.offset(getBlockState().get(FACING)));
			player.setPosition(initialPos.x, initialPos.y, initialPos.z);
		}
		invHandler = LazyOptional.of(this::createHandler);
	}

	protected void onExtract(ItemStack stack) {
		player.setHeldItem(Hand.MAIN_HAND, stack.copy());
		sendData();
		markDirty();
	}

	protected int getTimerSpeed() {
		return (int) (getSpeed() == 0 ? 0 : MathHelper.clamp(Math.abs(getSpeed() * 2), 8, 512));
	}

	@Override
	public void tick() {
		super.tick();

		if (getSpeed() == 0)
			return;
		if (!world.isRemote && player != null && player.blockBreakingProgress != null) {
			if (world.isAirBlock(player.blockBreakingProgress.getKey())) {
				world.sendBlockBreakProgress(player.getEntityId(), player.blockBreakingProgress.getKey(), -1);
				player.blockBreakingProgress = null;
			}
		}
		if (timer > 0) {
			timer -= getTimerSpeed();
			return;
		}
		if (world.isRemote)
			return;

		ItemStack stack = player.getHeldItemMainhand();
		if (state == State.WAITING) {
			if (!overflowItems.isEmpty()) {
				timer = getTimerSpeed() * 10;
				return;
			}

			boolean changed = false;
			for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
				if (overflowItems.size() > 10)
					break;
				ItemStack item = player.inventory.getStackInSlot(i);
				if (item.isEmpty())
					continue;
				if (item != stack || !filtering.test(item)) {
					overflowItems.add(item);
					player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
					changed = true;
				}
			}

			if (changed) {
				sendData();
				timer = getTimerSpeed() * 10;
				return;
			}

			Direction facing = getBlockState().get(FACING);
			if (mode == Mode.USE && !DeployerHandler.shouldActivate(stack, world, pos.offset(facing, 2), facing)) {
				timer = getTimerSpeed() * 10;
				return;
			}

			// Check for advancement conditions
			if (mode == Mode.PUNCH && !boop && startBoop(facing))
				return;
			if (redstoneLocked)
				return;

			start();
			return;
		}

		if (state == State.EXPANDING) {
			if (boop)
				triggerBoop();
			activate();

			state = State.RETRACTING;
			timer = 1000;
			sendData();
			return;
		}

		if (state == State.RETRACTING) {
			state = State.WAITING;
			timer = 500;
			sendData();
			return;
		}

	}

	protected void start() {
		state = State.EXPANDING;
		Vector3d movementVector = getMovementVector();
		Vector3d rayOrigin = VecHelper.getCenterOf(pos)
			.add(movementVector.scale(3 / 2f));
		Vector3d rayTarget = VecHelper.getCenterOf(pos)
			.add(movementVector.scale(5 / 2f));
		RayTraceContext rayTraceContext =
			new RayTraceContext(rayOrigin, rayTarget, BlockMode.OUTLINE, FluidMode.NONE, player);
		BlockRayTraceResult result = world.rayTraceBlocks(rayTraceContext);
		reach = (float) (.5f + Math.min(result.getHitVec()
			.subtract(rayOrigin)
			.length(), .75f));
		timer = 1000;
		sendData();
	}

	public boolean startBoop(Direction facing) {
		if (!world.isAirBlock(pos.offset(facing, 1)) || !world.isAirBlock(pos.offset(facing, 2)))
			return false;
		BlockPos otherDeployer = pos.offset(facing, 4);
		if (!world.isBlockPresent(otherDeployer))
			return false;
		TileEntity otherTile = world.getTileEntity(otherDeployer);
		if (!(otherTile instanceof DeployerTileEntity))
			return false;
		DeployerTileEntity deployerTile = (DeployerTileEntity) otherTile;
		if (world.getBlockState(otherDeployer)
			.get(FACING)
			.getOpposite() != facing || deployerTile.mode != Mode.PUNCH)
			return false;

		boop = true;
		reach = 1f;
		timer = 1000;
		state = State.EXPANDING;
		sendData();
		return true;
	}

	public void triggerBoop() {
		TileEntity otherTile = world.getTileEntity(pos.offset(getBlockState().get(FACING), 4));
		if (!(otherTile instanceof DeployerTileEntity))
			return;

		DeployerTileEntity deployerTile = (DeployerTileEntity) otherTile;
		if (!deployerTile.boop || deployerTile.state != State.EXPANDING)
			return;
		if (deployerTile.timer > 0)
			return;

		// everything should be met
		boop = false;
		deployerTile.boop = false;
		deployerTile.state = State.RETRACTING;
		deployerTile.timer = 1000;
		deployerTile.sendData();

		// award nearby players
		List<ServerPlayerEntity> players =
			world.getEntitiesWithinAABB(ServerPlayerEntity.class, new AxisAlignedBB(pos).grow(9));
		players.forEach(AllTriggers.DEPLOYER_BOOP::trigger);
	}

	protected void activate() {
		Vector3d movementVector = getMovementVector();
		Direction direction = getBlockState().get(FACING);
		Vector3d center = VecHelper.getCenterOf(pos);
		BlockPos clickedPos = pos.offset(direction, 2);
		player.rotationYaw = direction.getHorizontalAngle();
		player.rotationPitch = direction == Direction.UP ? -90 : direction == Direction.DOWN ? 90 : 0;

		if (direction == Direction.DOWN
			&& TileEntityBehaviour.get(world, clickedPos, TransportedItemStackHandlerBehaviour.TYPE) != null)
			return; // Belt processing handled in BeltDeployerCallbacks

		DeployerHandler.activate(player, center, clickedPos, movementVector, mode);
		if (player != null)
			heldItem = player.getHeldItemMainhand();
	}

	protected Vector3d getMovementVector() {
		if (!AllBlocks.DEPLOYER.has(getBlockState()))
			return Vector3d.ZERO;
		return Vector3d.of(getBlockState().get(FACING)
			.getDirectionVec());
	}

	@Override
	protected void fromTag(BlockState blockState, CompoundNBT compound, boolean clientPacket) {
		state = NBTHelper.readEnum(compound, "State", State.class);
		mode = NBTHelper.readEnum(compound, "Mode", Mode.class);
		timer = compound.getInt("Timer");
		redstoneLocked = compound.getBoolean("Powered");

		deferredInventoryList = compound.getList("Inventory", Constants.NBT.TAG_COMPOUND);
		overflowItems = NBTHelper.readItemList(compound.getList("Overflow", Constants.NBT.TAG_COMPOUND));
		if (compound.contains("HeldItem"))
			heldItem = ItemStack.read(compound.getCompound("HeldItem"));
		super.fromTag(blockState, compound, clientPacket);

		if (!clientPacket)
			return;
		reach = compound.getFloat("Reach");
		if (compound.contains("Particle")) {
			ItemStack particleStack = ItemStack.read(compound.getCompound("Particle"));
			SandPaperItem.spawnParticles(VecHelper.getCenterOf(pos)
				.add(getMovementVector().scale(reach + 1)), particleStack, this.world);
		}
	}

	@Override
	public void write(CompoundNBT compound, boolean clientPacket) {
		NBTHelper.writeEnum(compound, "Mode", mode);
		NBTHelper.writeEnum(compound, "State", state);
		compound.putInt("Timer", timer);
		compound.putBoolean("Powered", redstoneLocked);

		if (player != null) {
			compound.put("HeldItem", NBTSerializer.serializeNBT(player.getHeldItemMainhand()));
			ListNBT invNBT = new ListNBT();
			player.inventory.write(invNBT);
			compound.put("Inventory", invNBT);
			compound.put("Overflow", NBTHelper.writeItemList(overflowItems));
		}

		super.write(compound, clientPacket);

		if (!clientPacket)
			return;
		compound.putFloat("Reach", reach);
		if (player == null)
			return;
		compound.put("HeldItem", NBTSerializer.serializeNBT(player.getHeldItemMainhand()));
		if (player.spawnedItemEffects != null) {
			compound.put("Particle", NBTSerializer.serializeNBT(player.spawnedItemEffects));
			player.spawnedItemEffects = null;

		}
	}

	private IItemHandlerModifiable createHandler() {
		return new DeployerItemHandler(this);
	}

	public void redstoneUpdate() {
		if (world.isRemote)
			return;
		boolean blockPowered = world.isBlockPowered(pos);
		if (blockPowered == redstoneLocked)
			return;
		redstoneLocked = blockPowered;
		sendData();
	}

	public PartialModel getHandPose() {
		return mode == Mode.PUNCH ? AllBlockPartials.DEPLOYER_HAND_PUNCHING
			: heldItem.isEmpty() ? AllBlockPartials.DEPLOYER_HAND_POINTING : AllBlockPartials.DEPLOYER_HAND_HOLDING;
	}

//	@Override
//	public AxisAlignedBB makeRenderBoundingBox() {
//		return super.makeRenderBoundingBox().grow(3);
//	}

	@Override
	public void remove() {
		super.remove();
		if (invHandler != null)
			invHandler.invalidate();
	}

	public void changeMode() {
		mode = mode == Mode.PUNCH ? Mode.USE : Mode.PUNCH;
		markDirty();
		sendData();
	}

//	@Override
//	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
//		if (isItemHandlerCap(cap) && invHandler != null)
//			return invHandler.cast();
//		return super.getCapability(cap, side);
//	}

	@Override
	public boolean addToTooltip(List<ITextComponent> tooltip, boolean isPlayerSneaking) {
		if (super.addToTooltip(tooltip, isPlayerSneaking))
			return true;
		if (getSpeed() == 0)
			return false;
		if (overflowItems.isEmpty())
			return false;
		TooltipHelper.addHint(tooltip, "hint.full_deployer");
		return true;
	}

	@Override
	public boolean shouldRenderAsTE() {
		return true;
	}

	public float getHandOffset(float partialTicks) {
		if (isVirtual())
			return animatedOffset.getValue(partialTicks);

		float progress = 0;
		int timerSpeed = getTimerSpeed();
		PartialModel handPose = getHandPose();

		if (state == State.EXPANDING)
			progress = 1 - (timer - partialTicks * timerSpeed) / 1000f;
		if (state == State.RETRACTING)
			progress = (timer - partialTicks * timerSpeed) / 1000f;
		float handLength = handPose == AllBlockPartials.DEPLOYER_HAND_POINTING ? 0
			: handPose == AllBlockPartials.DEPLOYER_HAND_HOLDING ? 4 / 16f : 3 / 16f;
		float distance = Math.min(MathHelper.clamp(progress, 0, 1) * (reach + handLength), 21 / 16f);

		return distance;
	}

	public void setAnimatedOffset(float offset) {
		animatedOffset.setValue(offset);
	}

	RecipeWrapper recipeInv = new RecipeWrapper(new ItemStackHandler(2));
	SandPaperInv sandpaperInv = new SandPaperInv(ItemStack.EMPTY);

	@Nullable
	public IRecipe<?> getRecipe(ItemStack stack) {
		if (player == null)
			return null;
		ItemStack heldItemMainhand = player.getHeldItemMainhand();
		if (heldItemMainhand.getItem() instanceof SandPaperItem) {
			sandpaperInv.setInventorySlotContents(0, stack);
			return AllRecipeTypes.SANDPAPER_POLISHING.find(sandpaperInv, world)
				.orElse(null);
		}
		recipeInv.setInventorySlotContents(0, heldItemMainhand);
		recipeInv.setInventorySlotContents(1, stack);
		return AllRecipeTypes.DEPLOYING.find(recipeInv, world)
			.orElse(null);
	}

}
