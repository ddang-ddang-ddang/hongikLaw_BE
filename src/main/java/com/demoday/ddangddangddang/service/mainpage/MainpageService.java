package com.demoday.ddangddangddang.service.mainpage;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.CaseStatus;
import com.demoday.ddangddangddang.dto.home.CaseOnResponseDto;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import com.demoday.ddangddangddang.dto.mypage.UserArchiveResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MainpageService {
    private final UserRepository userRepository;
    private final CaseParticipationRepository caseParticipationRepository;
    private final ArgumentInitialRepository argumentInitialRepository;
    private final DefenseRepository defenseRepository;
    private final RebuttalRepository rebuttalRepository;

    //현재 핫한 재판(추후 2차 재판 기능 구현 완료되면 구현)

    //진행중인 재판
    public ApiResponse<List<CaseOnResponseDto>> getCaseList(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        List<CaseParticipation> participations = caseParticipationRepository.findActiveCasesByUser(user);

        List<CaseOnResponseDto> responseDtos = participations.stream()
                .map(participation -> {
                    //각 참여 기록에서 Case 객체를 가져옴
                    Case aCase = participation.getACase();

                    //해당 Case에 속한 모든 초기 의견들을 조회
                    List<ArgumentInitial> arguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

                    //초기 의견 객체 리스트에서 'mainArgument' 문자열만 추출하여 새로운 리스트 생성
                    List<String> mainArguments = arguments.stream()
                            .map(ArgumentInitial::getMainArgument)
                            .collect(Collectors.toList());

                    // 모은 정보들을 사용하여 DTO를 생성
                    return CaseOnResponseDto.builder()
                            .caseId(aCase.getId())
                            .title(aCase.getTitle())
                            .status(aCase.getStatus())
                            .mainArguments(mainArguments)
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("진행중인 재판 조회 성공",responseDtos);
    }

    //변호 이력
    public ApiResponse<UserDefenseRebuttalResponseDto> getDefenseAndRebuttal(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        List<Defense> defenses = defenseRepository.findDefenseByUser(user);

        List<Rebuttal> rebuttals = rebuttalRepository.findRebuttalByUser(user);

        List<UserDefenseRebuttalResponseDto.DefenseDto> defenseDtos = defenses.stream()
                .map(defense -> UserDefenseRebuttalResponseDto.DefenseDto.builder()
                        .caseId(defense.getACase().getId())
                        .defenseId(defense.getId())
                        .debateSide(defense.getType())
                        .title(defense.getTitle())
                        .content(defense.getContent())
                        .likeCount(defense.getLikesCount())
                        .build())
                .collect(Collectors.toList());

        List<UserDefenseRebuttalResponseDto.RebuttalDto> rebuttalDtos = rebuttals.stream()
                .map(rebuttal -> UserDefenseRebuttalResponseDto.RebuttalDto.builder()
                        .caseId(rebuttal.getDefense().getACase().getId())
                        .rebuttalId(rebuttal.getId())
                        .debateSide(rebuttal.getType())
                        .content(rebuttal.getContent())
                        .likeCount(rebuttal.getLikesCount())
                        .build())
                .collect(Collectors.toList());

        UserDefenseRebuttalResponseDto responseDto = UserDefenseRebuttalResponseDto.builder()
                .defenses(defenseDtos)
                .rebuttals(rebuttalDtos)
                .build();

        return ApiResponse.onSuccess("유저 변론 및 반론 조회 완료",responseDto);
    }
}
