package me.alphamode.varietyhunters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.alphamode.varietyhunters.manhunt.*;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class VarietyHunters extends JavaPlugin implements Listener {
   public static VarietyHunters PLUGIN;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static RandomManConfig RANDOM_MAN_DROPS;
   public void onEnable() {
      PLUGIN = this;
      PluginManager manager = getServer().getPluginManager();
      manager.registerEvents(new ManhuntEvents(), this);
      manager.registerEvents(new ManhuntGuiHandler(), this);
      this.getCommand("initgame").setExecutor(ManhuntCommands::initGame);
      this.getCommand("startgame").setExecutor(ManhuntCommands::startGame);
      this.getCommand("endgame").setExecutor(ManhuntCommands::endGame);
      PluginCommand add = this.getCommand("add");
      add.setExecutor(ManhuntCommands::add);
      add.setTabCompleter((sender, command, label, args) -> {
         if (args.length == 1) {
            List<String> selections = new ArrayList<>(4);
            for (ClassType type : ClassType.values()) {
               if (type.getCommand().startsWith(args[0])) {
                  selections.add(type.getCommand());
               }
            }
            return selections;
         } else {
            List<String> players = sender.getServer().getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());

            for(int i = 0; i < args.length; ++i) {
               if (i != 0) {
                  players.remove(args[i]);
               }
            }

            return players;
         }
      });
      Path config = Paths.get("config/random_man.json");
      if (!Files.exists(config)) {
         try (var output = Files.newOutputStream(config)) {
            List<MobEffect> potionBlacklist = List.of(
                    MobEffects.DAMAGE_RESISTANCE,
                    MobEffects.DAMAGE_BOOST
            );
            List<Item> blacklist = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
               if (item instanceof SpawnEggItem || item instanceof TippedArrowItem || item instanceof PotionItem)
                  blacklist.add(item);
               if (item.getFoodProperties() != null && item != Items.GOLDEN_APPLE)
                  blacklist.add(item);
            }
            blacklist.addAll(List.of(
                    Items.BEDROCK, Items.SPAWNER, Items.END_PORTAL_FRAME, Items.COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK, Items.COMMAND_BLOCK_MINECART,
                    Items.STRUCTURE_BLOCK, Items.JIGSAW, Items.BARRIER, Items.STRUCTURE_VOID, Items.DEBUG_STICK,
                    Items.DIAMOND_BLOCK, Items.NETHERITE_BLOCK, Items.ELYTRA, Items.TOTEM_OF_UNDYING, Items.WITHER_SKELETON_SKULL,
                    Items.END_CRYSTAL, Items.DRAGON_EGG, Items.TIPPED_ARROW, Items.LINGERING_POTION, Items.SPLASH_POTION
            ));
            List<Item> blacklistT2 = List.of(
                    Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE,
                    Items.IRON_INGOT, Items.RAW_IRON, Items.IRON_BLOCK, Items.RAW_IRON_BLOCK, Items.GOLD_INGOT, Items.RAW_GOLD, Items.GOLD_BLOCK, Items.RAW_GOLD_BLOCK,
                    Items.GOLDEN_SHOVEL, Items.GOLDEN_AXE, Items.GOLDEN_SWORD, Items.GOLDEN_HOE, Items.GOLDEN_PICKAXE,
                    Items.GOLDEN_BOOTS, Items.GOLDEN_LEGGINGS, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_HELMET,
                    Items.LAVA_BUCKET, Items.TURTLE_EGG, Items.LEATHER_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.IRON_HORSE_ARMOR,
                    Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SHOVEL, Items.IRON_SWORD, Items.IRON_PICKAXE,
                    Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET, Items.BOW,
                    Items.SHEARS, Items.FLINT_AND_STEEL, Items.TURTLE_HELMET
            );
            List<Item> blacklistT3 = List.of(
                    Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE,
                    Items.EMERALD_BLOCK, Items.OBSIDIAN, Items.CRYING_OBSIDIAN,
                    Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP, Items.ENCHANTING_TABLE,
                    Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL,
                    Items.TNT, Items.TNT_MINECART, Items.BEACON,
                    Items.DIAMOND, Items.EMERALD, Items.ENDER_PEARL,
                    Items.DIAMOND_HORSE_ARMOR, Items.GOLDEN_APPLE,
                    Items.DIAMOND_AXE, Items.DIAMOND_SWORD, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_PICKAXE,
                    Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET,
                    Items.CROSSBOW, Items.TRIDENT, Items.ENDER_CHEST
            );
            List<Item> blacklistT4 = new ArrayList<>(List.of(
                    Items.NETHERITE_INGOT, Items.SHULKER_SHELL,
                    Items.NETHERITE_AXE, Items.NETHERITE_HOE, Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_SWORD,
                    Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET,
                    Items.BLUE_SHULKER_BOX, Items.POTION, Items.BLAZE_POWDER, Items.BLAZE_ROD, Items.ENDER_EYE
            ));
            for (Item item : BuiltInRegistries.ITEM) {
               if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)
                  blacklistT4.add(blockItem);
               if (item instanceof RecordItem)
                  blacklistT4.add(item);
            }
            var result = RandomManConfig.CODEC.encodeStart(JsonOps.INSTANCE, new RandomManConfig(potionBlacklist, blacklist, blacklistT2, blacklistT3, blacklistT4)).result();
            if (result.isPresent()) {
               var json = result.get();
               IOUtils.write(GSON.toJson(json), output, StandardCharsets.UTF_8);
            }
         } catch (IOException e) {

         }
      }
      try (var input = Files.newInputStream(Paths.get("config/random_man.json"))) {
         DataResult<Pair<RandomManConfig, JsonElement>> result = RandomManConfig.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(input)));
         RANDOM_MAN_DROPS = result.result().get().getFirst();
      } catch (IOException e) {

      }
   }

   public void onDisable() {
      ManhuntGame game = ManhuntEvents.getCurrentGame();
      if (game != null)
         game.endGame();
   }
}
