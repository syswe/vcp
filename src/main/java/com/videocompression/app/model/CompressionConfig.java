package com.videocompression.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompressionConfig {
    public enum CompressionPreset {
        LOW(500_000, "slow", 28),      // Lower quality, smaller file
        MEDIUM(2_000_000, "medium", 23), // Balanced
        HIGH(5_000_000, "veryslow", 18);  // Higher quality, larger file

        private final int bitrate;
        private final String speed;
        private final int crf;

        CompressionPreset(int bitrate, String speed, int crf) {
            this.bitrate = bitrate;
            this.speed = speed;
            this.crf = crf;
        }

        public int getBitrate() { return bitrate; }
        public String getSpeed() { return speed; }
        public int getCrf() { return crf; }
    }

    public enum Resolution {
        ORIGINAL(0, 0, "original"),
        CUSTOM(0, 0, "custom"),  // For custom resolutions
        UHD_4K(3840, 2160, "4k"),
        QHD(2560, 1440, "1440p"),
        FHD(1920, 1080, "1080p"),
        HD(1280, 720, "720p"),
        SD(854, 480, "480p"),
        LOW(640, 360, "360p");

        private final int width;
        private final int height;
        private final String label;

        Resolution(int width, int height, String label) {
            this.width = width;
            this.height = height;
            this.label = label;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public String getLabel() { return label; }
    }

    @Builder.Default
    private CompressionPreset preset = CompressionPreset.MEDIUM;
    
    @Builder.Default
    private Resolution targetResolution = Resolution.ORIGINAL;
    
    // Custom resolution support
    private Integer customWidth;
    private Integer customHeight;
    
    @Builder.Default
    private boolean maintainAspectRatio = true;
    
    @Builder.Default
    private int frameRate = 30;
    
    @Builder.Default
    private boolean maintainOriginalSize = false;
    
    @Builder.Default
    private String outputFormat = "mp4";
    
    @Builder.Default
    private boolean preserveAudioQuality = true;
    
    @Builder.Default
    private String outputFileSuffix = "_compressed";

    // Helper method to get actual resolution dimensions
    public int getEffectiveWidth() {
        return targetResolution == Resolution.CUSTOM ? customWidth : targetResolution.getWidth();
    }

    public int getEffectiveHeight() {
        return targetResolution == Resolution.CUSTOM ? customHeight : targetResolution.getHeight();
    }
} 