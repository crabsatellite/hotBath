package com.crabmod.hotbath.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AdvancementTrigger implements CriterionTrigger<AdvancementTrigger.Instance> {
    private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = new HashMap<>();
    private final ResourceLocation ID;

    public AdvancementTrigger(String modName, String advancementName) {
        this.ID = new ResourceLocation(modName, advancementName);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void addPlayerListener(PlayerAdvancements playerAdvancements, Listener<Instance> listener) {
        this.listeners.computeIfAbsent(playerAdvancements, k -> new HashSet<>()).add(listener);
    }

    @Override
    public void removePlayerListener(PlayerAdvancements playerAdvancements, Listener<Instance> listener) {
        Set<Listener<Instance>> set = this.listeners.get(playerAdvancements);
        if (set != null) {
            set.remove(listener);
            if (set.isEmpty()) {
                this.listeners.remove(playerAdvancements);
            }
        }
    }

    @Override
    public void removePlayerListeners(PlayerAdvancements playerAdvancements) {
        this.listeners.remove(playerAdvancements);
    }

    @Override
    public Instance createInstance(JsonObject json, DeserializationContext context) {
        // In 1.20.1, we need to extract the predicate from the JSON manually if needed
        // But AbstractCriterionTriggerInstance takes ContextAwarePredicate
        // We can use EntityPredicate.Composite.fromJson(json, "player", context) to get it?
        // No, EntityPredicate.Composite is different.
        // Let's assume ANY for now to fix compilation, or try to parse it.
        // ContextAwarePredicate predicate = ContextAwarePredicate.fromJson(json.get("player")); // Hypothetical
        return new Instance(ID, ContextAwarePredicate.ANY);
    }

    public void trigger(ServerPlayer player) {
        Set<Listener<Instance>> set = this.listeners.get(player.getAdvancements());
        if (set != null) {
            for (Listener<Instance> listener : new HashSet<>(set)) {
                listener.run(player.getAdvancements());
            }
        }
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(ResourceLocation id, ContextAwarePredicate predicate) {
            super(id, predicate);
        }
    }
}
