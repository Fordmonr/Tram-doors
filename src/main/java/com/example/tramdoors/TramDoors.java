package com.example.tramdoors;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TramDoors.MOD_ID)
public class TramDoors {
    public static final String MOD_ID = "tram_doors";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Block> TRAM_DOOR = BLOCKS.register("tram_door",
        () -> new TramDoorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .strength(3.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<Item> TRAM_DOOR_ITEM = ITEMS.register("tram_door",
        () -> new BlockItem(TRAM_DOOR.get(), new Item.Properties()));

    public TramDoors() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(TramDoors::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TRAM_DOOR_ITEM);
        }
    }
}
