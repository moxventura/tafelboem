package nl.jeeninga.tafelboem.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.Shape;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * Puts a hub or an arena in its slot: either a plain {@link Shape} from code or a structure template
 * the kids saved. Placing again is how a hub repairs itself and an arena loses its craters.
 */
public final class ShapePlacer {
	private ShapePlacer() {
	}

	/** Empties a slot: every block to air, every entity but the players gone (items, chickens, frames). */
	public static void clear(ServerLevel level, ArenaBox slot) {
		clearBlocks(level, slot);
		clearEntities(level, slot);
	}

	/** Every block in the box to air. */
	public static void clearBlocks(ServerLevel level, ArenaBox box) {
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (!level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** Every entity in the box but the players: items, chickens, bombs in flight, frames. */
	public static void clearEntities(ServerLevel level, ArenaBox box) {
		level.getEntities((Entity) null, aabb(box), entity -> !(entity instanceof Player)).forEach(Entity::discard);
	}

	public static PlacedArea placeShape(ServerLevel level, BlockPos corner, Shape shape) {
		for (Shape.Cell cell : shape.cells()) {
			level.setBlock(corner.offset(cell.x(), cell.y(), cell.z()), Materials.state(cell), Block.UPDATE_CLIENTS);
		}
		for (Shape.Sign sign : shape.signs()) {
			placeSign(level, corner.offset(sign.x(), sign.y(), sign.z()), sign);
		}
		List<UUID> frames = new ArrayList<>();
		for (Shape.Frame frame : shape.frames()) {
			frames.add(placeFrame(level, corner.offset(frame.x(), frame.y(), frame.z()), frame));
		}
		ArenaBox bounds = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), shape.sizeX(), shape.sizeY(), shape.sizeZ());
		Shape.Spot stand = shape.standAt();
		Shape.Spot middle = shape.middle();
		return new PlacedArea(shape.name(), bounds, corner.offset(stand.x(), stand.y(), stand.z()), Materials.direction(stand.facing()).toYRot(),
				Vec3.atBottomCenterOf(corner.offset(middle.x(), middle.y(), middle.z())), doors(level, bounds), List.copyOf(frames));
	}

	/**
	 * Places a template. The kids arrive 3 blocks in front of its exit door, on the highest solid
	 * block there. A door faces whoever placed it, so one placed from outside the wall faces out:
	 * without ground in front of it inside the template, they arrive 3 blocks behind it, facing away
	 * from it. Failing that, and in a template without an exit door (a hub), they arrive in its
	 * middle; never at the bottom of the world.
	 */
	public static PlacedArea placeTemplate(ServerLevel level, BlockPos corner, String name, StructureTemplate template) {
		template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
		Vec3i size = template.getSize();
		ArenaBox bounds = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), size.getX(), size.getY(), size.getZ());
		Map<Integer, List<BlockPos>> doors = doors(level, bounds);
		int middleX = (int) Math.floor(bounds.middleX());
		int middleZ = (int) Math.floor(bounds.middleZ());
		BlockPos middle = ground(level, middleX, middleZ, bounds).orElse(new BlockPos(middleX, bounds.maxY() + 1, middleZ));

		List<BlockPos> exits = doors.getOrDefault(0, List.of());
		if (!exits.isEmpty()) {
			BlockPos exit = exits.getFirst();
			Direction facing = level.getBlockState(exit).getValue(LevelDoorBlock.FACING);
			for (Direction away : List.of(facing, facing.getOpposite())) {
				BlockPos spot = exit.relative(away, 3);
				Optional<BlockPos> stand = ground(level, spot.getX(), spot.getZ(), bounds);
				if (stand.isPresent()) {
					return new PlacedArea(name, bounds, stand.get(), away.toYRot(), Vec3.atBottomCenterOf(middle), doors, List.of());
				}
			}
		}
		return new PlacedArea(name, bounds, middle, Direction.NORTH.toYRot(), Vec3.atBottomCenterOf(middle), doors, List.of());
	}

	/** Every level door in the area, by level, lowest blocks first. */
	static Map<Integer, List<BlockPos>> doors(ServerLevel level, ArenaBox bounds) {
		Map<Integer, List<BlockPos>> doors = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlocks.LEVEL_DOOR)) {
				doors.computeIfAbsent(state.getValue(LevelDoorBlock.LEVEL), key -> new ArrayList<>()).add(pos.immutable());
			}
		}
		return doors;
	}

	/** The block above the highest solid block of this column inside the area, if it has one. */
	private static Optional<BlockPos> ground(ServerLevel level, int x, int z, ArenaBox bounds) {
		if (!bounds.containsColumn(x, z)) {
			return Optional.empty();
		}
		for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (level.getBlockState(pos).isSolid()) {
				return Optional.of(pos.above());
			}
		}
		return Optional.empty();
	}

	private static void placeSign(ServerLevel level, BlockPos pos, Shape.Sign sign) {
		level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Materials.direction(sign.facing())),
				Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof SignBlockEntity entity) {
			// Translatable text, so every PC shows it in its own game language.
			entity.setText(entity.getFrontText()
					.setMessage(1, Component.translatable("tafelboem.door.sign", sign.level()))
					.setMessage(2, Component.translatable("tafelboem.level." + sign.level())), true);
			entity.setWaxed(true);
			entity.setChanged();
			level.sendBlockUpdated(pos, entity.getBlockState(), entity.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * Item frames can only be made fixed through their saved data, so the vanilla command does it.
	 * A frame loads its block position from that data and wants it near its own position, hence both.
	 * Its id is chosen here, so the hub knows its own frames (see {@link Hub#isLeftOverFrame}).
	 */
	private static UUID placeFrame(ServerLevel level, BlockPos pos, Shape.Frame frame) {
		UUID id = UUID.randomUUID();
		int[] uuid = UUIDUtil.uuidToIntArray(id);
		String item = BuiltInRegistries.ITEM.getKey(ModBlocks.bombItem(frame.bomb())).toString();
		Vec3 centre = Vec3.atBottomCenterOf(pos);
		String data = String.format(Locale.ROOT,
				"{UUID:[I;%d,%d,%d,%d],Pos:[%.1fd,%.1fd,%.1fd],block_pos:[I;%d,%d,%d],Facing:%db,Fixed:1b,Item:{id:\"%s\",count:1},Tags:[\"%s\"]}",
				uuid[0], uuid[1], uuid[2], uuid[3], centre.x, centre.y, centre.z, pos.getX(), pos.getY(), pos.getZ(),
				Materials.direction(frame.facing()).get3DDataValue(), item, BombEffects.HUB_TAG);
		String command = String.format(Locale.ROOT, "summon minecraft:item_frame %.1f %.1f %.1f %s", centre.x, centre.y, centre.z, data);
		MinecraftServer server = level.getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(), command);
		return id;
	}

	private static AABB aabb(ArenaBox box) {
		return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
	}
}
