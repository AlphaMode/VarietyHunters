package me.alphamode.varietyhunters.manhunt;

import me.alphamode.varietyhunters.VarietyHunters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockIterator;

import java.util.*;

public class ManhuntGame {
   public static final Component PREFIX = Component.text("Variety").append(Component.text("Hunters", TextColor.color(NamedTextColor.RED))).append(Component.text(">> ", TextColor.color(NamedTextColor.GRAY))).color(TextColor.color(NamedTextColor.GRAY));
   private final GameTimer timer = new GameTimer();
   private final HashMap<ClassType, List<Player>> players;
   private final HashMap<ClassType, List<UUID>> offlinePlayers;
   private final HashMap<Player, Assassin> assassins;
   private final RandomSource random = RandomSource.create();
   private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
   private BukkitTask itemTask, t2, t3, t4;
   private boolean started = false;

   private int currentBlacklistLevel = 3;

   public ManhuntGame() {
      this.players = new HashMap<>();
      this.offlinePlayers = new HashMap<>();
      this.assassins = new HashMap<>();
      for (ClassType type : ClassType.values()) {
         Team team = this.scoreboard.registerNewTeam(type.getCommand());
         team.prefix(Component.text("[" + type.getDisplayName() + "]", type.getColor()));
      }
   }

   public void add(ClassType type, boolean slient, Player... players) {
      for (Player player : players) {
         this.players.computeIfAbsent(type, classType -> new ArrayList<>());
         this.players.get(type).add(player);
         if (type == ClassType.ASSASSIN)
            this.assassins.put(player, new Assassin(player));
         if (!slient)
            sendGameMessage(player.displayName().append(Component.text(" is now in the ").append(Component.text(type.getDisplayName(), type.getColor())).append(Component.text(" class."))));
         player.playerListName(Component.text("[" + type.getDisplayName() + "] ", type.getColor()).append(player.displayName()));
         this.scoreboard.getTeam(type.getCommand()).addEntity(player);
      }
   }

   public void remove(ClassType type, Player... players) {
      for (Player player : players) {
         getPlayers().get(type).remove(player);
         if (type == ClassType.ASSASSIN)
            getAssassins().remove(player);
         sendGameMessage(player.displayName().append(Component.text(" is no longer in the ").append(Component.text(type.getDisplayName(), type.getColor())).append(Component.text(" class."))));
      }
   }

   public void add(ClassType type, Player... players) {
      add(type, false, players);
   }

   public void startGame() {
      this.players.get(ClassType.RANDOM_MAN).forEach(player -> {
         player.setFoodLevel(20);
      });
      this.started = true;
      this.itemTask = Bukkit.getScheduler().runTaskTimer(VarietyHunters.PLUGIN, () -> {
         getRandomMen().forEach(player -> {
            ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
            ItemStack rolledStack = BuiltInRegistries.ITEM.getRandom(random).orElseThrow().value().getDefaultInstance();
            while (VarietyHunters.RANDOM_MAN_DROPS.getCurrentBlacklist(this.currentBlacklistLevel).contains(rolledStack.getItem())) {
               rolledStack = BuiltInRegistries.ITEM.getRandom(random).orElseThrow().value().getDefaultInstance();
            }
            if (rolledStack.is(ItemTags.WOOL)) {
               rolledStack.setCount(64);
            }
            if (rolledStack.is(Items.ENCHANTED_BOOK)) {
               Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.getRandom(random).get().value();
               EnchantmentHelper.setEnchantments(Map.of(enchantment, enchantment.getMaxLevel() == 1 ? 1 : random.nextInt(enchantment.getMinLevel(), enchantment.getMaxLevel() + 1)), rolledStack);
            }
            if (rolledStack.is(Items.POTION)) {
               MobEffect effect = BuiltInRegistries.MOB_EFFECT.getRandom(random).orElseThrow().value();
               while (VarietyHunters.RANDOM_MAN_DROPS.potionBlacklist().contains(effect)) {
                  effect = BuiltInRegistries.MOB_EFFECT.getRandom(random).orElseThrow().value();
               }
               PotionUtils.setCustomEffects(rolledStack, List.of(new MobEffectInstance(effect, effect.equals(MobEffects.HEAL) ? 1 : random.nextInt(30, 91) * 20, effect.equals(MobEffects.HEAL) ? 1 : 0)));
            }
            rolledStack.getBukkitStack().editMeta(itemMeta -> itemMeta.getPersistentDataContainer().set(ManhuntEvents.CAN_PICKUP, PersistentDataType.BYTE, (byte)0));
            serverPlayer.getInventory().placeItemBackInInventory(rolledStack);
         });
      }, 0, 100);
      this.t2 = Bukkit.getScheduler().runTaskLater(VarietyHunters.PLUGIN, () -> {
         this.currentBlacklistLevel--;
      }, 2400);
      this.t3 = Bukkit.getScheduler().runTaskLater(VarietyHunters.PLUGIN, () -> {
         this.currentBlacklistLevel--;
      }, 54000);
      this.t4 = Bukkit.getScheduler().runTaskLater(VarietyHunters.PLUGIN, () -> {
         this.currentBlacklistLevel--;
      }, 108000);
      sendGameMessage(Component.text("Game has been started! Don't die..."));
      getPlayers().forEach((classType, players) -> {
         if (classType != ClassType.RUNNER)
            players.forEach(player -> player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.COMPASS)));
      });
   }

   public void endGame() {
      if (isStarted()) {
         this.itemTask.cancel();
         this.t2.cancel();
         this.t3.cancel();
         this.t4.cancel();
      }
      for (Player player : Bukkit.getServer().getOnlinePlayers()) {
         player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
         player.playerListName(player.displayName());
      }
      sendGameMessage(Component.text("Game has ended."));
   }

   public boolean isStarted() {
      return this.started;
   }

   public List<Player> getHunters() {
      return this.players.computeIfAbsent(ClassType.HUNTER, classType -> new ArrayList<>());
   }

   public List<Player> getRunners() {
      return this.players.computeIfAbsent(ClassType.RUNNER, classType -> new ArrayList<>());
   }

   public Map<Player, Assassin> getAssassins() {
      return this.assassins;
   }

   public List<Player> getRandomMen() {
      return this.players.computeIfAbsent(ClassType.RANDOM_MAN, classType -> new ArrayList<>());
   }

   public HashMap<ClassType, List<Player>> getPlayers() {
      return this.players;
   }

   public HashMap<ClassType, List<UUID>> getOfflinePlayers() {
      return this.offlinePlayers;
   }

   public void sendGameMessage(Component message) {
      Bukkit.getServer().sendMessage(PREFIX.color(TextColor.color(NamedTextColor.GRAY)).append(message));
   }

   public void tick(int tickCount) {
      timer.tick();
      for (Player player : Bukkit.getServer().getOnlinePlayers()) {
         player.sendPlayerListFooter(
                 Component.text(
                         "Game Time: ", TextColor.color(NamedTextColor.GRAY)).append(Component.text(timer.getHour() + ":" + timer.getTime(timer.getMinute()) + ":" + timer.getTime(timer.getSeconds()))
                 )
         );
      }
      getAssassins().forEach((player, assassin) -> {
         assassin.tick();
         player.sendActionBar(Component.text("Status: ", TextColor.color(NamedTextColor.GRAY)).append(Component.text(assassin.isLinked() ? "LINKED" : "UNLINKED", TextColor.color(assassin.isLinked() ? NamedTextColor.GREEN : NamedTextColor.RED))));
         if (assassin.isFrozen() && assassin.getObserver() != null) {
            ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
            Vec3 assassinEye = serverPlayer.getEyePosition();
            for (float i = 0; i < 360; i++) {
               double angle = Math.toRadians(i);
               player.getWorld().spawnParticle(Particle.REDSTONE, assassinEye.x() + (.5 * Math.cos(angle)), serverPlayer.getY() + 1, assassinEye.z() + (.5 * Math.sin(angle)), 0, new Particle.DustOptions(Color.RED, 0.3f));
            }
         }
      });
      getRandomMen().forEach(player -> {
         player.sendActionBar(Component.text("Current Tier: ", TextColor.color(NamedTextColor.GRAY)).append(Component.text(3 - currentBlacklistLevel + 1, TextColor.color(NamedTextColor.GREEN))));
         if (player.getHealth() < player.getMaxHealth() && tickCount % 20 == 0) {
            // CraftBukkit - added regain reason of "REGEN" for filtering purposes.
            ((CraftPlayer)player).getHandle().heal(0.01F, org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.REGEN);
         }
      });
      getAssassins().forEach((player, assassin) -> {
         getRunners().forEach(runner -> {
            LivingEntity target = getTarget(runner);
            boolean frozen = player.equals(target);
            assassin.setFrozen(runner, frozen);
         });
      });
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
