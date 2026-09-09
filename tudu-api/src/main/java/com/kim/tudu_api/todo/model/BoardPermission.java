package com.kim.tudu_api.todo.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum BoardPermission {
    BOARD_CREATOR(4), // can do everything, assigned only to board's creator
    BOARD_WRITE(3), // can create/update
    LIST_WRITE(2), // can create/update lists
    ITEM_WRITE(1), // can create/update items
    ITEM_MARK(0); // can only mark items done/not done

    private final int strength;

    public static boolean satisfiesPermissionLevel(@NonNull BoardPermission requestLevel, @NonNull BoardPermission requiredLevel) {
        return requestLevel.strength >= requiredLevel.strength;
    }


    public static boolean hasSufficientPermissionToGrantPermission(@NonNull BoardPermission holderPermission, @NonNull BoardPermission requestedPermission) {
        if (BOARD_WRITE.strength > holderPermission.strength) {
            return false;
        }

        return holderPermission.strength - requestedPermission.strength > 0;
    }

    /**
     * Method that takes in a board permission and returns one on a level that is one higher
     *
     * @param boardPermission the permission that would like to take one higher of
     *
     * @return the result board permission
     */
    public static @NonNull BoardPermission stepUpPermission(@NonNull BoardPermission boardPermission) {
        if (BOARD_CREATOR.equals(boardPermission)) {
            return BOARD_CREATOR;
        }

        return fromPermissionStrength(boardPermission.getStrength());
    }

    private static BoardPermission fromPermissionStrength(int permissionStrength) {
        return Arrays.stream(values())
                .filter(bp -> bp.strength == (permissionStrength + 1))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid permission level passed in: " + permissionStrength));
    }
}
