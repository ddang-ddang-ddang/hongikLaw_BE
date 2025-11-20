package com.demoday.ddangddangddang.domain.event;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PostCreatedEvent {
    private final User user;
    private final ContentType contentType;
}
