package me.alphamode.varietyhunters.manhunt;

import net.kyori.adventure.text.Component;

public enum ClassType {
   HUNTER(Component.text("Hunter"), "hunter"),
   RUNNER(Component.text("Runner"), "runner"),
   ASSASSIN(Component.text("Assassin"), "assassin"),
   RANDOM_MAN(Component.text("Randon Man"), "random_man");

   private final Component displayName;
   private final String command;

   ClassType(Component name, String command) {
      this.displayName = name;
      this.command = command;
   }

   public Component getDisplayName() {
      return displayName;
   }

   public String getCommand() {
      return command;
   }

   public static ClassType fromString(String name) {
      return ClassType.valueOf(ClassType.class, name.toUpperCase());
   }
}
