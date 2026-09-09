package com.kim.tudu_api.todo;

import com.kim.tudu_api.todo.controller.dto.*;
import com.kim.tudu_api.todo.mapper.TodoMapper;
import com.kim.tudu_api.todo.model.*;
import com.kim.tudu_api.todo.repository.BoardRepository;
import com.kim.tudu_api.todo.repository.TodoItemRepository;
import com.kim.tudu_api.todo.repository.TodoListRepository;
import com.kim.tudu_api.todo.repository.UserBoardLinkRepository;
import com.kim.tudu_api.todo.service.TodoService;
import com.kim.tudu_api.user.model.UserEntity;
import com.kim.tudu_api.user.repository.UserRepository;
import com.kim.tudu_api.util.TestUsers;
import com.kim.tudu_api.util.error.AlreadyExistsException;
import com.kim.tudu_api.util.error.InsufficientPermissionException;
import com.kim.tudu_api.util.error.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@SpringBootTest
@Transactional
public class TodoServiceTest {

    @Autowired
    BoardRepository boardRepository;

    @Autowired
    UserBoardLinkRepository linkRepository;

    @Autowired
    TodoListRepository listRepository;

    @Autowired
    TodoItemRepository itemRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TodoService todoService;

    @Autowired
    TodoMapper todoMapper;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void beforeEach() {
        UserEntity userEntity = TestUsers.USER1.toBuilder()
                .boardLinks(new ArrayList<>())
                .build();
        userRepository.save(userEntity);

        TodoListEntity listEntity = TodoListEntity.builder()
                .name("Chores")
                .todoItems(new ArrayList<>())
                .build();

        for (String desc : List.of("Cooking", "Laundry", "Washing")) {
            TodoItemEntity item = TodoItemEntity.builder().description(desc).build();
            item.setList(listEntity);
            listEntity.getTodoItems().add(item);
        }

        BoardEntity boardEntity = BoardEntity.builder()
                .name("My board")
                .todoLists(new ArrayList<>(List.of(listEntity)))
                .userLinks(new ArrayList<>())
                .build();
        listEntity.setBoard(boardEntity);
        boardRepository.save(boardEntity);

        UserBoardLinkEntity linkEntity = UserBoardLinkEntity.builder()
                .user(userEntity)
                .board(boardEntity)
                .permissionLevel(BoardPermission.BOARD_CREATOR)
                .grantedAt(LocalDateTime.now())
                .build();
        boardEntity.getUserLinks().add(linkEntity);
        userEntity.getBoardLinks().add(linkEntity);
        linkRepository.save(linkEntity);
    }

    static Stream<BoardPermission> boardPermissionProvider() {
        return Arrays.stream(BoardPermission.values());
    }

    static Stream<Arguments> permissionCombinationProvider() {
        List<Map<String, BoardPermission>> argumentsList = new ArrayList<>();
        boardPermissionProvider().forEach(caller -> boardPermissionProvider()
                .forEach(target -> argumentsList.add(Map.of("caller", caller, "target", target))));

        return argumentsList.stream().map(m -> Arguments.of(m.get("caller"), m.get("target")));
    }

    @Test
    void testMapperConvertsCorrectly() {
        // item conversion
        TodoItemEntity itemEntity = itemRepository.findAll().stream().findFirst().orElseThrow();
        TodoItemDto itemDto = todoMapper.toDto(itemEntity);
        assertThat(itemDto.getId()).isEqualTo(itemEntity.getId());
        assertThat(itemDto.getDescription()).isEqualTo(itemEntity.getDescription());
        assertThat(itemDto.isCompleted()).isEqualTo(itemEntity.isCompleted());

        // list conversion
        TodoListEntity listEntity = listRepository.findAll().stream().findFirst().orElseThrow();
        TodoListDto listDto = todoMapper.toDto(listEntity);
        assertThat(listDto.getId()).isEqualTo(listEntity.getId());
        assertThat(listDto.getName()).isEqualTo(listEntity.getName());
        assertThat(listDto.getItems().size()).isEqualTo(listEntity.getTodoItems().size());

        BoardEntity boardEntity = listEntity.getBoard();

        UserBoardLinkEntity linkEntity = boardEntity.getUserLinks().stream().findFirst().orElseThrow();
        UserEntity userEntity = linkEntity.getUser();
        BoardUserDto boardUserDto = todoMapper.toDto(linkEntity);
        assertThat(boardUserDto.getId()).isEqualTo(userEntity.getId());
        assertThat(boardUserDto.getUsername()).isEqualTo(userEntity.getUsername());
        assertThat(boardUserDto.isAdmin()).isEqualTo(userEntity.isAdmin());
        assertThat(boardUserDto.getPermissionLevel()).isEqualTo(linkEntity.getPermissionLevel());

        BoardDto boardDto = todoMapper.toDto(boardEntity);
        assertThat(boardDto.getId()).isEqualTo(boardEntity.getId());
        assertThat(boardDto.getName()).isEqualTo(boardEntity.getName());
        assertThat(boardDto.getUsers().size()).isEqualTo(boardEntity.getUserLinks().size());
        assertThat(boardDto.getLists().size()).isEqualTo(boardEntity.getTodoLists().size());
    }

    @Test
    void shouldNotCreateBoardIfInvalidName() {
        UserEntity userEntity = userRepository.findAll().getFirst();
        CreateBoardRequest request = new CreateBoardRequest("mY bOaRd");

        assertThatCode(() -> todoService.createBoard(userEntity.getId(), request))
                .isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    void shouldCreateBoardIfValidName() {
        UserEntity userEntity = userRepository.findAll().getFirst();
        CreateBoardRequest request = new CreateBoardRequest("Second Board");

        todoService.createBoard(userEntity.getId(), request);

        assertThat(boardRepository.findAll().size()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("permissionCombinationProvider")
    void shouldAddUserToBoardIfHavePermissionLevel(BoardPermission userPermission, BoardPermission targetPermission) {
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        UserEntity callerEntity = userRepository.save(TestUsers.USER2.toBuilder()
                .boardLinks(new ArrayList<>())
                .build());
        UserBoardLinkEntity callerLinkEntity = linkRepository.save(UserBoardLinkEntity.builder()
                .user(callerEntity)
                .board(boardEntity)
                .permissionLevel(userPermission)
                .grantedAt(LocalDateTime.now())
                .build());
        callerEntity.getBoardLinks().add(callerLinkEntity);
        boardEntity.getUserLinks().add(callerLinkEntity);

        UserEntity targetEntity = userRepository.save(TestUsers.USER3.toBuilder()
                .boardLinks(new ArrayList<>())
                .build());

        if (BoardPermission.BOARD_WRITE.getStrength() <= userPermission.getStrength()
                && userPermission.getStrength() > targetPermission.getStrength()) {

            entityManager.flush();
            entityManager.clear();

            todoService.addUserToBoard(callerEntity.getId(),
                    new AddBoardUserRequest(boardEntity.getId(), targetEntity.getId(), targetPermission));


            BoardEntity updatedBoard = boardRepository.findAll().getFirst();
            assertThat(updatedBoard.getUserLinks().size()).isEqualTo(3);

            return;
        }

        assertThatCode(() -> todoService.addUserToBoard(callerEntity.getId(),
                new AddBoardUserRequest(boardEntity.getId(), targetEntity.getId(), targetPermission))
        ).isInstanceOf(InsufficientPermissionException.class);
    }

    @ParameterizedTest
    @MethodSource("boardPermissionProvider")
    void shouldCreateListIfHavePermissionLevel(BoardPermission userPermission) {
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        UserEntity callerEntity = userRepository.save(TestUsers.USER2.toBuilder()
                .boardLinks(new ArrayList<>())
                .build());
        UserBoardLinkEntity callerLinkEntity = linkRepository.save(UserBoardLinkEntity.builder()
                .user(callerEntity)
                .board(boardEntity)
                .permissionLevel(userPermission)
                .grantedAt(LocalDateTime.now())
                .build());
        callerEntity.getBoardLinks().add(callerLinkEntity);
        boardEntity.getUserLinks().add(callerLinkEntity);

        if (BoardPermission.LIST_WRITE.getStrength() <= userPermission.getStrength()) {
            todoService.createList(callerEntity.getId(),
                    new CreateListRequest(boardEntity.getId(), "Test List"));

            BoardEntity updatedBoard = boardRepository.findAll().getFirst();
            assertThat(updatedBoard.getTodoLists().size()).isEqualTo(2);
            return;
        }

        assertThatCode(() -> todoService.createList(callerEntity.getId(),
                new CreateListRequest(boardEntity.getId(), "Test List"))
        ).isInstanceOf(InsufficientPermissionException.class);
    }

    @ParameterizedTest
    @MethodSource("boardPermissionProvider")
    void shouldCreateListItemIfHavePermissionLevel(BoardPermission userPermission) {
        TodoListEntity listEntity = listRepository.findAll().getFirst();
        UserEntity callerEntity = userRepository.save(TestUsers.USER2.toBuilder()
                .boardLinks(new ArrayList<>())
                .build());
        UserBoardLinkEntity callerLinkEntity = linkRepository.save(UserBoardLinkEntity.builder()
                .user(callerEntity)
                .board(listEntity.getBoard())
                .permissionLevel(userPermission)
                .grantedAt(LocalDateTime.now())
                .build());
        callerEntity.getBoardLinks().add(callerLinkEntity);
        listEntity.getBoard().getUserLinks().add(callerLinkEntity);

        if (BoardPermission.ITEM_WRITE.getStrength() <= userPermission.getStrength()) {
            todoService.createListItem(callerEntity.getId(),
                    new CreateListItemRequest(listEntity.getId(), "Test Item"));

            TodoListEntity updatedListEntity = listRepository.findAll().getFirst();
            assertThat(updatedListEntity.getTodoItems().size()).isEqualTo(4);
            return;
        }

        assertThatCode(() -> todoService.createListItem(callerEntity.getId(),
                new CreateListItemRequest(listEntity.getId(), "Test Item"))
        ).isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void shouldGetBoardIfLinked() {
        UserEntity userEntity = userRepository.findAll().getFirst();
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        BoardDto dto = todoService.getBoardById(userEntity.getId(), boardEntity.getId());
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldGetBoardIfAdmin() {
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().admin(true).build());
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        BoardDto dto = todoService.getBoardById(userEntity.getId(), boardEntity.getId());
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldGetNotBoardIfNotLinked() {
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().build());
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        assertThatCode(() -> todoService.getBoardById(userEntity.getId(), boardEntity.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldGetBoardsIfSelf() {
        UserEntity userEntity = userRepository.findAll().getFirst();
        List<BoardDto> boardList = todoService.getBoardsByUserId(userEntity.getId(), userEntity.getId());
        assertThat(boardList).isNotNull();
        assertThat(boardList.size()).isEqualTo(1);
    }

    @Test
    void shouldGetBoardsIfAdmin() {
        UserEntity userEntity = userRepository.findAll().getFirst();
        UserEntity callerEntity = userRepository.save(TestUsers.USER2.toBuilder().admin(true).build());
        List<BoardDto> boardList = todoService.getBoardsByUserId(callerEntity.getId(), userEntity.getId());
        assertThat(boardList).isNotNull();
        assertThat(boardList.size()).isEqualTo(1);
    }

    @Test
    void shouldNotGetBoardsIfNotSelf() {
        UserEntity userEntity = userRepository.findAll().getFirst();
        UserEntity callerEntity = userRepository.save(TestUsers.USER2.toBuilder().build());
        assertThatCode(() -> todoService.getBoardsByUserId(callerEntity.getId(), userEntity.getId()))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void shouldGetListIfLinked() {
        TodoListEntity listEntity = listRepository.findAll().getFirst();
        UserEntity userEntity = userRepository.findAll().getFirst();
        TodoListDto dto = todoService.getListById(userEntity.getId(), listEntity.getId());
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldGetListIfAdmin() {
        TodoListEntity listEntity = listRepository.findAll().getFirst();
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder()
                .admin(true)
                .build());
        TodoListDto dto = todoService.getListById(userEntity.getId(), listEntity.getId());
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldNotGetListIfNotLinked() {
        TodoListEntity listEntity = listRepository.findAll().getFirst();
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().build());
        assertThatCode(() -> todoService.getListById(userEntity.getId(), listEntity.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldGetListsIfLinked() {
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        UserEntity userEntity = userRepository.findAll().getFirst();
        List<TodoListDto> dtoList = todoService.getListsByBoardId(userEntity.getId(), boardEntity.getId());
        assertThat(dtoList).isNotNull();
        assertThat(dtoList.size()).isEqualTo(1);
    }

    @Test
    void shouldGetListsIfAdmin() {
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().admin(true).build());
        List<TodoListDto> dtoList = todoService.getListsByBoardId(userEntity.getId(), boardEntity.getId());
        assertThat(dtoList).isNotNull();
        assertThat(dtoList.size()).isEqualTo(1);
    }

    @Test
    void shouldNotGetListsIfNotLinked() {
        BoardEntity boardEntity = boardRepository.findAll().getFirst();
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().build());
        assertThatCode(() -> todoService.getListsByBoardId(userEntity.getId(), boardEntity.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldGetItemIfLinked() {
        UserEntity userEntity = userRepository.findAll().getFirst();
        TodoItemEntity itemEntity = itemRepository.findAll().getFirst();
        TodoItemDto dto = todoService.getListItemById(userEntity.getId(), itemEntity.getId());
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldGetItemIfAdmin() {
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().admin(true).build());
        TodoItemEntity itemEntity = itemRepository.findAll().getFirst();
        TodoItemDto dto = todoService.getListItemById(userEntity.getId(), itemEntity.getId());
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldNotGetItemIfNotLinked() {
        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().build());
        TodoItemEntity itemEntity = itemRepository.findAll().getFirst();
        assertThatCode(() -> todoService.getListItemById(userEntity.getId(), itemEntity.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @ParameterizedTest
    @MethodSource("boardPermissionProvider")
    void shouldDeleteBoardIfHavePermissionLevel(BoardPermission userPermission) {
        UserEntity creatorEntity = userRepository
                .findByEmailEqualsIgnoreCaseOrUsernameEqualsIgnoreCase("", TestUsers.USER1.getUsername()).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find entity"));

        UserBoardLinkEntity linkEntity = creatorEntity.getBoardLinks().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find link"));

        BoardEntity boardEntity = boardRepository.findById(linkEntity.getBoard().getId())
                .orElseThrow(() -> new RuntimeException("Could not find board"));

        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().build());

        UserBoardLinkEntity userLinkEntity = linkRepository.save(UserBoardLinkEntity.builder()
                .user(userEntity)
                .board(boardEntity)
                .permissionLevel(userPermission)
                .grantedAt(LocalDateTime.now())
                .build());

        if (BoardPermission.satisfiesPermissionLevel(userPermission, BoardPermission.BOARD_CREATOR)) {
            entityManager.flush();
            entityManager.clear();

            todoService.deleteBoard(userEntity.getId(), boardEntity.getId());

            assertThat(boardRepository.findById(boardEntity.getId())).isEmpty();
            assertThat(linkRepository.findById(userLinkEntity.getId())).isEmpty();
            return;
        }

        assertThatCode(() -> todoService.deleteBoard(userEntity.getId(), boardEntity.getId()))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @ParameterizedTest
    @MethodSource("permissionCombinationProvider")
    void shouldRemoveUserFromBoardIfHavePermissionStrength(BoardPermission callerPermission, BoardPermission targetPermission) {
        UserEntity creatorEntity = userRepository
                .findByEmailEqualsIgnoreCaseOrUsernameEqualsIgnoreCase("", TestUsers.USER1.getUsername()).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find user"));

        UserBoardLinkEntity linkEntity = creatorEntity.getBoardLinks().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find link"));

        BoardEntity boardEntity = boardRepository.findById(linkEntity.getBoard().getId())
                .orElseThrow(() -> new RuntimeException("Could not find board"));

        UserEntity callerEntity = userRepository.save(TestUsers.USER2.toBuilder()
                .boardLinks(new ArrayList<>())
                .build());

        UserBoardLinkEntity callerLinkEntity = linkRepository.save(UserBoardLinkEntity.builder()
                .user(callerEntity)
                .board(boardEntity)
                .permissionLevel(callerPermission)
                .grantedAt(LocalDateTime.now())
                .build());

        callerEntity.getBoardLinks().add(callerLinkEntity);
        userRepository.save(callerEntity);

        UserEntity targetEntity = userRepository.save(TestUsers.USER3.toBuilder()
                .boardLinks(new ArrayList<>())
                .build());

        UserBoardLinkEntity targetLinkEntity = linkRepository.save(UserBoardLinkEntity.builder()
                .user(targetEntity)
                .board(boardEntity)
                .permissionLevel(targetPermission)
                .grantedAt(LocalDateTime.now())
                .build());

        targetEntity.getBoardLinks().add(targetLinkEntity);
        userRepository.save(targetEntity);

        entityManager.flush();
        entityManager.refresh(boardEntity);
        assertThat(boardEntity.getUserLinks().size()).isEqualTo(3);

        int permissionStrengthDiff = callerPermission.getStrength() - targetPermission.getStrength();
        if (BoardPermission.satisfiesPermissionLevel(callerPermission, BoardPermission.BOARD_WRITE) && permissionStrengthDiff > 0) {
            todoService.removeUserAccessFromBoard(callerEntity.getId(), boardEntity.getId(), targetEntity.getId());
            assertThat(boardEntity.getUserLinks().size()).isEqualTo(2);
            assertThat(targetEntity.getBoardLinks().size()).isEqualTo(0);

            assertThatCode(() -> linkRepository.findById(targetLinkEntity.getId())
                        .orElseThrow(() -> new NotFoundException("Cannot find link"))
            ).isInstanceOf(NotFoundException.class);
            return;
        }

        assertThatCode(() -> todoService.removeUserAccessFromBoard(callerEntity.getId(), boardEntity.getId(), targetEntity.getId()))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @ParameterizedTest
    @MethodSource("boardPermissionProvider")
    void shouldDeleteListFromBoardIfHavePermissionLevel(BoardPermission userPermission) {
        UserEntity creatorEntity = userRepository
                .findByEmailEqualsIgnoreCaseOrUsernameEqualsIgnoreCase("", TestUsers.USER1.getUsername()).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find entity"));

        UserBoardLinkEntity linkEntity = creatorEntity.getBoardLinks().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find link"));

        BoardEntity boardEntity = boardRepository.findById(linkEntity.getBoard().getId())
                .orElseThrow(() -> new RuntimeException("Could not find board"));

        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().build());

        linkRepository.save(UserBoardLinkEntity.builder()
                .user(userEntity)
                .board(boardEntity)
                .permissionLevel(userPermission)
                .grantedAt(LocalDateTime.now())
                .build());

        TodoListEntity listEntity = boardEntity.getTodoLists().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find list"));

        if (BoardPermission.satisfiesPermissionLevel(userPermission, BoardPermission.LIST_WRITE)) {
            int listSize = boardEntity.getTodoLists().size();

            TodoItemEntity itemEntity = listEntity.getTodoItems().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not get item"));

            todoService.deleteList(userEntity.getId(), listEntity.getId());

            entityManager.flush();
            entityManager.clear();

            assertThat(boardRepository.findById(boardEntity.getId()).orElseThrow().getTodoLists().size())
                    .isEqualTo(listSize - 1);
            assertThat(itemRepository.findById(itemEntity.getId())).isEmpty();
            return;
        }

        assertThatCode(() -> todoService.deleteList(userEntity.getId(), listEntity.getId()))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @ParameterizedTest
    @MethodSource("boardPermissionProvider")
    void shouldDeleteItemFromListIfHavePermissionLevel(BoardPermission userPermission) {
        UserEntity creatorEntity = userRepository
                .findByEmailEqualsIgnoreCaseOrUsernameEqualsIgnoreCase("", TestUsers.USER1.getUsername()).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find entity"));

        UserBoardLinkEntity linkEntity = creatorEntity.getBoardLinks().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find link"));

        BoardEntity boardEntity = boardRepository.findById(linkEntity.getBoard().getId())
                .orElseThrow(() -> new RuntimeException("Could not find board"));

        UserEntity userEntity = userRepository.save(TestUsers.USER2.toBuilder().build());

        linkRepository.save(UserBoardLinkEntity.builder()
                .user(userEntity)
                .board(boardEntity)
                .permissionLevel(userPermission)
                .grantedAt(LocalDateTime.now())
                .build());

        TodoListEntity listEntity = boardEntity.getTodoLists().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find list"));

        TodoItemEntity itemEntity = listEntity.getTodoItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find item"));

        if (BoardPermission.satisfiesPermissionLevel(userPermission, BoardPermission.ITEM_WRITE)) {
            int listSize = listEntity.getTodoItems().size();
            todoService.deleteListItem(userEntity.getId(), itemEntity.getId());

            TodoListEntity updatedListEntity = listRepository.findById(listEntity.getId())
                    .orElseThrow(() -> new RuntimeException("Could not find updated list"));

            assertThat(updatedListEntity).isNotNull();
            assertThat(updatedListEntity.getTodoItems().size()).isEqualTo(listSize - 1);

            entityManager.flush();
            entityManager.clear();

            assertThat(itemRepository.findById(itemEntity.getId())).isEmpty();
            return;
        }

        assertThatCode(() -> todoService.deleteListItem(userEntity.getId(), itemEntity.getId()))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}
