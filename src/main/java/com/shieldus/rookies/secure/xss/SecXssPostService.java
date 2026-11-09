package com.shieldus.rookies.secure.xss;

import com.shieldus.rookies.common.dtos.PostResponse;
import com.shieldus.rookies.common.entity.Post;
import com.shieldus.rookies.common.repository.PostRepository;
import com.shieldus.rookies.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecXssPostService {

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
        Post post = postRepository.findPublicById(id).orElseThrow();

        // 패치: 출력 직전에 허용 목록(Safelist) 기반으로 HTML 정제
        // basic(): <b>, <i>, <u>, <p>, <br>, <ul>, <a href> 등 서식 태그만 남김
        // <script>, <img>, on* 이벤트 속성, javascript: 링크는 제거
        // prettyPrint(false): 끄지 않으면 jsoup이 줄바꿈을 자동 정리해 본문 줄바꿈이 사라짐
        String safeContent = Jsoup.clean(post.getContent(), "", Safelist.basic(),
                new Document.OutputSettings().prettyPrint(false));

        return new PostResponse(post.getId(), post.getTitle(), safeContent, post.isSecret());
    }

    @Transactional
    public void create(String title, String content) {
        // 원본 그대로 저장 : 정제는 출력 시점에 수행
        Post post = Post.builder()
                .title(title)
                .content(content)
                .author(userRepository.getReferenceById(FIXED_AUTHOR_ID))
                .secret(false)
                .build();

        postRepository.save(post);
    }
}
