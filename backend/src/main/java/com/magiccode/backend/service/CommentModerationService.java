package com.magiccode.backend.service;

import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.model.CommentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentModerationService {
    private static final Pattern LINK_PATTERN = Pattern.compile("(https?://|www\\.)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_CHARACTER_PATTERN = Pattern.compile("(.)\\1{9,}");

    private final int maxLinks;
    private final List<String> blockedKeywords;
    private final boolean autoRejectBlockedKeywords;

    public CommentModerationService(
            @Value("${blog.comment.moderation.max-links:2}") int maxLinks,
            @Value("${blog.comment.moderation.blocked-keywords:}") String blockedKeywords,
            @Value("${blog.comment.moderation.auto-reject-blocked-keywords:true}") boolean autoRejectBlockedKeywords) {
        this.maxLinks = maxLinks;
        this.blockedKeywords = Arrays.stream(blockedKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .toList();
        this.autoRejectBlockedKeywords = autoRejectBlockedKeywords;
    }

    public ModerationResult review(CreateCommentRequest request) {
        String name = normalize(request.getName());
        String email = normalize(request.getEmail());
        String content = normalize(request.getContent());
        String combined = (name + " " + email + " " + content).toLowerCase(Locale.ROOT);

        String blockedKeyword = blockedKeywords.stream()
                .filter(combined::contains)
                .findFirst()
                .orElse(null);
        if (blockedKeyword != null) {
            return autoRejectBlockedKeywords
                    ? ModerationResult.rejected("blocked keyword")
                    : ModerationResult.pending("blocked keyword");
        }

        if (hasRepeatedCharacters(name) || hasRepeatedCharacters(email) || hasRepeatedCharacters(content)) {
            return ModerationResult.rejected("repeated characters");
        }

        int linkCount = countLinks(content);
        if (linkCount > maxLinks) {
            return ModerationResult.rejected("too many links");
        }

        if (content.length() < 20 && linkCount > 0) {
            return ModerationResult.rejected("short link comment");
        }

        return ModerationResult.pending(null);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasRepeatedCharacters(String value) {
        return REPEATED_CHARACTER_PATTERN.matcher(value).find();
    }

    private int countLinks(String value) {
        int count = 0;
        Matcher matcher = LINK_PATTERN.matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public record ModerationResult(CommentStatus status, String reason) {
        static ModerationResult pending(String reason) {
            return new ModerationResult(CommentStatus.PENDING, reason);
        }

        static ModerationResult rejected(String reason) {
            return new ModerationResult(CommentStatus.REJECTED, reason);
        }
    }
}
