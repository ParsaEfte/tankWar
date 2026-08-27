package org.example.audio;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundManager {

    private AudioNode shootAudio;
    private AudioNode explosionAudio;
    private boolean muted = false;

    public SoundManager(AssetManager assetManager, Node rootNode) {
        try {
            shootAudio = new AudioNode(assetManager, "sounds/shoot.wav", AudioData.DataType.Buffer);
            shootAudio.setPositional(false);
            shootAudio.setLooping(false);
            shootAudio.setVolume(0.85f);
            rootNode.attachChild(shootAudio);

            explosionAudio = new AudioNode(assetManager, "sounds/explosion.wav", AudioData.DataType.Buffer);
            explosionAudio.setPositional(false);
            explosionAudio.setLooping(false);
            explosionAudio.setVolume(1.0f);
            rootNode.attachChild(explosionAudio);
        } catch (Exception e) {
            System.err.println("Audio initialization error: " + e.getMessage());
        }
    }

    public void playShoot() {
        if (!muted && shootAudio != null) {
            shootAudio.playInstance();
        }
    }

    public void playExplosion() {
        if (!muted && explosionAudio != null) {
            explosionAudio.playInstance();
        }
    }

    public void toggleMute() {
        muted = !muted;
    }

    public boolean isMuted() {
        return muted;
    }



}