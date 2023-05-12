package me.alphamode.varietyhunters.manhunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public record RandomManConfig(List<MobEffect> potionBlacklist, List<Item> blacklist, List<Item> t2Blacklist, List<Item> t3Blacklist, List<Item> t4Blacklist) {
    public static final Codec<RandomManConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.MOB_EFFECT.byNameCodec().listOf().fieldOf("potion_blacklist").forGetter(RandomManConfig::potionBlacklist),
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("blacklist").forGetter(RandomManConfig::blacklist),
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("tier_2").forGetter(RandomManConfig::t2Blacklist),
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("tier_3").forGetter(RandomManConfig::t3Blacklist),
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("tier_4").forGetter(RandomManConfig::t4Blacklist)
            ).apply(inst, RandomManConfig::new));

    public List<Item> getCurrentBlacklist(int level) {
        List<Item> blacklist = new ArrayList<>(blacklist());
        if (level >= 3)
            blacklist.addAll(t2Blacklist());
        if (level >= 2)
            blacklist.addAll(t3Blacklist());
        if (level >= 1)
            blacklist.addAll(t4Blacklist());
        return blacklist;
    }
}
