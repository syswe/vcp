package com.videocompression.app.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompressionResult {
    private final String fileName;
    private final String resolution;
    private final long originalSize;
    private final long compressedSize;
    private final double compressionRatio;
    private final String originalResolution;
    private final int bitrate;
    private final long duration;
    private final String outputPath;
} 