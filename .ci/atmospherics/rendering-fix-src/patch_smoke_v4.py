from pathlib import Path

client = Path(
    '.ci/atmospherics/work/src/main/java/com/beash/atmospherics/AtmosphericsFabricClient.java'
)
source = client.read_text(encoding='utf-8')

old_stop = '''                if (smokeTick >= 120) {
                    LOGGER.info("Atmospherics Fabric smoke test completed after 120 client ticks");
                    client.stop();
                }'''
new_stop = '''                if (smokeTick >= 260) {
                    LOGGER.info("Atmospherics Fabric smoke test completed after 260 client ticks");
                    client.stop();
                }'''
if old_stop not in source:
    raise SystemExit('Old rendering smoke stop marker not found')
source = source.replace(old_stop, new_stop, 1)

old_helper = r'''      private static void runRenderingSmokeTest(Minecraft client, int smokeTick) {
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
new_helper = r'''      private static void runRenderingSmokeTest(Minecraft client, int smokeTick) {
          try {
              // The initial resource reload can replace screens opened too early. Retry the
              // real screen after startup, then exercise both restored button callbacks.
              if (smokeTick >= 120 && smokeTick <= 180) {
                  if (!(client.screen instanceof FogConfigScreen)) {
                      client.setScreen(new FogConfigScreen(client.screen));
                  }
              } else if (smokeTick == 190) {
                  if (!(client.screen instanceof FogConfigScreen screen)) {
                      throw new IllegalStateException("FogConfigScreen did not remain open after resource reload");
                  }
                  Field cloudButtonField = FogConfigScreen.class.getDeclaredField("cloudsModeButton");
                  cloudButtonField.setAccessible(true);
                  Button cloudButton = (Button) cloudButtonField.get(screen);
                  cloudButton.onPress();
              } else if (smokeTick == 215) {
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
              } else if (smokeTick == 240) {
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
if old_helper not in source:
    raise SystemExit('Old rendering smoke helper not found')
source = source.replace(old_helper, new_helper, 1)
client.write_text(source, encoding='utf-8')
