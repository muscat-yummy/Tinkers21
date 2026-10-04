package slimeknights.tconstruct.mixin;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Retains native brush particles for modifier-driven brushing. */
@Mixin(BrushItem.class)
public interface BrushItemInvoker {
  @Invoker("spawnDustParticles")
  void tconstruct$spawnDustParticles(Level level, BlockHitResult hit, BlockState state, Vec3 view, HumanoidArm arm);
}
