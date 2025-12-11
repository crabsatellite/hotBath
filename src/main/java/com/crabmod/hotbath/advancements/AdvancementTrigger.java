package com.crabmod.hotbath.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("removal")
public class AdvancementTrigger extends SimpleCriterionTrigger<AdvancementTrigger.Instance> {
    private final ResourceLocation id;

    public AdvancementTrigger(String modName, String advancementName) {
        this.id = new ResourceLocation(modName, advancementName);
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(this.id, player);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(ResourceLocation id, ContextAwarePredicate player) {
            super(id, player);
        }
    }
}
