package com.crabmod.hotbath.advancements;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class AdvancementTrigger implements CriterionTrigger<AdvancementTrigger.Instance> {
  private final ResourceLocation ID;
  private final List<Listener<Instance>> listeners = new ArrayList<>();

  @SuppressWarnings("removal")
  public AdvancementTrigger(String modName, String advancementName) {
    this.ID = new ResourceLocation(modName, advancementName);
  }

  @Override
  public @NotNull ResourceLocation getId() {
    return ID;
  }

  @Override
  public void addPlayerListener(
          @NotNull PlayerAdvancements playerAdvancements, @NotNull Listener<Instance> listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removePlayerListener(
          @NotNull PlayerAdvancements playerAdvancements, @NotNull Listener<Instance> listener) {
    this.listeners.remove(listener);
  }

  @Override
  public void removePlayerListeners(@NotNull PlayerAdvancements playerAdvancements) {
    this.listeners.clear();
  }

  @Override
  public @NotNull Instance createInstance(
          @NotNull JsonObject json, @NotNull DeserializationContext context) {
    return new Instance(ID);
  }

  public void trigger(ServerPlayer player) {
    List<Listener<Instance>> listenersCopy = new ArrayList<>(this.listeners);
    listenersCopy.forEach(listener -> listener.run(player.getAdvancements()));
  }

  public static class Instance implements CriterionTriggerInstance {
    private final ResourceLocation id;

    public Instance(ResourceLocation id) {
      this.id = id;
    }

    @Override
    public @NotNull ResourceLocation getCriterion() {
      return id;
    }

    // Implement the necessary methods for serialization if needed
    @Override
    public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context) {
      // Return a JSON object representing the conditions of this instance
      return new JsonObject();
    }
  }
}