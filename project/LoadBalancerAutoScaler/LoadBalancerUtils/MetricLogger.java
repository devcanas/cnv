package pt.ulisboa.tecnico.cnv.server;

import java.util.*; 

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

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
        System.out.println("> Logged to DynamoDB with id: " + item.getId());
    }

    public List<MetricItem> getLogs() {
        return mapper.scan(MetricItem.class, new DynamoDBScanExpression());
    }

    public List<MetricItem> getLogsSimilarTo(MetricItem item) {

        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();

        String solverStrategy = item.getSolverStrategy();
        int unassigned = item.getUnassigned();
        int nCol = item.getNCol();
        int nLin = item.getNLin();
        int boardSize = -1;
        
        int valCounter = 0;
        String expression = "";
        if (solverStrategy != null) {
            String val = ":val" + (++valCounter);
            eav.put(val, new AttributeValue().withS(solverStrategy));
            expression += "solverStrategy = " + val;
        }

        if (unassigned != -1) {
            String val1 = ":val" + (++valCounter);
            // lower bound for unassigned
            eav.put(val1, new AttributeValue().withN(String.valueOf(Math.floor(0.9*unassigned))));
            String val2 = ":val" + (++valCounter);
            // upper bound for unassigned
            eav.put(val2, new AttributeValue().withN(String.valueOf(Math.floor(1.1*unassigned))));
            expression += valCounter > 1 ? " and " : " ";
            expression += "unassigned between " + val1 + " and " + val2;
        }

        if (nCol != -1) boardSize = nCol;
        if (nLin != -1) boardSize = nLin;  
        if (boardSize != -1) {
            String val = ":val" + (++valCounter);
            eav.put(val, new AttributeValue().withN(String.valueOf(boardSize)));
            expression += valCounter > 1 ? " and " : " ";
            expression += "nCol = " + val;
        }

        System.out.println("> Query Expression: " + expression);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression(expression)
            .withExpressionAttributeValues(eav);

        return mapper.scan(MetricItem.class, scanExpression);
    }
}
