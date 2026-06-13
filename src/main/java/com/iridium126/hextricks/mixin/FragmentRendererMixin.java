package com.iridium126.hextricks.mixin;

import com.iridium126.hextricks.util.FragmentTextAccessor;
import com.iridium126.hextricks.util.ListPatternIotaValidator;
import dev.enjarai.trickster.render.fragment.FragmentRenderer;
import dev.enjarai.trickster.spell.Fragment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = FragmentRenderer.class, remap = false)
public interface FragmentRendererMixin {
    @ModifyVariable(method = "renderAsText", at = @At("HEAD"), argsOnly = true, index = 5)
    private static float hextricks$doubleRadiusForListIotaDisplay(float radius, Fragment fragment) {
        if (ListPatternIotaValidator.isListIotaDisplay(FragmentTextAccessor.getFormattedTextString(fragment))) {
            return radius * 2f;
        }
        return radius;
    }
}
