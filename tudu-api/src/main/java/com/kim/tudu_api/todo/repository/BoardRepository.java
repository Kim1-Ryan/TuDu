package com.kim.tudu_api.todo.repository;

import com.kim.tudu_api.todo.model.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
}
