package com.videocompression.app.service;

import com.videocompression.app.model.CompressionResult;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class VideoProcessingService {

    private final String uploadDir = "uploads";
    private final String processedDir = "processed";
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    // Define quality presets for different resolutions
    private static final class QualityPreset {
        final int width;
        final int height;
        final long bitrate;
        final String suffix;

        QualityPreset(int width, int height, long bitrate, String suffix) {
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
            this.suffix = suffix;
        }
    }

    private static final QualityPreset[] QUALITY_PRESETS = {
        new QualityPreset(1920, 1080, 5_000_000, "1080p"), // 5 Mbps for 1080p
        new QualityPreset(1280, 720, 2_500_000, "720p"),   // 2.5 Mbps for 720p
        new QualityPreset(854, 480, 1_000_000, "480p"),    // 1 Mbps for 480p
        new QualityPreset(640, 360, 750_000, "360p")       // 750 Kbps for 360p
    };

    public VideoProcessingService() throws IOException {
        this.ffmpeg = new FFmpeg("/opt/homebrew/bin/ffmpeg");
        this.ffprobe = new FFprobe("/opt/homebrew/bin/ffprobe");
        createDirectories();
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        Files.createDirectories(Paths.get(processedDir));
    }

    public List<CompressionResult> processVideo(MultipartFile file) throws IOException {
        List<CompressionResult> results = new ArrayList<>();
        
        // Save uploaded file
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path uploadPath = Paths.get(uploadDir, uniqueFilename);
        Files.copy(file.getInputStream(), uploadPath);

        // Probe video information
        FFmpegProbeResult probeResult = ffprobe.probe(uploadPath.toString());
        FFmpegStream videoStream = probeResult.getStreams().get(0);
        int width = videoStream.width;
        int height = videoStream.height;
        long duration = Math.round(probeResult.getFormat().duration);
        long originalSize = file.getSize();
        String originalResolution = width + "x" + height;

        // Calculate target resolutions based on aspect ratio
        List<QualityPreset> targetPresets = new ArrayList<>();
        for (QualityPreset preset : QUALITY_PRESETS) {
            if (width > preset.width || height > preset.height) {
                targetPresets.add(preset);
            }
        }

        if (targetPresets.isEmpty()) {
            // Just compress the original video with appropriate bitrate
            String outputPath = Paths.get(processedDir, "compressed_" + uniqueFilename).toString();
            long targetBitrate = calculateBitrate(width, height);
            compressVideo(uploadPath.toString(), outputPath, targetBitrate);
            
            File outputFile = new File(outputPath);
            results.add(CompressionResult.builder()
                .fileName(originalFilename)
                .resolution(originalResolution)
                .originalSize(originalSize)
                .compressedSize(outputFile.length())
                .compressionRatio((double) originalSize / outputFile.length())
                .originalResolution(originalResolution)
                .bitrate((int) targetBitrate)
                .duration(duration)
                .outputPath(outputPath)
                .build());
        } else {
            results.addAll(convertVideo(uploadPath, fileExtension, targetPresets, originalFilename, originalSize, originalResolution, duration));
        }

        // Delete the uploaded file
        Files.deleteIfExists(uploadPath);
        return results;
    }

    private List<CompressionResult> convertVideo(Path inputPath, String fileExtension, List<QualityPreset> presets,
                                               String originalFilename, long originalSize, String originalResolution,
                                               long duration) {
        List<CompressionResult> results = new ArrayList<>();
        String baseName = UUID.randomUUID().toString();

        for (QualityPreset preset : presets) {
            String outputFilename = String.format("%s_%s%s", baseName, preset.suffix, fileExtension);
            String outputPath = Paths.get(processedDir, outputFilename).toString();

            // Use scale filter with 'fit' mode to maintain aspect ratio perfectly
            String scaleFilter = String.format("scale=w=%d:h=%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2",
                preset.width, preset.height, preset.width, preset.height);

            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputPath.toString())
                .overrideOutputFiles(true)
                .addOutput(outputPath)
                .setVideoCodec("libx264")
                .setVideoFilter(scaleFilter)
                .setVideoFrameRate(30)
                .setVideoBitRate(preset.bitrate)
                .setAudioCodec("aac")
                .setAudioChannels(2)
                .setAudioBitRate(128_000)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .addExtraArgs("-preset", "slow") // Better quality encoding
                .addExtraArgs("-crf", "23") // Constant Rate Factor for better quality
                .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();
            
            File outputFile = new File(outputPath);
            results.add(CompressionResult.builder()
                .fileName(originalFilename)
                .resolution(preset.width + "x" + preset.height)
                .originalSize(originalSize)
                .compressedSize(outputFile.length())
                .compressionRatio((double) originalSize / outputFile.length())
                .originalResolution(originalResolution)
                .bitrate((int) preset.bitrate)
                .duration(duration)
                .outputPath(outputPath)
                .build());
        }

        return results;
    }

    private void compressVideo(String inputPath, String outputPath, long bitrate) {
        FFmpegBuilder builder = new FFmpegBuilder()
            .setInput(inputPath)
            .overrideOutputFiles(true)
            .addOutput(outputPath)
            .setVideoCodec("libx264")
            .setVideoFrameRate(30)
            .setVideoBitRate(bitrate)
            .setAudioCodec("aac")
            .setAudioChannels(2)
            .setAudioBitRate(128_000)
            .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
            .addExtraArgs("-preset", "slow") // Better quality encoding
            .addExtraArgs("-crf", "23") // Constant Rate Factor for better quality
            .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();
    }

    private long calculateBitrate(int width, int height) {
        // Calculate appropriate bitrate based on resolution
        long pixels = width * height;
        if (pixels > 2073600) // > 1080p
            return 5_000_000;
        else if (pixels > 921600) // > 720p
            return 2_500_000;
        else if (pixels > 409920) // > 480p
            return 1_000_000;
        else
            return 750_000;
    }
} 