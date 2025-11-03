package com.demoday.ddangddangddang.domain;

import com.demoday.ddangddangddang.domain.enums.Rank;
import com.demoday.ddangddangddang.dto.mypage.UserUpdateRequestDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_rank", nullable = false)
    private Rank rank;

    @Column(name = "nickname", nullable = false, unique = true, length = 255)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "exp", nullable = false)
    private Long exp;

    private String profileImageUrl;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "win_cnt")
    private Integer winCnt = 0;

    @Column(name = "lose_cnt")
    private Integer loseCnt = 0;

    @OneToMany(mappedBy = "user")
    private List<CaseParticipation> participations = new ArrayList<>();

    @Builder
    public User(Rank rank, String nickname, String email, String password, Long exp, Integer totalPoints, Integer winCnt, Integer loseCnt) {
        this.rank = rank;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.exp = exp;
        this.totalPoints = totalPoints;
        this.winCnt = winCnt;
        this.loseCnt = loseCnt;
    }

    // --- 비즈니스 로직 편의를 위한 메서드들 ---
    public void addExp(Long amount) {
        this.exp += amount;
        // 경험치 증가 후 랭크업 로직이 있다면 여기서 호출
    }

    public void addPoints(Integer amount) {
        this.totalPoints += amount;
    }

    public void deductPoints(Integer amount) {
        if (this.totalPoints < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.totalPoints -= amount;
    }

    public void updateMypageInfo(UserUpdateRequestDto dto) {
        if(dto.getEmail() != null) {
            this.email = dto.getEmail();
        }
        // 닉네임이 요청에 포함된 경우에만 업데이트
        if (dto.getNickname() != null) {
            this.nickname = dto.getNickname();
        }
        // 프로필 이미지가 요청에 포함된 경우에만 업데이트
        if (dto.getProfileImageUrl() != null) {
            this.profileImageUrl = dto.getProfileImageUrl();
        }
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}