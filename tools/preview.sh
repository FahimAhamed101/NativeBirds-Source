#!/bin/sh
# Recompile the desktop preview harness and render the given scenes.
set -e
cd "$(dirname "$0")/.."

OUT=build/preview
rm -rf "$OUT"
mkdir -p "$OUT"

javac -nowarn -d "$OUT" $(find tools/preview -name '*.java') \
  src/com/nativebirds/game/Vec2.java \
  src/com/nativebirds/game/Body.java \
  src/com/nativebirds/game/Collide.java \
  src/com/nativebirds/game/Arbiter.java \
  src/com/nativebirds/game/World.java \
  src/com/nativebirds/game/Entity.java \
  src/com/nativebirds/game/Block.java \
  src/com/nativebirds/game/Pig.java \
  src/com/nativebirds/game/Bird.java \
  src/com/nativebirds/game/Level.java \
  src/com/nativebirds/game/Game.java \
  src/com/nativebirds/game/Sfx.java \
  src/com/nativebirds/game/Store.java \
  src/com/nativebirds/game/SpriteBank.java \
  src/com/nativebirds/game/Renderer.java

# Render at the target phone's real landscape resolution by default so the
# preview matches what the device actually draws.
PREVIEW_W=${PREVIEW_W:-1520}
PREVIEW_H=${PREVIEW_H:-720}

for scene in "$@"; do
  java -cp "$OUT" Preview assets build/shots "$scene" "$PREVIEW_W" "$PREVIEW_H"
done
