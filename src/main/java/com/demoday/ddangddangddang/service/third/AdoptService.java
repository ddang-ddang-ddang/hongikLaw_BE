package com.demoday.ddangddangddang.service.third;

import com.demoday.ddangddangddang.domain.*;
import com.demoday.ddangddangddang.domain.enums.CaseMode;
import com.demoday.ddangddangddang.domain.enums.ContentType;
import com.demoday.ddangddangddang.domain.enums.DebateSide;
import com.demoday.ddangddangddang.domain.event.AdoptedEvent;
import com.demoday.ddangddangddang.dto.home.UserDefenseRebuttalResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptRequestDto;
import com.demoday.ddangddangddang.dto.third.AdoptResponseDto;
import com.demoday.ddangddangddang.dto.third.AdoptableItemDto;
import com.demoday.ddangddangddang.global.apiresponse.ApiResponse;
import com.demoday.ddangddangddang.global.code.GeneralErrorCode;
import com.demoday.ddangddangddang.global.exception.GeneralException;
import com.demoday.ddangddangddang.repository.*;
import com.demoday.ddangddangddang.service.ExpService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final ApplicationEventPublisher eventPublisher;
    private final ExpService expService;

    // 좋아요 많은 순으로 노출 (유저가 채택화면에서 보게 될 선택지)
    public ApiResponse<AdoptResponseDto> getOpinionBest(Long userId, Long caseId) {

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(()->new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        // [수정] 비로그인 유저 또는 참여자가 아닌 경우를 위한 로직 변경
        DebateSide userSide = null;

        if (userId != null) {
            // 유저가 로그인 상태라면, 해당 사건의 참여자인지 확인하고 진영(A/B)을 파악
            List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
            Optional<ArgumentInitial> myArg = allInitialArguments.stream()
                    .filter(arg -> arg.getUser().getId().equals(userId))
                    .findFirst();

            if (myArg.isPresent()) {
                userSide = myArg.get().getType();
            }
        }

        List<Defense> allDefenses;
        List<Rebuttal> allRebuttals;

        // [조건 변경]
        // 1. 솔로 모드인 경우
        // 2. 비로그인 유저인 경우 (userId == null)
        // 3. 로그인했으나 참여자가 아닌 경우 (userSide == null)
        // -> 위 경우에는 진영 구분 없이 블라인드 안 된 모든 목록 조회 (Observing Mode)
        if (aCase.getMode() == CaseMode.SOLO || userSide == null) {
            allDefenses = defenseRepository.findAllByaCase_Id(caseId).stream()
                    .filter(d -> !d.getIsBlind())
                    .toList();

            allRebuttals = rebuttalRepository.findAllByDefense_aCase_Id(caseId).stream()
                    .filter(r -> !r.getIsBlind())
                    .toList();
        } else {
            // 파티(VS) 모드이면서 실제 참여자인 경우: 내 진영만 조회
            allDefenses = defenseRepository.findAllByaCase_IdAndTypeAndIsBlindFalse(caseId, userSide);
            allRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsBlindFalse(caseId, userSide);
        }

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

        // 유저 권한 및 진영 파악
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        DebateSide mySide = userInitialArgument.getType();
        boolean isSoloMode = (aCase.getMode() == CaseMode.SOLO);

        // 1. 기존 채택 내역 조회 (내 진영)
        List<Defense> alreadyAdoptedDefenses = defenseRepository.findAllByaCase_IdAndTypeAndIsAdoptedTrue(caseId, mySide);
        List<Rebuttal> alreadyAdoptedRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsAdoptedTrue(caseId, mySide);

        if (isSoloMode) {
            // [수정] 솔로 모드: 진영 상관없이 이 사건에서 채택된 모든 것 조회
            alreadyAdoptedDefenses = defenseRepository.findByaCase_IdAndIsAdoptedAndIsBlindFalse(caseId, true);
            alreadyAdoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);
        } else {
            // 파티 모드: 내 진영 것만 조회
            alreadyAdoptedDefenses = defenseRepository.findAllByaCase_IdAndTypeAndIsAdoptedTrue(caseId, mySide);
            alreadyAdoptedRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsAdoptedTrue(caseId, mySide);
        }

        // 수정 모드 여부 확인
        boolean isEditMode = (!alreadyAdoptedDefenses.isEmpty() || !alreadyAdoptedRebuttals.isEmpty());

        // 기존 채택 해제
        for (Defense oldDefense : alreadyAdoptedDefenses) {
            oldDefense.markAsAdoptedFalse();
        }
        for (Rebuttal oldRebuttal : alreadyAdoptedRebuttals) {
            oldRebuttal.markAsAdoptedFalse();
        }

// 2. 새로운 Defense 채택 적용
        List<Long> defenseIds = adoptRequestDto.getDefenseId();
        if (defenseIds != null && !defenseIds.isEmpty()) {
            List<Defense> defensesToAdopt = defenseRepository.findAllById(defenseIds);
            for (Defense defense : defensesToAdopt) {
                // [수정] 솔로 모드이거나, 파티 모드일 때 내 진영과 같으면 채택
                if (isSoloMode || defense.getType() == mySide) {
                    defense.markAsAdopted();

                    if (!isEditMode) {
                        expService.addExp(defense.getUser(), 100L, "변론 채택됨 (작성자)");
                        eventPublisher.publishEvent(new AdoptedEvent(defense.getUser(), ContentType.DEFENSE));
                    }
                }
            }
        }

// 3. 새로운 Rebuttal 채택 적용
        List<Long> rebuttalIds = adoptRequestDto.getRebuttalId();
        if (rebuttalIds != null && !rebuttalIds.isEmpty()) {
            List<Rebuttal> rebuttalsToAdopt = rebuttalRepository.findAllById(rebuttalIds);
            for (Rebuttal rebuttal : rebuttalsToAdopt) {
                // [수정] 솔로 모드이거나, 파티 모드일 때 내 진영과 같으면 채택
                if (isSoloMode || rebuttal.getType() == mySide) {
                    rebuttal.markAsAdopted();

                    if (!isEditMode) {
                        expService.addExp(rebuttal.getUser(), 100L, "반론 채택됨 (작성자)");
                        eventPublisher.publishEvent(new AdoptedEvent(rebuttal.getUser(), ContentType.REBUTTAL));
                    }
                }
            }
        }

        String message = isEditMode ? "채택 목록이 수정되었습니다. (경험치 추가 지급 없음)" : "의견 채택 및 경험치 지급 완료";
        return ApiResponse.onSuccess("success", message);
    }

    // [수정] API를 통한 수동 자동 채택 (버튼 클릭 등)
    public ApiResponse<String> adoptAuto(Long userId, Long caseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        // 유저 권한 확인
        List<ArgumentInitial> allInitialArguments = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        ArgumentInitial userInitialArgument = allInitialArguments.stream()
                .filter(arg -> arg.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN_USER_NOT_PART_OF_DEBATE));

        // [로직 변경] 솔로 모드면 A, B 양쪽 다 수행 / 파티 모드면 내 진영만 수행
        if (aCase.getMode() == CaseMode.SOLO) {
            performAutoAdoptForSide(aCase, DebateSide.A);
            performAutoAdoptForSide(aCase, DebateSide.B);
        } else {
            performAutoAdoptForSide(aCase, userInitialArgument.getType());
        }

        return ApiResponse.onSuccess("success", "상위 5개 항목이 자동으로 채택되었습니다.");
    }

    // 스케줄러용 시스템 자동 채택
    // (참고: 스케줄러는 모든 ArgumentInitial(참여자)을 순회하므로 솔로 모드(참여자가 A, B 둘 다 있음)도 이미 정상 동작함)
    public void executeSystemAutoAdopt(Case aCase) {
        List<ArgumentInitial> participants = argumentInitialRepository.findByaCaseOrderByTypeAsc(aCase);
        for (ArgumentInitial participant : participants) {
            performAutoAdoptForSide(aCase, participant.getType());
        }
        aCase.setThird();
    }

    /**
     * [내부 공통 메서드] 특정 사건, 특정 진영에 대한 상위 5개 자동 채택 로직
     * userId 검증 없이 순수하게 DB 조회 및 업데이트만 수행
     */
    private void performAutoAdoptForSide(Case aCase, DebateSide type) {
        Long caseId = aCase.getId();
        List<Defense> allDefenses = defenseRepository.findAllByaCase_IdAndTypeAndIsBlindFalse(caseId, type);
        List<Rebuttal> allRebuttals = rebuttalRepository.findAllByDefense_aCase_IdAndTypeAndIsBlindFalse(caseId, type);

        Stream<AdoptableItemDto> defenseStream = allDefenses.stream()
                .map(d -> AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .likeCount(d.getLikesCount())
                        .build());

        Stream<AdoptableItemDto> rebuttalStream = allRebuttals.stream()
                .map(r -> AdoptableItemDto.builder()
                        .itemType(ContentType.REBUTTAL)
                        .id(r.getId())
                        .likeCount(r.getLikesCount())
                        .build());

        List<AdoptableItemDto> top5Items = Stream.concat(defenseStream, rebuttalStream)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed())
                .limit(5)
                .toList();

        if (top5Items.isEmpty()) return;

        for (AdoptableItemDto item : top5Items) {
            if (item.getItemType() == ContentType.DEFENSE) {
                defenseRepository.findById(item.getId()).ifPresent(defense -> {
                    if (!Boolean.TRUE.equals(defense.getIsAdopted())) {
                        defense.markAsAdopted();
                        expService.addExp(defense.getUser(), 100L, "변론 자동 채택");
                        eventPublisher.publishEvent(new AdoptedEvent(defense.getUser(),ContentType.DEFENSE));
                    }
                });
            } else if (item.getItemType() == ContentType.REBUTTAL) {
                rebuttalRepository.findById(item.getId()).ifPresent(rebuttal -> {
                    if (!Boolean.TRUE.equals(rebuttal.getIsAdopted())) {
                        rebuttal.markAsAdopted();
                        expService.addExp(rebuttal.getUser(), 100L, "반론 자동 채택");
                        eventPublisher.publishEvent(new AdoptedEvent(rebuttal.getUser(),ContentType.REBUTTAL));
                    }
                });
            }
        }
    }

    //채택된 반론&변론 조회
    public ApiResponse<AdoptResponseDto> getAdopt(Long caseId){
        Case acase = caseRepository.findById(caseId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.CASE_NOT_FOUND));

        List<Defense> adoptedDefenses = defenseRepository.findByaCase_IdAndIsAdoptedAndIsBlindFalse(caseId,Boolean.TRUE);
        List<Rebuttal> adoptedRebuttals = rebuttalRepository.findAdoptedRebuttalsByCaseId(caseId);

        Stream<AdoptableItemDto> defenseStream = adoptedDefenses.stream()
                .map(d -> AdoptableItemDto.builder()
                        .itemType(ContentType.DEFENSE)
                        .id(d.getId())
                        .caseId(d.getACase().getId())
                        .userId(d.getUser().getId())
                        .debateSide(d.getType())
                        .content(d.getContent())
                        .likeCount(d.getLikesCount())
                        .build()
                );

        Stream<AdoptableItemDto> rebuttalStream = adoptedRebuttals.stream()
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
                            .defenseId(r.getDefense().getId())
                            .parentId((parent != null) ? parent.getId() : null)
                            .parentContent((parent != null) ? parent.getContent() : null)
                            .build();
                });

        List<AdoptableItemDto> adoptedItems = Stream.concat(defenseStream, rebuttalStream)
                .sorted(Comparator.comparing(AdoptableItemDto::getLikeCount).reversed())
                .toList();

        AdoptResponseDto responseDto = AdoptResponseDto.builder()
                .items(adoptedItems)
                .build();

        return ApiResponse.onSuccess("채택된 반론 및 변론 조회 완료",responseDto);
    }
}