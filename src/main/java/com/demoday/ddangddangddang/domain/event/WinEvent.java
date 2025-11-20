package com.demoday.ddangddangddang.domain.event;

import com.demoday.ddangddangddang.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class WinEvent {
    private final User user;
}
