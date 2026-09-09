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


/*
package za.co.entelect.unwind_api.team;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.co.entelect.unwind_api.team.controller.dto.TeamDto;
import za.co.entelect.unwind_api.team.controller.dto.UpdateMemberRoleRequest;
import za.co.entelect.unwind_api.team.controller.dto.UpdateTeamRequest;
import za.co.entelect.unwind_api.team.exception.InsufficientPermissionsException;
import za.co.entelect.unwind_api.team.service.TeamService;
import za.co.entelect.unwind_api.user.domain.User;
import za.co.entelect.unwind_api.user.service.UserService;
import za.co.entelect.unwind_api.user.domain.relationship.MemberRelationship;
import za.co.entelect.unwind_api.user.domain.relationship.MemberRole;
import za.co.entelect.unwind_api.util.TestTeams;
import za.co.entelect.unwind_api.util.TestUsers;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@SpringBootTest
@Transactional
public class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    static Stream<MemberRole> roleProvider() {
        return Arrays.stream(MemberRole.values());
    }

    static Stream<Arguments> roleCombinationProvider() {
        List<Map<String, MemberRole>> argumentsList = new ArrayList<>();
        roleProvider().forEach(caller -> roleProvider()
                .forEach(target -> argumentsList.add(Map.of("caller", caller, "target", target))));

        return argumentsList.stream().map(m -> Arguments.of(m.get("caller"), m.get("target")));
    }

    @Test
    void test_saveTeam() {
        User user = userService.create(TestUsers.USER1.toBuilder().build());
        final TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());
        user = userService.getById(user.getId());

        assertThat(team).isNotNull();
        assertThat(team.getId()).isNotNull();

        assertThat(team.getMembers()).isNotNull();
        assertThat(team.getMembers().size()).isEqualTo(1);

        MemberRelationship relationship = user.getTeamsJoined().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not added to team"));

        assertThat(relationship.getRole()).isEqualTo(MemberRole.CAPTAIN);
    }

    @ParameterizedTest
    @MethodSource("roleProvider")
    void test_updateTeamWithDifferentRoles(MemberRole testRole) {
        User creator = userService.create(TestUsers.USER1.toBuilder().build());
        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), creator.getId());
        creator = userService.getById(creator.getId());

        User testUser = userService.create(TestUsers.USER2.toBuilder()
                .teamsJoined(Set.of(MemberRelationship.builder()
                        .team(creator.getTeamsJoined().stream().findFirst().get().getTeam())
                        .role(testRole)
                        .build()))
                .build());

        if (testRole.getAdminLevel() > 0) {
            TeamDto updatedTeam = teamService.updateTeam(testUser.getId(), new UpdateTeamRequest(
                    team.getId(),
                    TestTeams.TEAM2.getName(),
                    null));

            assertThat(updatedTeam.getName()).isEqualTo(TestTeams.TEAM2.getName());
            return;
        }

        assertThatCode(() -> {
            teamService.updateTeam(testUser.getId(), new UpdateTeamRequest(
                    team.getId(),
                    TestTeams.TEAM2.getName(),
                    null));
        }).isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void test_updateTeamAsNonTeamMember() {
        User nonMember = userService.create(TestUsers.USER1.toBuilder().build());

        User user = userService.create(TestUsers.USER2.toBuilder().build());

        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());

        assertThatCode(() -> {
            teamService.updateTeam(nonMember.getId(), new UpdateTeamRequest(
                    team.getId(),
                    TestTeams.TEAM2.getName(),
                    null));
        }).isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void test_updateTeamAsAdmin() {
        User admin = userService.create(TestUsers.USER1.toBuilder()
                .admin(true)
                .build());

        User user = userService.create(TestUsers.USER2.toBuilder().build());

        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());

        TeamDto updatedTeam = teamService.updateTeam(admin.getId(), new UpdateTeamRequest(
                team.getId(),
                TestTeams.TEAM2.getName(),
                null));

        assertThat(updatedTeam.getName()).isEqualTo(TestTeams.TEAM2.getName());
    }

    @ParameterizedTest
    @MethodSource("roleCombinationProvider")
    void test_updateRoleAsDifferentRoles(MemberRole callerRole, MemberRole targetRole) {
        User creator = userService.create(TestUsers.USER1.toBuilder().build());
        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), creator.getId());
        creator = userService.getById(creator.getId());

        User caller = userService.create(TestUsers.USER2.toBuilder()
                .teamsJoined(Set.of(MemberRelationship.builder()
                        .team(creator.getTeamsJoined().stream().findFirst().get().getTeam())
                        .role(callerRole)
                        .build()))
                .build());

        User target = userService.create(TestUsers.USER3.toBuilder()
                .teamsJoined(Set.of(MemberRelationship.builder()
                        .team(creator.getTeamsJoined().stream().findFirst().get().getTeam())
                        .role(targetRole)
                        .build()))
                .build());

        int adminLevelDifference = callerRole.getAdminLevel() - targetRole.getAdminLevel();
        MemberRole expectedRole = targetRole == MemberRole.CO_CAPTAIN ? MemberRole.CAPTAIN : MemberRole.CO_CAPTAIN;
        if (callerRole.getAdminLevel() > 0 && adminLevelDifference > 0) {
            teamService.updateTeamRole(caller.getId(), new UpdateMemberRoleRequest(
                    team.getId(),
                    target.getId(),
                    expectedRole));

            User updatedTarget = userService.getById(target.getId());

            assertThat(target.getTeamsJoined().stream().findFirst().get().getRole().equals(targetRole));
            assertThat(updatedTarget.getTeamsJoined().stream().findFirst().get().getRole().equals(expectedRole));
            return;
        }

        assertThatCode(() -> {
            teamService.updateTeamRole(caller.getId(), new UpdateMemberRoleRequest(
                    team.getId(),
                    target.getId(),
                    expectedRole));
        }).isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void test_updateRoleAsNonTeamMember() {
        User user = userService.create(TestUsers.USER1.toBuilder().build());
        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());
        user = userService.getById(user.getId());

        User nonMember = userService.create(TestUsers.USER2.toBuilder().build());

        User recipient = userService.create(TestUsers.USER3.toBuilder()
                .teamsJoined(Set.of(MemberRelationship.builder()
                        .team(user.getTeamsJoined().stream().findFirst().get().getTeam())
                        .role(MemberRole.PLAYER)
                        .build()))
                .build());


        assertThatCode(() -> {
            teamService.updateTeamRole(nonMember.getId(), new UpdateMemberRoleRequest(
                    team.getId(),
                    recipient.getId(),
                    MemberRole.CAPTAIN));
        }).isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void test_updateRoleAsAdmin() {
        User user = userService.create(TestUsers.USER1.toBuilder().build());
        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());
        user = userService.getById(user.getId());

        User recipient = userService.create(TestUsers.USER2.toBuilder()
                .teamsJoined(Set.of(MemberRelationship.builder()
                        .team(user.getTeamsJoined().stream().findFirst().get().getTeam())
                        .role(MemberRole.PLAYER)
                        .build()))
                .build());

        User admin = userService.create(TestUsers.USER3.toBuilder().admin(true).build());

        teamService.updateTeamRole(admin.getId(), new UpdateMemberRoleRequest(
                team.getId(),
                recipient.getId(),
                MemberRole.CO_CAPTAIN));

        User updatedRecipient = userService.getById(recipient.getId());

        assertThat(recipient.getTeamsJoined().stream().findFirst().get().getRole().equals(MemberRole.PLAYER));
        assertThat(updatedRecipient.getTeamsJoined().stream().findFirst().get().getRole().equals(MemberRole.CO_CAPTAIN));
    }

    @ParameterizedTest
    @MethodSource("roleCombinationProvider")
    void test_removeMemberAsDifferentRoles(MemberRole callerRole, MemberRole targetRole) {
        User creator = userService.create(TestUsers.USER1.toBuilder().build());
        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), creator.getId());
        creator = userService.getById(creator.getId());

        User caller = userService.create(TestUsers.USER2.toBuilder()
                .teamsJoined(Set.of(MemberRelationship.builder()
                        .team(creator.getTeamsJoined().stream().findFirst().get().getTeam())
                        .role(callerRole)
                        .build()))
                .build());

        User target = userService.create(TestUsers.USER2.toBuilder()
                .teamsJoined(Set.of(MemberRelationship.builder()
                        .team(creator.getTeamsJoined().stream().findFirst().get().getTeam())
                        .role(targetRole)
                        .build()))
                .build());

        int adminLevelDifference = callerRole.getAdminLevel() - targetRole.getAdminLevel();
        if (callerRole.getAdminLevel() > 0 && adminLevelDifference > 0) {
            teamService.removeMemberFromTeam(caller.getId(), team.getId(), target.getId());
            User updatedTarget = userService.getById(target.getId());

            assertThat(updatedTarget.getTeamsJoined().size()).isEqualTo(0);
            return;
        }

        assertThatCode(() -> {
            teamService.removeMemberFromTeam(caller.getId(), team.getId(), target.getId());
        }).isInstanceOf(InsufficientPermissionsException.class);
    }

//    @Test
//    void test_updateRoleAsTeamNonAdmin() {
//        User user = userService.create(TestUsers.USER1.toBuilder().build());
//        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());
//        user = userService.getById(user.getId());
//
//        User nonAdmin = userService.create(TestUsers.USER2.toBuilder()
//                .teamsJoined(Set.of(MemberRelationship.builder()
//                        .team(user.getTeamsJoined().stream().findFirst().get().getTeam())
//                        .role(MemberRole.PLAYER)
//                        .build()))
//                .build());
//
//        User recipient = userService.create(TestUsers.USER3.toBuilder()
//                .teamsJoined(Set.of(MemberRelationship.builder()
//                        .team(user.getTeamsJoined().stream().findFirst().get().getTeam())
//                        .role(MemberRole.PLAYER)
//                        .build()))
//                .build());
//
//
//        assertThatCode(() -> {
//            teamService.updateTeamRole(nonAdmin.getId(), new UpdateMemberRoleRequest(
//                    team.getId(),
//                    recipient.getId(),
//                    MemberRole.CAPTAIN));
//        }).hasMessageContaining("User lacks leadership permission in team to update");
//    }
//
//    @Test
//    void test_updateRoleAsNonTeamMember() {
//        User user = userService.create(TestUsers.USER1.toBuilder().build());
//        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());
//        user = userService.getById(user.getId());
//
//        User nonMember = userService.create(TestUsers.USER2.toBuilder().build());
//
//        User recipient = userService.create(TestUsers.USER3.toBuilder()
//                .teamsJoined(Set.of(MemberRelationship.builder()
//                        .team(user.getTeamsJoined().stream().findFirst().get().getTeam())
//                        .role(MemberRole.PLAYER)
//                        .build()))
//                .build());
//
//
//        assertThatCode(() -> {
//            teamService.updateTeamRole(nonMember.getId(), new UpdateMemberRoleRequest(
//                    team.getId(),
//                    recipient.getId(),
//                    MemberRole.CAPTAIN));
//        }).hasMessageContaining("User is not an admin and doesn't belong to team");
//    }
//
//    @Test
//    void test_updateRoleAsAdmin() {
//        User user = userService.create(TestUsers.USER1.toBuilder().build());
//        TeamDto team = teamService.createTeam(TestTeams.TEAM1.getName(), user.getId());
//        user = userService.getById(user.getId());
//
//        User recipient = userService.create(TestUsers.USER2.toBuilder()
//                .teamsJoined(Set.of(MemberRelationship.builder()
//                        .team(user.getTeamsJoined().stream().findFirst().get().getTeam())
//                        .role(MemberRole.PLAYER)
//                        .build()))
//                .build());
//
//        User admin = userService.create(TestUsers.USER3.toBuilder().admin(true).build());
//
//        teamService.updateTeamRole(admin.getId(), new UpdateMemberRoleRequest(
//                team.getId(),
//                recipient.getId(),
//                MemberRole.CO_CAPTAIN));
//
//        User updatedRecipient = userService.getById(recipient.getId());
//
//        assertThat(recipient.getTeamsJoined().stream().findFirst().get().getRole().equals(MemberRole.PLAYER));
//        assertThat(updatedRecipient.getTeamsJoined().stream().findFirst().get().getRole().equals(MemberRole.CO_CAPTAIN));
//    }
}


*/
