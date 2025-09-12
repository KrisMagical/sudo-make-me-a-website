package com.magiccode.backend.service;

import com.magiccode.backend.model.Comment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpNotificationService implements NotificationService{
    @Override
    public void sendCommentNotification(Comment comment) {
        // 什么都不做
    }
}
