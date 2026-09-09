package com.kim.tudu_api.todo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "todo_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean completed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private TodoListEntity list;
}

