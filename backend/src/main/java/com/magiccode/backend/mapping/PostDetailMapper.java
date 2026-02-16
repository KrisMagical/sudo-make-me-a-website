package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.PostDetailDto;
import com.magiccode.backend.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostDetailMapper {
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "comments", source = "comments")
    PostDetailDto toPostDetailDto(Post post);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "dislikeCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "published", ignore = true)
    Post toPostEntity(PostDetailDto dto);
}
