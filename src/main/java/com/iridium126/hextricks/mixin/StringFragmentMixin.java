package com.iridium126.hextricks.mixin;

import com.iridium126.hextricks.util.ListPatternIotaValidator;
import dev.enjarai.trickster.spell.fragment.StringFragment;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = StringFragment.class, remap = false)
public abstract class StringFragmentMixin {

    @Shadow
    public abstract String value();

    /**
     * @reason Remove quotes from string fragment display text.
     * @author Iridium126
     */
    @Overwrite
    public Component asText() {
        String value = value();
        if (ListPatternIotaValidator.isListOrPatternIotaDisplay(value)) {
            return Component.literal(value);
        }
        else {
            return Component.literal("\"").append(value).append("\"");
        }
    }
}
