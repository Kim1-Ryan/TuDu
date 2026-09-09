package com.kim.tudu_api.todo.model;

import com.kim.tudu_api.user.model.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_board_link")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBoardLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    private BoardPermission permissionLevel;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;
}
