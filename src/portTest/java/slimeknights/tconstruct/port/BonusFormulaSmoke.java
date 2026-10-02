package slimeknights.tconstruct.port;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import slimeknights.tconstruct.tools.modifiers.loot.BonusFormulaCodec;

import java.util.List;
import java.util.Map;

public final class BonusFormulaSmoke {
  private static void sameResults(ApplyBonusCount.Formula expected, ApplyBonusCount.Formula actual) {
    for (int level : new int[]{0, 1, 2, 5}) {
      for (int count : new int[]{1, 3, 8}) {
        for (long seed : new long[]{0, 1, 42, 9001}) {
          int want = expected.calculateNewCount(RandomSource.create(seed), count, level);
          int got = actual.calculateNewCount(RandomSource.create(seed), count, level);
          if (want != got) throw new AssertionError("Formula result changed: " + want + " != " + got);
        }
      }
    }
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
    var codec = BonusFormulaCodec.CODEC.codec();
    List<String> json = List.of(
      "{\"formula\":\"minecraft:ore_drops\"}",
      "{\"formula\":\"minecraft:binomial_with_bonus_count\",\"parameters\":{\"extra\":3,\"probability\":0.35}}",
      "{\"formula\":\"minecraft:uniform_bonus_count\",\"parameters\":{\"bonusMultiplier\":2}}");
    List<ApplyBonusCount.Formula> originals = List.of(
      new ApplyBonusCount.OreDrops(), new ApplyBonusCount.BinomialWithBonusCount(3, 0.35f),
      new ApplyBonusCount.UniformBonusCount(2));
    var stream = ByteBufCodecs.fromCodecWithRegistries(codec);
    for (int index = 0; index < json.size(); index++) {
      var decoded = codec.parse(ops, JsonParser.parseString(json.get(index))).getOrThrow();
      sameResults(originals.get(index), decoded);
      var encoded = codec.encodeStart(ops, decoded).getOrThrow();
      sameResults(originals.get(index), codec.parse(ops, encoded).getOrThrow());
      var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
      try {
        stream.encode(buffer, decoded);
        sameResults(originals.get(index), stream.decode(buffer));
      } finally {
        buffer.release();
      }
    }
    if (codec.parse(ops, JsonParser.parseString("{\"formula\":\"port_test:unknown\"}")).error().isEmpty()) {
      throw new AssertionError("Unknown formula accepted");
    }
    if (codec.parse(ops, JsonParser.parseString("{}")).error().isEmpty()) {
      throw new AssertionError("Missing formula accepted");
    }
    System.out.println("Native bonus formula legacy JSON, deterministic drop counts and JSON/wire round trips: PASS");
  }
}
