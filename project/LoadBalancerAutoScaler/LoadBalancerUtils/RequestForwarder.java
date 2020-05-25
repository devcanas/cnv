package LoadBalancerAutoScaler.LoadBalancerUtils;

import java.io.IOException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RequestForwarder {
    public static boolean forwardRequest(HttpExchange t, String urlString) throws IOException {
        String body = Parser.parseRequestBody(t.getRequestBody());

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
            return false;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return true;
    }
}