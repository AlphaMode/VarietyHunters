package me.alphamode.varietyhunters.manhunt;

import org.bukkit.entity.Player;

public class Assassin {
   private final Player player;
   private Player observer;
   private boolean frozen;

   public Assassin(Player player) {
      this.player = player;
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
      return frozen;
   }

   public void setFrozen(Player observer, boolean frozen) {
      this.frozen = frozen;
      if (frozen)
         this.observer = observer;
      else
         this.observer = null;
   }

   public Player getPlayer() {
      return player;
   }

   public Player getObserver() {
      return observer;
   }
}
