package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptRequestDto;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptableItemDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class AdoptService {
    private final CaseRepository caseRepository;
    private final RebuttalRepository rebuttalRepository;
    private final DefenseRepository defenseRepository;
    private final UserRepository userRepository;
    private final ArgumentInitialRepository argumentInitialRepository;

    //좋아요 많은 순으로 노출 (유저가 채택화면에서 보게 될 선택지)
    public ApiResponse<AdoptResponseDto> getOpinionBest(Long userId, Long caseId) {

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        //유저가 initial한 사건인지 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        //하나라도 유저가 참여한 항목 반환
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        //유저 진영 확인
        DebateSide type = userInitialArgument.getType();

        List<Defense> allDefenses = defenseRepository.findAllByaCase_IdAndType(caseId,type);

        List<Rebuttal> allRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndType(caseId,type);

        Stream<AdoptableItemDto> defenseStream = allDefenses.stream()
                .map(d -> AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .caseId(d.getACase().getId())
                        .userId(d.getUser().getId())
                        .debateSide(d.getType())
                        .content(d.getContent())
                        .likeCount(d.getLikesCount())
                        // defenseId, parentId, parentContent는 null (자동)
                        .build()
                );

        Stream<AdoptableItemDto> rebuttalStream = allRebuttals.stream()
                .map(r -> {
                    Rebuttal parent = r.getParent();
                    return AdoptableItemDto.builder()
                            .itemType(ContentType.REBUTTAL)
                            .id(r.getId())
                            .caseId(r.getDefense().getACase().getId())
                            .userId(r.getUser().getId())
                            .debateSide(r.getType())
                            .content(r.getContent())
                            .likeCount(r.getLikesCount())
                            // --- Rebuttal 전용 필드 ---
                            .defenseId(r.getDefense().getId())
                            .parentId((parent != null) ? parent.getId() : null)
                            .parentContent((parent != null) ? parent.getContent() : null)
                            .build();
                });

        // 3. [변경] 두 스트림을 합치고(concat), 정렬(sorted)하고, 5개만(limit) 선택
        List<AdoptableItemDto> top5Items = Stream.concat(defenseStream, rebuttalStream)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed()) // 좋아요 순 정렬
                .limit(5)
                .toList();

        // 4. [변경] 새로운 DTO 형식으로 응답 생성
        AdoptResponseDto responseDto = AdoptResponseDto.builder()
                .items(top5Items) // 'items' 필드에 통합 리스트를 담아 전달
                .build();

        return ApiResponse.onSuccess("좋아요 많은 반론 및 변론 조회 완료",responseDto);
    }

    //사건 진행 third로 바꿈
    public ApiResponse<Void> changeThird(Long userId, Long caseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        //유저가 initial한 사건인지 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        //하나라도 유저가 참여한 항목 반환
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        aCase.setThird();

        return ApiResponse.onSuccess("사건 상태 THIRD로 변경 완료",null);
    }

    //채택(선택함)
    public ApiResponse<String> createAdopt(Long userId, Long caseId, AdoptRequestDto adoptRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        //유저가 initial한 사건인지 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);

        //하나라도 유저가 참여한 항목 반환
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
                defense.getUser().updateExp(100L);
            }
        }

        // 2. 채택할 반론(Rebuttal) ID 목록 가져오기
        List<Long> rebuttalIds = adoptRequestDto.getRebuttalId();
        if (rebuttalIds != null && !rebuttalIds.isEmpty()) {
            // 2-1. ID 목록으로 모든 Rebuttal 엔티티 조회
            List<Rebuttal> rebuttalsToAdopt = rebuttalRepository.findAllById(rebuttalIds);

            // 2-2. 각 엔티티의 상태를 '채택'으로 변경
            for (Rebuttal rebuttal : rebuttalsToAdopt) {
                rebuttal.getUser().updateExp(100L);
                rebuttal.markAsAdopted(); // (Rebuttal 엔티티에는 이미 존재)
            }
        }
        return ApiResponse.onSuccess("success","최종심에 반영될 의견 채택 완료");
    }

    //채택된 반론&변론 조회
    public ApiResponse<AdoptResponseDto> getAdopt(Long caseId){
        Case acase = caseRepository.findById(caseId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdopted(caseId,Boolean.TRUE);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        List<AdoptResponseDto.DefenseAdoptDto> defenseDtos = adoptedDefenses.stream()
                .map(defense -> AdoptResponseDto.DefenseAdoptDto.builder()
                        .caseId(defense.getACase().getId()) // Case 엔티티에서 ID 가져오기
                        .userId(defense.getUser().getId()) // User 엔티티에서 ID 가져오기
                        .defenseId(defense.getId())
                        .debateSide(defense.getType())
                        .content(defense.getContent())
                        .likeCount(defense.getLikesCount())
                        .build())
                .toList();

        List<AdoptResponseDto.RebuttalAdoptDto> rebuttalDtos = adoptedRebuttals.stream()
                .map(rebuttal -> {
                    Rebuttal parent = rebuttal.getParent();
                    Long parentId = (parent != null) ? parent.getId() : null;
                    String parentContent = (parent != null) ? parent.getContent() : null;

                    return AdoptResponseDto.RebuttalAdoptDto.builder()
                            .caseId(rebuttal.getDefense().getACase().getId())
                            .userId(rebuttal.getUser().getId())
                            .defenseId(rebuttal.getDefense().getId())
                            .rebuttalId(rebuttal.getId())
                            .parentId(parentId)
                            .parentContent(parentContent)
                            .debateSide(rebuttal.getType())
                            .content(rebuttal.getContent())
                            .likeCount(rebuttal.getLikesCount())
                            .build();
                })
                .toList();

        AdoptResponseDto responseDto = AdoptResponseDto.builder()
                .defenses(defenseDtos)
                .rebuttals(rebuttalDtos)
                .build();

        return ApiResponse.onSuccess("채택된 반론 및 변론 조회 완료",responseDto);
    }
}