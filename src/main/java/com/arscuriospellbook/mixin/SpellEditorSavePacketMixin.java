package com.arscuriospellbook.mixin;

import com.arscuriospellbook.CurioSpellbookUtil;
import com.arscuriospellbook.server.CurioEditSessions;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateParticleTimeline;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Server side of editing the worn spellbook. These are the two packets Ars Nouveau's spell editor
 * sends to save changes: {@link PacketUpdateCaster} (spell recipe and name) and
 * {@link PacketUpdateParticleTimeline} (particle effects). Both look up the book with
 * {@code player.getItemInHand(hand)}.
 *
 * <p>While the player has a curio edit session open, that lookup returns the book in the session's
 * ars_spellbook slot instead. It returns EMPTY, which both packets already treat as "nothing to
 * save", if the book was removed from that slot in the meantime, or if the session is blocked
 * because the client's editor showed a different slot from the server's selection. Either way the
 * edit is dropped rather than written to whatever is in the hand.
 */
@Mixin({PacketUpdateCaster.class, PacketUpdateParticleTimeline.class})
public abstract class SpellEditorSavePacketMixin {

    @ModifyExpressionValue(
            method = "onServerReceived",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ars_curio_spellbook$useWornBook(ItemStack original, MinecraftServer server, ServerPlayer player) {
        Integer bookSlot = CurioEditSessions.getBookSlot(player);
        if (bookSlot == null) {
            return original;
        }
        return CurioSpellbookUtil.getEquippedSpellbook(player, bookSlot);
    }
}
