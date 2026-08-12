package com.library.lms.dto.response;

import com.library.lms.model.User;

import java.util.Date;
import java.util.List;

/**
 * Outward-facing representation of a {@link User}.
 * Deliberately omits {@code passwordHash} and every Spring Security flag.
 */
public record UserResponse(
        String id,
        String userId,
        String name,
        String email,
        String phone,
        String role,
        Date cardStartDate,
        Date cardEndDate,
        boolean cardExpired,
        List<String> previouslyReadGenre,
        boolean deleted
) {
    public static UserResponse from(User user) {
        boolean expired = user.getCardEndDate() != null
                && user.getCardEndDate().before(new Date());

        return new UserResponse(
                user.getId(),
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCardStartDate(),
                user.getCardEndDate(),
                expired,
                user.getPreviouslyReadGenre(),
                user.isDeleted()
        );
    }
}
