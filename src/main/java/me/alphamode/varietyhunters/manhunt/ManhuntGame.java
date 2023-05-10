package me.alphamode.varietyhunters.manhunt;

import me.alphamode.varietyhunters.VarietyHunters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ManhuntGame {
   private final HashMap<ClassType, List<Player>> players;
   private final HashMap<Player, Assassin> assassins;
   private final RandomSource random = RandomSource.create();
   private BukkitTask itemTask;
   private boolean started = false;

   public ManhuntGame() {
      this.players = new HashMap<>();
      this.assassins = new HashMap<>();
   }

   public void add(ClassType type, Player... players) {
      for (Player player : players) {
         this.players.computeIfAbsent(type, classType -> new ArrayList<>());
         this.players.get(type).add(player);
         if (type == ClassType.ASSASSIN)
            this.assassins.put(player, new Assassin(player));
         player.getServer().sendMessage(player.displayName().append(Component.text(" is now in the ").append(type.getDisplayName()).append(Component.text(" class."))));
      }
   }

   public void startGame() {
      this.players.get(ClassType.RANDOM_MAN).forEach(player -> {
         player.setFoodLevel(20);
         player.setUnsaturatedRegenRate(2);
         player.setSaturatedRegenRate(2);
      });
      this.started = true;
      this.itemTask = Bukkit.getScheduler().runTaskTimer(VarietyHunters.PLUGIN, () -> {
         getRandomMen().forEach(player -> {
            ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
            serverPlayer.getInventory().add(BuiltInRegistries.ITEM.getRandom(random).orElseThrow().value().getDefaultInstance());
         });
      }, 0, 200);
   }

   public void endGame() {
      if (isStarted())
         this.itemTask.cancel();
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

   public void tick(int tickCount) {
      getAssassins().forEach((player, assassin) -> {
         player.sendActionBar(Component.text("Status: ", TextColor.color(NamedTextColor.GRAY)).append(Component.text(assassin.isLinked() ? "LINKED" : "UNLINKED", TextColor.color(assassin.isLinked() ? NamedTextColor.GREEN : NamedTextColor.RED))));
         if (assassin.isFrozen()) {
            ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
            ServerPlayer linkedPlayer = ((CraftPlayer)assassin.getObserver()).getHandle();
            Vec3 assassinEye = serverPlayer.getEyePosition();
            Vec3 observerEye = linkedPlayer.getEyePosition();
            double distance = assassinEye.distanceTo(observerEye);
            Vec3 vector = new Vec3(observerEye.x(), observerEye.y(), observerEye.z()).subtract(assassinEye).normalize().multiply(3, 3, 3);
            for (float i = 0; i < distance; i += 3) {
               player.getWorld().spawnParticle(Particle.REDSTONE, assassinEye.x(), assassinEye.y(), assassinEye.z(), 0, new Particle.DustOptions(Color.RED, 1));
               assassinEye = assassinEye.add(vector);
            }
         }
      });
      getRandomMen().forEach(player -> {
         if (player.getHealth() < player.getMaxHealth() && tickCount % 20 == 0) {
            // CraftBukkit - added regain reason of "REGEN" for filtering purposes.
            ((CraftPlayer)player).getHandle().heal(1.0F, org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.REGEN);
         }
      });
   }
}
