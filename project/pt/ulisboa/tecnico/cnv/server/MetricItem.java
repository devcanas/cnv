package pt.ulisboa.tecnico.cnv.server;

import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="CNV-Project-logs")
public class MetricItem {

    private String id;
    private String example;

    // Primary key on dynamodb do not change the attr name
    @DynamoDBHashKey(attributeName="id")
    public String getId() { return this.id; }
    public void setId(String id) { this.id = id; }

    // add new getter and set with every new value for metric
    // as done in this example
    @DynamoDBAttribute(attributeName="example")
    public String getExample() {return example; }
    public void setExample(String example) { this.example = example; }
}