package com.TheRPGAdventurer.ROTD.entity.breath.sound;

import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Created by TGG on 24/06/2016.
 * ComponentSound is a single sound that can
 * 1) loop repeatedly
 * 2) move position
 * 3) change volume
 * 4) turn on/off
 * <p>
 * Typical usage:
 * 1a) Optional: preload the sound in advance (reduces lag between triggering sound and it actually playing) using
 * createComponentSound (ResourceLocation, PRELOAD)
 * 1b) createComponentSoundPreload to create  the sound with an initial volume, repeat yes/no, plus a
 * ComponentSoundSettings that will be used to read the updated settings while the sound is playing
 * 2)  Minecraft.getMinecraft().getSoundHandler().playSound(sound);
 * 3)  Update the ComponentSoundSettings object as the sound properties change.  Minecraft automatically ticks the
 * sound which will update itself to the new properties.
 * 3b)  getPlayCountdown()/setPlayCountdown() can be used by the creator to determine how long the sound has been
 * playing
 * 3c) isDonePlaying()/setDonePlaying() can be used to stop the sound without destructing it
 */
class ComponentSound extends PositionedSound implements ITickableSound {
    /**
     * Create a ComponentSound for the given sound.
     *
     * @param i_resourceLocation null means silence
     * @param initialVolume
     * @param i_repeat
     * @param i_soundSettings
     * @return
     */
    static public ComponentSound createComponentSound(ResourceLocation i_resourceLocation, float initialVolume, RepeatType i_repeat, ComponentSoundSettings i_soundSettings) {
        //        if (i_resourceLocation == null) {
        //            return new ComponentSoundSilent();
        //        }
        return new ComponentSound(i_resourceLocation, initialVolume, i_repeat, i_soundSettings);
    }

    /**
     * Create a preload ComponentSound for the given sound. (reduces lag between triggering sound and it actually playing)
     *
     * @param i_resourceLocation null means silence
     */
    static public ComponentSound createComponentSoundPreload(ResourceLocation i_resourceLocation) {
        //        if (i_resourceLocation == null) {
        //            return new ComponentSoundSilent();
        //        }
        return new ComponentSound(i_resourceLocation);
    }

    /**
     * Create a ComponentSound for the given sound.
     *
     * @param soundEffectName null means silence
     * @param initialVolume
     * @param i_repeat
     * @param i_soundSettings
     * @return
     */
    static public ComponentSound createComponentSound(SoundEffectNames soundEffectName, float initialVolume, RepeatType i_repeat, ComponentSoundSettings i_soundSettings) {
//        if (soundEffectName==null) {
//            return new ComponentSoundSilent();
//        }
        ResourceLocation resourceLocation=new ResourceLocation(soundEffectName.getJsonName());
        return new ComponentSound(resourceLocation, initialVolume, i_repeat, i_soundSettings);
    }

    /**
     * Create a preload ComponentSound for the given sound. (reduces lag between triggering sound and it actually playing)
     *
     * @param soundEffectName null means silence
     */
    static public ComponentSound createComponentSoundPreload(SoundEffectNames soundEffectName) {
//        if (soundEffectName==null) {
//            return new ComponentSoundSilent();
//        }
        ResourceLocation resourceLocation=new ResourceLocation(soundEffectName.getJsonName());
        return new ComponentSound(resourceLocation);
    }

    /**
     * Creates the sound ready for playing
     *
     * @param i_resourceLocation
     * @param initialVolume
     * @param i_repeat
     * @param i_soundSettings
     */
    protected ComponentSound(ResourceLocation i_resourceLocation, float initialVolume, RepeatType i_repeat, ComponentSoundSettings i_soundSettings) {
        super(i_resourceLocation, SoundCategory.HOSTILE);
        repeat=(i_repeat==RepeatType.REPEAT);
        volume=initialVolume;
        attenuationType=AttenuationType.NONE;
        soundSettings=i_soundSettings;
        playMode=Mode.PLAY;
    }

    /**
     * Preload for this sound (plays at very low volume).
     *
     * @param i_resourceLocation the sound to be played
     */
    protected ComponentSound(ResourceLocation i_resourceLocation) {
        super(i_resourceLocation, SoundCategory.HOSTILE);
        repeat=false;
        final float VERY_LOW_VOLUME=0.001F;
        volume=VERY_LOW_VOLUME;
        attenuationType=AttenuationType.NONE;
        soundSettings=null;
        playMode=Mode.PRELOAD;
        preloadTimeCountDown=5;  // play for a few ticks only
    }

    // settings for each component sound
    protected static class ComponentSoundSettings {
        public ComponentSoundSettings(float i_volume) {
            masterVolume=i_volume;
        }

        public float masterVolume;  // multiplier for the volume = 0 .. 1
        public Vec3d soundEpicentre;
        public float playerDistanceToEpicentre;
        public boolean playing;
    }

    public enum RepeatType {REPEAT, NO_REPEAT}

    public enum Mode {PRELOAD, PLAY}


    public int getPlayCountdown() {
        return playTimeCountDown;
    }

    public void setPlayCountdown(int countdown) {
        playTimeCountDown=countdown;
    }

    @Override
    public boolean isDonePlaying() {
        return donePlaying;
    }

    public void setDonePlaying() {
        donePlaying=true;
    }

    @Override
    public void update() {
        final float OFF_VOLUME=0.0F;

        if (playMode==Mode.PRELOAD) {
            if (--preloadTimeCountDown <= 0) {
                this.volume=OFF_VOLUME;
            }
            return;
        }

        --playTimeCountDown;
        if (!soundSettings.playing) {
            this.volume=OFF_VOLUME;
        } else {
            this.xPosF=(float) soundSettings.soundEpicentre.x;
            this.yPosF=(float) soundSettings.soundEpicentre.y;
            this.zPosF=(float) soundSettings.soundEpicentre.z;
            this.volume=soundSettings.masterVolume * volumeAdjustmentForDistance(soundSettings.playerDistanceToEpicentre);
        }
    }

    public static float volumeAdjustmentForDistance(float distanceToEpicentre) {
        final float MINIMUM_VOLUME=0.10F;
        final float MAXIMUM_VOLUME=1.00F;
        final float INSIDE_VOLUME=1.00F;
        if (distanceToEpicentre < 0.01F) {
            return INSIDE_VOLUME;
        } else {
            final float MINIMUM_VOLUME_DISTANCE=40.0F;
            float fractionToMinimum=distanceToEpicentre / MINIMUM_VOLUME_DISTANCE;
            return MathX.clamp(MAXIMUM_VOLUME - fractionToMinimum * (MAXIMUM_VOLUME - MINIMUM_VOLUME), MINIMUM_VOLUME, MAXIMUM_VOLUME);
        }

    }

    private int playTimeCountDown=-1;
    private int preloadTimeCountDown=0;
    private boolean donePlaying;
    private ComponentSoundSettings soundSettings;
    private Mode playMode;

}