package pt.ulisboa.tecnico.cnv.server;

import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

public class MetricLogger {

    private static AmazonDynamoDB dynamoDB;
    private static MetricLogger instance = null;
    private static DynamoDBMapper mapper;

    private MetricLogger() { this.setUp(); }
    
    public static MetricLogger getInstance() {
        if (instance == null) {
            instance = new MetricLogger();
            setUp();
        }
        return instance;
    }

    private static void setUp() {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.", e);
        }

        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("us-east-1")
            .build();

        mapper = new DynamoDBMapper(dynamoDB);
    }

    public void log(MetricItem item) {
        mapper.save(item);
    }

    public List<MetricItem> getLogs() {
        return mapper.scan(MetricItem.class, new DynamoDBScanExpression());
    }
}
