package com.demoday.ddangddangddang.service.mypage;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.Rank;
import com.demoday.ddangddangddang.domain.enums.achieve.AchieveEnum;
import com.demoday.ddangddangddang.dto.mypage.RankResponseDto;
import com.demoday.ddangddangddang.dto.mypage.RecordResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserAchievementResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserArchiveResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.demoday.ddangddangddang.domain.ExpLog;
import com.demoday.ddangddangddang.dto.mypage.ExpHistoryResponseDto;
import com.demoday.ddangddangddang.repository.ExpLogRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.demoday.ddangddangddang.domain.enums.Rank.getRankByExp;

@Service
@RequiredArgsConstructor
@Transactional
public class MypageService {
    //등급, 전적, 사건기록, 진행중인 사건, 업적
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final CaseParticipationRepository caseParticipationRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ExpLogRepository expLogRepository;

    // 경험치 히스토리 조회 메서드 추가
    @Transactional(readOnly = true)
    public ApiResponse<List<ExpHistoryResponseDto>> getExpHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));

        List<ExpLog> logs = expLogRepository.findAllByUserOrderByCreatedAtDesc(user);

        List<ExpHistoryResponseDto> responseDtos = logs.stream()
                .map(log -> ExpHistoryResponseDto.builder()
                        .id(log.getId())
                        .amount(log.getAmount())
                        .description(log.getDescription())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("경험치 내역 조회 성공", responseDtos);
    }

    public ApiResponse<RankResponseDto> getRank(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));

        Rank rank = getRankByExp(user.getExp());

        RankResponseDto rankResponseDto = RankResponseDto.builder()
                .id(user.getId())
                .rank(rank)
                .exp(user.getExp())
                .build();
        return ApiResponse.onSuccess("유저 랭크 조회 성공", rankResponseDto);
    }

    public ApiResponse<RecordResponseDto> getRecord(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));

        RecordResponseDto recordResponseDto = RecordResponseDto.builder()
                .id(user.getId())
                .winCnt(user.getWinCnt())
                .lossCnt(user.getLoseCnt())
                .build();

        return ApiResponse.onSuccess("유저 등급 전적 조회 성공", recordResponseDto);
    }

    public ApiResponse<List<UserArchiveResponseDto>> getUserCases (Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        // 1. 참여 기록 조회
        List<CaseParticipation> participations = caseParticipationRepository.findByUser(user);

        // 2. 참여한 모든 Case 추출
        List<Case> cases = participations.stream()
                .map(CaseParticipation::getACase)
                .toList();

        // 3. [최적화] 모든 Case의 입장문을 '한 번의 쿼리'로 조회 (IN 절 사용)
        List<ArgumentInitial> allArguments = argumentInitialRepository.findByaCaseInOrderByTypeAsc(cases);

        // 4. 조립을 위해 CaseId를 키(Key)로 하는 Map으로 변환
        Map<Long, List<String>> argumentsMap = allArguments.stream()
                .collect(Collectors.groupingBy(
                        arg -> arg.getACase().getId(),
                        Collectors.mapping(ArgumentInitial::getMainArgument, Collectors.toList())
                ));

        // 5. DTO 변환 (이제 반복문 안에서 DB 조회를 하지 않음)
        List<UserArchiveResponseDto> responseDtos = participations.stream()
                .map(participation -> {
                    Case aCase = participation.getACase();

                    // Map에서 꺼내 쓰기 (DB 조회 X)
                    List<String> mainArguments = argumentsMap.getOrDefault(aCase.getId(), Collections.emptyList());

                    return UserArchiveResponseDto.builder()
                            .caseId(aCase.getId())
                            .title(aCase.getTitle())
                            .status(aCase.getStatus())
                            .caseResult(participation.getResult())
                            .mainArguments(mainArguments)
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("유저 사건 리스트 조회 성공", responseDtos);
    }

    //업적 조회
    public ApiResponse<List<UserAchievementResponseDto>> getAchievement(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        List<UserAchievement> achievements = userAchievementRepository.findByUser(user);

        List<UserAchievementResponseDto> responseDtos = achievements.stream()
                .map(userAchievement -> {
                    AchieveEnum achievement = userAchievement.getAchievement();

                    return UserAchievementResponseDto.builder()
                            .userId(userId)
                            .achievementId(userAchievement.getId())
                            .achievementName(achievement.getName())
                            .achievementDescription(achievement.getDescription())
                            .achievementIconUrl(achievement.getIconUrl())
                            .achievementTime(userAchievement.getEarnedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("유저 업적 조회 성공",responseDtos);
    }
}
