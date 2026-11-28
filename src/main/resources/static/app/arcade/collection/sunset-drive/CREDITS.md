# Credits and licences — TEB RUN

TEB RUN is an original arcade racer inspired by 80s arcade racers. The code,
the art of the full game and the music were made for this project. This page
lists the third-party material shipped in `public/` and the work this project
learned from.

## Code

Written with Claude Code (Anthropic), from a written spec and per-track
implementation plans. TypeScript, Vite, Canvas 2D, Web Audio API, Vitest,
Playwright. No game engine, zero runtime dependencies. MIT License, see
`LICENSE`.

The pseudo-3D road follows the classic technique described in Jake Gordon's
*How to build a racing game* tutorial (javascript-racer, MIT,
<https://github.com/jakesgordon/javascript-racer>). The code is written from
scratch, but some projection constants and easing functions match the
tutorial's. Credited here because it's the right thing to do.

The on-screen font is a hand-written 8×8 grid in `src/ui/text.ts`. No external
fonts, no CDN.

## Art

This repository ships **no illustrated art**: every sprite, background and
illustration is drawn by code in `src/art/procedural/`, which is what you see
when you run it. The full game at <https://tebrun.io> uses illustrated sprites
generated with an image model from written prompts, then checked and fixed by
hand; those images are reserved and not part of this repository.

`tools/process-sprites.py` and `tools/webp-sprites.py` are the pipeline that
turns raw images into game sprites (background removal, cropping, resizing to
`tools/sprite-targets.json`, WebP for the site and portal builds). If you put
your own PNGs in `public/sprites/` with a `manifest.json`, the game uses them
and falls back to the procedural drawing for anything missing.

## Music and sound effects

The three songs, *Mirrorball Mile* (disco), *Amber Lagoon* (chill-out) and
*Tunnel Nine* (techno), are original compositions written as data in
`src/audio/songs/` (reserved, see `LICENSE`) and played by a six-channel
sequencer with chorus and reverb (`src/audio/sequencer.ts`). Melodies, chords
and drums are synthesized at runtime. The other sound effects (drift, dirt,
impact, checkpoint, fanfare, blips) are synthesized in `src/audio/sfx.ts`.

## Engine (`public/audio/engine_low.ogg`, `engine_high.ogg`)

| | |
|---|---|
| Collection | GOLD TAPE Sound Effects Library, 13.1 Cars |
| Author | USC Cinema / Sunset Editorial Collection |
| Source | <https://archive.org/details/GOLD_TAPE_13_13.1_Cars> |
| Licence | CC0 1.0 Universal (<https://creativecommons.org/publicdomain/zero/1.0/>) |
| Downloaded | 2026-09-28 |

- `engine_low.ogg` ← *G13-16-Low Engine Idling*: 4.0–8.5 s, mono 44.1 kHz,
  15 ms declick fades, 80 ms loop crossfade, peak normalised to −3 dBFS.
- `engine_high.ogg` ← *G13-19-Car Steady 30 MPH*: 3.0–7.5 s, same processing.

CC0 needs no attribution; it's given anyway so anyone reusing this repository
can trace the source. The game plays them pitched up by seven semitones
(`src/audio/engine-sampled.ts`).

**Drums.** Synthesized in code (`src/audio/synth.ts`). An earlier version used
seven samples from an archive labelled "CC0"; the label had been added by the
uploader, not the rights holder, and the original licence forbids
redistribution, so they were removed. Don't reintroduce them.
