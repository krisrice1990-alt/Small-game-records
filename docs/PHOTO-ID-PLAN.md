# Small Game Records NL — Photo Identification Plan

**Status: Stage 1 GO (2026-10-28 00:18 NDT) — app integration still parked.**
Kris, 2026-10-28 ~00:17: "We're not going to build it in the app yet, but keep
this plan saved so I can come back." Then, one minute later, the clarification:
"the kaggle stuff we're going to do right now — find all the pictures you can
online and put it into kaggle to get us a model first to run on first."
So: the dataset gathering + Kaggle training (Stage 1 below) is ACTIVE; wiring
the model into the app stays parked until Kris says so.

Dataset research behind this plan: `~/workspace/sgr-phase3/DATASET-PLAN.md`
(groundwork report, 2026-10-28 — species counts, licences, season spread, gaps).

## The idea (Kris's, 2026-10-27 23:46 – 2026-10-28 00:15 NDT)

While logging a hunt, the hunter takes a photo of the animal. The app tries to
identify it — species, and male/female where a photo can honestly tell — and the
photo is kept as part of that day's record (a "picture record").

## The flow (order matters — Kris specified AI-first)

1. Hunter takes/attaches a photo in the day's record.
2. **The AI guesses FIRST**, before the hunter says anything:
   - species (one of the app's list, or "not sure" when confidence is low)
   - sex — male / female / can't tell — ONLY for species where sex shows in a
     photo (see the sex table below). Never guess sex for species that don't show it.
3. The hunter confirms the guess or fixes it. **The hunter's answer is the record.**
4. The confirmed photo + labels are saved:
   - in the day's record (the picture record), and
   - in an on-phone **learn file** pile (photo + hunter-confirmed labels).
5. When the learn-file pile is big enough, it is **sent off to Kaggle** (a
   deliberate send, never silent) for a retrain round that mixes the real
   in-hand photos in. The improved model returns through the app's normal
   update channel.

## Stage 1 — the first model (internet photos → Kaggle)

Kris's method (2026-10-27 23:53): "take pictures from the internet and put it
in Kaggle AI and get it to learn there."

- Species = exactly the app's shipped list: Snowshoe hare, Ruffed grouse,
  Spruce grouse, Willow ptarmigan, Rock ptarmigan, Porcupine, Coyote, Fox
  (red fox), Wolf. ("Other" is a catch-all, not a learnable class.)
- Source: iNaturalist research-grade photos, licence-clean only (per-photo
  CC0 / CC-BY / CC-BY-NC; ~88% of the usable pool is CC-BY-NC — fine for
  personal use; the manifest keeps a per-photo licence column so the NC slice
  stays separable if anything is ever shared). Counts verified 2026-10-28:
  hare 15,299 · ruffed grouse 18,811 · spruce grouse 6,784 · willow ptarmigan
  3,498 · rock ptarmigan 3,587 · porcupine 23,617 · coyote 92,606 · fox
  100,622 · wolf 7,833 usable observations. Quantity is not a problem.
- No off-the-shelf Kaggle dataset fits this species list — build the custom
  set from the iNaturalist API, upload as a PRIVATE Kaggle dataset, train there.
- Training shape (from the groundwork): ~1,500–2,000 photos per class (~15k
  total), max 2–3 photos per observation, splits BY observation (no leakage),
  fine-tune a general pretrained image model on Kaggle's GPU, export a small
  on-device model (TFLite int8, ~4–10 MB — respect the ~12 MB APK ceiling).
  Fully on-device afterwards: no photo leaves the phone for identification.
- Balance the seasons: ptarmigan sources are summer-heavy (rock ptarmigan
  winter-white only ~107 usable) — over-sample winter/fall birds, cap summer.
- Report per-species accuracy AND a confusion matrix, with accuracy measured
  SEPARATELY on live-animal photos and harvested/in-hand photos (never averaged).

## The honest gap (told to Kris before he decides)

Internet photos are overwhelmingly LIVE animals. In-hand harvest photos are
scarce everywhere (dead-annotated reusable photos: ruffed grouse 866, coyote
2,885, hare 326, spruce grouse 53, willow ptarmigan 27, rock ptarmigan 13 —
true in-hand shots are a subset). So **v1 will be strong on live-animal
photos and noticeably weaker on a bird in the hand**. That is a data fact,
not a modelling failure — and Stage 2 (the learn-file loop above) is exactly
how it gets fixed over time.

## Sex identification — what a photo can honestly tell

| Species | Sex from a photo? |
|---|---|
| Spruce grouse | Yes, usually clear — male dark with red eye comb; female barred brown |
| Ruffed grouse | Often — male's dark tail band usually unbroken; female's broken in the middle. Only when the tail shows |
| Willow / Rock ptarmigan | Sometimes — depends on season/plumage stage |
| Snowshoe hare | No — sexes look the same; the app must not ask or guess |
| Porcupine, Coyote, Fox, Wolf | No reliable photo cue — don't guess |

Model output per photo: species + (sex | can't tell | not applicable),
always followed by hunter confirmation.

Known hard pairs to measure explicitly: ruffed vs spruce grouse (hens),
willow vs rock ptarmigan in winter white, coyote vs wolf (no scale cue in a
photo, juveniles), red-fox colour morphs (cross/silver common in NL) vs coyote.

## Standing rules that bind this feature

- Kris's SGR rules: NO personal information in the app/code/feed; everything
  ships as generic defaults; records live only on each user's phone.
- Learn files stay ON the phone until the owner deliberately sends them.
  No silent uploads, ever.
- The hunter's confirmed answer always wins over the AI's guess.
- Dataset photos: public wildlife photos only, licences respected and recorded.

## When Kris comes back to this

Say the word in the main chat and the next step is Stage 1: build the dataset
(download the inventoried photos), create the private Kaggle dataset, run the
first training, and report per-species + per-slice (live vs in-hand) accuracy
before anything is wired into the app.
