"""
Census - virtual experiment.

Mirrors the mod's actual constants/formulas (see BigFive, ImportanceScorer,
Appraiser, UtilityScorer, Census, SocialBonds) to predict emergent behaviour
without running the game for hours. Pure stdlib.

Tick reference: 20 ticks = 1 second; 24000 ticks = 1 in-game day (20 min).
"""
import random
import statistics as stats

TPS = 20
DAY = 24000

# ---- persona generation (BigFive.roll / traits) -------------------------------
def roll():
    return (random.random() + random.random()) * 0.5  # triangular, mean 0.5

def persona():
    return dict(O=roll(), C=roll(), E=roll(), A=roll(), N=roll())

def traits(p):
    t = set()
    if p["N"] < 0.35: t.add("brave")
    if p["N"] > 0.70: t.add("cowardly")
    if p["A"] < 0.30 and p["N"] > 0.50: t.add("vengeful")
    if p["A"] > 0.70 and p["N"] < 0.40: t.add("forgiving")
    if p["O"] > 0.65: t.add("curious")
    if p["C"] > 0.65: t.add("diligent")
    if p["C"] < 0.30: t.add("lazy")
    if p["E"] > 0.65: t.add("sociable")
    if p["E"] < 0.30: t.add("reclusive")
    if p["A"] > 0.70: t.add("kind")
    if p["A"] < 0.25: t.add("callous")
    return t

# ---- formulas from the mod ----------------------------------------------------
HARMED_BASE, HARMED_VAL = 6.0, -0.7
REP_SCALE = 3.0
FLEE_THRESHOLD = 0.35
PERSONAL_HATE = -60.0

def importance(base, val, N):
    return max(0.0, min(10.0, base * abs(val) * (1 + N * 0.5)))

def harm_rep_delta(N):
    imp = importance(HARMED_BASE, HARMED_VAL, N)
    return imp * HARMED_VAL * REP_SCALE  # negative

def fear_gain(N):
    return 0.30 * (1 + N * 0.5)  # Appraiser HARMED->FEAR base 0.3, modulated

def timidity(p):
    return max(0.2, min(1.1, 0.5 + p["N"] * 0.6 - p["E"] * 0.2))

def flee_desire(opinion, fear, p):
    dislike = max(0.0, -opinion) / 100.0
    return (dislike * 1.0 + fear * 0.35) * timidity(p)

# ===============================================================================
print("=" * 64)
print("CENSUS VIRTUAL EXPERIMENT")
print("=" * 64)

# --- A. trait rarity -----------------------------------------------------------
print("\n[A] Trait frequency in the population (200k villagers)")
N = 200_000
counts = {}
for _ in range(N):
    for t in traits(persona()):
        counts[t] = counts.get(t, 0) + 1
for t, c in sorted(counts.items(), key=lambda kv: -kv[1]):
    print(f"    {t:10s} {100*c/N:5.1f}%")
van = counts.get("vengeful", 0) / N
print(f"    -> ~1 in {1/van:.0f} villagers can become an Avenger")

# --- B. hits to flee / to avenge, by archetype ---------------------------------
print("\n[B] Provoking a villager (punches every 1s; fear decays 0.98/tick)")
archetypes = {
    "average":  dict(O=.5, C=.5, E=.5, A=.5, N=.5),
    "brave":    dict(O=.5, C=.5, E=.6, A=.5, N=.2),
    "cowardly": dict(O=.5, C=.5, E=.3, A=.5, N=.8),
    "vengeful": dict(O=.5, C=.5, E=.4, A=.2, N=.7),
}
DT = 20  # punch interval ticks
print(f"    {'archetype':10s} {'hits->flee':>10s} {'hits->avenger':>13s}")
for name, p in archetypes.items():
    opinion, fear = 0.0, 0.0
    flee_at = avenge_at = None
    veng = "vengeful" in traits(p)
    for hit in range(1, 41):
        fear *= 0.98 ** DT
        opinion += harm_rep_delta(p["N"])
        fear = min(1.0, fear + fear_gain(p["N"]))
        if flee_at is None and flee_desire(opinion, fear, p) >= FLEE_THRESHOLD:
            flee_at = hit
        if avenge_at is None and veng and opinion <= PERSONAL_HATE:
            avenge_at = hit
    print(f"    {name:10s} {str(flee_at):>10s} "
          f"{(str(avenge_at) if veng else '- (not vengeful)'):>13s}")

# --- C. courtship time (bond growth) -------------------------------------------
print("\n[C] Time for a pair to fall in love (bond >= 40)")
BOND_PER_MEET, COURT = 2.5, 40.0
ROUND = 200          # social round ticks
MEET_CHANCE = 0.25
for k in (3, 5, 8):  # villagers in the local cluster
    times = []
    for _ in range(20000):
        bond, rounds = 0.0, 0
        while bond < COURT:
            rounds += 1
            # A meets and picks B, or B meets and picks A
            ab = (random.random() < MEET_CHANCE) and (random.randint(1, k - 1) == 1)
            ba = (random.random() < MEET_CHANCE) and (random.randint(1, k - 1) == 1)
            if ab or ba:
                bond += BOND_PER_MEET   # meet() bumps both directions
        times.append(rounds * ROUND)
    med = stats.median(times)
    print(f"    cluster of {k}: median {med/DAY*20:5.1f} min "
          f"({med} ticks)   [gossip would speed this up]")

# --- D. population growth (couple-driven reproduction) --------------------------
print("\n[D] Village population over 30 in-game days (medians of 300 runs)")
POP_CAP, COOLDOWN, RCHANCE = 16, DAY, 0.10
MATURE = DAY  # baby -> adult
def run_village(start_couples):
    # villagers: list of [adult_at_tick, last_child_tick, paired]
    vill = []
    for _ in range(start_couples * 2):
        vill.append([0, -10**9])
    pairs = [(2*i, 2*i+1) for i in range(start_couples)]
    t = 0
    snapshots = {}
    while t <= 30 * DAY:
        if t % ROUND == 0:
            adults = sum(1 for v in vill if t - v[0] >= 0 and v[0] <= t)
            for (i, j) in pairs:
                if len(vill) >= POP_CAP:
                    break
                a, b = vill[i], vill[j]
                if t - a[1] < COOLDOWN or t - b[1] < COOLDOWN:
                    continue
                if a[0] > t or b[0] > t:   # still a baby
                    continue
                if random.random() < RCHANCE:
                    vill.append([t + MATURE, t])  # child matures after MATURE
                    a[1] = b[1] = t
        if t % DAY == 0:
            snapshots[t // DAY] = len(vill)
        t += ROUND
    return snapshots

import collections
agg = collections.defaultdict(list)
for _ in range(300):
    snaps = run_village(start_couples=3)
    for d, n in snaps.items():
        agg[d].append(n)
print("    day :  pop")
for d in (0, 1, 3, 5, 10, 20, 30):
    if d in agg:
        print(f"    {d:3d} : {int(stats.median(agg[d])):4d}")
print(f"    (start: 3 couples = 6 villagers; local cap {POP_CAP})")
print("\nDone.")
