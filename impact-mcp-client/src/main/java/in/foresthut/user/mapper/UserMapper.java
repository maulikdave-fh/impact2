package in.foresthut.user.mapper;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.nirvighna.www.commons.config.AppConfig;
import in.foresthut.user.entity.UserDao;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class UserMapper {
    public static UserDao toUserDao(in.foresthut.impact.models.user.User user) {
        return new UserDao(user.getId(), user.getName(), user.getEmail(), user.getProfilePhotoUrl());
    }

    public static in.foresthut.impact.models.user.User toUser(UserDao userDao) {
        return in.foresthut.impact.models.user.User.newBuilder()
                                                   .setId(userDao.userId())
                                                   .setName(userDao.userName())
                                                   .setEmail(userDao.email())
                                                   .setProfilePhotoUrl(userDao.profileImageUrl())
                                                   .build();
    }

    public static UserDao toUserDao(String idTokenString) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new GsonFactory();
//        JsonFactory jsonFactory =
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                // Specify the WEB_CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(AppConfig.getInstance()
                                                                .get("google.web.client.id")))
                // Or, if multiple clients access the backend:
                //.setAudience(Arrays.asList(WEB_CLIENT_ID_1, WEB_CLIENT_ID_2, WEB_CLIENT_ID_3))
                .build();

        // (Receive idTokenString by HTTPS POST)

        GoogleIdToken idToken = null;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            return new UserDao(
                    payload.getSubject(), (String) payload.get("name"), payload.getEmail(),
                    (String) payload.get("picture"));
        } else {
            return null;
        }
    }
}
