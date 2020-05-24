package LoadBalancerAutoScaler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.AmazonServiceException;

class LoadBalancer implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {

        try {
            for (Instance instance : Main.instances) {
                if (!instance.getPublicDnsName().equals("ec2-52-90-115-66.compute-1.amazonaws.com") &&
                        instance.getState().getName().equals("running"))
                    forwardRequest(t, instance.getPublicDnsName());
            }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr =  new InputStreamReader(is,"utf-8");
        BufferedReader br = new BufferedReader(isr);

        // From now on, the right way of moving from bytes to utf-8 characters:

        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);

        }

        br.close();
        isr.close();

        return buf.toString();
    }

    private static void forwardRequest(HttpExchange t, String urlString) throws IOException {
        String body = parseRequestBody(t.getRequestBody());

        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + urlString + ":8000" + t.getRequestURI().toString());
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "text/plain");
            con.setRequestProperty("Content-Length", Integer.toString(body.getBytes("UTF-8").length));
            con.setDoOutput(true);
            OutputStream op = con.getOutputStream();
            op.write(body.getBytes("UTF-8"));
            op.close();

            int status = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            final Headers hdrs = t.getResponseHeaders();

            hdrs.add("Content-Type", "application/json");

            hdrs.add("Access-Control-Allow-Origin", "*");

            hdrs.add("Access-Control-Allow-Credentials", "true");
            hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
            hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            System.out.println(content);

            t.sendResponseHeaders(200, content.length());
            OutputStream os = t.getResponseBody();
            os.write(String.valueOf(content).getBytes());
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}