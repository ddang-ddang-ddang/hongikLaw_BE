package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType; // 좋아요를 받은 콘텐츠의 타입

    @Builder
    public Like(User user, Long contentId, ContentType contentType) {
        this.user = user;
        this.contentId = contentId;
        this.contentType = contentType;
    }
}