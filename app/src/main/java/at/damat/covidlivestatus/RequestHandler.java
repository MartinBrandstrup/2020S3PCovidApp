package at.damat.covidlivestatus;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

public class RequestHandler {

    private static final String TAG = "RequestHandler";

    public static class RequestAsync extends AsyncTask<String, Integer, JSONArray> {
        String s_url;
        String method;
        JSONObject urlparams;
        ProgressBar progressBar;

        public RequestAsync(String s_url, String method, JSONObject urlparams, ProgressBar progressBar) {
            this.s_url = s_url;
            this.method = method;
            this.urlparams = urlparams;
            this.progressBar = progressBar;
        }

        @Override
        protected JSONArray doInBackground(String... strings) {
            try {
                if (method.equals("GET")) {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "HTTP GET :: " + s_url + " :: " + urlparams);
                    }
                    return new JSONArray(sendGet(s_url));
                } else if (method.equals("POST")) {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "HTTP POST :: " + s_url + " :: " + urlparams);
                    }
                    return new JSONArray(sendPost(s_url, urlparams));
                } else if (method.equals("MULTIPOST")) {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "HTTP POST :: " + s_url + " :: " + urlparams);
                    }
                } else if(method.equals("POST4E")) {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "HTTP POST :: " + s_url + " :: " + urlparams);
                    }
                    return new JSONArray(sendPost4E(s_url, urlparams));
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Unknown method :: " + method);
                    }
                    return new JSONArray("Unkown method");
                }
            } catch (Exception ex) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Exception :: " + ex.getMessage(), ex);
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(JSONArray s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }
    }

    public static String sendPost(String r_url, JSONObject postDataParams) throws Exception {
        URL url = new URL(r_url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(20000);
        conn.setConnectTimeout(20000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(encodeParams(postDataParams));
        writer.flush();
        writer.close();
        os.close();
        int responseCode = conn.getResponseCode();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Response Code :: " + responseCode);
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                break;
            }
            in.close();
            return sb.toString();
        }
        return null;
    }

    public static String sendMultiPost(String r_url, JSONArray postDataParams) throws Exception {
        URL url = new URL(r_url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(20000);
        conn.setConnectTimeout(20000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        for (int i = 0; i < postDataParams.length(); i++) {
            writer.write(encodeParams(postDataParams.getJSONObject(i)));
            if (i != 0) writer.write("&");
        }
        writer.flush();
        writer.close();
        os.close();
        int responseCode = conn.getResponseCode();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Response Code :: " + responseCode);
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                break;
            }
            in.close();
            return sb.toString();
        }
        return null;
    }

    public static String sendPost4E(String r_url, JSONObject postDataParams) {
        Boolean wasSuccess = false;
        while (!wasSuccess) {
            try {
                URL url = new URL(r_url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(20000);
                conn.setConnectTimeout(20000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(encodeParams(postDataParams));
                writer.flush();
                writer.close();
                os.close();
                int responseCode = conn.getResponseCode();
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Response Code :: " + responseCode);
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }
                    in.close();
                    wasSuccess = true;
                    return sb.toString();
                }
                return null;
            } catch (Exception ex) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Post failed :: " + ex.getMessage(), ex);
                }
                wasSuccess = false;
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException iex) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "InterruptedException :: " + iex.getMessage(), iex);
                    }
                }
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Retrying POST :: " + r_url);
                }
            }
        }
        return null;
    }

    public static String sendGet(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Response Code :: " + responseCode);
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            return "[]";
        }
    }

    private static String encodeParams(JSONObject params) throws Exception {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        Iterator<String> itr = params.keys();
        while (itr.hasNext()) {
            String key = itr.next();
            Object value = params.get(key);
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));
        }
        return result.toString();
    }
}
