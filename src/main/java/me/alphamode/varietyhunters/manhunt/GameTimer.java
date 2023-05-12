package me.alphamode.varietyhunters.manhunt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GameTimer {
    private int hour, minute, seconds, tick;

    public void tick() {
        tick++;
        if (this.tick == 20) {
            this.seconds++;
            tick = 0;
        }
        if (this.seconds == 60) {
            this.minute++;
            this.seconds = 0;
        }
        if (this.minute == 60) {
            this.hour++;
            this.minute = 0;
        }
    }

    public String getTime(int time) {
        if (time >= 10)
            return String.valueOf(time);
        return "0" + time;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSeconds() {
        return seconds;
    }
}
