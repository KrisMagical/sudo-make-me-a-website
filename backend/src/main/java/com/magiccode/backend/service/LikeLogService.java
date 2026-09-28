package com.magiccode.backend.service;

import com.magiccode.backend.dto.LikeResponseDto;
import com.magiccode.backend.model.LikeLog;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.repository.LikeLogRepository;
import com.magiccode.backend.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class LikeLogService {
    private final LikeLogRepository likeLogRepository;
    private final PostRepository postRepository;

    public int countLikesByPostId(Long postId) {
        return likeLogRepository.countByPostIdAndPositive(postId, true);
    }

    public int countDisLikesByPostId(Long postId) {
        return likeLogRepository.countByPostIdAndPositive(postId, false);
    }

    public void addLikeOrDislike(Long postId, String identifier, boolean positive) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post Not Found"));

        Optional<LikeLog> existing = likeLogRepository.findByPostIdAndIdentifier(postId, identifier);
        if (existing.isPresent()) {
            LikeLog existingLog = existing.get();
            if (existingLog.isPositive() == positive) {
                updatePostCounts(postId);
                log.info("reaction duplicate postId={} positive={} client={}", postId, positive, maskIdentifier(identifier));
                throw new RuntimeException("You have already " + (positive ? "liked" : "disliked") + " this post");
            }
            existingLog.setPositive(positive);
            likeLogRepository.save(existingLog);
            log.info("reaction switched postId={} positive={} client={}", postId, positive, maskIdentifier(identifier));
        } else {
            LikeLog likeLog = LikeLog.builder()
                    .post(post)
                    .identifier(identifier)
                    .positive(positive)
                    .build();
            likeLogRepository.save(likeLog);
            log.debug("reaction created postId={} positive={} client={}", postId, positive, maskIdentifier(identifier));
        }
        updatePostCounts(postId);
    }

    public LikeResponseDto getLikeAndDislikeCountBySlug(String slug) {
        Post post = postRepository.findBySlug(slug);
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post Not Found");
        }
        return new LikeResponseDto(post.getLikeCount(), post.getDislikeCount());
    }

    public void deleteAllByPostId(Long postId) {
        likeLogRepository.deleteAllByPostId(postId);
    }

    private void updatePostCounts(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post Not Found"));
        int likes = countLikesByPostId(postId);
        int dislikes = countDisLikesByPostId(postId);
        post.setLikeCount(likes);
        post.setDislikeCount(dislikes);
        postRepository.save(post);
    }

    public void deleteAllByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return;
        likeLogRepository.deleteByPostIdIn(postIds);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "unknown";
        }
        return Integer.toHexString(identifier.hashCode());
    }
}
