package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public final class CustomFluidCapabilities {
    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(HotBath.MOD_ID, "custom_fluid_container");

    private CustomFluidCapabilities() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack.is(Items.BUCKET)) {
            event.addCapability(CAPABILITY_ID, createProvider(stack, ContainerKind.BUCKET));
        } else if (stack.is(Items.GLASS_BOTTLE)) {
            event.addCapability(CAPABILITY_ID, createProvider(stack, ContainerKind.BOTTLE));
        }
    }

    public static ICapabilityProvider createProvider(ItemStack stack, boolean bucket) {
        return createProvider(stack, bucket ? ContainerKind.BUCKET : ContainerKind.BOTTLE);
    }

    private static ICapabilityProvider createProvider(ItemStack stack, ContainerKind kind) {
        return new CustomFluidContainerProvider(new CustomFluidContainerHandler(stack, kind));
    }

    private enum ContainerKind {
        BUCKET(CustomFluidStackHelper.BUCKET_AMOUNT),
        BOTTLE(CustomFluidStackHelper.BOTTLE_AMOUNT);

        final int amount;

        ContainerKind(int amount) {
            this.amount = amount;
        }
    }

    private static final class CustomFluidContainerProvider implements ICapabilityProvider {
        private final LazyOptional<IFluidHandlerItem> handler;

        private CustomFluidContainerProvider(IFluidHandlerItem handler) {
            this.handler = LazyOptional.of(() -> handler);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
                return handler.cast();
            }
            return LazyOptional.empty();
        }
    }

    private static final class CustomFluidContainerHandler implements IFluidHandlerItem {
        private ItemStack container;
        private final ContainerKind kind;

        private CustomFluidContainerHandler(ItemStack container, ContainerKind kind) {
            this.container = container;
            this.kind = kind;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? getContainedFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? kind.amount : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && CustomFluidStackHelper.getFluidId(stack) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(resource);
            if (fluidId == null || resource.getAmount() < kind.amount || !isEmptyContainer()) {
                return 0;
            }

            if (action.execute()) {
                container = switch (kind) {
                    case BUCKET -> CustomFluidAPI.createBucket(fluidId);
                    case BOTTLE -> CustomFluidAPI.createBottle(fluidId);
                };
            }
            return kind.amount;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack contained = getContainedFluid();
            if (contained.isEmpty()
                    || resource.getAmount() < kind.amount
                    || !CustomFluidStackHelper.hasSameFluidId(contained, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(kind.amount, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contained = getContainedFluid();
            if (contained.isEmpty() || maxDrain < kind.amount) {
                return FluidStack.EMPTY;
            }

            if (action.execute()) {
                container = switch (kind) {
                    case BUCKET -> new ItemStack(Items.BUCKET);
                    case BOTTLE -> new ItemStack(Items.GLASS_BOTTLE);
                };
            }
            return contained;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        private FluidStack getContainedFluid() {
            ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(container);
            if (fluidId == null || !isFilledContainer()) {
                return FluidStack.EMPTY;
            }
            return CustomFluidStackHelper.createStack(fluidId, kind.amount);
        }

        private boolean isEmptyContainer() {
            return switch (kind) {
                case BUCKET -> container.is(Items.BUCKET);
                case BOTTLE -> container.is(Items.GLASS_BOTTLE);
            };
        }

        private boolean isFilledContainer() {
            return switch (kind) {
                case BUCKET -> container.is(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
                case BOTTLE -> container.is(CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());
            };
        }
    }
}
