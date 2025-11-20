package com.demoday.ddangddangddang.domain.event;

import com.demoday.ddangddangddang.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LikedEvent {
    private final User user;
}
