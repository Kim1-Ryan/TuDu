package com.kim.tudu_api.todo.repository;

import com.kim.tudu_api.todo.model.UserBoardLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBoardLinkRepository extends JpaRepository<UserBoardLinkEntity, Long> {

    Optional<UserBoardLinkEntity> findByUser_IdAndBoard_Id(Long userId, Long boardId);
}
