package com.iridium126.hextricks.mixin;

import at.petrak.hexcasting.client.render.be.BlockEntitySlateRenderer;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.blocks.circles.BlockSlate;
import com.iridium126.hextricks.compat.SlateKnotHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockEntitySlateRenderer.class, remap = false)
public abstract class BlockEntitySlateRendererMixin {
    @Inject(method = "render(Lat/petrak/hexcasting/common/blocks/circles/BlockEntitySlate;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("RETURN"))
    private void hextricks$renderKnot(BlockEntitySlate slate, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay, CallbackInfo ci) {
        if (!(slate instanceof SlateKnotHolder holder)) {
            return;
        }
        ItemStack knot = holder.hextricks$getKnot();
        if (knot.isEmpty()) {
            return;
        }

        BlockState state = slate.getBlockState();
        Direction normal = getNormal(state);
        poseStack.pushPose();
        translateToSlateFace(poseStack, normal);
        rotateToSlateFace(poseStack, normal);
        poseStack.scale(0.4f, 0.4f, 0.4f);
        poseStack.mulPose(Axis.YP.rotation(animationTime(slate, partialTick) * 0.1f));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                knot,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                slate.getLevel(),
                0
        );
        poseStack.popPose();
    }

    private static float animationTime(BlockEntitySlate slate, float partialTick) {
        if (slate.getLevel() == null) {
            return partialTick;
        }
        return slate.getLevel().getGameTime() + partialTick;
    }

    private static Direction getNormal(BlockState state) {
        AttachFace attachFace = state.getValue(BlockSlate.ATTACH_FACE);
        if (attachFace == AttachFace.FLOOR) {
            return Direction.UP;
        }
        if (attachFace == AttachFace.CEILING) {
            return Direction.DOWN;
        }
        return state.getValue(BlockSlate.FACING);
    }

    private static void translateToSlateFace(PoseStack poseStack, Direction normal) {
        double x = 0.5;
        double y = 0.5;
        double z = 0.5;
        switch (normal) {
            case DOWN -> y = 0.92;
            case UP -> y = 0.08;
            case NORTH -> z = 0.08;
            case SOUTH -> z = 0.92;
            case WEST -> x = 0.08;
            case EAST -> x = 0.92;
        }
        poseStack.translate(x, y, z);
    }

    private static void rotateToSlateFace(PoseStack poseStack, Direction normal) {
        switch (normal) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
            case UP -> {
            }
        }
    }
}
