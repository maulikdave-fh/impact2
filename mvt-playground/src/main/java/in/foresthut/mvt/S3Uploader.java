package in.foresthut.mvt;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3Uploader {
    private final S3Client s3;
    private final String bucket;

    public S3Uploader(String bucket) {
        this.bucket = bucket;
        this.s3 = S3Client.builder()
                          .region(Region.AP_SOUTH_1)
                          .credentialsProvider(DefaultCredentialsProvider.create())
                          .build();
    }

    public void upload(int x, int y, int z, byte[] mvtBytes) {
        if (mvtBytes == null || mvtBytes.length == 0) {
            System.out.printf("⏭️  Skipped upload for empty tile z=%d x=%d y=%d%n", z, x, y);
            return;
        }

        String tilePath = String.format("tiles/%d/%d/%d.mvt", z, x, y);
        // Upload to S3
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(bucket)
                                                   .key(tilePath)
                                                   .contentType("application/vnd.mapbox-vector-tile")
                                                   .build();

        s3.putObject(request, RequestBody.fromBytes(mvtBytes));

        System.out.println("✅ Uploaded MVT to S3: s3://" + bucket + "/" + tilePath);
    }

}
