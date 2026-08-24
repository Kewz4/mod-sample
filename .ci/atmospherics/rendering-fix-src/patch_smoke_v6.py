from pathlib import Path
import re

client = Path(
    '.ci/atmospherics/work/src/main/java/com/beash/atmospherics/AtmosphericsFabricClient.java'
)
source = client.read_text(encoding='utf-8')

import_marker = 'import net.minecraft.client.gui.components.Button;\n'
if 'import net.minecraft.client.gui.screens.TitleScreen;' not in source:
    if import_marker not in source:
        raise SystemExit('Button import marker not found')
    source = source.replace(
        import_marker,
        import_marker + 'import net.minecraft.client.gui.screens.TitleScreen;\n',
        1,
    )

field_marker = '    private static final AtomicInteger SMOKE_TEST_TICKS = new AtomicInteger();\n'
field_block = (
    field_marker
    + '    private static int RENDERING_SMOKE_STAGE;\n'
    + '    private static int RENDERING_SMOKE_STAGE_TICKS;\n'
    + '    private static boolean RENDERING_SMOKE_COMPLETE;\n'
)
if 'private static int RENDERING_SMOKE_STAGE;' not in source:
    if field_marker not in source:
        raise SystemExit('Smoke-test field marker not found')
    source = source.replace(field_marker, field_block, 1)

old_stop = '''                if (smokeTick >= 260) {
                    LOGGER.info("Atmospherics Fabric smoke test completed after 260 client ticks");
                    client.stop();
                }'''
new_stop = '''                if (Boolean.getBoolean("atmospherics.fabric.port.renderingSmokeTest")) {
                    if (RENDERING_SMOKE_COMPLETE) {
                        LOGGER.info(
                                "Atmospherics Fabric rendering smoke test completed after {} client ticks",
                                smokeTick);
                        client.stop();
                    } else if (smokeTick >= 1200) {
                        throw new IllegalStateException(
                                "Atmospherics rendering compatibility smoke test timed out");
                    }
                } else if (smokeTick >= 120) {
                    LOGGER.info("Atmospherics Fabric smoke test completed after 120 client ticks");
                    client.stop();
                }'''
if old_stop not in source:
    raise SystemExit('Rendering smoke-test stop marker not found')
source = source.replace(old_stop, new_stop, 1)

helper_pattern = re.compile(
    r'\n      private static void runRenderingSmokeTest\(Minecraft client, int smokeTick\) \{.*?\n      \}\n(?=\n\})',
    re.DOTALL,
)
new_helper = r'''
      private static void runRenderingSmokeTest(Minecraft client, int smokeTick) {
          if (RENDERING_SMOKE_COMPLETE) {
              return;
          }

          RENDERING_SMOKE_STAGE_TICKS++;
          try {
              switch (RENDERING_SMOKE_STAGE) {
                  case 0 -> {
                      // Waiting for the real title screen ensures Minecraft's initial
                      // resource reload has finished before the config UI is exercised.
                      if (client.screen instanceof TitleScreen) {
                          FogConfigScreen screen = new FogConfigScreen(client.screen);
                          Field scalePromptField =
                                  FogConfigScreen.class.getDeclaredField("guiScalePromptHandled");
                          scalePromptField.setAccessible(true);
                          scalePromptField.setBoolean(screen, true);
                          client.setScreen(screen);
                          advanceRenderingSmokeStage("opened FogConfigScreen");
                      }
                  }
                  case 1 -> {
                      if (client.screen instanceof FogConfigScreen screen) {
                          Field cloudButtonField =
                                  FogConfigScreen.class.getDeclaredField("cloudsModeButton");
                          cloudButtonField.setAccessible(true);
                          Button cloudButton = (Button) cloudButtonField.get(screen);
                          if (cloudButton == null) {
                              throw new IllegalStateException("Cloud mode button was not initialized");
                          }
                          cloudButton.onPress();
                          advanceRenderingSmokeStage("pressed Clouds mode");
                      } else if (RENDERING_SMOKE_STAGE_TICKS > 100) {
                          throw new IllegalStateException("FogConfigScreen did not remain open");
                      }
                  }
                  case 2 -> {
                      if (client.screen instanceof FogConfigScreen screen) {
                          Field editModeField = FogConfigScreen.class.getDeclaredField("editMode");
                          editModeField.setAccessible(true);
                          Object editMode = editModeField.get(screen);
                          if (!(editMode instanceof Enum<?> mode) || !"CLOUDS".equals(mode.name())) {
                              throw new IllegalStateException(
                                      "Cloud mode callback did not select CLOUDS: " + editMode);
                          }

                          Field storyButtonField =
                                  FogConfigScreen.class.getDeclaredField("storyCloudSettingsButton");
                          storyButtonField.setAccessible(true);
                          Button storyButton = (Button) storyButtonField.get(screen);
                          if (storyButton == null) {
                              throw new IllegalStateException(
                                      "Story Mode Clouds settings button was not initialized");
                          }
                          storyButton.onPress();
                          advanceRenderingSmokeStage("opened Story Mode Clouds settings");
                      } else if (RENDERING_SMOKE_STAGE_TICKS > 100) {
                          throw new IllegalStateException(
                                  "FogConfigScreen did not remain open after selecting clouds");
                      }
                  }
                  case 3 -> {
                      if (client.screen != null
                              && client.screen.getClass().getName()
                                      .endsWith("$StoryCloudSettingsScreen")) {
                          LOGGER.info("Atmospherics cloud controls smoke test completed");
                          RENDERING_SMOKE_COMPLETE = true;
                      } else if (RENDERING_SMOKE_STAGE_TICKS > 100) {
                          throw new IllegalStateException(
                                  "Story Mode Clouds settings screen did not open; current screen="
                                          + (client.screen == null
                                                  ? "null"
                                                  : client.screen.getClass().getName()));
                      }
                  }
                  default -> throw new IllegalStateException(
                          "Unknown Atmospherics rendering smoke stage: " + RENDERING_SMOKE_STAGE);
              }
          } catch (ReflectiveOperationException error) {
              throw new RuntimeException(
                      "Atmospherics rendering compatibility smoke test failed", error);
          }
      }

      private static void advanceRenderingSmokeStage(String description) {
          RENDERING_SMOKE_STAGE++;
          RENDERING_SMOKE_STAGE_TICKS = 0;
          LOGGER.info(
                  "Atmospherics rendering smoke stage {}: {}",
                  RENDERING_SMOKE_STAGE,
                  description);
      }
'''
source, count = helper_pattern.subn('\n' + new_helper.strip('\n') + '\n', source, count=1)
if count != 1:
    raise SystemExit(f'Expected to replace one rendering smoke helper, replaced {count}')

client.write_text(source, encoding='utf-8')
