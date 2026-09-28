# Ars Curio Spellbook

**Ars Curio Spellbook** adds a dedicated Curios slot for Ars Nouveau spellbooks. Equip a book there and use it entirely from hotkeys, so your hands stay free for a sword, a shield, tools, a wand or even a second spellbook.

### Features

- **A slot just for Ars spellbooks.** It has its own slot type, so it doesn't compete with other mods' spellbook or charm slots. Any Ars Nouveau spellbook fits, including spellbooks added by Ars addons, with no tag editing required.
- **Cast without holding the book.** Casting from the worn book behaves like casting from your hand: mana costs, spell rules and "invalid spell" feedback all work as usual. One cast per key press.
- **Pick spells with Ars' own radial menu**, opened for the worn book, or step through its spells with next/previous keys.
- **Edit the worn book** in Ars Nouveau's normal spell editor, whatever you're holding. Changes are saved to the worn book, never to the item in your hand.
- **Carry several books.** If your modpack (or a command) gives you more than one spellbook slot, switch between them with a hotkey. Your choice is remembered through relogs, server restarts and death.
- **Always know what you'll cast.** A brief action-bar message shows the selected slot, book and spell whenever you switch books or spells.
- **Plays nicely with Ars Nouveau's own keys.** The book in your hand still works exactly as before, and Ars' Head Curio Menu key isn't disrupted by the worn book.
- **Multiplayer-ready.** All changes are made and checked by the server.

### Hotkeys

All of these can be rebound under **Options → Controls → Key Binds → Ars Curio Spellbook**, including to mouse buttons or with Shift/Ctrl/Alt modifiers:

- **Cast Worn Spellbook Spell**
- **Select Worn Spellbook Spell** (radial menu)
- **Edit Worn Spellbook**
- **Select Next Worn Spellbook** / **Select Previous Worn Spellbook**
- **Next Spell in Worn Spellbook** / **Previous Spell in Worn Spellbook**

### How to use

1. Put an Ars Nouveau spellbook in the **Ars Nouveau Spellbook** (Curios slot id `ars_spellbook`) slot of the Curios inventory.
2. Use the select-spell key to choose a spell, and the cast key to cast it.
3. With more than one spellbook slot, use the next/previous spellbook keys to choose which book the other keys act on.

### Requirements

- Minecraft 1.21.1 with NeoForge
- Ars Nouveau 5.11 or newer
- Curios API

### For modpack makers

- Players get one spellbook slot by default. Add more with standard Curios slot data or `/curios add ars_spellbook <player>`.
- Items in the `curios:ars_spellbook` item tag are also accepted in the slot.

## More Details

NeoForge mod for Minecraft 1.21.1. It adds a Curios slot called `ars_spellbook` that holds an Ars Nouveau
spellbook, plus hotkeys that use the worn book without it being in your hand. Modpacks can give players more
than one `ars_spellbook` slot; the hotkeys then act on the selected one.

| Key | Default   | What it does |
|---|-----------|---|
| Select Worn Spellbook Spell | `H`       | Opens Ars Nouveau's radial spell menu for the worn book. |
| Cast Worn Spellbook Spell | `B`       | Casts the worn book's selected spell. |
| Edit Worn Spellbook | `Shift+K` | Opens Ars Nouveau's spell editor on the worn book. |
| Select Next Worn Spellbook | `K`       | With several `ars_spellbook` slots, selects the next slot (empty or not). |
| Select Previous Worn Spellbook | `Ctrl+K`  | Selects the previous slot. |
| Next Spell in Worn Spellbook | `Ctrl+L`  | Selects the selected book's next spell (through every spell slot, wrapping), like Ars' X for a held book. |
| Previous Spell in Worn Spellbook | `Shift+L` | Selects its previous spell, like Ars' Z. |

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

- **Ars' "Head Curio Menu" key (G by default) ignores the worn book.** That key is meant for the Alchemist's
  Crown. It toggles the radial menu of every worn item that has one, so a worn spellbook would get caught in it.
  With the crown also worn, the menu would open and immediately close. With the book alone, the book's menu
  would open, but a spell picked there would save to the book in your hand. `HeadCurioHotkeyMixin` (client)
  hides every book in an `ars_spellbook` slot (not just the selected one) from that one loop, so the key behaves exactly as in plain Ars
  whatever it's bound to. Use this mod's H key to pick the worn book's spell.
- **The mixins depend on Ars Nouveau internals.** They're marked required, so if a future Ars version
  changes the editor's constructor, the save packets or the head-curio key handler, the game will fail to start with a mixin error instead
  of silently saving to the wrong book. Casting and the spell menu use only Ars' public API.
