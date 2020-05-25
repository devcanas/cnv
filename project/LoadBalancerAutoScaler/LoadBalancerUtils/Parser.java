package LoadBalancerAutoScaler.LoadBalancerUtils;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.ArrayList;

public class Parser {
    public static ArrayList<String> parseRequestParams(String query)
    {
        final String[] params = query.split("&");

        final ArrayList<String> newArgs = new ArrayList<>();
        for (final String p : params) {
            final String[] splitParam = p.split("=");
            newArgs.add(splitParam[1]);
        }

        return newArgs;
    }

    public static String parseRequestBody(InputStream is) throws IOException
    {
        InputStreamReader isr =  new InputStreamReader(is,"utf-8");
        BufferedReader br = new BufferedReader(isr);

        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);
        }

        br.close();
        isr.close();

        return buf.toString();
    }
}