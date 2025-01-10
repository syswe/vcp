package com.videocompression.app.controller;

import com.videocompression.app.model.CompressionResult;
import com.videocompression.app.service.VideoProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
public class VideoController {

    private final VideoProcessingService videoProcessingService;

    @Autowired
    public VideoController(VideoProcessingService videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadVideo(@RequestParam("video") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a video file to upload");
            return "redirect:/";
        }

        try {
            List<CompressionResult> results = videoProcessingService.processVideo(file);
            redirectAttributes.addFlashAttribute("results", results);
            redirectAttributes.addFlashAttribute("success", 
                "Video processed successfully! Generated " + results.size() + " versions.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error processing video: " + e.getMessage());
        }

        return "redirect:/";
    }
} 