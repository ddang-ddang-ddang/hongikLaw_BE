package com.demoday.ddangddangddang.service.mypage;

import com.demoday.ddangddangddang.domain.ArgumentInitial;
import com.demoday.ddangddangddang.domain.Case;
import com.demoday.ddangddangddang.domain.CaseParticipation;
import com.demoday.ddangddangddang.domain.User;
import com.demoday.ddangddangddang.domain.mypage.dto.RankResponseDto;
import com.demoday.ddangddangddang.domain.mypage.dto.RecordResponseDto;
import com.demoday.ddangddangddang.domain.mypage.dto.UserArchiveResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.ArgumentInitialRepository;
import com.demoday.ddangddangddang.repository.CaseParticipationRepository;
import com.demoday.ddangddangddang.repository.CaseRepository;
import com.demoday.ddangddangddang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MypageService {
    //등급, 전적, 사건기록, 상세조회
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final CaseParticipationRepository caseParticipationRepository;
    private final ArgumentInitialRepository argumentInitialRepository;

    public ApiResponse<RankResponseDto> getRank(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));

        RankResponseDto rankResponseDto = RankResponseDto.builder()
                .id(user.getId())
                .rank(user.getRank())
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
                .orElseThrow(()->new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저를 찾을 수 없습니다."));

        List<CaseParticipation> participations = caseParticipationRepository.findByUser(user);

        List<UserArchiveResponseDto> responseDtos = participations.stream()
                .map(participation -> {
                    //각 참여 기록에서 Case 객체를 가져옴
                    Case aCase = participation.getACase();

                    //해당 Case에 속한 모든 초기 의견들을 조회
                    List<ArgumentInitial> arguments = argumentInitialRepository.findByaCase(aCase);

                    //초기 의견 객체 리스트에서 'mainArgument' 문자열만 추출하여 새로운 리스트 생성
                    List<String> mainArguments = arguments.stream()
                            .map(ArgumentInitial::getMainArgument)
                            .collect(Collectors.toList());

                    // 모은 정보들을 사용하여 DTO를 생성
                    return UserArchiveResponseDto.builder()
                            .caseId(aCase.getId())
                            .title(aCase.getTitle())
                            .status(aCase.getStatus())
                            .caseResult(participation.getResult())
                            .mainArguments(mainArguments)
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("유저 사건 리스트 조회 성공",responseDtos);
    }
}
