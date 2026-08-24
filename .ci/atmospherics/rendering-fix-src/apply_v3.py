from pathlib import Path
import json

root = Path('.ci/atmospherics/work')
build = root / 'build.gradle'
text = build.read_text(encoding='utf-8')

loom_marker = "loom {\n    runs {"
loom_replacement = (
    "loom {\n"
    "    mixin {\n"
    "        defaultRefmapName = 'atmospherics.refmap.json'\n"
    "    }\n\n"
    "    runs {"
)
if loom_marker not in text:
    raise SystemExit('loom runs marker not found')
text = text.replace(loom_marker, loom_replacement, 1)

exclude_marker = "        exclude 'atmospherics.mixins.json'"
exclude_replacement = "\n".join([
    exclude_marker,
    "        exclude 'com/beash/atmospherics/mixin/**'",
    "        exclude 'resourcepacks/atmospherics_pack/pack.mcmeta'",
    "        exclude 'resourcepacks/atmospherics_pack/assets/minecraft/textures/environment/sun.png'",
    "        exclude 'assets/minecraft/shaders/core/particle_no_cutoff.fsh'",
    "        exclude 'assets/minecraft/shaders/core/particle_no_cutoff.json'",
    "        exclude 'assets/minecraft/shaders/core/particle_no_cutoff.vsh'",
])
if exclude_marker not in text:
    raise SystemExit('JAR exclude marker not found')
text = text.replace(exclude_marker, exclude_replacement, 1)
text = text.replace(
    "            vmArg '-Datmospherics.fabric.port.smokeTest=true'",
    "            vmArg '-Datmospherics.fabric.port.smokeTest=true'\n"
    "            vmArg '-Datmospherics.fabric.port.renderingSmokeTest=true'",
    1,
)
build.write_text(text, encoding='utf-8')

mixin_dir = root / 'src/main/java/com/beash/atmospherics/mixin'
for java in mixin_dir.glob('*.java'):
    source = java.read_text(encoding='utf-8')
    source = source.replace('(ClientLevel)this', '(ClientLevel)(Object)this')
    source = source.replace('cir.setReturnValue((Object)0);', 'cir.setReturnValue(0);')
    source = source.replace(
        'cir.setReturnValue((Object)new Vec3(r, g, b));',
        'cir.setReturnValue(new Vec3(r, g, b));',
    )
    source = source.replace(
        'cir.setReturnValue((Object)original);',
        'cir.setReturnValue(original);',
    )
    java.write_text(source, encoding='utf-8')

sky = mixin_dir / 'WorldRendererSkyMixin.java'
source = sky.read_text(encoding='utf-8')
source = source.replace(
    'import com.beash.atmospherics.Atmospherics;\n',
    'import com.beash.atmospherics.Atmospherics;\n'
    'import com.beash.atmospherics.compat.ShaderCompat;\n',
    1,
)
method_marker = (
    '    private void atmospherics$reimplementSky(Matrix4f positionMatrix, '
    'Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, '
    'Runnable fogCallback, CallbackInfo ci) {\n'
)
if method_marker not in source:
    raise SystemExit('WorldRendererSkyMixin renderSky handler marker not found')
source = source.replace(
    method_marker,
    method_marker
    + '        if (ShaderCompat.isShaderPackInUse()) {\n'
    + '            return;\n'
    + '        }\n',
    1,
)
sky.write_text(source, encoding='utf-8')

mixin_config = {
    'required': True,
    'minVersion': '0.8',
    'package': 'com.beash.atmospherics.mixin',
    'compatibilityLevel': 'JAVA_21',
    'client': [
        'AmbientFlowMixin',
        'LightningEntityRendererMixin',
        'ClientWorldSkyColorMixin',
        'DimensionEffectsSunriseMixin',
        'BackgroundRendererMixin',
        'WeatherSoundMixin',
        'WorldRendererSkyMixin',
        'WorldRendererCloudMixin',
        'FogConfigScreenCloudSupportMixin',
    ],
    'injectors': {'defaultRequire': 1},
    'refmap': 'atmospherics.refmap.json',
}
(root / 'src/main/resources/atmospherics.mixins.json').write_text(
    json.dumps(mixin_config, indent=2) + '\n',
    encoding='utf-8',
)

client = root / 'src/main/java/com/beash/atmospherics/AtmosphericsFabricClient.java'
source = client.read_text(encoding='utf-8')
source = source.replace(
    'import com.beash.atmospherics.particle.AtmosphericsParticleSheets;\n',
    'import com.beash.atmospherics.particle.AtmosphericsParticleSheets;\n'
    'import com.beash.atmospherics.screen.FogConfigScreen;\n',
    1,
)
source = source.replace(
    'import net.fabricmc.loader.api.FabricLoader;\n',
    'import net.fabricmc.loader.api.FabricLoader;\n'
    'import net.minecraft.client.Minecraft;\n'
    'import net.minecraft.client.gui.components.Button;\n',
    1,
)
source = source.replace(
    'import java.util.concurrent.atomic.AtomicInteger;\n',
    'import java.lang.reflect.Field;\n'
    'import java.util.concurrent.atomic.AtomicInteger;\n',
    1,
)

old_tick = '''        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Atmospherics.onClientTick(new ClientTickEvent.Post());
            if (Boolean.getBoolean("atmospherics.fabric.port.smokeTest")
                    && SMOKE_TEST_TICKS.incrementAndGet() >= 120) {
                LOGGER.info("Atmospherics Fabric smoke test completed after 120 client ticks");
                client.stop();
            }
        });'''
new_tick = '''        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Atmospherics.onClientTick(new ClientTickEvent.Post());
            if (Boolean.getBoolean("atmospherics.fabric.port.smokeTest")) {
                int smokeTick = SMOKE_TEST_TICKS.incrementAndGet();
                if (Boolean.getBoolean("atmospherics.fabric.port.renderingSmokeTest")) {
                    runRenderingSmokeTest(client, smokeTick);
                }
                if (smokeTick >= 120) {
                    LOGGER.info("Atmospherics Fabric smoke test completed after 120 client ticks");
                    client.stop();
                }
            }
        });'''
if old_tick not in source:
    raise SystemExit('Fabric client smoke-test marker not found')
source = source.replace(old_tick, new_tick, 1)

helper = r'''

      private static void runRenderingSmokeTest(Minecraft client, int smokeTick) {
          try {
              if (smokeTick == 30) {
                  client.setScreen(new FogConfigScreen(null));
              } else if (smokeTick == 55) {
                  if (!(client.screen instanceof FogConfigScreen screen)) {
                      throw new IllegalStateException("FogConfigScreen did not open");
                  }
                  Field cloudButtonField = FogConfigScreen.class.getDeclaredField("cloudsModeButton");
                  cloudButtonField.setAccessible(true);
                  Button cloudButton = (Button) cloudButtonField.get(screen);
                  cloudButton.onPress();
              } else if (smokeTick == 75) {
                  if (!(client.screen instanceof FogConfigScreen screen)) {
                      throw new IllegalStateException("FogConfigScreen did not remain open after selecting clouds");
                  }
                  Field editModeField = FogConfigScreen.class.getDeclaredField("editMode");
                  editModeField.setAccessible(true);
                  Object editMode = editModeField.get(screen);
                  if (!(editMode instanceof Enum<?> mode) || !"CLOUDS".equals(mode.name())) {
                      throw new IllegalStateException("Cloud mode callback did not select CLOUDS");
                  }
                  Field storyButtonField = FogConfigScreen.class.getDeclaredField("storyCloudSettingsButton");
                  storyButtonField.setAccessible(true);
                  Button storyButton = (Button) storyButtonField.get(screen);
                  storyButton.onPress();
              } else if (smokeTick == 95) {
                  if (client.screen == null
                          || !client.screen.getClass().getName().endsWith("$StoryCloudSettingsScreen")) {
                      throw new IllegalStateException("Story Mode Clouds settings screen did not open");
                  }
                  LOGGER.info("Atmospherics cloud controls smoke test completed");
              }
          } catch (ReflectiveOperationException error) {
              throw new RuntimeException("Atmospherics rendering compatibility smoke test failed", error);
          }
      }
'''
closing = '\n}\n'
if not source.endswith(closing):
    raise SystemExit('Unexpected AtmosphericsFabricClient class ending')
source = source[:-len(closing)] + helper + closing
client.write_text(source, encoding='utf-8')
