package com.magiccode.backend.service;

import com.magiccode.backend.model.Comment;

public interface NotificationService {
    void sendCommentNotification(Comment comment);
}
