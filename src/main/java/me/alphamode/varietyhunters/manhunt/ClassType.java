package me.alphamode.varietyhunters.manhunt;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum ClassType {
   HUNTER("Hunter", TextColor.color(NamedTextColor.LIGHT_PURPLE), "hunter"),
   RUNNER("Runner", TextColor.color(NamedTextColor.GREEN), "runner"),
   ASSASSIN("Assassin", TextColor.color(NamedTextColor.RED), "assassin"),
   RANDOM_MAN("Random Man", TextColor.color(NamedTextColor.AQUA), "random_man");

   private final String displayName;
   private final TextColor color;
   private final String command;

   ClassType(String name, TextColor color, String command) {
      this.displayName = name;
      this.color = color;
      this.command = command;
   }

   public String getDisplayName() {
      return displayName;
   }

   public TextColor getColor() {
      return color;
   }

   public String getCommand() {
      return command;
   }
}
