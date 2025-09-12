package com.magiccode.backend.service;

import com.magiccode.backend.dto.LikeResponseDto;
import com.magiccode.backend.model.LikeLog;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.repository.LikeLogRepository;
import com.magiccode.backend.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
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
        // 检查文章是否存在
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));

        // 检查是否已存在点赞/点踩记录
        if (likeLogRepository.existsByPostIdAndIdentifier(postId, identifier)) {
            // 如果允许切换点赞/点踩状态，可以先删除旧记录
            LikeLog existingLog = likeLogRepository.findByPostIdAndIdentifier(postId, identifier);
            if (existingLog.isPositive() != positive) {
                // 如果现有记录的状态与请求不同，更新状态
                existingLog.setPositive(positive);
                likeLogRepository.save(existingLog);
            } else {
                throw new RuntimeException("You have already " + (positive ? "liked" : "disliked") + " this post");
            }
        } else {
            // 新增点赞/点踩记录
            LikeLog likeLog = LikeLog.builder()
                    .post(post)
                    .identifier(identifier)
                    .positive(positive)
                    .build();
            likeLogRepository.save(likeLog);
        }
    }

    public LikeResponseDto getLikeAndDislikeCountBySlug(String slug) {
        Post post = postRepository.findBySlug(slug);
        if (post == null) {
            throw new RuntimeException("Post Not Found");
        }
        return new LikeResponseDto(
                countLikesByPostId(post.getId()),
                countDisLikesByPostId(post.getId())
        );
    }

    public void deleteAllByPostId(Long postId) {
        likeLogRepository.deleteAllByPostId(postId);
    }
}
