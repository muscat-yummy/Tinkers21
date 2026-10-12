package slimeknights.tconstruct.port;

import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import slimeknights.mantle.network.NetworkEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.entity.FancyItemFrameEntity;
import slimeknights.tconstruct.gadgets.entity.FrameType;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;
import slimeknights.tconstruct.tables.network.StationTabPacket;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class SmallBugsGameTest {
  @GameTest(template="bootstrap",timeoutTicks=100)
  public static void stationTabPreservesCursor(GameTestHelper helper) throws Exception {
    var player = new ExtendedGameTestHelper(helper.testInfo).makeTickingMockServerPlayerInLevel(GameType.SURVIVAL);
    ItemStack carried = new ItemStack(Items.DIAMOND, 7);
    carried.set(DataComponents.CUSTOM_NAME, Component.literal("Cursor survives"));
    player.containerMenu.setCarried(carried.copy());
    IPayloadContext payload = (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
      new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> switch(method.getName()) {
        case "flow" -> PacketFlow.SERVERBOUND;
        case "player" -> player;
        default -> throw new UnsupportedOperationException(method.getName());
      });
    var constructor = NetworkEvent.Context.class.getDeclaredConstructor(IPayloadContext.class);
    constructor.setAccessible(true);
    var context = constructor.newInstance(payload);
    BlockPos unloaded = new BlockPos(29000000, 80, 29000000);
    helper.assertTrue(!helper.getLevel().hasChunkAt(unloaded), "Fixture target is loaded");
    new StationTabPacket(unloaded).handleThreadsafe(context);
    helper.assertTrue(ItemStack.matches(player.containerMenu.getCarried(), carried), "Unloaded tab lost cursor item");
    BlockPos relative = new BlockPos(1, 1, 1);
    helper.setBlock(relative, TinkerTables.craftingStation.get());
    new StationTabPacket(helper.absolutePos(relative)).handleThreadsafe(context);
    helper.assertTrue(player.containerMenu instanceof CraftingStationContainerMenu, "Valid tab did not open station");
    helper.assertTrue(ItemStack.matches(player.containerMenu.getCarried(), carried), "Valid tab changed cursor components/count");
    helper.assertTrue(player.getInventory().isEmpty(), "Tab duplicated cursor into inventory");
    // Closing must preserve the seven items exactly once.
    player.closeContainer();
    helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 7, "Closing tab lost or duplicated cursor");
    TConstruct.LOG.info("TiC actual unloaded and valid station tab cursor conservation PASS");
    helper.succeed();
  }

  @GameTest(template="bootstrap",timeoutTicks=100)
  public static void craftingFullAndPartialDestinations(GameTestHelper helper) {
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    BlockPos relative = new BlockPos(1, 1, 1);
    helper.setBlock(relative, TinkerTables.craftingStation.get());
    var absolute = helper.absolutePos(relative);
    player.setPos(absolute.getX()+0.5,absolute.getY()+1,absolute.getZ()+0.5);
    var tile = (CraftingStationBlockEntity)helper.getLevel().getBlockEntity(absolute);
    var menu = new CraftingStationContainerMenu(1, player.getInventory(), tile);
    for (int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
    tile.setItem(0,new ItemStack(Items.OAK_LOG,2));
    helper.assertTrue(menu.getSlot(9).getItem().is(Items.OAK_PLANKS), "Fixture crafting recipe missing");
    helper.assertTrue(menu.quickMoveStack(player,9).isEmpty(), "Full inventory unexpectedly crafted");
    helper.assertTrue(tile.getItem(0).getCount()==2 && menu.getSlot(9).getItem().getCount()==4,
      "Full inventory consumed inputs or lost result");
    player.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,62));
    helper.assertTrue(!menu.quickMoveStack(player,9).isEmpty(), "Partial stack refused crafting");
    helper.assertTrue(player.getInventory().getItem(0).getCount()==64 && tile.getItem(0).getCount()==1,
      "Partial craft changed input/output counts");
    var drops=helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
      player.getBoundingBox().inflate(3));
    helper.assertTrue(drops.stream().filter(e->e.getItem().is(Items.OAK_PLANKS)).mapToInt(e->e.getItem().getCount()).sum()==2,
      "Partial craft lost overflow instead of dropping it");
    player.getInventory().setItem(1,ItemStack.EMPTY);
    helper.assertTrue(!menu.quickMoveStack(player,9).isEmpty() && tile.getItem(0).isEmpty()
      && player.getInventory().getItem(1).getCount()==4, "Empty destination failed normal craft");
    // The attached chest remains a valid destination with the player inventory full.
    for (int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
    helper.setBlock(relative.east(),Blocks.CHEST);
    var chest=(ChestBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(relative.east()));
    var withChest=new CraftingStationContainerMenu(2,player.getInventory(),tile);
    helper.assertTrue(!withChest.subContainers.isEmpty(), "Attached chest not detected");
    tile.setItem(0,new ItemStack(Items.OAK_LOG));
    helper.assertTrue(withChest.getSlot(9).getItem().is(Items.OAK_PLANKS)
      && !withChest.quickMoveStack(player,9).isEmpty() && chest.countItem(Items.OAK_PLANKS)==4,
      "Attached chest failed craft destination");
    TConstruct.LOG.info("TiC actual full partial empty and attached crafting destinations conservation PASS");
    helper.succeed();
  }

  @GameTest(template="bootstrap",timeoutTicks=100)
  public static void fancyFrameRotationReload(GameTestHelper helper) {
    for(FrameType type:FrameType.values()) {
      int rotation=type.hasMoreRotations()?13:5;
      if(type==FrameType.DIAMOND) rotation=16;
      var frame=new FancyItemFrameEntity(helper.getLevel(),helper.absolutePos(new BlockPos(1,1,1)),Direction.NORTH,type);
      frame.setItem(new ItemStack(Items.DIAMOND),false);
      frame.setRotation(rotation);
      var saved=new CompoundTag();
      frame.saveWithoutId(saved);
      var restored=new FancyItemFrameEntity(slimeknights.tconstruct.gadgets.TinkerGadgets.itemFrameEntity.get(),helper.getLevel());
      restored.load(saved);
      helper.assertTrue(restored.getFrameType()==type && restored.getRotation()==rotation && restored.getItem().is(Items.DIAMOND),
        "Frame variant/rotation/item changed on reload: "+type+" got "+restored.getRotation());
      var again=new CompoundTag(); restored.saveWithoutId(again);
      helper.assertTrue(again.getInt("RotationTimer")==saved.getInt("RotationTimer"),"Frame timer changed on reload");
      restored.load(again);
      helper.assertTrue(restored.getRotation()==rotation,"Second frame reload changed rotation");
    }
    TConstruct.LOG.info("TiC actual six fancy frame variants rotation timer item save reload PASS");
    helper.succeed();
  }
}
