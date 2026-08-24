/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.SimpleParticleType
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.biome.Biomes
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.material.FogType
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.beash.atmospherics.mixin;

import com.beash.atmospherics.Atmospherics;
import com.beash.atmospherics.config.AirHazeSettings;
import com.beash.atmospherics.particle.AirHazeBiomeSettings;
import com.beash.atmospherics.particle.AtmosphericsParticles;
import com.beash.atmospherics.particle.WindStreakTuning;
import com.beash.atmospherics.util.DryStormBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientLevel.class})
public abstract class AmbientFlowMixin {
    private static final int MINIMUM_WIND_HEIGHT = 83;
    private static final double CLEAR_MULTIPLIER = 0.015;
    private static final double RAIN_MULTIPLIER = 0.0225;
    private static final double THUNDER_MULTIPLIER = 0.03;
    private static final double CLEAR_DRIFT_X = 0.45;
    private static final double STORM_DRIFT_X = 1.25;
    private static final double DRIFT_Z_SPREAD = 0.03;

    @Inject(method={"getSkyFlashTime"}, at={@At(value="HEAD")}, cancellable=true)
    private void atmospherics$hideDryStormLightningFlash(CallbackInfoReturnable<Integer> cir) {
        ClientLevel world = (ClientLevel)this;
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && world.isThundering() && DryStormBiomes.isAt(world, client.player.blockPosition())) {
            cir.setReturnValue((Object)0);
        }
    }

    @Inject(method={"setSkyFlashTime"}, at={@At(value="HEAD")}, cancellable=true)
    private void atmospherics$blockDryStormLightningFlash(int lightningTicksLeft, CallbackInfo ci) {
        ClientLevel world = (ClientLevel)this;
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && world.isThundering() && DryStormBiomes.isAt(world, client.player.blockPosition())) {
            ci.cancel();
        }
    }

    @Inject(method={"playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void atmospherics$muteDryStormLightningSound(double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
        if (sound == SoundEvents.LIGHTNING_BOLT_THUNDER || sound == SoundEvents.LIGHTNING_BOLT_IMPACT) {
            ClientLevel world = (ClientLevel)this;
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null && world.isThundering() && DryStormBiomes.isAt(world, client.player.blockPosition())) {
                ci.cancel();
            }
        }
    }

    @Inject(method={"doAnimateTick"}, at={@At(value="TAIL")})
    private void atmospherics$spawnWind(int centerX, int centerY, int centerZ, int radius, RandomSource random, Block block, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
        ClientLevel world = (ClientLevel)this;
        WindStreakTuning.sanitize();
        if (WindStreakTuning.enabled && world.dimension() == Level.OVERWORLD && pos.getY() >= 83 && world.canSeeSky((BlockPos)pos)) {
            double multiplier = world.isThundering() ? 0.03 : (world.isRaining() ? 0.0225 : 0.015);
            double frequency = Math.max(0.1, (double)(WindStreakTuning.baseChance / 0.15f));
            if (!(random.nextDouble() * 100.0 > frequency * multiplier)) {
                boolean strong = world.isRaining() || world.isThundering();
                double driftX = strong ? 1.25 : 0.45;
                double driftZ = (random.nextDouble() - 0.5) * 0.03;
                world.addParticle(strong ? (ParticleOptions)AtmosphericsParticles.STRONG_WIND_STREAK.get() : (ParticleOptions)AtmosphericsParticles.WIND_STREAK.get(), (double)pos.getX() + random.nextDouble(), (double)pos.getY() + random.nextDouble(), (double)pos.getZ() + random.nextDouble(), driftX, 0.0, driftZ);
            }
        }
    }

    @Inject(method={"doAnimateTick"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", shift=At.Shift.AFTER)}, require=0)
    private void atmospherics$spawnPolytoneMist(int centerX, int centerY, int centerZ, int radius, RandomSource random, Block block, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
        ClientLevel world = (ClientLevel)this;
        if (!Atmospherics.getConfig().airHazeEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gameRenderer != null && mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        Block actualBlock = world.getBlockState((BlockPos)pos).getBlock();
        if (!(actualBlock != Blocks.AIR && actualBlock != Blocks.GRASS_BLOCK && actualBlock != Blocks.SAND && actualBlock != Blocks.RED_SAND && actualBlock != Blocks.END_STONE && actualBlock != Blocks.DIRT_PATH && actualBlock != Blocks.PODZOL && actualBlock != Blocks.MUD && actualBlock != Blocks.SNOW_BLOCK && actualBlock != Blocks.MYCELIUM || actualBlock == Blocks.AIR && world.getBrightness(LightLayer.SKY, (BlockPos)pos) > 0)) {
            AirHazeSettings settings = AirHazeBiomeSettings.current();
            if (settings.enabled) {
                AmbientFlowMixin.spawnMist(world, random, (BlockPos)pos, (SimpleParticleType)AtmosphericsParticles.FOG.get(), 0.12, 2.0, AmbientFlowMixin.randomMistDy(random), true);
            }
        }
    }

    private static double randomMistDy(RandomSource random) {
        return 0.001 + random.nextDouble() * 0.001;
    }

    private static void spawnMist(ClientLevel world, RandomSource random, BlockPos pos, SimpleParticleType particle, double chance, double yOffset, double dy, boolean lowerCorner) {
        AmbientFlowMixin.spawnMist(world, random, pos, particle, chance, yOffset, dy, lowerCorner, 0.0, 0.0);
    }

    private static void spawnMist(ClientLevel world, RandomSource random, BlockPos pos, SimpleParticleType particle, double chance, double yOffset, double dy, boolean lowerCorner, double xOffset, double zOffset) {
        double spawnRate = Math.max(0.0, Math.min(3.0, (double)AirHazeBiomeSettings.current().spawnRate));
        if (!(random.nextDouble() >= Math.min(1.0, chance * spawnRate))) {
            double spawnBase = lowerCorner ? 0.0 : 0.5;
            double x = (double)pos.getX() + spawnBase + xOffset;
            double y = (double)pos.getY() + spawnBase + yOffset;
            double z = (double)pos.getZ() + spawnBase + zOffset;
            double dx = random.nextDouble() / 75.0 * 0.1;
            double dz = random.nextDouble() / 75.0 * 0.1;
            world.addParticle((ParticleOptions)particle, true, x, y, z, dx, dy, dz);
        }
    }

    private static boolean isForestMistBiome(ClientLevel world, BlockPos pos) {
        return AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.FOREST) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.TAIGA) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SNOWY_TAIGA) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.OLD_GROWTH_PINE_TAIGA) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.OLD_GROWTH_SPRUCE_TAIGA) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.FLOWER_FOREST) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.OLD_GROWTH_BIRCH_FOREST) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DARK_FOREST) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.BIRCH_FOREST);
    }

    private static boolean isPlainsMistBiome(ClientLevel world, BlockPos pos) {
        return AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.PLAINS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SUNFLOWER_PLAINS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.MUSHROOM_FIELDS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.MEADOW) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.CHERRY_GROVE) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SNOWY_PLAINS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SNOWY_SLOPES) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SAVANNA) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SAVANNA_PLATEAU) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.WINDSWEPT_SAVANNA);
    }

    private static boolean isJungleMistBiome(ClientLevel world, BlockPos pos) {
        return AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.JUNGLE) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SPARSE_JUNGLE) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.BAMBOO_JUNGLE);
    }

    private static boolean isRainMistBiome(ClientLevel world, BlockPos pos) {
        return AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.WINDSWEPT_HILLS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.WINDSWEPT_FOREST) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.WINDSWEPT_GRAVELLY_HILLS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.STONY_SHORE) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.RIVER) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.BEACH) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.COLD_OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DEEP_COLD_OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DARK_FOREST) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DEEP_OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.WARM_OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.LUKEWARM_OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DEEP_LUKEWARM_OCEAN);
    }

    private static boolean isCaveMistBiome(ClientLevel world, BlockPos pos) {
        return AmbientFlowMixin.isRainMistBiome(world, pos) || AmbientFlowMixin.isForestMistBiome(world, pos) || AmbientFlowMixin.isPlainsMistBiome(world, pos) || AmbientFlowMixin.isJungleMistBiome(world, pos) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.BADLANDS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.ERODED_BADLANDS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.WOODED_BADLANDS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DESERT) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DRIPSTONE_CAVES) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.LUSH_CAVES) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.MANGROVE_SWAMP) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SNOWY_BEACH) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.FROZEN_RIVER) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.FROZEN_OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.FROZEN_PEAKS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.DEEP_FROZEN_OCEAN) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.ICE_SPIKES) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.JAGGED_PEAKS) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.GROVE) || AmbientFlowMixin.biomeMatches(world, pos, (ResourceKey<Biome>)Biomes.SWAMP);
    }

    private static boolean biomeMatches(ClientLevel world, BlockPos pos, ResourceKey<Biome> biome) {
        return world.getBiome(pos).is(biome);
    }
}

