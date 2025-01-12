package com.videocompression.app.controller;

import com.videocompression.app.model.CompressionConfig;
import com.videocompression.app.model.CompressionResult;
import com.videocompression.app.service.VideoProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Video Compression API", description = "API endpoints for video compression and management")
public class ApiController {

    private final VideoProcessingService videoProcessingService;

    @Autowired
    public ApiController(VideoProcessingService videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @Operation(
        summary = "Compress video files",
        description = "Upload and compress one or more video files with specified compression settings"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Videos processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error during processing")
    })
    @PostMapping(value = "/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<CompressionResult>> compressVideo(
            @Parameter(description = "Video files to compress", required = true)
            @RequestParam("files") List<MultipartFile> files,
            
            @Parameter(description = "Compression quality preset (LOW/MEDIUM/HIGH)")
            @RequestParam(value = "preset", defaultValue = "MEDIUM") CompressionConfig.CompressionPreset preset,
            
            @Parameter(description = "Target resolutions (e.g., ORIGINAL, FHD, HD)")
            @RequestParam(value = "resolutions", required = false) List<String> resolutions,
            
            @Parameter(description = "Target frame rate (fps)")
            @RequestParam(value = "frameRate", defaultValue = "30") int frameRate,
            
            @Parameter(description = "Whether to maintain the original file size")
            @RequestParam(value = "maintainOriginalSize", defaultValue = "false") boolean maintainOriginalSize,
            
            @Parameter(description = "Whether to preserve original audio quality")
            @RequestParam(value = "preserveAudioQuality", defaultValue = "true") boolean preserveAudioQuality,
            
            @Parameter(description = "Output video format (mp4, webm, mkv)")
            @RequestParam(value = "outputFormat", defaultValue = "mp4") String outputFormat) {
        
        try {
            List<CompressionResult> results = processVideos(files, preset, resolutions, frameRate,
                maintainOriginalSize, preserveAudioQuality, outputFormat);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Detect available resolutions",
        description = "Analyze a video file and return available compression resolutions"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available resolutions detected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid video file or processing error")
    })
    @PostMapping("/detect-resolutions")
    public ResponseEntity<List<CompressionConfig.Resolution>> detectResolutions(
            @Parameter(description = "Video file to analyze", required = true)
            @RequestParam("video") MultipartFile file) {
        try {
            List<CompressionConfig.Resolution> resolutions = videoProcessingService.getAvailableResolutions(file);
            return ResponseEntity.ok(resolutions);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Get output directory",
        description = "Retrieve the current output directory for compressed videos"
    )
    @GetMapping("/output-directory")
    public ResponseEntity<Map<String, String>> getOutputDirectory() {
        return ResponseEntity.ok(Map.of("directory", videoProcessingService.getOutputDirectory()));
    }

    @Operation(
        summary = "Set output directory",
        description = "Set a custom output directory for compressed videos"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Output directory updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid directory path")
    })
    @PostMapping("/output-directory")
    public ResponseEntity<Map<String, String>> setOutputDirectory(
            @Parameter(description = "New output directory path", required = true)
            @RequestParam String directory) {
        try {
            videoProcessingService.setCustomOutputDirectory(directory);
            return ResponseEntity.ok(Map.of("directory", videoProcessingService.getOutputDirectory()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private List<CompressionResult> processVideos(List<MultipartFile> files, CompressionConfig.CompressionPreset preset,
            List<String> resolutions, int frameRate, boolean maintainOriginalSize, boolean preserveAudioQuality,
            String outputFormat) throws IOException {
        
        if (resolutions == null || resolutions.isEmpty()) {
            resolutions = List.of("ORIGINAL");
        }

        List<CompressionResult> allResults = new ArrayList<>();
        
        for (String resolution : resolutions) {
            CompressionConfig.Resolution targetResolution;
            Integer customWidth = null;
            Integer customHeight = null;
            boolean maintainAspectRatio = true;

            if (resolution.startsWith("CUSTOM_")) {
                String[] parts = resolution.substring(7).split("_");
                String[] dimensions = parts[0].split("x");
                customWidth = Integer.parseInt(dimensions[0]);
                customHeight = Integer.parseInt(dimensions[1]);
                maintainAspectRatio = parts.length > 1 && parts[1].equals("ASPECT");
                targetResolution = CompressionConfig.Resolution.CUSTOM;
            } else {
                targetResolution = CompressionConfig.Resolution.valueOf(resolution);
            }

            CompressionConfig config = CompressionConfig.builder()
                .preset(preset)
                .targetResolution(targetResolution)
                .customWidth(customWidth)
                .customHeight(customHeight)
                .maintainAspectRatio(maintainAspectRatio)
                .frameRate(frameRate)
                .maintainOriginalSize(maintainOriginalSize)
                .preserveAudioQuality(preserveAudioQuality)
                .outputFormat(outputFormat)
                .build();

            for (MultipartFile file : files) {
                allResults.addAll(videoProcessingService.processVideo(file, config));
            }
        }

        return allResults;
    }
} 