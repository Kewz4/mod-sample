#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "work"
SRC = ROOT / "src" / "main"
JAVA = SRC / "java"
RES = SRC / "resources"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")


# Keep the Atmospherics particle shader. It is a uniquely named shader used by
# the air-haze particles, not a replacement for a vanilla particle shader.
build_file = ROOT / "build.gradle"
build = read(build_file)
build = "\n".join(
    line
    for line in build.splitlines()
    if not ("exclude" in line and "particle_no_cutoff" in line)
)

# The old UI smoke flag opened the config screen while Minecraft's first
# resource reload was still active. Keep the normal startup smoke test only.
build = "\n".join(
    line for line in build.splitlines()
    if "atmospherics.fabric.port.renderingSmokeTest" not in line
)

# Never package Atmospherics-provided celestial textures. The renderer still
# references Minecraft's normal resource IDs, allowing vanilla or the user's
# highest-priority resource pack to provide the images.
celestial_excludes = """

tasks.named('jar') {
    exclude 'assets/minecraft/textures/environment/sun.png'
    exclude 'assets/minecraft/textures/environment/moon_phases.png'
    exclude 'resourcepacks/atmospherics_pack/assets/minecraft/textures/environment/sun.png'
    exclude 'resourcepacks/atmospherics_pack/assets/minecraft/textures/environment/moon_phases.png'
}
"""
if "resourcepacks/atmospherics_pack/assets/minecraft/textures/environment/moon_phases.png" not in build:
    build += celestial_excludes
write(build_file, build.rstrip() + "\n")

# Explicitly undo the abandoned fallback that disabled air haze whenever Iris
# was active. Air-haze particles remain enabled under shader packs.
ambient_candidates = list(JAVA.rglob("AmbientFlowMixin.java"))
if len(ambient_candidates) != 1:
    raise RuntimeError(f"Expected one AmbientFlowMixin.java, found {ambient_candidates}")
ambient_path = ambient_candidates[0]
ambient = read(ambient_path)
ambient = re.sub(
    r"\n\s*if\s*\(ShaderCompat\.isShaderPackInUse\(\)\)\s*\{\s*return;\s*\}\s*",
    "\n",
    ambient,
)
ambient = re.sub(r"^import com\.beash\.atmospherics\.compat\.ShaderCompat;\n", "", ambient, flags=re.M)
write(ambient_path, ambient)

# Confirm the full custom sky is bypassed under Iris. A separate post-sky mixin
# adds only Atmospherics haze/comets after Iris has rendered its own sky.
sky_candidates = list(JAVA.rglob("WorldRendererSkyMixin.java"))
if len(sky_candidates) != 1:
    raise RuntimeError(f"Expected one WorldRendererSkyMixin.java, found {sky_candidates}")
sky = read(sky_candidates[0])
if "ShaderCompat.isShaderPackInUse()" not in sky:
    raise RuntimeError("WorldRendererSkyMixin is missing the Iris sky bypass")

# Register the post-Iris pass and force the production refmap name.
mixin_file = RES / "atmospherics.mixins.json"
mixins = json.loads(read(mixin_file))
mixins["refmap"] = "atmospherics.refmap.json"
registered = False
for key in ("client", "mixins"):
    entries = mixins.get(key)
    if isinstance(entries, list) and "WorldRendererSkyMixin" in entries:
        if "ShaderHazePassMixin" not in entries:
            index = entries.index("WorldRendererSkyMixin") + 1
            entries.insert(index, "ShaderHazePassMixin")
        registered = True
        break
if not registered:
    mixins.setdefault("client", []).append("ShaderHazePassMixin")
write(mixin_file, json.dumps(mixins, indent=2) + "\n")

# Mark both built-in and standalone SMC metadata as native MC 1.21.1 packs.
pack_meta = RES / "resourcepacks" / "atmospherics_pack" / "pack.mcmeta"
if pack_meta.exists():
    data = json.loads(read(pack_meta))
    data.setdefault("pack", {})["pack_format"] = 34
    data["pack"].pop("supported_formats", None)
    description = data["pack"].get("description", "Story Mode Clouds for Atmospherics")
    data["pack"]["description"] = description
    write(pack_meta, json.dumps(data, indent=2) + "\n")

standalone_meta = Path(__file__).resolve().parent / "story-mode-clouds-pack.mcmeta"
if standalone_meta.exists():
    data = json.loads(read(standalone_meta))
    data.setdefault("pack", {})["pack_format"] = 34
    data["pack"].pop("supported_formats", None)
    write(standalone_meta, json.dumps(data, indent=2) + "\n")

# Delete any celestial images that may have been copied from the source JAR.
for relative in (
    "assets/minecraft/textures/environment/sun.png",
    "assets/minecraft/textures/environment/moon_phases.png",
    "resourcepacks/atmospherics_pack/assets/minecraft/textures/environment/sun.png",
    "resourcepacks/atmospherics_pack/assets/minecraft/textures/environment/moon_phases.png",
):
    target = RES / relative
    if target.exists():
        target.unlink()

# Give resource reloads enough time during CI; the old 120-tick stop could end
# while the built-in SMC pack was still compiling its cloud shader.
for client_file in JAVA.rglob("AtmosphericsFabricClient.java"):
    client = read(client_file)
    client = client.replace(">= 120", ">= 300")
    client = client.replace("after 120 client ticks", "after 300 client ticks")
    write(client_file, client)

# Distinguish the combined rendering build from the earlier startup-only fix.
mod_json = RES / "fabric.mod.json"
mod = json.loads(read(mod_json))
mod["version"] = "2.6.5-fabric.2+smc-haze"
write(mod_json, json.dumps(mod, indent=2) + "\n")

# Final source-level assertions.
shader_pass = JAVA / "com" / "beash" / "atmospherics" / "mixin" / "ShaderHazePassMixin.java"
if not shader_pass.exists():
    raise RuntimeError("ShaderHazePassMixin.java was not copied into the generated project")
if "ShaderHazePassMixin" not in json.loads(read(mixin_file)).get("client", []) and \
        "ShaderHazePassMixin" not in json.loads(read(mixin_file)).get("mixins", []):
    raise RuntimeError("ShaderHazePassMixin is not registered")
if "ShaderCompat.isShaderPackInUse()" in read(ambient_path):
    raise RuntimeError("Air haze is still disabled under Iris")

print("Applied Atmospherics Fabric rendering compatibility v4")
