package com.demoday.ddangddangddang;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

@RequiredArgsConstructor
@RestController
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "스웨거테스트")
    public String home() {
        return "멋쟁이사자처럼 신촌연합 데모데이 땅땅땅 서버입니다.";
    }
}
