package com.shieldus.rookies.common.dtos;

import com.shieldus.rookies.common.entity.Post;

public record PostResponse(
        Long id,
        String title,
        String content,
        boolean secret)
{
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getTitle(), post.getContent(), post.isSecret());
    }
}
