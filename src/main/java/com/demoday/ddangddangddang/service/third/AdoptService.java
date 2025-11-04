package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptRequestDto;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdoptService {
    private final CaseRepository caseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final DefenseRepository defenseRepository;
    private final UserRepository userRepository;
    private final ArgumentInitialRepository argumentInitialRepository;

    //좋아요 많은 순으로 노출 todo: 의견 영역에 따라 다르게 보여야함
    public ApiResponse<AdoptResponseDto> getOpinionBest(Long caseId) {
        List<Defense> defenses = defenseRepository.findTop10ByACase_IdOrderByLikesCountDesc(caseId);

        List<Rebuttal> rebuttals = rebuttalRepository.findTop10ByOrderByLikesCountDesc(caseId);

        List<AdoptResponseDto.DefenseAdoptDto> defenseDtos = defenses.stream()
                .map(defense -> AdoptResponseDto.DefenseAdoptDto.builder()
                        .caseId(defense.getACase().getId()) // Case 엔티티에서 ID 가져오기
                        .userId(defense.getUser().getId()) // User 엔티티에서 ID 가져오기
                        .defenseId(defense.getId())
                        .debateSide(defense.getType())
                        .title(defense.getTitle())
                        .content(defense.getContent())
                        .likeCount(defense.getLikesCount())
                        .build())
                .toList();

        List<AdoptResponseDto.RebuttalAdoptDto> rebuttalDtos = rebuttals.stream()
                .map(rebuttal -> AdoptResponseDto.RebuttalAdoptDto.builder()
                        .caseId(rebuttal.getDefense().getACase().getId())
                        .userId(rebuttal.getUser().getId())
                        .defenseId(rebuttal.getDefense().getId())
                        .rebuttalId(rebuttal.getId())
                        .parentId(rebuttal.getParent().getId())
                        .parentContent(rebuttal.getParent().getContent())
                        .debateSide(rebuttal.getType())
                        .content(rebuttal.getContent())
                        .likeCount(rebuttal.getLikesCount())
                        .build())
                .toList();

        AdoptResponseDto responseDto = AdoptResponseDto.builder()
                .defenses(defenseDtos)
                .rebuttals(rebuttalDtos)
                .build();

        return ApiResponse.onSuccess("좋아요 많은 반론 및 변론 조회 완료",responseDto);
    }

    //채택, todo : user가 initial한 사건인지, 유저의 debate 상태 반영 A측 의견이면 A만 채택하도록
    public ApiResponse<String> createAdopt(Long userId, Long caseId, AdoptRequestDto adoptRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        // 1. 채택할 변론(Defense) ID 목록 가져오기
        List<Long> defenseIds = adoptRequestDto.getDefenseId();
        if (defenseIds != null && !defenseIds.isEmpty()) {
            // 1-1. ID 목록으로 모든 Defense 엔티티 조회
            List<Defense> defensesToAdopt = defenseRepository.findAllById(defenseIds);

            // 1-2. 각 엔티티의 상태를 '채택'으로 변경
            for (Defense defense : defensesToAdopt) {
                defense.markAsAdopted();
            }
        }

        // 2. 채택할 반론(Rebuttal) ID 목록 가져오기
        List<Long> rebuttalIds = adoptRequestDto.getRebuttalId();
        if (rebuttalIds != null && !rebuttalIds.isEmpty()) {
            // 2-1. ID 목록으로 모든 Rebuttal 엔티티 조회
            List<Rebuttal> rebuttalsToAdopt = rebuttalRepository.findAllById(rebuttalIds);

            // 2-2. 각 엔티티의 상태를 '채택'으로 변경
            for (Rebuttal rebuttal : rebuttalsToAdopt) {
                rebuttal.markAsAdopted(); // (Rebuttal 엔티티에는 이미 존재)
            }
        }
        return ApiResponse.onSuccess("success","최종심에 반영될 의견 채택 완료");
    }

    //채택된 반론&변론 조회
    public ApiResponse<AdoptResponseDto> getAdopt(Long caseId){
        Case acase = caseRepository.findById(caseId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        List<Defense> adoptedDefenses = defenseRepository.findByACase_IdAndIsAdopted(caseId,Boolean.TRUE);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        List<AdoptResponseDto.DefenseAdoptDto> defenseDtos = adoptedDefenses.stream()
                .map(defense -> AdoptResponseDto.DefenseAdoptDto.builder()
                        .caseId(defense.getACase().getId()) // Case 엔티티에서 ID 가져오기
                        .userId(defense.getUser().getId()) // User 엔티티에서 ID 가져오기
                        .defenseId(defense.getId())
                        .debateSide(defense.getType())
                        .title(defense.getTitle())
                        .content(defense.getContent())
                        .likeCount(defense.getLikesCount())
                        .build())
                .toList();

        List<AdoptResponseDto.RebuttalAdoptDto> rebuttalDtos = adoptedRebuttals.stream()
                .map(rebuttal -> AdoptResponseDto.RebuttalAdoptDto.builder()
                        .caseId(rebuttal.getDefense().getACase().getId())
                        .userId(rebuttal.getUser().getId())
                        .defenseId(rebuttal.getDefense().getId())
                        .rebuttalId(rebuttal.getId())
                        .parentId(rebuttal.getParent().getId())
                        .parentContent(rebuttal.getParent().getContent())
                        .debateSide(rebuttal.getType())
                        .content(rebuttal.getContent())
                        .likeCount(rebuttal.getLikesCount())
                        .build())
                .toList();

        AdoptResponseDto responseDto = AdoptResponseDto.builder()
                .defenses(defenseDtos)
                .rebuttals(rebuttalDtos)
                .build();

        return ApiResponse.onSuccess("채택된 반론 및 변론 조회 완료",responseDto);
    }
}
