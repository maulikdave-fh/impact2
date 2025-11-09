package in.foresthut.user.firebase;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import in.foresthut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseInitializer.class);


    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return; // ✅ skip if already done

        try (FileInputStream serviceAccount = new FileInputStream("/home/maulik-dave/etc/secrets/impact-web-e5193-firebase-adminsdk-fbsvc-a7c604be3f.json")) {
            FirebaseOptions options = FirebaseOptions.builder()
                                                     .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                                     .build();

            if (FirebaseApp.getApps().isEmpty()) { // ✅ check before creating
                FirebaseApp.initializeApp(options);
                logger.info("✅ Firebase Admin initialized successfully");
            }

            initialized = true;
        } catch (IOException e) {
            logger.error("Error: ", e);
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }
}

