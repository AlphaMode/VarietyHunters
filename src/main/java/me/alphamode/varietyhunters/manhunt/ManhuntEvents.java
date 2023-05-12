package me.alphamode.varietyhunters.manhunt;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;

public class ManhuntEvents implements Listener {
   private static ManhuntGame currentGame = null;
   public static final NamespacedKey CAN_PICKUP = NamespacedKey.fromString("can_pickup");

   public static ManhuntGame getCurrentGame() {
      return currentGame;
   }

   public static void setCurrentGame(ManhuntGame currentGame) {
      if (currentGame == null)
         ManhuntEvents.currentGame.endGame();
      ManhuntEvents.currentGame = currentGame;
   }

   public static boolean isGameInProgress() {
      return currentGame != null && currentGame.isStarted();
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      if (currentGame != null) {
         for (ClassType type : ClassType.values()) {
            if (currentGame.getOfflinePlayers().containsKey(type) && currentGame.getOfflinePlayers().get(type).contains(event.getPlayer().getUniqueId())) {
               currentGame.add(type, true, event.getPlayer());
               event.getPlayer().sendMessage(ManhuntGame.PREFIX.append(Component.text("You were reassigned to the ").append(Component.text(type.getDisplayName(), type.getColor()).append(Component.text(" class.", TextColor.color(NamedTextColor.GRAY))))));
            }
         }
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      if (currentGame != null) {
         currentGame.getPlayers().forEach((classType, players) -> {
            Player player = event.getPlayer();
            if (players.contains(player)) {
               players.remove(player);
               if (classType == ClassType.ASSASSIN)
                  currentGame.getAssassins().remove(player);
               currentGame.getOfflinePlayers().computeIfAbsent(classType, type -> new ArrayList<>()).add(player.getUniqueId());
            }
         });
      }
   }

   @EventHandler
   public void onMoveEvent(PlayerMoveEvent event) {
      if (isGameInProgress() && currentGame.getAssassins().containsKey(event.getPlayer())) {
         Assassin assassin = currentGame.getAssassins().get(event.getPlayer());
         currentGame.getRunners().forEach(runner -> {
            event.setCancelled(assassin.isFrozen());
         });
      }
   }

   @EventHandler
   public void onEntityCombust(EntityCombustEvent event) {
      if (isGameInProgress() && currentGame.getAssassins().containsKey(event.getEntity())) {
         event.setCancelled(true);
         ServerPlayer entity = ((CraftPlayer)event.getEntity()).getHandle();
         int duration = event.getDuration() * 20;

         int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, entity) + 6;
         if (i > 0) {
            duration -= Mth.floor((float)duration * (float)i * 0.15F);
         }
         if (entity.remainingFireTicks < duration) {
            entity.setRemainingFireTicks(0);
         }
      }
   }

   @EventHandler
   public void onAssassinDamaged(EntityDamageEvent event) {
      if (isGameInProgress() && (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || event.getCause() == EntityDamageEvent.DamageCause.LAVA) && currentGame.getAssassins().containsKey(event.getEntity())) {
         double f = Mth.clamp(13, 0.0F, 20.0F);
         double newDamage = (event.getDamage(EntityDamageEvent.DamageModifier.MAGIC)) - (event.getDamage() * (1.0F - f / 25.0F));
         event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, newDamage);
      }
   }

   @EventHandler
   public void onAttackPlayer(EntityDamageByEntityEvent event) {
      if (isGameInProgress()) {
         if (currentGame.getAssassins().containsKey(event.getDamager())) {
            Assassin assassin = currentGame.getAssassins().get(event.getDamager());
            if (currentGame.getAssassins().get(event.getDamager()).isLinked()) {
               Entity var3 = event.getEntity();
               if (var3 instanceof Player damagee && currentGame.getRunners().contains(damagee) && !assassin.isFrozen()) {
                  damagee.setHealth(0);
               }
            }

            if (assassin.isFrozen() || !assassin.isLinked()) {
               if (event.getEntity() instanceof Player)
                  event.setCancelled(true);
            }
         }
         if (currentGame.getAssassins().containsKey(event.getEntity()) && currentGame.getAssassins().get(event.getEntity()).isLinked() && event.getDamager() instanceof Player) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onAppliedInvisibility(EntityPotionEffectEvent event) {
      if (isGameInProgress() && event.getAction() == EntityPotionEffectEvent.Action.ADDED && event.getModifiedType() == PotionEffectType.INVISIBILITY && currentGame.getAssassins().containsKey(event.getEntity())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onThrowPearl(PlayerInteractEvent event) {
      if (isGameInProgress() && event.hasItem() && event.getItem().getType() == Material.ENDER_PEARL) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onProjectileImpact(ProjectileHitEvent event) {
      ProjectileSource var3 = event.getEntity().getShooter();
      if (isGameInProgress() && var3 instanceof Player player && currentGame.getAssassins().containsKey(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onFoodChange(FoodLevelChangeEvent event) {
      if (isGameInProgress() && currentGame.getRandomMen().contains(event.getEntity())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onPlayerDied(PlayerDeathEvent event) {
      if (isGameInProgress()) {
         event.getDrops().removeIf(stack -> stack.getType() == Material.COMPASS);
         if (currentGame.getRandomMen().contains(event.getPlayer()))
            event.getDrops().forEach(itemStack -> itemStack.editMeta(itemMeta -> itemMeta.getPersistentDataContainer().set(CAN_PICKUP, PersistentDataType.BYTE, (byte)0)));
      }
   }

   @EventHandler
   public void onHunterRespawn(PlayerRespawnEvent event) {
      if (isGameInProgress() && !currentGame.getRunners().contains(event.getPlayer()))
         event.getPlayer().getInventory().addItem(new ItemStack(Material.COMPASS));
   }

   @EventHandler
   public void onPickupItem(PlayerAttemptPickupItemEvent event) {
      if (isGameInProgress()) {
         if (currentGame.getRandomMen().contains(event.getPlayer())) {
            event.setCancelled(!event.getItem().getItemStack().getItemMeta().getPersistentDataContainer().has(CAN_PICKUP));
         } else if (event.getItem().getItemStack().getItemMeta().getPersistentDataContainer().has(CAN_PICKUP))
            event.setCancelled(true);
      }
   }

   @EventHandler
   public void onDropItem(PlayerDropItemEvent event) {
      if (isGameInProgress() && currentGame.getRandomMen().contains(event.getPlayer())) {
         event.getItemDrop().getItemStack().editMeta(itemMeta -> itemMeta.getPersistentDataContainer().set(CAN_PICKUP, PersistentDataType.BYTE, (byte)0));
      }
   }

   @EventHandler
   public void onOpenInventory(InventoryOpenEvent event) {
      if (isGameInProgress()) {
         Inventory inventory = event.getInventory();
         if (currentGame.getRandomMen().contains(event.getPlayer())) {
            BlockState state = ((CraftBlock)((Chest)inventory.getHolder()).getBlock()).getNMS();
            if (!(inventory.getHolder() instanceof Barrel || inventory instanceof CraftingInventory || state.is(Blocks.ENDER_CHEST))) {
               event.setCancelled(true);
            }
         } else {
            if (inventory.getHolder() instanceof Barrel) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onServerTick(ServerTickEndEvent event) {
      if (isGameInProgress()) {
         currentGame.tick(event.getTickNumber());
      }
   }
}
