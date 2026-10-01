package slimeknights.mantle.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import slimeknights.mantle.Mantle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dumps the block tags governing tool harvest restrictions in 1.21. */
public class HarvestTiersCommand {
  /** Diagnostic output; 1.21 no longer has a global tier ordering file. */
  protected static final ResourceLocation HARVEST_TIERS = ResourceLocation.fromNamespaceAndPath("neoforge", "incorrect_blocks_for_drops.json");
  /** Path for saving the harvest restriction diagnostic */
  private static final String HARVEST_TIER_PATH = HARVEST_TIERS.getNamespace() + "/" + HARVEST_TIERS.getPath();

  // command messages
  private static final Component SUCCESS_LOG = Component.translatable("command.mantle.harvest_tiers.success_log");
  private static final Component EMPTY = Component.translatable("command.mantle.tag.empty");

  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(sender -> sender.hasPermission(MantleCommand.PERMISSION_EDIT_SPAWN))
              .then(Commands.literal("save").executes(source -> run(source, true)))
              .then(Commands.literal("log").executes(source -> run(source, false)))
              .then(Commands.literal("list").executes(HarvestTiersCommand::list));
  }

  /** Creates a clickable component for a block tag */
  private static Component getTagComponent(TagKey<Block> tag) {
    ResourceLocation id = tag.location();
    return Component.literal(id.toString()).withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, "/mantle dump_tag " + Registries.BLOCK.location() + " " + id + " save")));
  }

  /** Collects actual tier restriction tags, including modded tier tags and loaded named restrictions. */
  private static Map<TagKey<Block>,List<ResourceLocation>> getRestrictions(CommandSourceStack source) {
    Map<TagKey<Block>,List<ResourceLocation>> restrictions = new LinkedHashMap<>();
    BuiltInRegistries.ITEM.forEach(item -> {
      if (item instanceof TieredItem tiered) {
        restrictions.computeIfAbsent(tiered.getTier().getIncorrectBlocksForDrops(), ignored -> new ArrayList<>())
                    .add(BuiltInRegistries.ITEM.getKey(item));
      }
    });
    source.registryAccess().registryOrThrow(Registries.BLOCK).getTagNames()
          .filter(tag -> tag.location().getPath().startsWith("incorrect_for_"))
          .forEach(tag -> restrictions.computeIfAbsent(tag, ignored -> new ArrayList<>()));
    Map<TagKey<Block>,List<ResourceLocation>> sorted = new LinkedHashMap<>();
    restrictions.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
                .forEach(entry -> {
                  entry.getValue().sort(Comparator.comparing(ResourceLocation::toString));
                  sorted.put(entry.getKey(), entry.getValue());
                });
    return sorted;
  }

  /** Lists the restriction tags; there is no global tier ordering in 1.21. */
  private static int list(CommandContext<CommandSourceStack> context) {
    Map<TagKey<Block>,List<ResourceLocation>> restrictions = getRestrictions(context.getSource());
    MutableComponent output = Component.translatable("command.mantle.harvest_tiers.success_list")
                                       .append(" (incorrect blocks for drops)");
    if (restrictions.isEmpty()) {
      output.append("\n* ").append(EMPTY);
    } else {
      restrictions.keySet().forEach(tag -> output.append("\n* ").append(getTagComponent(tag)));
    }
    context.getSource().sendSuccess(() -> output, true);
    return restrictions.size();
  }

  /** Exports the actual tag membership and item users, rather than inventing a total ordering. */
  private static int run(CommandContext<CommandSourceStack> context, boolean saveFile) throws CommandSyntaxException {
    Map<TagKey<Block>,List<ResourceLocation>> restrictions = getRestrictions(context.getSource());
    Registry<Block> blocks = context.getSource().registryAccess().registryOrThrow(Registries.BLOCK);
    JsonObject tags = new JsonObject();
    restrictions.forEach((tag, items) -> {
      JsonObject entry = new JsonObject();
      JsonArray itemIds = new JsonArray();
      items.forEach(id -> itemIds.add(id.toString()));
      entry.add("items", itemIds);
      JsonArray blockIds = new JsonArray();
      blocks.getTag(tag).ifPresent(values -> values.stream()
        .map(holder -> blocks.getKey(holder.value()).toString()).sorted().forEach(blockIds::add));
      entry.add("blocks", blockIds);
      tags.add(tag.location().toString(), entry);
    });
    JsonObject json = new JsonObject();
    json.addProperty("format", "incorrect_blocks_for_drops");
    json.add("tags", tags);
    if (saveFile) {
      File output = new File(DumpAllTagsCommand.getOutputFile(context), HARVEST_TIER_PATH);
      Path path = output.toPath();
      try {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
          writer.write(DumpTagCommand.GSON.toJson(json));
        }
      } catch (IOException ex) {
        Mantle.logger.error("Couldn't save harvest restrictions to {}", path, ex);
        throw new SimpleCommandExceptionType(Component.literal("Could not save harvest restrictions: " + path)).create();
      }
      context.getSource().sendSuccess(() -> Component.translatable("command.mantle.harvest_tiers.success_save", GeneratePackHelper.getOutputComponent(output)), true);
    } else {
      context.getSource().sendSuccess(() -> SUCCESS_LOG, true);
      Mantle.logger.info("Dump of harvest restrictions:\n{}", DumpTagCommand.GSON.toJson(json));
    }
    return restrictions.size();
  }
}
