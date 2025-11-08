package com.demoday.ddangddangddang.controller.mypage;

import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.dto.mypage.UserResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserUpdateRequestDto;
import com.demoday.ddangddangddang.dto.mypage.UserUpdateResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.security.UserDetailsImpl;
import com.demoday.ddangddangddang.service.mypage.UserInfoModifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Mypage", description = "마이페이지 API -by 황신애")
@RequiredArgsConstructor
@RequestMapping("/api/users")
@SecurityRequirement(name = "JWT TOKEN")
public class UserInfoUpdateController {
    private final UserInfoModifyService userInfoModifyService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 정보 조회 성공",
                        content = @Content(mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class),
                                examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "COMMON2000",
                      "message": "유저 정보 조회 성공",
                      "result": {
                        "nickname": "tlsdo",
                        "profileImageUrl": "프로필 이미지",
                        "email": "test123@naver.com"
                      },
                      "error": null
                    }
                    """)))
        })
    @GetMapping("/getInfo")
    public ApiResponse<UserResponseDto> getUserInfo(@AuthenticationPrincipal UserDetailsImpl user) {
        Long userId = user.getUser().getId();
        return userInfoModifyService.getUsers(userId);
    }

    @Operation(summary = "내 정보 수정 (닉네임, 이메일)", description = "현재 로그인한 사용자의 정보를 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 정보 업데이트 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "COMMON2000",
                      "message": "유저 정보 업데이트 성공",
                      "result": {
                        "nickname": "tlsdo",
                        "profileImageUrl": "프로필 이미지",
                        "email": "test123@naver.com"
                      },
                      "error": null
                    }
                    """)))
    })
    @PatchMapping("/modify")
    public ApiResponse<UserUpdateResponseDto> modifyUserInfo(@AuthenticationPrincipal UserDetailsImpl user, @RequestBody
    @io.swagger.v3.oas.annotations.parameters.RequestBody( // @RequestBody 추가
            description = "수정할 사용자 정보 (email 등)",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserUpdateRequestDto.class),
                    examples = @ExampleObject(value = """
                    {
                      "email": "test123@naver.com"
                    }
                    """))
    )UserUpdateRequestDto requestDto) {
        Long userId = user.getUser().getId();
        return userInfoModifyService.modifyUser(userId,requestDto);
    }

    @Operation(summary = "프로필 이미지 등록/수정", description = "프로필 이미지를 업로드(수정)합니다. (Content-Type: multipart/form-data)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 사진 변경 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "COMMON2000",
                      "message": "프로필 사진 변경 완료 S3_Image_URL",
                      "result": {},
                      "error": null
                    }
                    """)))
    })
    @PostMapping("/image")
    public ApiResponse<String> updateProfileImage(@AuthenticationPrincipal UserDetailsImpl user,
                                                  @Parameter(description = "업로드할 프로필 이미지 (form-data key: profileImage)", required = true)
                                                  @RequestParam("profileImage") MultipartFile profileImage) {
        Long userId = user.getUser().getId();
        return userInfoModifyService.updateUserProfileImage(userId,profileImage);
    }
}
