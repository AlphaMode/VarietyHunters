package me.alphamode.varietyhunters.manhunt;

import org.bukkit.entity.Player;

public class Assassin {
   private final Player player;
   private Player observer;
   private boolean frozen;
   private int ticksUntilUnfrozen = 0;

   public Assassin(Player player) {
      this.player = player;
   }

   public void tick() {
      if (!frozen && ticksUntilUnfrozen != 0)
         ticksUntilUnfrozen--;
   }

   public boolean isLinked() {
      if (ManhuntEvents.isGameInProgress()) {
         for (Player runner : ManhuntEvents.getCurrentGame().getHunters()) {
            if (runner.getLocation().distance(this.player.getLocation()) < 100.0)
               return true;
         }
         for (Player runner : ManhuntEvents.getCurrentGame().getRandomMen()) {
            if (runner.getLocation().distance(this.player.getLocation()) < 100.0)
               return true;
         }
      }
      return false;
   }

   public boolean isFrozen() {
      return frozen || ticksUntilUnfrozen != 0;
   }

   public void setFrozen(Player observer, boolean frozen) {
      if (!frozen && this.frozen) this.ticksUntilUnfrozen = 10;
      this.frozen = frozen;
      if (frozen)
         this.observer = observer;
      else
         this.observer = null;

   }

   public Player getObserver() {
      return observer;
   }
}
