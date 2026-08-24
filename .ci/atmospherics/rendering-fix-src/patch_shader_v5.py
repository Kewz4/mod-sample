from pathlib import Path

root = Path('.ci/atmospherics/work')

# Atmospherics registers particle_no_cutoff during every shader reload. Keep the
# shader assets it expects; the previous build removed them and forced Minecraft
# into resource-pack recovery mode.
build = root / 'build.gradle'
text = build.read_text(encoding='utf-8')
for line in (
    "        exclude 'assets/minecraft/shaders/core/particle_no_cutoff.fsh'\n",
    "        exclude 'assets/minecraft/shaders/core/particle_no_cutoff.json'\n",
    "        exclude 'assets/minecraft/shaders/core/particle_no_cutoff.vsh'\n",
):
    text = text.replace(line, '')
build.write_text(text, encoding='utf-8')

# Air Haze uses custom translucent particle geometry in addition to the horizon
# haze renderer. Both paths are skipped only while an Iris shader pack is active;
# the normal non-shader renderer keeps the complete Atmospherics effect.
ambient = root / 'src/main/java/com/beash/atmospherics/mixin/AmbientFlowMixin.java'
source = ambient.read_text(encoding='utf-8')
import_marker = 'import com.beash.atmospherics.Atmospherics;\n'
if 'import com.beash.atmospherics.compat.ShaderCompat;' not in source:
    if import_marker not in source:
        raise SystemExit('AmbientFlowMixin import marker not found')
    source = source.replace(
        import_marker,
        import_marker + 'import com.beash.atmospherics.compat.ShaderCompat;\n',
        1,
    )

method_marker = (
    '    private void atmospherics$spawnPolytoneMist(int centerX, int centerY, '
    'int centerZ, int radius, RandomSource random, Block block, '
    'BlockPos.MutableBlockPos pos, CallbackInfo ci) {\n'
)
if method_marker not in source:
    raise SystemExit('AmbientFlowMixin haze method marker not found')
source = source.replace(
    method_marker,
    method_marker
    + '        if (ShaderCompat.isShaderPackInUse()) {\n'
    + '            return;\n'
    + '        }\n',
    1,
)
ambient.write_text(source, encoding='utf-8')
