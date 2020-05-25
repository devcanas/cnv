package LoadBalancerAutoScaler;

import LoadBalancerAutoScaler.InstanceState;
import LoadBalancerAutoScaler.LoadBalancer;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class Main {

    static public AmazonEC2 ec2;
    static public AmazonCloudWatch cloudWatch;
    static public HashMap<Instance, InstanceState> instances = new HashMap<>();

    public static void main(String[] args) throws Exception
    {
        //Connects to Amazon and initializes instances hashMap
        init();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/sudoku", new LoadBalancer());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        //Starts the autoscaler that periodically performs the health checks and scalling pollicies

        System.out.println(server.getAddress().toString());
        AutoScaler.start();
    }

    private static void init() throws Exception
    {

        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        ec2 = AmazonEC2ClientBuilder.standard().withRegion("us-east-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
        cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion("us-east-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesResult.getReservations();

        //Fills the instance hasmMap with the current running instances except the Load Balancer and Auto Scaler instance
        for (Reservation reservation : reservations) {
            for(Instance instance: reservation.getInstances()){
                boolean skipInstance = false;
                if(instance.getTags().size() > 0) {
                    for (Tag tag : instance.getTags()) {
                        if (tag.getKey().equals("Name") && tag.getValue().equals("Load Balancer"))
                            skipInstance = true;
                    }
                }
                if (skipInstance)
                    continue;
                if(instance.getState().getName().equals("running")) {
                    instances.put(instance, new InstanceState());
                }
            }
        }

    }

}