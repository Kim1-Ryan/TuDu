package com.kim.tudu_api.todo.service;

import com.kim.tudu_api.todo.controller.dto.*;
import com.kim.tudu_api.todo.mapper.TodoMapper;
import com.kim.tudu_api.todo.model.*;
import com.kim.tudu_api.todo.repository.BoardRepository;
import com.kim.tudu_api.todo.repository.TodoItemRepository;
import com.kim.tudu_api.todo.repository.TodoListRepository;
import com.kim.tudu_api.todo.repository.UserBoardLinkRepository;
import com.kim.tudu_api.user.model.UserEntity;
import com.kim.tudu_api.user.repository.UserRepository;
import com.kim.tudu_api.util.error.AlreadyExistsException;
import com.kim.tudu_api.util.error.InsufficientPermissionException;
import com.kim.tudu_api.util.error.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TodoService {

    private final TodoListRepository listRepository;

    private final TodoItemRepository itemRepository;

    private final UserBoardLinkRepository linkRepository;

    private final BoardRepository boardRepository;

    private final UserRepository userRepository;

    private final TodoMapper todoMapper;

    public BoardDto createBoard(Long userId, CreateBoardRequest request) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Could not find user: " + userId));
        String boardName = request.name().trim();
        if (userEntity.getBoardLinks().stream()
                .anyMatch(link -> BoardPermission.BOARD_CREATOR.equals(link.getPermissionLevel())
                        && link.getBoard().getName().equalsIgnoreCase(boardName))) {
            throw new AlreadyExistsException("User already has a board with name: " + boardName);
        }

        BoardEntity boardEntity = boardRepository.save(BoardEntity.builder()
                .name(boardName)
                .todoLists(new ArrayList<>())
                .build());

        linkRepository.save(UserBoardLinkEntity.builder()
                .user(userEntity)
                .board(boardEntity)
                .permissionLevel(BoardPermission.BOARD_CREATOR)
                .grantedAt(LocalDateTime.now())
                .build());

        return todoMapper.toDto(boardRepository.findById(boardEntity.getId())
                .orElseThrow(() -> new NotFoundException("Could not find updated board with id: " + boardEntity.getId())));
    }

    public void addUserToBoard(Long userId, AddBoardUserRequest request) {
        checkUserHasSufficientPermission(userId, request.boardId(), BoardPermission.BOARD_WRITE);
        checkUserHasSufficientPermissionToGrantPermission(userId, request.boardId(), request.boardPermission());
        if (linkRepository.findByUser_IdAndBoard_Id(request.userId(), request.boardId()).isPresent()) {
            throw new AlreadyExistsException("User already linked to board");
        }

        BoardEntity boardEntity = boardRepository.findById(request.boardId())
                .orElseThrow(() -> new NotFoundException("Could not get board: " + request.boardId()));
        UserEntity requestUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("Could not get user: " + request.userId()));

        linkRepository.save(UserBoardLinkEntity.builder()
                .user(requestUser)
                .board(boardEntity)
                .permissionLevel(request.boardPermission())
                .grantedAt(LocalDateTime.now())
                .build());
    }

    public TodoListDto createList(Long userId, CreateListRequest request) {
        checkUserHasSufficientPermission(userId, request.boardId(), BoardPermission.LIST_WRITE);
        String listName = request.name().trim();
        BoardEntity boardEntity = boardRepository.findById(request.boardId())
                .orElseThrow(() -> new NotFoundException("Could not find board: " + request.boardId()));
        if (boardEntity.getTodoLists().stream()
                .anyMatch(list -> listName.equalsIgnoreCase(list.getName()))) {
            throw new AlreadyExistsException("Board already has a list with name: " + listName);
        }

        TodoListEntity listEntity = listRepository.save(TodoListEntity.builder()
                .name(listName)
                .todoItems(new ArrayList<>())
                .board(boardEntity)
                .build());
        boardEntity.getTodoLists().add(listEntity);
        return todoMapper.toDto(listEntity);
    }

    public TodoItemDto createListItem(Long userId, CreateListItemRequest request) {
        TodoListEntity listEntity = listRepository.findById(request.listId())
                .orElseThrow(() -> new NotFoundException("Could not find list: " + request.listId()));
        checkUserHasSufficientPermission(userId, listEntity.getBoard().getId(), BoardPermission.ITEM_WRITE);
        String description = request.description().trim();
        if (listEntity.getTodoItems().stream()
                .anyMatch(item -> description.equalsIgnoreCase(item.getDescription()))) {
            throw new AlreadyExistsException("Board already has a list with name: " + description);
        }

        TodoItemEntity itemEntity = itemRepository.save(TodoItemEntity.builder()
                .description(description)
                .completed(false)
                .list(listEntity)
                .build());
        listEntity.getTodoItems().add(itemEntity);
        return todoMapper.toDto(itemEntity);
    }

    public BoardDto getBoardById(Long userId, Long boardId) {
        checkUserHasSufficientPermission(userId, boardId, BoardPermission.ITEM_MARK);
        BoardEntity boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException("Could not find board: " + boardId));
        return todoMapper.toDto(boardEntity);
    }

    public List<BoardDto> getBoardsByUserId(Long callerUserId, Long userId) {
        UserEntity callerEntity = userRepository.findById(callerUserId)
                .orElseThrow(() -> new NotFoundException("Could not find user: " + callerUserId));
        if (!callerEntity.isAdmin() && !callerUserId.equals(userId)) {
            throw new InsufficientPermissionException("Insufficient permission to view user");
        }

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Could not find user: " + userId));
        return userEntity.getBoardLinks().stream()
                .map(link -> todoMapper.toDto(link.getBoard()))
                .toList();
    }

    public TodoListDto getListById(Long userId, Long listId) {
        TodoListEntity listEntity = listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("Could not find list: " + listId));
        checkUserHasSufficientPermission(userId, listEntity.getBoard().getId(), BoardPermission.ITEM_MARK);
        return todoMapper.toDto(listEntity);
    }

    public List<TodoListDto> getListsByBoardId(Long userId, Long boardId) {
        checkUserHasSufficientPermission(userId, boardId, BoardPermission.ITEM_MARK);
        BoardEntity boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException("Could not find board: " + boardId));
        return boardEntity.getTodoLists().stream()
                .map(todoMapper::toDto)
                .toList();
    }

    public TodoItemDto getListItemById(Long userId, Long itemId) {
        TodoItemEntity itemEntity = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Could not find item: " + itemId));
        checkUserHasSufficientPermission(userId, itemEntity.getList().getBoard().getId(), BoardPermission.ITEM_MARK);
        return todoMapper.toDto(itemEntity);
    }

    public List<TodoItemDto> getListItemsByListId(Long userId, Long listId) {
        TodoListEntity listEntity = listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("Could not find list: " + listId));
        checkUserHasSufficientPermission(userId, listEntity.getBoard().getId(), BoardPermission.ITEM_MARK);
        return listEntity.getTodoItems().stream()
                .map(todoMapper::toDto)
                .toList();
    }

    public BoardDto updateBoard(Long userId, UpdateBoardRequest request) {
        checkUserHasSufficientPermission(userId, request.id(), BoardPermission.BOARD_WRITE);
        BoardEntity boardEntity = boardRepository.findById(request.id())
                .orElseThrow(() -> new NotFoundException("Could not find board: " + request.id()));
        todoMapper.updateBoardEntity(boardEntity, request);
        return todoMapper.toDto(boardRepository.save(boardEntity));
    }

    public void deleteBoard(Long userId, Long boardId) {
        checkUserHasSufficientPermission(userId, boardId, BoardPermission.BOARD_CREATOR);
        boardRepository.deleteById(boardId);
    }

    public void removeUserAccessFromBoard(Long callerUserId, Long boardId, Long userId) {
        checkUserHasSufficientPermissionOverUser(callerUserId, boardId, userId);
        UserBoardLinkEntity linkEntity = linkRepository.findByUser_IdAndBoard_Id(userId, boardId)
                .orElseThrow(() -> new NotFoundException("Could not find link for user " + userId + " and board " + boardId));
        linkEntity.getUser().getBoardLinks().remove(linkEntity);
        linkEntity.getBoard().getUserLinks().remove(linkEntity);
        linkRepository.delete(linkEntity);
    }

    public void deleteList(Long userId, Long listId) {
        TodoListEntity listEntity = listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("Could not find list: " + listId));
        BoardEntity boardEntity = listEntity.getBoard();
        checkUserHasSufficientPermission(userId, boardEntity.getId(), BoardPermission.LIST_WRITE);
        boardEntity.getTodoLists().removeIf(list -> Objects.equals(listId, list.getId()));
    }

    public void deleteListItem(Long userId, Long itemId) {
        TodoItemEntity itemEntity = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Could not find list item: " + itemId));
        TodoListEntity listEntity = itemEntity.getList();
        Long boardId = itemEntity.getList().getBoard().getId();
        checkUserHasSufficientPermission(userId, boardId, BoardPermission.ITEM_WRITE);
        listEntity.getTodoItems().removeIf(item -> Objects.equals(itemId, item.getId()));
    }

    private void checkUserHasSufficientPermission(Long userId, Long boardId, BoardPermission requiredLevel) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Cound not find user: " + userId));
        if (userEntity.isAdmin()) {
            return;
        }
        UserBoardLinkEntity entity = linkRepository.findByUser_IdAndBoard_Id(userId, boardId)
                .orElseThrow(() -> new NotFoundException("Could not find link for user " + userId + " and board " + boardId));
        if (!BoardPermission.satisfiesPermissionLevel(entity.getPermissionLevel(), requiredLevel)) {
            throw new InsufficientPermissionException(String.format(
                    "User id [%d] fails to satisfy permission requirement of [%s], current level: [%s]",
                    userId,
                    requiredLevel,
                    entity.getPermissionLevel()));
        }
    }

    private void checkUserHasSufficientPermissionToGrantPermission(Long userId, Long boardId, BoardPermission requestedPermission) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Could not find user: " + userId));
        if (userEntity.isAdmin()) {
            return;
        }
        UserBoardLinkEntity linkEntity = userEntity.getBoardLinks().stream()
                .filter(link -> Objects.equals(boardId, link.getBoard().getId()))
                .findFirst()
                .orElseThrow(() -> new InsufficientPermissionException("User does not have access to board"));
        if (!BoardPermission.hasSufficientPermissionToGrantPermission(
                linkEntity.getPermissionLevel(),
                requestedPermission)) {
            throw new InsufficientPermissionException("User lacks permission strength to grant requested permission");
        }
    }

    private void checkUserHasSufficientPermissionOverUser(Long callerUserId, Long boardId, Long userId) {
        UserBoardLinkEntity callerLinkEntity = linkRepository.findByUser_IdAndBoard_Id(callerUserId, boardId)
                .orElseThrow(() -> new NotFoundException("Could not find link for user " + callerUserId + " and board " + boardId));
        boolean isCallerAdmin = callerLinkEntity.getUser().isAdmin();
        if (!isCallerAdmin && !BoardPermission.satisfiesPermissionLevel(callerLinkEntity.getPermissionLevel(), BoardPermission.BOARD_WRITE)) {
            throw new InsufficientPermissionException(String.format(
                    "User id [%d] fails to satisfy permission requirement of [%s], current level: [%s]",
                    callerUserId,
                    BoardPermission.BOARD_WRITE,
                    callerLinkEntity.getPermissionLevel()));
        }

        UserBoardLinkEntity linkEntity = linkRepository.findByUser_IdAndBoard_Id(userId, boardId)
                .orElseThrow(() -> new NotFoundException("Could not find link for user " + userId + " and board " + boardId));
        int permissionLevelDifference = callerLinkEntity.getPermissionLevel().getStrength() - linkEntity.getPermissionLevel().getStrength();
        if (!isCallerAdmin && permissionLevelDifference < 1) {
            throw new InsufficientPermissionException(String.format(
                    "User id [%d] with permission level [%s] does not have enough permission power over other user [%s]",
                    callerUserId,
                    callerLinkEntity.getPermissionLevel(),
                    linkEntity.getPermissionLevel()));
        }
    }
}
