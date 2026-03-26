package com.punchy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ItemFetcher {
    public static List<ItemInfo> getAllItems() {
        List<ItemInfo> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            items.add(new ItemInfo(item, id));
        }
        items.sort(Comparator.comparing(i -> i.id.toString()));
        return items;
    }

    public record ItemInfo(Item item, ResourceLocation id) {}
}
