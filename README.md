# Ars Curio Spellbook

NeoForge mod for Minecraft 1.21.1. It adds a Curios slot called `ars_spellbook` that holds an Ars Nouveau
spellbook, plus hotkeys that use the worn book without it being in your hand. Modpacks can give players more
than one `ars_spellbook` slot; the hotkeys then act on the selected one.

| Key | Default | What it does |
|---|---|---|
| Select Worn Spellbook Spell | `H` | Opens Ars Nouveau's radial spell menu for the worn book. |
| Cast Worn Spellbook Spell | `B` | Casts the worn book's selected spell. |
| Edit Worn Spellbook | `Shift+K` | Opens Ars Nouveau's spell editor on the worn book. |
| Select Next Worn Spellbook | `K` | With several `ars_spellbook` slots, selects the next slot (empty or not). |
| Select Previous Worn Spellbook | `Ctrl+K` | Selects the previous slot. |
| Next Spell in Worn Spellbook | *unbound* | Selects the selected book's next spell (through every spell slot, wrapping), like Ars' X for a held book. |
| Previous Spell in Worn Spellbook | *unbound* | Selects its previous spell, like Ars' Z. |

You can rebind all of them under **Options → Controls → Key Binds → Ars Curio Spellbook**. You can also bind them to
mouse buttons or add modifier keys.

## Requirements

- Minecraft 1.21.1 and NeoForge 21.1.x
- Ars Nouveau 5.11 or newer. The project compiles against `5.13.1.1415`.
- Curios 9.x. The project compiles against `9.3.1+1.21.1`.

## Building

```
./gradlew build        # jar is written to build/libs/
./gradlew runClient    # dev client with Ars Nouveau + Curios on the classpath
```

## How it works

**Slot.** These data files define the slot:
- `data/ars_curio_spellbook/curios/slots/ars_spellbook.json`
- `data/ars_curio_spellbook/curios/entities/ars_spellbook.json`

The slot accepts two things:
- Items in the `curios:ars_spellbook` tag. By default these are the four Ars Nouveau spellbooks, and modpacks
  can add more.
- Any item that extends Ars Nouveau's `SpellBook` class. This check is a Curios validator,
  `ars_curio_spellbook:is_spellbook`, so spellbooks added by other mods work without tag edits.

**Several spellbook slots.** Curios lets modpacks add more `ars_spellbook` slots. The spell menu, cast and edit
keys all act on the *selected* slot.

- **The selection is a slot number, empty or not**, like the hotbar. K and Ctrl+K step through every slot,
  including empty ones, wrapping around. There's no fallback: if the selected slot is empty, the keys do
  nothing and say `Spellbook slot N: empty`, rather than quietly using another book.
- **The server owns the selection.** K / Ctrl+K ask the server to step it (`CycleSelectedBookPayload`). The
  server stores it as a player data attachment, saved with the player and kept on death, and sends it to the
  client at login and whenever it changes (`SyncSelectedBookPayload`). It starts at slot 1.
- **Requests don't name a book.** Casting, picking a spell and the editor session all use the server's stored
  selection. The client checks its copy first so it can report an empty slot straight away. The server checks
  again, since the client's view of the slots can lag Curios' sync by a tick.
- **If the selected slot is removed**, the selection resets to slot 1 and the player is told
  (`Spellbook slot N was removed. Slot 1 selected.`). Curios removes the highest-numbered slots and hands any
  book in them back to the player's inventory. The server checks when Curios fires `SlotModifiersUpdatedEvent`
  (slot count changed during play), at login (a pack can reduce the slot count while you're offline), and after
  a datapack reload (Curios rebuilds inventories then without firing that event, so the check runs at low
  priority, after Curios). **A count of 0 never triggers a reset:** Curios also fires that event in the middle
  of rebuilding an inventory from saved data, when its slot map is briefly empty, and an earlier version wrongly
  reset the selection because of it.
- **Open screens close if the selection changes.** If our spell menu or a worn-book editor is open when the
  server changes the selection, the client closes it, since it belongs to the previous book. That also signals
  that something happened.

**Seeing the selected spell.** Ars Nouveau's HUD only shows the spell of the book in your selected hotbar slot,
so worn books aren't shown there, and this mod doesn't add a permanent display. Instead, every change shows one
line on the action bar (the brief text above the hotbar), in Ars' own `<slot> <name>` format for the spell:
`Spellbook slot 2: Archmage Spell Book — 3 Fireball`. It appears when you switch books with K / Ctrl+K, pick a
spell in the H menu, or use the next/previous spell keys, and whenever a key finds the selected slot empty. With
a single spellbook slot, K "switches" to the same slot, so it doubles as a key to check what B will cast.

**Next / previous spell.** Like Ars' own X/Z for a held book, these keys send only a direction
(`CycleSelectedSpellPayload`). The server steps the selected book's spell, saves it and sends back the status
line, and Curios syncs the changed book to the client. Stepping on the client's copy instead could lose presses:
the server's sync from one press can arrive after the next press and briefly roll the client's copy back. The
spell menu (H) still updates the client's copy immediately when you pick, since it makes one pick per opening
and has no such race; its status line comes from the server like the others.

**Radial menu (H).** The mod reuses Ars Nouveau's own `GuiRadialMenu`. Ars' version of that menu closes as soon
as Ars' own radial key (V) is released. `CurioSpellbookRadialScreen` turns that behavior off and applies the
same logic to *this* mod's key instead: releasing the key (hold mode) or pressing it again (toggle mode)
closes the menu. Whether the menu closes on release (hold) or stays open
until you press H again (toggle) follows Ars' own client config setting `toggleSelectionHUD`. That way the two
menus always behave the same. When you pick a slot, the client sends `SetCurioBookSlotPayload`, and the server
updates the book in the Curios slot and replies with the status line. The client updates its own copy of the
book immediately so it looks right straight away, but the message always comes from the server, so it describes
what was actually applied.

**Cast (B).** The client sends `CastCurioBookPayload` with the camera rotation. On the server,
`CurioBookCasting` does the same work as `AbstractCaster#castSpell`, with one difference. Ars' code always
reads the book from the player's hand, so this version passes the worn book in directly everywhere a stack
is needed:
- `SpellContext`
- entity casts
- the `UseOnContext` for block casts

Your hands are never touched. Ars' overridable hooks still run: `getSpell`, `modifySpellBeforeCasting` and
`getSpellResolver`. Each key press casts once. Holding the key doesn't repeat, which is the same rule Ars
Nouveau uses for its own quick-cast keys (QC1–QC10).

**Editing (Shift+K).** This opens Ars Nouveau's own spell editor. The editor assumes the book is in your hand,
so this feature uses two mixins:
- `SpellSlottedScreenMixin` (client) runs when an editor screen is built. That class is the base of the spell
  editor and the particle editor. For editors opened with this key, the mixin gives the screen the worn book
  instead of the held item. It does the same for screens opened from those editors, like the particle editor.
  Editors opened any other way are untouched.
- `SpellEditorSavePacketMixin` (server) runs when Ars' two save packets arrive (`PacketUpdateCaster` and
  `PacketUpdateParticleTimeline`). While a worn-book editor is on screen, the client keeps an edit session
  open on the server (`SetCurioEditSessionPayload`), and those packets save to the worn book instead of the
  hand. If the book is taken off in the meantime, the save does nothing, so worn-book edits never land on a
  held item.
- **The session names the slot the editor is showing.** The server only starts a normal session if that
  matches its own selection. If the client's copy is stale (for example, the server has just reset the
  selection because a slot was removed), the editor is showing a different book from the one the server would
  save to. The server then starts a *blocked* session that drops every save, rather than no session at all
  (which would let Ars save to the book in your hand), and re-sends the selection, which closes the editor.

The key works whatever you're holding, including another spellbook.

## Known interactions

- **Why B, H and K.** B keeps casting on the left hand, within reach of WASD. H and K put the menu, editor and
  book selection on the right side of the keyboard. None of them is used by vanilla Minecraft, Ars Nouveau (C, V, X, Z, G) or Iron's Spells 'n Spellbooks
  (R, V, Left Alt).
- **B is JourneyMap's and Xaero's Minimap's default "create waypoint" key.** With either map mod installed,
  casting will also drop a waypoint until one of the two is rebound.
- **Keys are read once per client tick**, as NeoForge recommends for key bindings. Minecraft also counts the
  operating system's auto-repeat as presses, so the cast and book-selection keys only fire when they weren't
  already held on the previous tick. That gives one cast, or one step through the books or spells, per press.
- **A single press never does two things.** If someone rebinds the two keys to a pair like `X` and
  `Shift+X`, NeoForge only triggers the binding whose modifier matches, so they stay separate. If both are bound
  to exactly the same key, only the first in this order acts: spell menu, editor, next book, previous book,
  next spell, previous spell, cast.
- If you rebind the cast key to Ars Nouveau's "Selection HUD" key (V by default) and you're holding a spellbook
  or other radial item, Ars' menu for the held item opens and the worn book does **not** cast.
- A held book won't cast when you right-click a block entity such as a chest, because the right-click goes to
  the block. The cast key has no block interaction of its own, so it always casts.
- **Ars' "Head Curio Menu" key (G by default) ignores the worn book.** That key is meant for the Alchemist's
  Crown. It toggles the radial menu of every worn item that has one, so a worn spellbook would get caught in it.
  With the crown also worn, the menu would open and immediately close. With the book alone, the book's menu
  would open, but a spell picked there would save to the book in your hand. `HeadCurioHotkeyMixin` (client)
  hides every book in an `ars_spellbook` slot (not just the selected one) from that one loop, so the key behaves exactly as in plain Ars
  whatever it's bound to. Use this mod's H key to pick the worn book's spell.
- **The mixins depend on Ars Nouveau internals.** They're marked required, so if a future Ars version
  changes the editor's constructor, the save packets or the head-curio key handler, the game will fail to start with a mixin error instead
  of silently saving to the wrong book. Casting and the spell menu use only Ars' public API.
- The slot icon (`textures/slot/ars_spellbook.png`) is a simple placeholder. Swap in your own 16×16 image.
- **Translations.** The lang folder covers the same 29 languages Ars Nouveau ships, plus `en_us`. Where Ars'
  own word for "spellbook" is a real one (Grimorio, スペルブック, 마도서, Книга заклять…), these files use it so the
  terms match. Where Ars' translation says "spelling book" instead (ar_sa, da_dk, el_gr, he_il, no_no, sv_se,
  vi_vn, and similar in af_za, hu_hu, nl_nl), these files use the proper word for spellbook. These are
  machine-drafted and should be checked by native speakers; corrections are welcome. The key category keeps the
  mod's name untranslated. Any missing key falls back to English.
