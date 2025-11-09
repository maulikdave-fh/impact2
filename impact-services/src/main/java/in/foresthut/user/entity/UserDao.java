package in.foresthut.user.entity;

import java.util.Objects;

public record UserDao(String userId, String userName, String email, String profileImageUrl) {
    public UserDao {
        Objects.requireNonNull(userId, "Userid cannot be null.");
        Objects.requireNonNull(userName, "Username cannot be null.");
        Objects.requireNonNull(email, "Email cannot be null.");
    }
}
