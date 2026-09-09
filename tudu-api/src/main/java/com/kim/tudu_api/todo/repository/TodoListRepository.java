package com.kim.tudu_api.todo.repository;

import com.kim.tudu_api.todo.model.TodoListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoListRepository extends JpaRepository<TodoListEntity, Long> {
}
