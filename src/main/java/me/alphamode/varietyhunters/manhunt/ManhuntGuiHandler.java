package me.alphamode.varietyhunters.manhunt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ManhuntGuiHandler implements Listener {
   protected static Inventory selectionInv;
   private OfflinePlayer selectedPlayer = null;
   private static final NamespacedKey CLASS_KEY = new NamespacedKey("variety_hunters", "class");

   public static Inventory createSelectionInventory(Collection<? extends Player> onlinePlayers) {
      Inventory players = Bukkit.createInventory(null, 54, Component.text("Select Players"));
      onlinePlayers.forEach(player -> {
         ItemStack head = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta skull = (SkullMeta)head.getItemMeta();
         skull.setOwningPlayer(player);
         skull.displayName(player.displayName().decoration(TextDecoration.ITALIC, false));
         List<Component> lore = new ArrayList<>();
         lore.add(text("Current classes:", TextColor.color(NamedTextColor.GRAY)));
         boolean any = false;
         if (ManhuntEvents.getCurrentGame().getRunners().contains(player)) {
            lore.add(text("  Runner", TextColor.color(NamedTextColor.GREEN)));
            any = true;
         }
         if (ManhuntEvents.getCurrentGame().getHunters().contains(player)) {
            lore.add(text("  Hunter", TextColor.color(NamedTextColor.LIGHT_PURPLE)));
            any = true;
         }
         if (ManhuntEvents.getCurrentGame().getAssassins().containsKey(player)) {
            lore.add(text("  Assassin", TextColor.color(NamedTextColor.RED)));
            any = true;
         }
         if (ManhuntEvents.getCurrentGame().getRandomMen().contains(player)) {
            lore.add(text("  Random Man", TextColor.color(NamedTextColor.AQUA)));
            any = true;
         }
         if (!any)
            lore.add(text("  None", TextColor.color(NamedTextColor.GRAY)));
         skull.lore(lore);
         head.setItemMeta(skull);
         players.addItem(new ItemStack(head));
      });
      ItemStack startButton = new ItemStack(Material.LIME_DYE);
      startButton.editMeta(itemMeta -> itemMeta.displayName(text("Start Game", TextColor.color(NamedTextColor.GREEN))));
      players.setItem(53, startButton);
      return players;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      ItemStack clicked = event.getCurrentItem();
      if (!event.getInventory().equals(selectionInv)) return;
      ManhuntGame game = ManhuntEvents.getCurrentGame();
      if (clicked == null)
         return;
      if (clicked.getType() == Material.GREEN_STAINED_GLASS_PANE) {
         event.setCancelled(true);
         game.startGame();
         event.getWhoClicked().closeInventory();
         return;
      }
      if (clicked.getItemMeta() instanceof SkullMeta skullMeta) {
         this.selectedPlayer = skullMeta.getOwningPlayer();
         selectionInv = Bukkit.createInventory(event.getWhoClicked(), 27, Component.text("Select class for: " + selectedPlayer.getName()));
         ItemStack runnerClass = new ItemStack(Material.DIAMOND_BOOTS);
         runnerClass.editMeta(itemMeta -> {
            itemMeta.displayName(text("Runner", TextColor.color(NamedTextColor.GREEN)));
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.getPersistentDataContainer().set(CLASS_KEY, PersistentDataType.STRING, "runner");
         });
         selectionInv.setItem(10, runnerClass);
         ItemStack assassinClass = new ItemStack(Material.IRON_SWORD);
         ItemStack hunterClass = new ItemStack(Material.GOLDEN_HORSE_ARMOR);
         hunterClass.editMeta(itemMeta -> {
            itemMeta.displayName(text("Hunter", TextColor.color(NamedTextColor.LIGHT_PURPLE)));
            itemMeta.getPersistentDataContainer().set(CLASS_KEY, PersistentDataType.STRING, "hunter");
         });
         selectionInv.setItem(12, hunterClass);
         assassinClass.editMeta(itemMeta -> {
            itemMeta.displayName(text("Assassin", TextColor.color(NamedTextColor.RED)));
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.getPersistentDataContainer().set(CLASS_KEY, PersistentDataType.STRING, "assassin");
         });
         selectionInv.setItem(14, assassinClass);
         ItemStack randomClass = new ItemStack(Material.CHORUS_FRUIT);
         randomClass.editMeta(itemMeta -> {
            itemMeta.displayName(text("Random Man", TextColor.color(NamedTextColor.AQUA)));
            itemMeta.getPersistentDataContainer().set(CLASS_KEY, PersistentDataType.STRING, "random");
         });
         selectionInv.setItem(16, randomClass);

         for (int i = 0; i < 27; i++) {
            if (selectionInv.getItem(i) == null) {
               ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
               filler.editMeta(itemMeta -> itemMeta.displayName(Component.empty()));
               selectionInv.setItem(i, filler);
            }
         }
         event.getWhoClicked().openInventory(selectionInv);
      }
      if (clicked.getItemMeta().getPersistentDataContainer().has(CLASS_KEY)) {
         PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();
         Player player = this.selectedPlayer.getPlayer();
         switch (data.get(CLASS_KEY, PersistentDataType.STRING)) {
            case "assassin" -> {
               if (game.getAssassins().containsKey(player))
                  game.remove(ClassType.ASSASSIN, player);
               else
                  game.add(ClassType.ASSASSIN, player);
            }
            case "runner" -> {
               if (game.getRunners().contains(player))
                  game.remove(ClassType.RUNNER, player);
               else
                  game.add(ClassType.RUNNER, player);
            }
            case "hunter" -> {
               if (game.getHunters().contains(player))
                  game.remove(ClassType.HUNTER, player);
               else
                  game.add(ClassType.HUNTER, player);
            }
            case "random" -> {
               if (game.getRandomMen().contains(player))
                  game.remove(ClassType.RANDOM_MAN, player);
               else
                  game.add(ClassType.RANDOM_MAN, player);
            }
         }
         selectionInv = createSelectionInventory(Bukkit.getServer().getOnlinePlayers());
         event.getWhoClicked().openInventory(selectionInv);
      }
      event.setCancelled(true);
   }

   public static Component text(String text, TextColor color) {
      return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getInventory().equals(selectionInv))
         event.setCancelled(true);
   }
}
