package com.arscuriospellbook;

import com.arscuriospellbook.network.ModNetworking;
import com.arscuriospellbook.server.SpellbookSelection;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import top.theillusivec4.curios.api.CuriosApi;

@Mod(ArsCurioSpellbook.MODID)
public final class ArsCurioSpellbook {
    public static final String MODID = "ars_curio_spellbook";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Curios slot identifier. Defined in data/ars_curio_spellbook/curios/slots/ars_spellbook.json. */
    public static final String SLOT_ID = "ars_spellbook";

    /**
     * Slot validator referenced from the slot JSON. Accepts any {@link SpellBook} (including addon
     * subclasses) in addition to whatever is in the {@code curios:ars_spellbook} item tag.
     */
    public static final ResourceLocation SPELLBOOK_PREDICATE = id("is_spellbook");

    public ArsCurioSpellbook(IEventBus modBus) {
        modBus.addListener(ModNetworking::register);
        SpellbookSelection.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CuriosApi.registerCurioPredicate(
                SPELLBOOK_PREDICATE,
                slotResult -> slotResult.stack().getItem() instanceof SpellBook));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
