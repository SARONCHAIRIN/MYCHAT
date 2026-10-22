package com.rindev.chat.controller;

import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.AudioStreamResponse;
import com.rindev.chat.dto.response.MessageResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.VoiceMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/voice-messages", "/api/voice-messages"})
@Tag(name = "Voice Messages", description = "Upload, stream, and manage voice messages")
@SecurityRequirement(name = "bearerAuth")
public class VoiceMessageController {

    private final VoiceMessageService voiceMessageService;

    public VoiceMessageController(VoiceMessageService voiceMessageService) {
        this.voiceMessageService = voiceMessageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload and send a voice message",
            description = "Uploads a voice recording file (M4A/AAC, MP3, WAV, WebM, OGG up to 15 MB) and creates a voice message in the specified conversation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Voice message created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid audio format, empty file, or oversized upload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not a member of the target conversation or blocked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> uploadVoiceMessage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @Parameter(description = "Target conversation ID", required = true)
            @RequestParam("conversationId") Long conversationId,
            @Parameter(description = "Audio file (M4A, MP3, WAV, WebM, OGG, max 15MB)", required = true)
            @RequestPart("audio") MultipartFile audio,
            @Parameter(description = "Audio duration in seconds (optional)")
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds) {

        MessageResponse response = voiceMessageService.sendVoiceMessage(
                principal.getId(),
                conversationId,
                audio,
                durationSeconds);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping("/{messageId}/audio")
    @Operation(
            summary = "Stream voice message audio",
            description = "Streams voice message audio for authorized conversation participants. Supports HTTP Range requests (RFC 7233) for seeking/scrubbing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Full audio content returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "206", description = "Partial content returned for Range request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to access this conversation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Message or audio attachment not found / deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "416", description = "Requested byte range not satisfiable")
    })
    public ResponseEntity<byte[]> streamAudio(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        AudioStreamResponse stream = voiceMessageService.streamVoiceMessage(
                principal.getId(),
                messageId,
                rangeHeader);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(stream.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(stream.isPartial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType);

        if (stream.isPartial()) {
            builder.header(
                    HttpHeaders.CONTENT_RANGE,
                    "bytes " + stream.rangeStart() + "-" + stream.rangeEnd() + "/" + stream.totalLength());
            builder.contentLength(stream.data().length);
        } else {
            builder.contentLength(stream.totalLength());
        }

        return builder.body(stream.data());
    }
}
