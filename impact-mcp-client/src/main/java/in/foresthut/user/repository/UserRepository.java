package in.foresthut.user.repository;

import in.foresthut.user.entity.UserDao;

public interface UserRepository {
    void add(UserDao userDao);
    UserDao get(String userId);
}
