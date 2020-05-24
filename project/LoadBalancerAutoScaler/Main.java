package LoadBalancerAutoScaler;

import LoadBalancerAutoScaler.LoadBalancer;
import com.amazonaws.AmazonClientException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    static AmazonEC2 ec2;
    static Set<Instance> instances = new HashSet<Instance>();

    public static void main(String[] args) throws Exception {
        init();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/sudoku", new LoadBalancer());
        server.setExecutor(null); // creates a default executor
        server.start();

        while(true){
            for (Instance instance : instances) {
                URL url = new URL("http://" + instance.getPublicDnsName() +":8000/ping");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                int status = con.getResponseCode();
                if(status != 200){
                    //Check if instance is runnning
                    //Stop instance if its not running
                    //Remove instance from this.instances
                    //See if there needs to be another instance running
                    //Launches new instance if needed
                }
            }
            Thread.sleep(60000);
        }
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
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesResult.getReservations();

        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances());
        }
    }

}