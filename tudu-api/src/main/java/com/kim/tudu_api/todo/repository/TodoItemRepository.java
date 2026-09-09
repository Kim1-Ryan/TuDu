package com.kim.tudu_api.todo.repository;

import com.kim.tudu_api.todo.model.TodoItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoItemRepository extends JpaRepository<TodoItemEntity, Long> {
}
