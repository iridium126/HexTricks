```json
{
  "title": "Hex Tricks",
  "icon": "minecraft:amethyst_shard",
  "category": "trickster:tricks",
  "ordinal": 99,
  "additional_search_terms": [
    "Read Offhand Iota",
    "Run List/Pattern Iota String",
    "Hex Casting",
    "Iota"
  ]
}
```

Some tricks are written with one hand in a circle and the other on a staff.
Hex Tricks allows fragments to cross the boundary between Trickster spells and Hex Casting iotas.

;;;;;

<|trick@trickster:templates|trick-id=trickster:read_iota_offhand|>

Reads the Hex Casting iota stored in the caster's other hand and returns it as a Trickster fragment.

If the iota is a list or a pattern, it is returned as text in the same display form that Hex Casting shows to the player. Entity and continuation values inside lists carry hidden metadata so they can be restored later.

;;;;;

This trick is most useful when the other hand holds an item that can store iotas, such as a focus or another Hex Casting data holder.

Numbers, booleans, vectors, entities, lists, and several special Hex iotas are converted into the closest Trickster fragment representation.

;;;;;

<|trick@trickster:templates|trick-id=trickster:run_list_pattern_iota_string|>

Runs a Hex Casting pattern or list of patterns from Trickster.

The first argument may be a string containing a Hex Casting list or pattern display, or a Trickster list fragment. The value is restored into a Hex iota and executed as though a staff had cast it.

;;;;;

Any extra arguments are converted into Hex iotas and placed on the Hex Casting stack before execution, in the same order they are passed to the trick. The final extra argument is therefore on top of the stack.

The trick returns the top iota left on the Hex stack, converted back into a Trickster fragment. If the Hex cast fails or returns no value, it returns void.

;;;;;

The bridge is literal: a string must look like Hex Casting's own display text for a list or pattern. A Trickster list fragment, on the other hand, is converted element by element into a Hex list before being executed.

This makes it possible to read an iota from Hex Casting, manipulate it as a Trickster value, and then hand the result back to Hex Casting.
