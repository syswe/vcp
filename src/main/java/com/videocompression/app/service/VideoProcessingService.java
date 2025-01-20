package com.videocompression.app.service;

import com.videocompression.app.model.CompressionConfig;
import com.videocompression.app.model.CompressionResult;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoProcessingService {

    @Value("${vcp.output.directory:processed}")
    private String processedDir;

    @Value("${vcp.upload.directory:uploads}")
    private String uploadDir;

    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    @PostConstruct
    public void init() throws IOException {
        try {
            // Create directories first
            createDirectories();

            // Then initialize FFmpeg
            String ffmpegPath = detectFFmpegPath();
            String ffprobePath = detectFFprobePath();
            
            log.info("Using FFmpeg from: {}", ffmpegPath);
            log.info("Using FFprobe from: {}", ffprobePath);
            
            this.ffmpeg = new FFmpeg(ffmpegPath);
            this.ffprobe = new FFprobe(ffprobePath);
        } catch (Exception e) {
            log.error("Failed to initialize FFmpeg/FFprobe: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize video processing service. Please ensure FFmpeg is properly installed.", e);
        }
    }

    private void createDirectories() throws IOException {
        // Create paths in user's home directory
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path processedPath = userHome.resolve("VCP").resolve("processed");
        Path uploadPath = userHome.resolve("VCP").resolve("uploads");

        // Create directories if they don't exist
        Files.createDirectories(processedPath);
        Files.createDirectories(uploadPath);

        // Update paths to absolute paths
        processedDir = processedPath.toString();
        uploadDir = uploadPath.toString();

        log.info("Created directory: {}", processedPath);
        log.info("Created directory: {}", uploadPath);
    }

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

    private String detectFFmpegPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String[] possiblePaths;

        if (os.contains("win")) {
            possiblePaths = new String[]{
                "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                System.getenv("ProgramFiles") + "\\ffmpeg\\bin\\ffmpeg.exe"
            };
        } else if (os.contains("mac")) {
            possiblePaths = new String[]{
                "/opt/homebrew/bin/ffmpeg",
                "/usr/local/bin/ffmpeg",
                "/opt/local/bin/ffmpeg"
            };
        } else {
            // Linux and others
            possiblePaths = new String[]{
                "/usr/bin/ffmpeg",
                "/usr/local/bin/ffmpeg",
                "/opt/ffmpeg/bin/ffmpeg"
            };
        }

        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }

        throw new RuntimeException("FFmpeg not found. Please install FFmpeg and ensure it's in one of the expected locations.");
    }

    private String detectFFprobePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String[] possiblePaths;

        if (os.contains("win")) {
            possiblePaths = new String[]{
                "C:\\Program Files\\ffmpeg\\bin\\ffprobe.exe",
                "C:\\ffmpeg\\bin\\ffprobe.exe",
                System.getenv("ProgramFiles") + "\\ffmpeg\\bin\\ffprobe.exe"
            };
        } else if (os.contains("mac")) {
            possiblePaths = new String[]{
                "/opt/homebrew/bin/ffprobe",
                "/usr/local/bin/ffprobe",
                "/opt/local/bin/ffprobe"
            };
        } else {
            // Linux and others
            possiblePaths = new String[]{
                "/usr/bin/ffprobe",
                "/usr/local/bin/ffprobe",
                "/opt/ffmpeg/bin/ffprobe"
            };
        }

        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }

        throw new RuntimeException("FFprobe not found. Please install FFmpeg and ensure it's in one of the expected locations.");
    }

    public List<CompressionResult> processVideos(List<MultipartFile> files, CompressionConfig config) throws IOException {
        List<CompressionResult> allResults = new ArrayList<>();
        for (MultipartFile file : files) {
            allResults.addAll(processVideo(file, config));
        }
        return allResults;
    }

    public List<CompressionResult> processVideo(MultipartFile file) throws IOException {
        return processVideo(file, CompressionConfig.builder().build());
    }

    public List<CompressionResult> processVideo(MultipartFile file, CompressionConfig config) throws IOException {
        List<CompressionResult> results = new ArrayList<>();
        
        // Save uploaded file
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path uploadPath = Paths.get(uploadDir, uniqueFilename);
        Files.copy(file.getInputStream(), uploadPath);

        try {
            // Probe video information
            FFmpegProbeResult probeResult = ffprobe.probe(uploadPath.toString());
            FFmpegStream videoStream = probeResult.getStreams().get(0);
            int originalWidth = videoStream.width;
            int originalHeight = videoStream.height;
            long duration = Math.round(probeResult.getFormat().duration);
            long originalSize = file.getSize();
            String originalResolution = originalWidth + "x" + originalHeight;

            // Process based on configuration
            int targetWidth, targetHeight;
            String suffix;

            if (config.getTargetResolution() == CompressionConfig.Resolution.ORIGINAL) {
                targetWidth = originalWidth;
                targetHeight = originalHeight;
                suffix = config.getOutputFileSuffix();
            } else if (config.getTargetResolution() == CompressionConfig.Resolution.CUSTOM) {
                targetWidth = config.getCustomWidth();
                targetHeight = config.getCustomHeight();
                suffix = String.format("_%dx%d", targetWidth, targetHeight);
            } else {
                targetWidth = config.getTargetResolution().getWidth();
                targetHeight = config.getTargetResolution().getHeight();
                suffix = "_" + config.getTargetResolution().getLabel();
            }

            String outputPath = generateOutputPath(originalFilename, suffix, config.getOutputFormat());
            results.add(compressVideo(uploadPath.toString(), outputPath, targetWidth, targetHeight, 
                config, originalSize, originalResolution, duration));

        } finally {
            // Clean up uploaded file
            Files.deleteIfExists(uploadPath);
        }

        return results;
    }

    private String generateOutputPath(String originalFilename, String suffix, String format) {
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        return Paths.get(processedDir, baseName + suffix + "." + format).toString();
    }

    private CompressionResult compressVideo(String inputPath, String outputPath, 
            int targetWidth, int targetHeight, CompressionConfig config,
            long originalSize, String originalResolution, long duration) {
        
        // Calculate scale filter based on aspect ratio preference
        String scaleFilter;
        if (config.isMaintainAspectRatio()) {
            scaleFilter = String.format("scale=w=%d:h=%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2",
                targetWidth, targetHeight, targetWidth, targetHeight);
        } else {
            scaleFilter = String.format("scale=w=%d:h=%d", targetWidth, targetHeight);
        }

        FFmpegBuilder builder = new FFmpegBuilder()
            .setInput(inputPath)
            .overrideOutputFiles(true);

        if ("m3u8".equals(config.getOutputFormat())) {
            // For HLS, create a directory for segments
            String hlsDir = outputPath.substring(0, outputPath.lastIndexOf('.')) + "_hls";
            try {
                Files.createDirectories(Paths.get(hlsDir));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create HLS directory", e);
            }
            
            // Configure HLS output
            builder.addOutput(Paths.get(hlsDir, "stream.m3u8").toString())
                .setFormat("hls")
                .setVideoCodec("libx264")
                .setVideoFilter(scaleFilter)
                .setVideoFrameRate(config.getFrameRate())
                .setVideoBitRate(config.getPreset().getBitrate())
                .setAudioCodec("aac")
                .setAudioChannels(2)
                .setAudioBitRate(config.isPreserveAudioQuality() ? 192_000 : 128_000)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .addExtraArgs("-hls_time", "10") // 10 second segments
                .addExtraArgs("-hls_list_size", "0") // Keep all segments
                .addExtraArgs("-hls_segment_filename", Paths.get(hlsDir, "segment%d.ts").toString())
                .addExtraArgs("-preset", config.getPreset().getSpeed())
                .addExtraArgs("-crf", String.valueOf(config.getPreset().getCrf()))
                .done();
        } else {
            // Standard video output
            builder.addOutput(outputPath)
                .setVideoCodec("libx264")
                .setVideoFilter(scaleFilter)
                .setVideoFrameRate(config.getFrameRate())
                .setVideoBitRate(config.getPreset().getBitrate())
                .setAudioCodec("aac")
                .setAudioChannels(2)
                .setAudioBitRate(config.isPreserveAudioQuality() ? 192_000 : 128_000)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .addExtraArgs("-preset", config.getPreset().getSpeed())
                .addExtraArgs("-crf", String.valueOf(config.getPreset().getCrf()))
                .done();
        }

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        if ("m3u8".equals(config.getOutputFormat())) {
            // For HLS, calculate total size of all segments
            String hlsDir = outputPath.substring(0, outputPath.lastIndexOf('.')) + "_hls";
            long totalSize = 0;
            try {
                totalSize = Files.walk(Paths.get(hlsDir))
                    .filter(p -> p.toString().endsWith(".ts"))
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
            } catch (IOException e) {
                log.error("Error calculating HLS segments size", e);
            }

            return CompressionResult.builder()
                .fileName(new File(outputPath).getName())
                .resolution(targetWidth + "x" + targetHeight)
                .originalSize(originalSize)
                .compressedSize(totalSize)
                .compressionRatio(totalSize > 0 ? (double) originalSize / totalSize : 0.0)
                .originalResolution(originalResolution)
                .bitrate(config.getPreset().getBitrate())
                .duration(duration)
                .outputPath(outputPath.substring(0, outputPath.lastIndexOf('.')) + "_hls/stream.m3u8")
                .build();
        } else {
            File outputFile = new File(outputPath);
            return CompressionResult.builder()
                .fileName(new File(outputPath).getName())
                .resolution(targetWidth + "x" + targetHeight)
                .originalSize(originalSize)
                .compressedSize(outputFile.length())
                .compressionRatio((double) originalSize / outputFile.length())
                .originalResolution(originalResolution)
                .bitrate(config.getPreset().getBitrate())
                .duration(duration)
                .outputPath(outputPath)
                .build();
        }
    }

    public List<CompressionConfig.Resolution> getAvailableResolutions(MultipartFile file) throws IOException {
        // Save uploaded file temporarily
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path uploadPath = Paths.get(uploadDir, uniqueFilename);
        
        try {
            Files.copy(file.getInputStream(), uploadPath);
            
            // Probe video information
            FFmpegProbeResult probeResult = ffprobe.probe(uploadPath.toString());
            FFmpegStream videoStream = probeResult.getStreams().get(0);
            int width = videoStream.width;
            int height = videoStream.height;

            // Always include ORIGINAL resolution
            List<CompressionConfig.Resolution> availableResolutions = new ArrayList<>();
            availableResolutions.add(CompressionConfig.Resolution.ORIGINAL);

            // Add all resolutions that are smaller than or equal to the original video
            availableResolutions.addAll(
                Arrays.stream(CompressionConfig.Resolution.values())
                    .filter(res -> res != CompressionConfig.Resolution.ORIGINAL)
                    .filter(res -> res.getWidth() <= width && res.getHeight() <= height)
                    .collect(Collectors.toList())
            );

            return availableResolutions;
        } finally {
            Files.deleteIfExists(uploadPath);
        }
    }

    public void setCustomOutputDirectory(String outputDir) throws IOException {
        if (outputDir != null && !outputDir.isEmpty()) {
            this.processedDir = outputDir;
            Files.createDirectories(Paths.get(outputDir));
        }
    }

    public String getOutputDirectory() {
        return processedDir;
    }
} 