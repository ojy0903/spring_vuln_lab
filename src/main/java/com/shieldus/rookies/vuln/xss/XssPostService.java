package com.shieldus.rookies.vuln.xss;

import com.shieldus.rookies.common.dtos.PostResponse;
import com.shieldus.rookies.common.entity.Post;
import com.shieldus.rookies.common.repository.PostRepository;
import com.shieldus.rookies.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Profile("local")
@Service
@RequiredArgsConstructor
public class XssPostService {

    // 아직 로그인 서비스 구현 전이므로 게시물 작성자는 id 2번 회원으로 고정 진행
    private static final long FIXED_AUTHOR_ID = 2L;

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public List<PostResponse> search(String keyword) {
        List<Post> posts = postRepository.findPublicPostsByTitle(keyword);
        List<PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(PostResponse.from(post));
        }
        return responses;
    }

    public PostResponse findById(Long id) {
        return postRepository.findPublicById(id)
                .map(PostResponse::from)
                .orElseThrow();
    }

    @Transactional
    public void create(String title, String content) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .author(userRepository.getReferenceById(FIXED_AUTHOR_ID))
                .secret(false)
                .build();

        postRepository.save(post);
    }
}
