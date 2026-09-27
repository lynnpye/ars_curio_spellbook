package com.arscuriospellbook.mixin;

import com.arscuriospellbook.client.CurioSpellbookEditor;
import com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client side of editing the worn spellbook. {@link SpellSlottedScreen} is the base class of Ars
 * Nouveau's spell editor ({@code GuiSpellBook}) and its particle editor ({@code ParticleOverviewScreen}).
 * Its constructor reads the book with {@code player.getItemInHand(hand)}.
 *
 * <p>When {@link CurioSpellbookEditor} says the screen being built belongs to a worn-book edit (the
 * edit key was just pressed, or the screen is being opened from another worn-book editor screen),
 * the worn book is substituted, and the finished screen is registered as curio-bound. Every other
 * editor is untouched.
 */
@Mixin(SpellSlottedScreen.class)
public abstract class SpellSlottedScreenMixin {

    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/world/InteractionHand;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ars_curio_spellbook$useWornBook(ItemStack original) {
        return CurioSpellbookEditor.beginScreenConstruction(original);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/InteractionHand;)V", at = @At("RETURN"))
    private void ars_curio_spellbook$registerScreen(InteractionHand hand, CallbackInfo ci) {
        CurioSpellbookEditor.endScreenConstruction((SpellSlottedScreen) (Object) this);
    }
}
