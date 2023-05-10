package me.alphamode.varietyhunters.manhunt;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BlockIterator;

import java.util.ArrayList;
import java.util.List;

public class ManhuntEvents implements Listener {
   private static ManhuntGame currentGame = null;
   private static final NamespacedKey CAN_PICKUP = NamespacedKey.fromString("can_pickup");

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
   public void onMoveEvent(PlayerMoveEvent event) {
      if (isGameInProgress() && currentGame.getAssassins().containsKey(event.getPlayer())) {
         currentGame.getRunners().forEach(runner -> {
            LivingEntity target = getTarget(runner);
            boolean frozen = currentGame.getAssassins().containsKey(target);
            currentGame.getAssassins().get(event.getPlayer()).setFrozen(runner, frozen);

            event.setCancelled(frozen);
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
            entity.getBukkitEntity().sendMessage(Component.text("Orginial: " + event.getDuration() * 20));
            entity.getBukkitEntity().sendMessage(Component.text("New: " + duration));
            entity.setRemainingFireTicks(duration);
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
               event.setCancelled(true);
            }
         }
         if (currentGame.getAssassins().containsKey(event.getEntity()) && currentGame.getAssassins().get(event.getEntity()).isLinked()) {
            event.setCancelled(true);
         }
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
            if (!(inventory.getHolder() instanceof Barrel || (inventory.getHolder() instanceof Chest chest && chest.getBlock().getBlockData().getMaterial() == Material.ENDER_CHEST))) {
               event.setCancelled(true);
               return;
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

   // from https://github.com/NateKomodo/DreamManHunt/blob/a514684093c386beef7374d17cfc2b7cf35c7fdc/src/main/java/cloud/lagrange/assassin/Worker.java#L107
   private LivingEntity getTarget(Player player) {
      int range = 60;
      List<Entity> nearbyEntities = player.getNearbyEntities(range, range, range);
      ArrayList<LivingEntity> entities = new ArrayList<>();

      for (Entity e : nearbyEntities) {
         if (e instanceof LivingEntity) {
            entities.add((LivingEntity) e);
         }
      }

      LivingEntity target = null;
      BlockIterator bItr = new BlockIterator(player, range);
      Block block;
      Location loc;
      int bx, by, bz;
      double ex, ey, ez;
      // loop through player's line of sight
      while (bItr.hasNext()) {
         block = bItr.next();
         if (!block.getType().equals(Material.AIR) && !block.getType().equals(Material.WATER)) break;
         bx = block.getX();
         by = block.getY();
         bz = block.getZ();
         // check for entities near this block in the line of sight
         for (LivingEntity e : entities) {
            loc = e.getLocation();
            ex = loc.getX();
            ey = loc.getY();
            ez = loc.getZ();
            if ((bx - .15 <= ex && ex <= bx + 1.15)
                    && (bz - .15 <= ez && ez <= bz + 1.15)
                    && (by - 1 <= ey && ey <= by + 1)) {
               // entity is close enough, set target and stop
               target = e;
               break;
            }
         }
      }
      return target;
   }
}
