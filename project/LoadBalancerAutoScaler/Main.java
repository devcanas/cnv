package LoadBalancerAutoScaler;

import LoadBalancerAutoScaler.LoadBalancer;
import LoadBalancerAutoScaler.InstanceState;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.sun.net.httpserver.HttpServer;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import com.amazonaws.services.ec2.model.Tag;

public class Main {

    static AmazonEC2 ec2;
    static AmazonCloudWatch cloudWatch;
    static HashMap<Instance, InstanceState> instances = new HashMap<>();

    public static void main(String[] args) throws Exception {
        init();

        AutoScaler.start();

        AutoScaler.newInstance();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/sudoku", new LoadBalancer());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private static void init() throws Exception {

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