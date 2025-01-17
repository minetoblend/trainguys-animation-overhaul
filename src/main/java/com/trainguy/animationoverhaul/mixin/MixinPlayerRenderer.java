package com.trainguy.animationoverhaul.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.trainguy.animationoverhaul.access.LivingEntityAccess;
import com.trainguy.animationoverhaul.util.AnimCurveUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class MixinPlayerRenderer {
    @Inject(method = "scale", at = @At("HEAD"), cancellable = true)
    private void modifyPlayerRendererTransforms(AbstractClientPlayer abstractClientPlayer, PoseStack poseStack, float f, CallbackInfo ci){
        // Variables
        float bodyRot = Mth.lerp(f, abstractClientPlayer.yBodyRotO, abstractClientPlayer.yBodyRot);
        float headRot = Mth.lerp(f, abstractClientPlayer.yHeadRotO, abstractClientPlayer.yHeadRot);
        float differenceRot = headRot - bodyRot;

        // Bow player rotation
        boolean isLeftHanded = abstractClientPlayer.getMainArm() == HumanoidArm.LEFT;
        boolean holdingBowInRightHand = !isLeftHanded ? abstractClientPlayer.getMainHandItem().getUseAnimation() == UseAnim.BOW : abstractClientPlayer.getOffhandItem().getUseAnimation() == UseAnim.BOW;
        boolean holdingBowInLeftHand = isLeftHanded ? abstractClientPlayer.getMainHandItem().getUseAnimation() == UseAnim.BOW : abstractClientPlayer.getOffhandItem().getUseAnimation() == UseAnim.BOW;
        if(holdingBowInRightHand && holdingBowInLeftHand){
            holdingBowInRightHand = !isLeftHanded;
        }
        boolean usingBow = abstractClientPlayer.getUseItem().getItem() == Items.BOW;

        float bowAmount = ((LivingEntityAccess)abstractClientPlayer).getAnimationVariable("bowPoseAmount");
        float bowPoseAmount = AnimCurveUtils.linearToEaseInOutQuadratic(Mth.clamp(usingBow ? bowAmount * 1.5F - 0.5F : bowAmount * 1.5F, 0, 1));
        poseStack.mulPose(Vector3f.YP.rotationDegrees((differenceRot + (holdingBowInRightHand ? -70 : 70)) * bowPoseAmount));

        // Riding in minecart stuff
        boolean isRidingInMinecart = abstractClientPlayer.isPassenger() && abstractClientPlayer.getRootVehicle().getType() == EntityType.MINECART;
        if(isRidingInMinecart){
            poseStack.translate(0, -0.5, 0);
        }

        // Swimming/Crawling rotation
        float swimAmount = abstractClientPlayer.getSwimAmount(f);
        if(swimAmount > 0){
            float staticBodyRotationX = abstractClientPlayer.isInWater() ? -90.0F - abstractClientPlayer.getXRot() : -90.0F;
            float oldBodyRotationX = Mth.lerp(swimAmount, 0.0F, staticBodyRotationX);
            if (abstractClientPlayer.isVisuallySwimming()) {
                poseStack.translate(0.0D, -1, 0);
            }
            poseStack.mulPose(Vector3f.XP.rotationDegrees(oldBodyRotationX));
            float smoothSwimAmount = AnimCurveUtils.linearToEaseInOutQuadratic(swimAmount);
            float bodyRotationX = Mth.lerp(smoothSwimAmount, 0.0F, staticBodyRotationX);
            poseStack.mulPose(Vector3f.XP.rotationDegrees(-bodyRotationX));
            poseStack.translate(0.0D, 1 * smoothSwimAmount, 0F * smoothSwimAmount);
        }
    }
}
