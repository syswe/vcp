# Video Compression Platform API Documentation

This document provides detailed information about the Video Compression Platform (VCP) API endpoints, including request formats, parameters, and example responses.

## Table of Contents

- [Authentication](#authentication)
- [Base URL](#base-url)
- [Endpoints](#endpoints)
  - [Standard Compression](#standard-compression)
  - [Multi-Resolution Compression](#multi-resolution-compression)
  - [HLS Streaming](#hls-streaming)
  - [Format Conversion](#format-conversion)
  - [Custom Resolution](#custom-resolution)
  - [Available Resolutions](#available-resolutions)
- [Response Formats](#response-formats)
- [Error Handling](#error-handling)

## Authentication

Currently, the API is designed for internal use and doesn't require authentication. If implementing in a production environment, consider adding appropriate authentication mechanisms.

## Base URL

```
http://localhost:8080/api
```

## Endpoints

### Standard Compression

Compresses a video file to a single target resolution.

```http
POST /compress
Content-Type: multipart/form-data
```

#### Parameters

| Parameter            | Type    | Required | Description            | Default    | Options                                     |
| -------------------- | ------- | -------- | ---------------------- | ---------- | ------------------------------------------- |
| file                 | File    | Yes      | Video file to compress | -          | -                                           |
| targetResolution     | String  | No       | Target resolution      | "ORIGINAL" | "1080p", "720p", "480p", "360p", "ORIGINAL" |
| outputFormat         | String  | No       | Output format          | "mp4"      | "mp4", "webm", "mkv", "m3u8"                |
| maintainAspectRatio  | Boolean | No       | Maintain aspect ratio  | true       | true, false                                 |
| preserveAudioQuality | Boolean | No       | Preserve audio quality | true       | true, false                                 |
| frameRate            | Integer | No       | Target frame rate      | 30         | 1-60                                        |
| preset               | String  | No       | Compression preset     | "MEDIUM"   | "FAST", "MEDIUM", "SLOW"                    |

#### Example Request

```bash
curl -X POST http://localhost:8080/api/compress \
  -F "file=@video.mp4" \
  -F "targetResolution=720p" \
  -F "outputFormat=mp4" \
  -F "maintainAspectRatio=true" \
  -F "preserveAudioQuality=true" \
  -F "frameRate=30" \
  -F "preset=MEDIUM"
```

#### Example Response

```json
{
  "success": true,
  "result": {
    "fileName": "processed_video.mp4",
    "resolution": "1280x720",
    "originalSize": 20971520,
    "compressedSize": 10485760,
    "compressionRatio": 2.0,
    "originalResolution": "1920x1080",
    "bitrate": 2000000,
    "duration": 180,
    "outputPath": "/processed/processed_video.mp4"
  }
}
```

### Multi-Resolution Compression

Compresses a video file to multiple resolutions in a single request.

```http
POST /compress/multi
Content-Type: multipart/form-data
```

#### Parameters

| Parameter            | Type    | Required | Description            | Default  | Options                           |
| -------------------- | ------- | -------- | ---------------------- | -------- | --------------------------------- |
| file                 | File    | Yes      | Video file to compress | -        | -                                 |
| resolutions          | Array   | Yes      | Target resolutions     | -        | ["1080p", "720p", "480p", "360p"] |
| outputFormat         | String  | No       | Output format          | "mp4"    | "mp4", "webm", "mkv", "m3u8"      |
| maintainAspectRatio  | Boolean | No       | Maintain aspect ratio  | true     | true, false                       |
| preserveAudioQuality | Boolean | No       | Preserve audio quality | true     | true, false                       |
| frameRate            | Integer | No       | Target frame rate      | 30       | 1-60                              |
| preset               | String  | No       | Compression preset     | "MEDIUM" | "FAST", "MEDIUM", "SLOW"          |

#### Example Request

```bash
curl -X POST http://localhost:8080/api/compress/multi \
  -F "file=@video.mp4" \
  -F "resolutions=1080p,720p,480p" \
  -F "outputFormat=mp4" \
  -F "maintainAspectRatio=true" \
  -F "preserveAudioQuality=true" \
  -F "frameRate=30" \
  -F "preset=MEDIUM"
```

#### Example Response

```json
{
  "success": true,
  "results": [
    {
      "fileName": "video_1080p.mp4",
      "resolution": "1920x1080",
      "originalSize": 20971520,
      "compressedSize": 10485760,
      "compressionRatio": 2.0,
      "originalResolution": "1920x1080",
      "bitrate": 2000000,
      "duration": 180,
      "outputPath": "/processed/video_1080p.mp4"
    },
    {
      "fileName": "video_720p.mp4",
      "resolution": "1280x720",
      "originalSize": 20971520,
      "compressedSize": 5242880,
      "compressionRatio": 4.0,
      "originalResolution": "1920x1080",
      "bitrate": 1500000,
      "duration": 180,
      "outputPath": "/processed/video_720p.mp4"
    },
    {
      "fileName": "video_480p.mp4",
      "resolution": "854x480",
      "originalSize": 20971520,
      "compressedSize": 2621440,
      "compressionRatio": 8.0,
      "originalResolution": "1920x1080",
      "bitrate": 1000000,
      "duration": 180,
      "outputPath": "/processed/video_480p.mp4"
    }
  ]
}
```

### HLS Streaming

Converts a video file to HLS (HTTP Live Streaming) format.

```http
POST /compress
Content-Type: multipart/form-data
```

#### Parameters

| Parameter            | Type    | Required | Description            | Default    | Options                                     |
| -------------------- | ------- | -------- | ---------------------- | ---------- | ------------------------------------------- |
| file                 | File    | Yes      | Video file to convert  | -          | -                                           |
| targetResolution     | String  | No       | Target resolution      | "ORIGINAL" | "1080p", "720p", "480p", "360p", "ORIGINAL" |
| outputFormat         | String  | Yes      | Must be "m3u8"         | "m3u8"     | "m3u8"                                      |
| maintainAspectRatio  | Boolean | No       | Maintain aspect ratio  | true       | true, false                                 |
| preserveAudioQuality | Boolean | No       | Preserve audio quality | true       | true, false                                 |
| frameRate            | Integer | No       | Target frame rate      | 30         | 1-60                                        |
| preset               | String  | No       | Compression preset     | "MEDIUM"   | "FAST", "MEDIUM", "SLOW"                    |

#### Example Request

```bash
curl -X POST http://localhost:8080/api/compress \
  -F "file=@video.mp4" \
  -F "targetResolution=1080p" \
  -F "outputFormat=m3u8" \
  -F "maintainAspectRatio=true" \
  -F "preserveAudioQuality=true" \
  -F "frameRate=30" \
  -F "preset=MEDIUM"
```

#### Example Response

```json
{
  "success": true,
  "result": {
    "fileName": "video_1080p.m3u8",
    "resolution": "1920x1080",
    "originalSize": 20971520,
    "compressedSize": 10485760,
    "compressionRatio": 2.0,
    "originalResolution": "1920x1080",
    "bitrate": 2000000,
    "duration": 180,
    "outputPath": "/processed/video_1080p_hls/stream.m3u8"
  }
}
```

### Format Conversion

Converts a video file to a different format without compression.

```http
POST /convert
Content-Type: multipart/form-data
```

#### Parameters

| Parameter       | Type    | Required | Description               | Default | Options                      |
| --------------- | ------- | -------- | ------------------------- | ------- | ---------------------------- |
| file            | File    | Yes      | Video file to convert     | -       | -                            |
| outputFormat    | String  | Yes      | Target format             | -       | "mp4", "webm", "mkv", "m3u8" |
| preserveQuality | Boolean | No       | Preserve original quality | true    | true, false                  |

#### Example Request

```bash
curl -X POST http://localhost:8080/api/convert \
  -F "file=@video.mp4" \
  -F "outputFormat=webm" \
  -F "preserveQuality=true"
```

#### Example Response

```json
{
  "success": true,
  "result": {
    "fileName": "converted_video.webm",
    "resolution": "1920x1080",
    "originalSize": 20971520,
    "compressedSize": 20971520,
    "compressionRatio": 1.0,
    "originalResolution": "1920x1080",
    "bitrate": 2000000,
    "duration": 180,
    "outputPath": "/processed/converted_video.webm"
  }
}
```

### Custom Resolution

Compresses a video file to a custom resolution.

```http
POST /compress/custom
Content-Type: multipart/form-data
```

#### Parameters

| Parameter            | Type    | Required | Description            | Default  | Options                      |
| -------------------- | ------- | -------- | ---------------------- | -------- | ---------------------------- |
| file                 | File    | Yes      | Video file to compress | -        | -                            |
| width                | Integer | Yes      | Custom width           | -        | 1-3840                       |
| height               | Integer | Yes      | Custom height          | -        | 1-2160                       |
| outputFormat         | String  | No       | Output format          | "mp4"    | "mp4", "webm", "mkv", "m3u8" |
| maintainAspectRatio  | Boolean | No       | Maintain aspect ratio  | true     | true, false                  |
| preserveAudioQuality | Boolean | No       | Preserve audio quality | true     | true, false                  |
| frameRate            | Integer | No       | Target frame rate      | 30       | 1-60                         |
| preset               | String  | No       | Compression preset     | "MEDIUM" | "FAST", "MEDIUM", "SLOW"     |

#### Example Request

```bash
curl -X POST http://localhost:8080/api/compress/custom \
  -F "file=@video.mp4" \
  -F "width=1280" \
  -F "height=720" \
  -F "outputFormat=mp4" \
  -F "maintainAspectRatio=true" \
  -F "preserveAudioQuality=true" \
  -F "frameRate=30" \
  -F "preset=MEDIUM"
```

#### Example Response

```json
{
  "success": true,
  "result": {
    "fileName": "custom_video.mp4",
    "resolution": "1280x720",
    "originalSize": 20971520,
    "compressedSize": 10485760,
    "compressionRatio": 2.0,
    "originalResolution": "1920x1080",
    "bitrate": 2000000,
    "duration": 180,
    "outputPath": "/processed/custom_video.mp4"
  }
}
```

### Available Resolutions

Gets the list of available resolutions for a video file.

```http
POST /resolutions
Content-Type: multipart/form-data
```

#### Parameters

| Parameter | Type | Required | Description           |
| --------- | ---- | -------- | --------------------- |
| file      | File | Yes      | Video file to analyze |

#### Example Request

```bash
curl -X POST http://localhost:8080/api/resolutions \
  -F "file=@video.mp4"
```

#### Example Response

```json
{
  "success": true,
  "availableResolutions": ["ORIGINAL", "1080p", "720p", "480p", "360p"]
}
```

## Response Formats

### Success Response Structure

```json
{
    "success": true,
    "result": {
        "fileName": string,
        "resolution": string,
        "originalSize": number,
        "compressedSize": number,
        "compressionRatio": number,
        "originalResolution": string,
        "bitrate": number,
        "duration": number,
        "outputPath": string
    }
}
```

### Error Response Structure

```json
{
    "success": false,
    "error": {
        "code": string,
        "message": string
    }
}
```

## Error Handling

### Common Error Codes

| Code               | Description                         |
| ------------------ | ----------------------------------- |
| INVALID_FORMAT     | Unsupported output format specified |
| FILE_TOO_LARGE     | Input file exceeds size limit       |
| INVALID_RESOLUTION | Invalid resolution specified        |
| PROCESSING_ERROR   | Error during video processing       |
| FILE_NOT_FOUND     | Input file not found                |
| INVALID_PARAMETER  | Invalid parameter value provided    |

### Example Error Response

```json
{
  "success": false,
  "error": {
    "code": "INVALID_FORMAT",
    "message": "The specified output format 'xyz' is not supported. Supported formats are: mp4, webm, mkv, m3u8"
  }
}
```

## Additional Notes

1. **File Size Limits**:

   - Maximum file size: 500MB (default)
   - This can be configured in the application properties

2. **Supported Input Formats**:

   - MP4 (.mp4)
   - AVI (.avi)
   - MOV (.mov)
   - MKV (.mkv)
   - WebM (.webm)

3. **Compression Presets**:

   - FAST: Faster compression, larger file size
   - MEDIUM: Balanced compression (recommended)
   - SLOW: Better compression, smaller file size

4. **HLS Output Structure**:
   When using HLS format (m3u8), the output will be structured as follows:

   ```
   /processed/
   └── video_name_hls/
       ├── stream.m3u8
       ├── segment0.ts
       ├── segment1.ts
       └── segment2.ts
   ```

5. **Performance Considerations**:

   - Processing time depends on video length, resolution, and preset
   - Multiple simultaneous requests are supported
   - Progress monitoring is not currently supported

6. **Best Practices**:
   - Use multi-resolution compression for adaptive streaming
   - Choose appropriate presets based on your needs
   - Monitor disk space when processing multiple files
   - Clean up processed files periodically
