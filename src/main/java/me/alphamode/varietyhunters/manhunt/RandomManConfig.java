package me.alphamode.varietyhunters.manhunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record RandomManConfig(List<ItemStack> drops) {
    public static final Codec<RandomManConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(ItemStack.CODEC.listOf().fieldOf("drops").forGetter(randomManConfig -> randomManConfig.drops)).apply(inst, RandomManConfig::new));
}
