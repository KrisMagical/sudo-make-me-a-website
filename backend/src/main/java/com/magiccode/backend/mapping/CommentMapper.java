package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentDto toCommentDto(Comment comment);

    List<CommentDto> toCommentDtoList(List<Comment> comments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Comment toCommentEntity(CreateCommentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Comment toCommentEntity(CommentDto dto);
}
