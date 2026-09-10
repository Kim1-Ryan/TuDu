package com.kim.tudu_api.todo.controller;

import com.kim.tudu_api.todo.controller.dto.*;
import com.kim.tudu_api.todo.service.TodoService;
import com.kim.tudu_api.util.Authorities;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/todo")
public class TodoController {

    private final TodoService todoService;

    @PostMapping("/board")
    @Secured(Authorities.USER)
    public BoardDto createBoard(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateBoardRequest request) {
        log.info("Request to create new board [{}] by user [{}]", request, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.createBoard(authenticatedUserId, request);
    }

    @PostMapping("/user-access")
    @Secured(Authorities.USER)
    public void addUserToBoard(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddBoardUserRequest request) {
        log.info("Request to add user [{}] to board [{}] by user [{}]",
                request.userId(),
                request.boardId(),
                jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        todoService.addUserToBoard(authenticatedUserId, request);
    }

    @PostMapping("/list")
    @Secured(Authorities.USER)
    public TodoListDto createList(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateListRequest request) {
        log.info("Request to add list [{}] to board [{}] by user [{}]",
                request.name(),
                request.boardId(),
                jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.createList(authenticatedUserId, request);
    }

    @PostMapping("/item")
    @Secured(Authorities.USER)
    public TodoItemDto createListItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateListItemRequest request) {
        log.info("Request to add item [{}] to list [{}] by user [{}]",
                request.description(),
                request.listId(),
                jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.createListItem(authenticatedUserId, request);
    }

    @GetMapping("/board/{boardId}")
    @Secured(Authorities.USER)
    public BoardDto getBoardById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long boardId) {
        log.info("Request to get board by id [{}] by user [{}]", boardId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.getBoardById(authenticatedUserId, boardId);
    }

    @GetMapping("/board/by-user/{userId}")
    @Secured(Authorities.USER)
    public List<BoardDto> getBoardsByUserId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId) {
        log.info("Request to get boards by id [{}] by user [{}]", userId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.getBoardsByUserId(authenticatedUserId, userId);
    }

    @GetMapping("/list/{listId}")
    @Secured(Authorities.USER)
    public TodoListDto getListById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long listId) {
        log.info("Request to get list by id [{}] by user [{}]", listId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.getListById(authenticatedUserId, listId);
    }

    @GetMapping("/list/by-board/{boardId}")
    @Secured(Authorities.USER)
    public List<TodoListDto> getListsByBoardId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long boardId) {
        log.info("Request to get list by board [{}] by user [{}]", boardId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.getListsByBoardId(authenticatedUserId, boardId);
    }

    @GetMapping("/item/{itemId}")
    @Secured(Authorities.USER)
    public TodoItemDto getListItemById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        log.info("Request to get item [{}] by user [{}]", itemId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.getListItemById(authenticatedUserId, itemId);
    }

    @GetMapping("/item/by-list/{listId}")
    @Secured(Authorities.USER)
    public List<TodoItemDto> getItemsByListId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long listId) {
        log.info("Request to get items by list [{}] by user [{}]", listId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.getListItemsByListId(authenticatedUserId, listId);
    }

    @PutMapping("/board")
    @Secured(Authorities.USER)
    public BoardDto updateBoard(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateBoardRequest request) {
        log.info("Request to update board [{}] by user [{}]", request, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        return todoService.updateBoard(authenticatedUserId, request);
    }

    @PutMapping("/user-access")
    @Secured(Authorities.USER)
    public void updateUserAccessToBoard(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateBoardUserRequest request) {
        log.info("Request to update user access to board [{}] by user [{}]", request, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        todoService.updateUserAccessToBoard(authenticatedUserId, request);
    }

    // TODO: update todo list
    // TODO: update todo item
    // TODO: mark item complete/not complete


    @DeleteMapping("/board/{boardId}")
    @Secured(Authorities.USER)
    public void deleteBoard(@AuthenticationPrincipal Jwt jwt, @PathVariable Long boardId) {
        log.info("Request to delete board [{}] by user [{}]", boardId, jwt.getSubject());

        Long authenticationUserId = jwt.getClaim("id");
        todoService.deleteBoard(authenticationUserId, boardId);
    }

    @DeleteMapping("/user-access/{boardId}/{userId}")
    @Secured(Authorities.USER)
    public void removeUserAccessFromBoard(@AuthenticationPrincipal Jwt jwt, @PathVariable Long boardId, @PathVariable Long userId) {
        log.info("Request to remove user [{}] access to board [{}] by [{}]", userId, boardId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        todoService.removeUserAccessFromBoard(authenticatedUserId, boardId, userId);
    }

    @DeleteMapping("/list/{listId}")
    @Secured(Authorities.USER)
    public void deleteTodoList(@AuthenticationPrincipal Jwt jwt, @PathVariable Long listId) {
        log.info("Request to delete list [{}] by user [{}]", listId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        todoService.deleteList(authenticatedUserId, listId);
    }

    @DeleteMapping("/item/{itemId}")
    @Secured(Authorities.USER)
    public void deleteTodoItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        log.info("Request to delete item [{}] by user [{}]", itemId, jwt.getSubject());

        Long authenticatedUserId = jwt.getClaim("id");
        todoService.deleteListItem(authenticatedUserId, itemId);
    }
}
