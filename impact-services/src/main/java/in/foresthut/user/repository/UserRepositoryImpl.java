package in.foresthut.user.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.nirvighna.mongodb.commons.DatabaseConfig;
import in.foresthut.user.entity.UserDao;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRepositoryImpl implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);

    private final MongoCollection<UserDao> userCollection;

    public UserRepositoryImpl(DatabaseConfig databaseConfig) {
        this.userCollection = databaseConfig
                                            .database()
                                            .getCollection("users", UserDao.class);
    }

    @Override
    public void add(UserDao userDao) {
        userCollection.insertOne(userDao);
    }

    @Override
    public UserDao get(String userId) {
        Bson filter = Filters.eq("userId", userId);
        return userCollection.find(filter).first();
    }
}
