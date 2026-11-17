"""Extract sprites + sound effects from the Angry Birds Chrome source into the native game."""
import json, os, shutil
from PIL import Image

ROOT = r"C:\Users\Admin\Desktop\Angry-Birds\angrybirdschrome-source-html5\cors\fowl"
OUT = r"C:\Users\Admin\Desktop\Angry-Birds\NativeBirds\assets"
SPR = os.path.join(OUT, "sprites")
SFX = os.path.join(OUT, "sfx")
os.makedirs(SPR, exist_ok=True)
os.makedirs(SFX, exist_ok=True)

def atlas(name):
    d = json.load(open(os.path.join(ROOT, "sprite_sheets", name + ".json")))
    img = Image.open(os.path.join(ROOT, "sprite_sheets", name + ".png")).convert("RGBA")
    frames = {}
    for k, v in d.items():
        if k.startswith("sprite_"):
            frames[v["id"]] = v
    return img, frames

def crop(img, f, out_name):
    box = (f["x"], f["y"], f["x"] + f["width"], f["y"] + f["height"])
    img.crop(box).save(os.path.join(SPR, out_name), optimize=True)

# ---------------- birds ----------------
img, fr = atlas("INGAME_BIRDS")
birds = {
    "red": "BIRD_RED", "yellow": "BIRD_YELLOW", "black": "BIRD_BLACK",
    "blue": "BIRD_BLUE", "white": "BIRD_WHITE", "green": "BIRD_GREEN",
}
for name, pre in birds.items():
    for suffix, tag in (("_1", "idle"), ("_FLYING", "fly")):
        if pre + suffix in fr:
            crop(img, fr[pre + suffix], "bird_%s_%s.png" % (name, tag))

# ---------------- pigs ----------------
img, fr = atlas("INGAME_PIGS")
pigs = {"small": "PIG_BASIC_SMALL_01", "medium": "PIG_BASIC_MEDIUM_01",
        "big": "PIG_BASIC_BIG_01", "helmet": "PIG_HELMET_01", "king": "PIG_KING_01"}
for name, sid in pigs.items():
    if sid in fr:
        crop(img, fr[sid], "pig_%s.png" % name)
    if sid + "_BLINK" in fr:
        crop(img, fr[sid + "_BLINK"], "pig_%s_blink.png" % name)

# ---------------- blocks ----------------
img, fr = atlas("INGAME_BLOCKS_BASIC")
mat_prefix = {"wood": "WOOD", "stone": "STONE", "ice": "ICE"}
sizes = ["1X1", "2X1", "4X1", "2X2", "4X2", "8X1", "10X1"]
count = 0
for mat, pre in mat_prefix.items():
    for size in sizes:
        for state in (1, 2, 3, 4):
            sid = "%s_BLOCK_%s_%d" % (pre, size, state)
            if sid in fr:
                crop(img, fr[sid], "blk_%s_%s_%d.png" % (mat, size.lower(), state))
                count += 1

# ---------------- slingshot + clouds ----------------
for src, dst in [
    ("images/Slingshot/SLINGSHOT_02.png", "sling.png"),
    ("images/Slingshot/SLINGSHOT_01_BACK.png", "sling_back.png"),
    ("images/Slingshot/SLINGSHOT_01_FRONT.png", "sling_front.png"),
    ("images/Slingshot/SLINGSHOT_HOLDER.png", "sling_holder.png"),
    ("images/BackgroundThemes/BACKGROUND_10_CLOUD_1.png", "cloud1.png"),
    ("images/BackgroundThemes/BACKGROUND_10_CLOUD_2.png", "cloud2.png"),
    ("images/BackgroundThemes/BACKGROUND_10_CLOUD_3.png", "cloud3.png"),
]:
    p = os.path.join(ROOT, src)
    if os.path.exists(p):
        Image.open(p).convert("RGBA").save(os.path.join(SPR, dst), optimize=True)

# ---------------- sounds ----------------
A = os.path.join(ROOT, "audio")
sounds = {
    "bird_select": "Birds/Bird_Red/Bird_Red_Selected_1.mp3",
    "bird_launch": "Birds/Bird_Red/Bird_Red_Slingshot_1.mp3",
    "bird_fly": "Birds/Bird_Red/Bird_Red_Flying_1.mp3",
    "bird_hit": "Birds/Bird_Red/Bird_Red_Collision_1.mp3",
    "yellow_dash": "Birds/Bird_Yellow/Bird_Yellow_Special_1.mp3",
    "black_bomb": "Birds/Bird_Black/Bird_Black_Special_1.mp3",
    "blue_split": "Birds/Bird_Blue/Bird_Blue_Special_1.mp3",
    "white_egg": "Birds/Bird_White/Bird_White_Special_1.mp3",
    "wood_hit": "Blocks/Wood_Blocks/Wood_Collision_1.mp3",
    "wood_break": "Blocks/Wood_Blocks/Wood_Destroyed_1.mp3",
    "stone_hit": "Blocks/Stone_Blocks/Stone_Collision_1.mp3",
    "stone_break": "Blocks/Stone_Blocks/Stone_Destroyed_1.mp3",
    "ice_hit": "Blocks/Ice_Blocks/Ice_Collision_1.mp3",
    "ice_break": "Blocks/Ice_Blocks/Ice_Destroyed_1.mp3",
    "pig_hit": "Pigs/Pigs_Set1_SmallAndMedium/Pigs_Set1_Collision_1.mp3",
    "pig_damage": "Pigs/Pigs_Set1_SmallAndMedium/Pigs_Set1_Damage_1.mp3",
    "pig_pop": "Pigs/Pigs_Set1_SmallAndMedium/Pigs_Set1_Destroyed_1.mp3",
    "pig_talk": "Pigs/Pigs_Set1_SmallAndMedium/Pigs_Set1_Talk_1.mp3",
    "win": "LevelStartEnd/LevelWinBirds_1.mp3",
    "lose": "LevelStartEnd/LevelFailedPigs_1.mp3",
    "star": "LevelStartEnd/LevelWinStar_1.mp3",
    "score": "LevelStartEnd/LevelWinScoreCount.mp3",
    "start": "LevelStartEnd/LevelStartsBirds_1.mp3",
}
missing = []
for name, rel in sounds.items():
    p = os.path.join(A, rel)
    if os.path.exists(p):
        shutil.copyfile(p, os.path.join(SFX, name + ".mp3"))
    else:
        missing.append(rel)

print("blocks:", count)
print("sprites:", len(os.listdir(SPR)), "sounds:", len(os.listdir(SFX)))
print("missing:", missing)
