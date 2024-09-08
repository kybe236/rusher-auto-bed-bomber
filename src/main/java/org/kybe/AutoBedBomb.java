package org.kybe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.render.EventRender3D;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.render.IRenderer3D;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.utils.ColorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Auto Bed Bomb module
 *
 * @author kybe236
 */
public class AutoBedBomb extends ToggleableModule {
	final static MutableComponent prefix = Component.literal("[BED BOMBER] ")
			.withStyle(ChatFormatting.RED);

	private static final ArrayList<Entity> ENTITIES = new ArrayList<>();
	private static final Item[] bedItems = {
			Items.WHITE_BED, Items.RED_BED, Items.BLUE_BED, Items.YELLOW_BED, Items.BLACK_BED,
			Items.GREEN_BED, Items.BROWN_BED, Items.PINK_BED, Items.GRAY_BED, Items.LIGHT_BLUE_BED,
			Items.CYAN_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED, Items.MAGENTA_BED, Items.ORANGE_BED,
			Items.PURPLE_BED
	};
	private final List<BlockPos> checks = new ArrayList<>();
	int blockevents = 0;

	/*
	 * Settings
	 */
	private final BooleanSetting inventory = new BooleanSetting("Inventory", true);
	private final NumberSetting<Integer> range = new NumberSetting<>("Range", "Range", 5, 0, 7);
	private final NumberSetting<Integer> minDamage = new NumberSetting<>("Min Damage", "Min Damage", 5, 0, 20);
	private final NumberSetting<Integer> maxDamage = new NumberSetting<>("Max Self Damage", "Max Self Damage", 20, 0, 20);
	private final BooleanSetting antiSuicide = new BooleanSetting("Anti Suicide", true);
	private final NumberSetting<Integer> maxBlocksBreakAndPlacePerTick = new NumberSetting<>("max Blocks Break And Place Per Tick", "max Blocks Break And Place Per Tick", 5, 0, 20);
	private static final BooleanSetting rayTrace = new BooleanSetting("RayTrace", false);

	public AutoBedBomb() {
		super("Auto Bed Bomb", "Auto Bed Bomb", ModuleCategory.CLIENT);

		this.registerSettings(
				inventory,
				range,
				minDamage,
				maxDamage,
				maxBlocksBreakAndPlacePerTick,
				antiSuicide,
				rayTrace
		);
	}

	public static void placeBed(BlockPos pos) {
		if (mc.player == null || mc.level == null) return;

		ChatUtils.print(
				Component
						.empty()
						.append(prefix)
						.append(Component.literal("Placing bed at " + pos))
		);

		ChatUtils.print(
				Component
						.empty()
						.append(prefix)
						.append(Component.literal("Current Item " + mc.player.getMainHandItem().getItem()))
		);

		RusherHackAPI.interactions().placeBlock(
				pos,
				InteractionHand.MAIN_HAND,
				rayTrace.getValue()
		);
	}

	public static boolean isBed(Item selectedItem) {
		for (Item bed : bedItems) {
			if (selectedItem == bed) {
				return true;
			}
		}

		return false;
	}

	public static int getBedInInventory() {
		for (Item bed : bedItems) {
			int slot = InventoryUtils.findItem(bed, true, false);
			if (slot != -1) {
				return slot;
			}
		}
		return -1;
	}

	public static Player getTarget(double range) {
		if (mc.level == null || mc.player == null) return null;
		Iterable<Entity> targetList = mc.level.entitiesForRendering();
		for (Entity entity : targetList) {
			if (entity instanceof Player) {
				if (mc.player.distanceTo(entity) <= range && entity != mc.player) {
					return (Player) entity;
				}
			}
		}
		return null;
	}

	@Subscribe
	public void onTick(EventUpdate e) {
		if (mc.level == null || mc.player == null) return;
		try {
			checks.clear();
			if (mc.level.dimensionType().bedWorks()) {
				ChatUtils.print(
						Component
								.empty()
								.append(prefix)
								.append(Component.literal("Bed bombing is disabled in this dimension"))
				);
				this.setToggled(false);
				return;
			}
			if (mc.player.isUsingItem() || mc.options.keyUse.isDown()) {
				return;
			}
			Player target = getTarget(range.getValue());
			if (target == null) {
				return;
			}
			ChatUtils.print(
					Component
							.empty()
							.append(prefix)
							.append(Component.literal("Found target: "))
							.append(target.getDisplayName())
			);

			//BlockPos placePos = findPlace(target);
			BlockPos placePos = findPlace(target);

			if (placePos == null) {
				return;
			}


			int bedSlot = -1;
			if (inventory.getValue()) {
				if (isBed(mc.player.getMainHandItem().getItem())) {
					ChatUtils.print(
							Component
									.empty()
									.append(prefix)
									.append(Component.literal("You are holding a bed"))
					);
				} else {
					bedSlot = getBedInInventory();
					ChatUtils.print(
							Component
									.empty()
									.append(prefix)
									.append(Component.literal("Bed slot: " + bedSlot))
					);
					if (bedSlot == -1) {
						ChatUtils.print(
								Component
										.empty()
										.append(prefix)
										.append(Component.literal("No bed in inventory"))
						);
						return;
					}
					// swap items
					mc.setScreen(new InventoryScreen(mc.player));
					mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, bedSlot, mc.player.getInventory().selected, ClickType.SWAP, mc.player);
					mc.setScreen(null);
				}
			}

			if (placePos != null) {
				placeBed(placePos);
				makeBedExplode(placePos);
				checks.add(placePos);
			}

			if (bedSlot != -1) {
				mc.setScreen(new InventoryScreen(mc.player));
				mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, bedSlot, mc.player.getInventory().selected, ClickType.SWAP, mc.player);
				mc.setScreen(null);
			}
			blockevents = 0;
		} catch (Exception ex) {
			this.getLogger().error(ex.getMessage());
		}
	}

	public BlockPos findPlace(Player target) {
		List<Entity> nearbyEntities = mc.level.getEntities(null, target.getBoundingBox().inflate(5));
		int horizontalRadius = 1;
		int verticalHeight = 2;

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<BlockPos>> futures = new ArrayList<>();

		for (int yOffset = -verticalHeight; yOffset < verticalHeight; yOffset++) {
			BlockPos centerPos = target.blockPosition().above(yOffset);

			for (int xOffset = -horizontalRadius; xOffset <= horizontalRadius; xOffset++) {
				final int x = xOffset;
				for (int zOffset = -horizontalRadius; zOffset <= horizontalRadius; zOffset++) {
					final int z = zOffset;

					Callable<BlockPos> task = () -> {
						for (Direction dir : Direction.values()) {
							BlockPos checkPos = centerPos.offset(x, 0, z);
							BlockPos offsetPos = checkPos.relative(dir, 1);

							boolean entityInCenter = nearbyEntities.stream().anyMatch(entity -> entity.getBoundingBox().intersects(new AABB(checkPos)));
							boolean entityInOffset = nearbyEntities.stream().anyMatch(entity -> entity.getBoundingBox().intersects(new AABB(offsetPos)));

							if (entityInCenter || entityInOffset) continue;

							float headSelfDamage = DamageUtils.getBedDamage(new Vec3(checkPos.getX(), checkPos.getY(), checkPos.getZ()), mc.player);
							float offsetSelfDamage = DamageUtils.getBedDamage(new Vec3(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), mc.player);

							if (mc.level.getBlockState(checkPos).canBeReplaced()
									&& mc.level.getBlockState(offsetPos).canBeReplaced()
									&& DamageUtils.getBedDamage(new Vec3(checkPos.getX(), checkPos.getY(), checkPos.getZ()), target) >= minDamage.getValue()
									&& !mc.level.getBlockState(checkPos.below()).canBeReplaced()
									&& offsetSelfDamage < maxDamage.getValue()
									&& headSelfDamage < maxDamage.getValue()
									&& (!antiSuicide.getValue() || mc.player.getHealth() - headSelfDamage > 5)
									&& (!antiSuicide.getValue() || mc.player.getHealth() - offsetSelfDamage > 5)
							) {
								return checkPos;
							}
						}
						return null;
					};

					futures.add(executor.submit(task));
				}
			}
		}

		BlockPos result = null;
		for (Future<BlockPos> future : futures) {
			try {
				BlockPos pos = future.get();
				if (pos != null) {
					result = pos;
					break; // Stop if we find a valid position
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		executor.shutdown();
		return result;
	}


	public void makeBedExplode(BlockPos pos) {
		if (pos == null) return;

		if (!(mc.level.getBlockState(pos).getBlock() instanceof BedBlock)) return;

		boolean wasSneaking = mc.player.isShiftKeyDown();
		if (wasSneaking) mc.player.setShiftKeyDown(false);

		BlockHitResult hit = RusherHackAPI.interactions().getBlockHitResult(
				pos,
				false,
				rayTrace.getValue(),
				range.getValue()
		);

	    RusherHackAPI.interactions().useBlock(
				hit,
			    InteractionHand.MAIN_HAND,
			    rayTrace.getValue()
	    );

		mc.player.swing(InteractionHand.MAIN_HAND);

		if (wasSneaking) mc.player.setShiftKeyDown(wasSneaking);
	}


	@Subscribe
	public void onRender3D(EventRender3D e) {
		final IRenderer3D renderer = e.getRenderer();

		renderer.begin(e.getMatrixStack());

		final int fcolor = ColorUtils.transparency(Color.white.getRGB(), 0.5f);

		for (BlockPos pos : checks) {
			renderer.drawBox(
					pos,
					true,
					true,
					fcolor
			);
		}

		renderer.end();
	}
	@Override
	public void onEnable() {
		blockevents = 0;
	}

	@Override
	public void onDisable() {
		blockevents = 0;
	}
}
