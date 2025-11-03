package com.demoday.ddangddangddang.repository;

import com.demoday.ddangddangddang.domain.Like;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like,Long> {
    Optional<Like> findByUserAndContentIdAndContentType(User user, Long contentId, ContentType contentType);
}
