package me.alphamode.varietyhunters.manhunt;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ManhuntCommands {
   public static boolean initGame(@NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, String[] args) {
      if (sender instanceof Player commandPlayer) {
         if (ManhuntEvents.getCurrentGame() != null) {
            sender.sendMessage(Component.text("A game already exists!"));
            return true;
         }

         ManhuntEvents.setCurrentGame(new ManhuntGame());
         Inventory players = ManhuntGuiHandler.createSelectionInventory(sender.getServer().getOnlinePlayers());
         commandPlayer.openInventory(players);
         ManhuntGuiHandler.selectionInv = players;
      }

      return true;
   }

   public static boolean startGame(@NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, String[] args) {
      if (ManhuntEvents.getCurrentGame() == null) {
         sender.sendMessage(Component.text("No game in progress."));
      }
      ManhuntEvents.getCurrentGame().startGame();
      sender.sendMessage(Component.text("Game has been started! Don't die..."));
      return true;
   }

   public static boolean endGame(@NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, String[] args) {
      ManhuntEvents.setCurrentGame(null);
      return true;
   }

   public static boolean add(@NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, String[] a) {
      if (ManhuntEvents.getCurrentGame() == null) {
         ManhuntEvents.setCurrentGame(new ManhuntGame());
         sender.sendMessage(Component.text("No game in progress creating one!"));
      }

      Server server = sender.getServer();
      ClassType classType = ClassType.valueOf(a[0]);
      List<String> args = new ArrayList<>(List.of(a));
      args.remove(0);

      for(String arg : args) {
         Player player = server.getPlayer(arg);
         if (player == null) {
            sender.sendMessage(Component.text("Could not find player: " + arg));
         } else {
            ManhuntEvents.getCurrentGame().add(classType, player);
            sender.sendMessage(Component.text("Added " + player.getName() + " to the " + classType + " class."));
         }
      }

      return true;
   }
}
