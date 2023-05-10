package me.alphamode.varietyhunters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.alphamode.varietyhunters.manhunt.*;
import org.apache.commons.io.IOUtils;
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
            var result = RandomManConfig.CODEC.encodeStart(JsonOps.INSTANCE, new RandomManConfig(List.of())).result();
            if (result.isPresent()) {
               var json = result.get();
               IOUtils.write(GSON.toJson(json), output, StandardCharsets.UTF_8);
            }
         } catch (IOException e) {

         }
      } else {
         try (var input = Files.newInputStream(Paths.get("config/random_man.json"))) {
            DataResult<Pair<RandomManConfig, JsonElement>> result = RandomManConfig.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(input)));
            RANDOM_MAN_DROPS = result.result().get().getFirst();
         } catch (IOException e) {

         }
      }

   }

   public void onDisable() {
   }
}
