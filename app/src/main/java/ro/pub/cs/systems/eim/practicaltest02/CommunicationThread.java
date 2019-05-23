package ro.pub.cs.systems.eim.practicaltest02;

import android.text.Html;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

public class CommunicationThread extends Thread {
    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (Website)");
            String website = bufferedReader.readLine();
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] " + website);

            String query = "http://" + website + "/";
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] " + query);

            String result = null;
            HttpResponse response;
            String pageSourceCode;

            if (serverThread.getData(query) != null) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from cache");
                pageSourceCode = serverThread.getData(query);
            } else {

                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(query);

                Log.i(Constants.TAG, "[COMMUNICATION THREAD] URI: " + httpGet.getURI());
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] URI: " + httpGet.getRequestLine());


                //ResponseHandler<String> responseHandler = new BasicResponseHandler();
                response = httpClient.execute(httpGet);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                StringBuilder builder = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    builder.append(line).append("\n");
                }
                pageSourceCode = builder.substring(0);
                serverThread.setData(query, pageSourceCode);

            }
            //String pageSourceCode = httpClient.execute(httpPost, responseHandler);

/*            if (response == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                return;
            }

            System.out.println(response.getEntity().getContent().toString());
*/
            Document document = Jsoup.parse(pageSourceCode);
            Element element = document.child(0);
            Elements elements = element.getElementsByTag(Constants.SCRIPT_TAG);
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] " + elements.toString());

            /*
            for (Element script: elements) {
                String scriptData = script.data();
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] " + scriptData);

                if (scriptData.contains(Constants.SEARCH_KEY)) {
                    int position = scriptData.indexOf(Constants.SEARCH_KEY) + Constants.SEARCH_KEY.length();
                    scriptData = scriptData.substring(position);
                    JSONObject content = new JSONObject(scriptData);
                    JSONObject currentObservation = content.getJSONObject(Constants.CURRENT_OBSERVATION);
                    result = currentObservation.toString();
                    break;
                }
            }

            if (result == null) {
                String lines[] = elements.toString().split("\\r?\\n");
                result = lines[1];
                JSONObject jsonObj = new JSONObject(lines[1]);

                String final_result = "";

                JSONArray arr = (JSONArray) jsonObj.get("RESULTS");
                for (int i = 0; i < arr.length(); i++) {
                    final_result = final_result + arr.getJSONObject(i).get("name") + "; ";
                }

                Log.i(Constants.TAG, "[COMMUNICATION THREAD] JSON " + final_result);

                result = final_result;

            }
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] " + result);
            */
            printWriter.println(elements.toString());
            printWriter.flush();

        } catch (IOException ioException) {
            Log.e(Constants.TAG, "![COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        /*} catch (JSONException jsonException) {
            Log.e(Constants.TAG, "@[COMMUNICATION THREAD] An exception has occurred: " + jsonException.getMessage());
            if (Constants.DEBUG) {
                jsonException.printStackTrace();
            }*/
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "#[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }
}
