package com.videocompression.app.controller;

import com.videocompression.app.model.CompressionConfig;
import com.videocompression.app.model.CompressionResult;
import com.videocompression.app.service.VideoProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
public class VideoController {

    private final VideoProcessingService videoProcessingService;

    @Autowired
    public VideoController(VideoProcessingService videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("presets", CompressionConfig.CompressionPreset.values());
        model.addAttribute("resolutions", CompressionConfig.Resolution.values());
        model.addAttribute("frameRates", new int[]{24, 30, 60});
        return "index";
    }

    @PostMapping("/upload")
    public String uploadVideo(
            @RequestParam("video") List<MultipartFile> files,
            @RequestParam(value = "preset", defaultValue = "MEDIUM") CompressionConfig.CompressionPreset preset,
            @RequestParam(value = "resolutions", required = false) List<String> resolutions,
            @RequestParam(value = "frameRate", defaultValue = "30") int frameRate,
            @RequestParam(value = "maintainOriginalSize", defaultValue = "false") boolean maintainOriginalSize,
            @RequestParam(value = "preserveAudioQuality", defaultValue = "true") boolean preserveAudioQuality,
            @RequestParam(value = "outputFormat", defaultValue = "mp4") String outputFormat,
            @RequestParam(value = "outputDirectory", required = false) String outputDirectory,
            RedirectAttributes redirectAttributes) {
        
        if (files.isEmpty() || files.get(0).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one video file to upload");
            return "redirect:/";
        }

        if (resolutions == null || resolutions.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one target resolution");
            return "redirect:/";
        }

        try {
            if (outputDirectory != null && !outputDirectory.isEmpty()) {
                videoProcessingService.setCustomOutputDirectory(outputDirectory);
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

            redirectAttributes.addFlashAttribute("results", allResults);
            redirectAttributes.addFlashAttribute("success", 
                String.format("Processed %d video(s) successfully! Generated %d version(s).", 
                    files.size(), allResults.size()));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error processing video: " + e.getMessage());
        }

        return "redirect:/";
    }
} 