package slimeknights.tconstruct.shared.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fluid particle options preserving fluid components in data and network codecs. */
public final class FluidParticleData implements ParticleOptions {
  private final ParticleType<FluidParticleData> type;
  private final FluidStack fluid;

  public FluidParticleData(ParticleType<FluidParticleData> type, FluidStack fluid) {
    this.type = type;
    this.fluid = fluid;
  }

  @Override
  public ParticleType<FluidParticleData> getType() {
    return type;
  }

  public FluidStack getFluid() {
    return fluid;
  }

  /** Formats the registry-aware SNBT accepted by the 1.21 particle command. */
  public String writeToString(HolderLookup.Provider registries) {
    var tag = type.codec().codec().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    return BuiltInRegistries.PARTICLE_TYPE.getKey(type) + tag.toString();
  }

  public static final class Type extends ParticleType<FluidParticleData> {
    private final MapCodec<FluidParticleData> codec =
      FluidStack.CODEC.fieldOf("fluid").xmap(fluid -> new FluidParticleData(this, fluid), FluidParticleData::getFluid);
    private final StreamCodec<RegistryFriendlyByteBuf,FluidParticleData> streamCodec =
      FluidStack.STREAM_CODEC.map(fluid -> new FluidParticleData(this, fluid), FluidParticleData::getFluid);

    public Type() {
      super(false);
    }

    @Override
    public MapCodec<FluidParticleData> codec() {
      return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf,FluidParticleData> streamCodec() {
      return streamCodec;
    }
  }
}
