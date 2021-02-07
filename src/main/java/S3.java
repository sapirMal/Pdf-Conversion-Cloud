import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.File;

public class S3 {
    private static AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

    private static final String bucketName = "dsps192ass1-41b1213c-29a3-41e9-af94-d5f97db8f811";

    // create bucket
    public static void createBucket(){
        for (Bucket bucket : s3.listBuckets()){
            if (bucket.getName().equals(bucketName)){
                return;
            }
        }
        s3.createBucket(bucketName);
    }

    public static String getBucketName(){
        return bucketName;
    }
    // remove bucket
    public static void removeBucket(String bucketName){
        s3.deleteBucket(bucketName);
    }
    // list buckets (debugging)
    public static void listBuckets(){
        System.out.println("Listing buckets");
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println(" - " + bucket.getName());
        }
        System.out.println();
    }
    // upload file to bucket
    public static String uploadFile(File file){
        String key = file.getName().replace('\\', '-').replace('/','-').replace(':', '-');
        PutObjectRequest req = new PutObjectRequest(bucketName, key, file);
        req.setCannedAcl(CannedAccessControlList.PublicRead);
        s3.putObject(req);
        return key;
    }

    // download file from bucket
    public static S3Object downloadFile(String key){
        //System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
        return s3.getObject(new GetObjectRequest(bucketName, key));

    }

    // get file URL
    public static String getFileURL(String key){
        return s3.getUrl(bucketName, key).toString();
    }

    // remove file from bucket
    public static void removeFile(String key){
        s3.deleteObject(bucketName, key);
    }
    // list files in bucket (debugging)
    public static void listFiles(String bucketName){
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                .withBucketName(bucketName));
        System.out.println("Listing files in "+bucketName+":");
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            System.out.println(" - " + objectSummary.getKey() + "  " +
                    "(size = " + objectSummary.getSize() + ")");
        }
        System.out.println();
    }

}
