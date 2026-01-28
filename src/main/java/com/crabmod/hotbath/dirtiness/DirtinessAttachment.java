package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registers the dirtiness data attachment for players.
 * Uses NeoForge's Attachment system for storing per-player data.
 */
public class DirtinessAttachment {
    
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HotBath.MOD_ID);
    
    public static final Supplier<AttachmentType<DirtinessData>> DIRTINESS = 
            ATTACHMENT_TYPES.register("dirtiness", () -> AttachmentType.serializable(DirtinessData::new).build());
    
    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
