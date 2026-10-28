# Small Game Records NL — Photo-ID Dataset Plan (Phase 3 groundwork)

Status: GROUNDWORK ONLY (2026-10-27). No photos downloaded at scale, no training run
started, no app changes. Counts measured live from the iNaturalist and GBIF APIs on
2026-10-27. Awaiting main's go before any download or Kaggle run.

## 1. The species list — exactly as the app ships it

From `speciesNames` in the app code (`android/app/src/main/assets/index.html`, v2.0.0):

| App name | Scientific name | iNaturalist taxon_id |
|---|---|---|
| Snowshoe hare | *Lepus americanus* | 43132 |
| Ruffed grouse | *Bonasa umbellus* | 890 |
| Spruce grouse | *Canachites canadensis* (syn. *Falcipennis canadensis*) | 1289698 |
| Willow ptarmigan | *Lagopus lagopus* | 931 |
| Rock ptarmigan | *Lagopus muta* | 949 |
| Porcupine | *Erethizon dorsatum* | 44026 |
| Coyote | *Canis latrans* | 42051 |
| Fox | *Vulpes vulpes* (red fox — the NL fox) | 42069 |
| Wolf | *Canis lupus* | 42048 |
| Other | — catch-all, NOT a learnable class — | — |

9 trainable classes. "Other" stays a manual pick in the app; the model also needs a
low-confidence "not sure" outcome rather than forcing one of the 9.

Method warning, learned during counting: the iNaturalist API's `taxon_name` text
parameter fuzzy-matches (e.g. "Lagopus lagopus" also matched *Buteo lagopus* and two
plants — a first pass overstated willow ptarmigan ~100×, and wolf ~3× via domestic-dog
subspecies). All counts below were re-done with exact `taxon_id`s and are trustworthy.

## 2. Inventory — iNaturalist (primary source)

Research-grade observations with photos, counted 2026-10-27. "Reusable" = per-photo
licence CC0 / CC-BY / CC-BY-NC. "Strict" = CC0 / CC-BY only. Counts are OBSERVATIONS;
usable photos are ≥ this (many observations carry 2–4 photos).

| Species | Research-grade w/ photos | Reusable (CC0/BY/BY-NC) | Strict (CC0/BY) |
|---|---|---|---|
| Snowshoe hare | 20,384 | 15,299 | 1,897 |
| Ruffed grouse | 25,589 | 18,811 | 2,322 |
| Spruce grouse | 9,521 | 6,784 | 744 |
| Willow ptarmigan | 4,880 | 3,498 | 422 |
| Rock ptarmigan | 4,891 | 3,587 | 433 |
| Porcupine | 31,425 | 23,617 | 2,482 |
| Coyote | 124,797 | 92,606 | 9,380 |
| Fox (red) | 134,942 | 100,622 | 12,571 |
| Wolf | 12,304 | 7,833 | 669 |

Headline: **quantity is not the problem anywhere.** Even the thinnest classes
(ptarmigans, ~3,500 reusable observations each) are 2–3× what a fine-tune needs.
~88% of the reusable pool is CC-BY-NC — the same posture as Kris's earlier dataset
work: fine for strictly personal, non-commercial use, but the NC slice must stay
partitionable (per-photo licence recorded in the manifest) so it can be dropped if
the model is ever shared or published. The strict CC0/BY pool alone still covers
every class (thinnest: ptarmigans ~420–430 observations each — workable with
transfer learning, tight but real).

Season / plumage spread (reusable pool, by observation month):
- Willow ptarmigan — winter (Dec–Feb, white): 386 · summer (Jun–Aug, brown): 1,872 · fall hunting season (Sep–Nov, transitional): 371
- Rock ptarmigan — winter: **107 (thin)** · summer: 2,233 · fall: 530
- Snowshoe hare — winter white (Dec–Mar): 3,407 · summer brown: 6,623 — both coats well covered

Both ptarmigan species are summer-heavy: winter-white shots exist but are the
minority (rock ptarmigan winter is the single thinnest slice in the whole plan).
Sampling must deliberately over-draw winter/fall birds for both ptarmigans and cap
summer birds, or the model will learn "brown ptarmigan" and miss the white bird a
winter hunter actually photographs.

## 3. GBIF (secondary / mirror source)

Occurrences with a still image, counted 2026-10-27 (species_key matched at 96–99%):
hare 16,651 · ruffed grouse 22,422 · spruce grouse 7,321 · willow ptarmigan 8,251 ·
rock ptarmigan 7,224 · porcupine 22,915 · coyote 96,844 · red fox 156,639 · wolf 36,716.

GBIF media records carry per-record licences (willow ptarmigan's 8,251 split exactly
into CC0 1,600 + CC-BY 2,007 + CC-BY-NC 4,644), so filtering is clean. But for these
species GBIF largely republishes the same iNaturalist research-grade set — treat it
as a backup pipe and a dedupe cross-check, not independent extra volume.

## 4. Existing Kaggle datasets — checked, none fits off the shelf

- The iNaturalist competition sets (iNat2017–2021, hosted on Kaggle) include these
  species among thousands of classes, but per-class counts for our 9 are small and
  unbalanced, and the grouse/ptarmigan coverage is thin.
- Bird-only sets (Birds525, NABirds, CUB-200) cover some of the birds, none of the
  mammals, and licences vary by photo.
- No Kaggle dataset targets this NL list. Conclusion: build the custom set from the
  iNaturalist API (full control of balance, season mix, and per-photo licence) and
  upload it to Kaggle as a private dataset for the training run — which is also
  exactly Kris's picture: photos from the internet, into Kaggle, learn there.

## 5. The harvested / in-hand gap — measured, not papered over

iNaturalist/GBIF photos are overwhelmingly LIVE animals. The use case that matters
most in the app — a hunter photographing what they're holding or what is on the
ground/tailgate — is the rarest slice in these sources.

Proxy measurement: iNaturalist's "Alive or Dead" annotation (sparse — only a
fraction of observations are annotated at all), reusable research-grade pool:
- Annotated DEAD: ruffed grouse 866 · coyote 2,885 · snowshoe hare 326 · spruce
  grouse 53 · willow ptarmigan 27 · rock ptarmigan 13 (annotated-alive for the same
  species run into the hundreds/thousands)
- And "dead" is a mixed bag — roadkill, window strikes, predator kills — not
  clean hunter-harvest shots. True in-hand harvest photos are a subset of a subset.

Honest read: for ptarmigans the harvested-style pool is a few dozen photos at best;
for grouse/hare/coyote it's hundreds. This does not block v1 — the ID features are
plumage/fur, which transfer — but in-hand accuracy WILL start lower than live-animal
accuracy and must be measured and reported separately, never averaged away.

Proposed handling:
1. Train on the live-animal pool + include every reusable dead-annotated photo as a
   supplementary slice.
2. Report accuracy on two held-out test slices: live-style and harvested-style.
3. Pose/context augmentation (tight crops, rotation, background suppression) to
   narrow the domain gap.
4. Grow the missing slice over time from the app itself: when a hunter photographs
   a harvest and confirms/corrects the species on the record, that photo is exactly
   the missing distribution (a Teach-style loop like the dragon project; any such
   collection is a separate, later decision with its own consent wording — not part
   of this groundwork).

## 6. Training shape for the Kaggle step (when main gives the go)

1. Harvest: pull per-species observation lists from the iNaturalist API by exact
   taxon_id (research-grade, photo licence in CC0/CC-BY/CC-BY-NC), record per-photo
   licence + observation id in a manifest, take max 2–3 photos per observation.
2. Balance: ~1,500–2,000 training photos per class (9 classes ≈ 15k images); cap
   the huge classes (fox/coyote), use nearly all ptarmigan; over-sample ptarmigan
   winter/fall; spruce grouse gets a boost relative to ruffed.
3. Splits BY OBSERVATION (never by photo) into train/val/test so the same animal
   can't appear on both sides.
4. Base model: a general pretrained image network — EfficientNetV2 / MobileNetV3
   class backbone (ImageNet/iNat-pretrained weights), fine-tuned on Kaggle GPU.
   This is a 9-way fine-grained task, well inside what transfer learning does well.
5. Export: TFLite, int8-quantized (target ~4–10 MB — must respect Kris's ~12 MB
   APK ceiling from the other projects) for fully on-device inference in the app.
   No photo leaves the phone at run time.
6. Honest measurement: per-species accuracy + full confusion matrix, reported
   separately for the live-style and harvested-style test slices. Known hard pairs
   to watch: ruffed vs spruce grouse (hen birds), willow vs rock ptarmigan in
   winter white, coyote vs wolf (no scale cue in a photo; juveniles), red-fox
   colour morphs (cross/silver, common in NL) vs coyote.
7. Licence hygiene: manifest keeps the licence column end-to-end; NC slice stays
   partitionable; nothing personal is in the set (public wildlife photos only, no
   Kris data of any kind).

## 7. What waits on main's go

- Downloading at scale (~15k photos) and building the dataset + manifest.
- Creating the Kaggle dataset and the training notebook/run.
- Any app-side photo-ID work (camera capture in a record, model wiring).
