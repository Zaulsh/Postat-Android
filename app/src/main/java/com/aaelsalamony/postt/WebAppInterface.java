package com.aaelsalamony.postt;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WebAppInterface {

    private Context context;
    private String token;

    public WebAppInterface(String token, Context context) {
        this.context = context;
        this.token = token;
    }

    @JavascriptInterface
    public void processHTML(final String html) {
        Log.i("processed html",html);

        if (html.contains("JavaScript")){

            Thread OauthFetcher = new Thread(new Runnable() {
                @Override
                public void run() {
                    String htmlString = html;


                    Document doc = Jsoup.parse(htmlString);



                    Element link = doc.select("img").first();

                    String text = doc.body().text(); // "An example link"
                    String linkHref = link.attr("alt"); // "http://example.com/"

                    Log.i("oAuthDetails", "  text " + text + "  linkHref " + linkHref);

                    UserNameFromWelcomePage userNameFromWelcomePage = new UserNameFromWelcomePage();
                    userNameFromWelcomePage.execute(token, linkHref);

               /* String[] strings = htmlString.split("\n");

                if (strings.length < 26) return;
                if (strings[26].contains("JavaScript")) {
                    String usernames = strings[26].replace("</a>", "");
                    String[] names = usernames.split(">");


                }*/

                }
            });OauthFetcher.start();
        }


    }



    public class UserNameFromWelcomePage extends AsyncTask<String,Void,String> {

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("resultPost",s + "  ss " );
        }

        @Override
        protected String doInBackground(String... voids) {
            //

            String token = voids[0];
            String user_name = voids[1];
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("_username",user_name);
                jsonObject.put("_uuid",token);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i("jsonobject",jsonObject.toString());


            StringBuilder myresult = null;
            URL url ;
            HttpURLConnection conn = null;

            try {
                url = new URL("https://www.postat.com/user/change-uuid");
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("POST");

                conn.setRequestProperty("Content-type", "application/json");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                bw.write(jsonObject.toString());
                bw.flush();
                bw.close();
                conn.connect();

                Log.d("testtt",conn.getResponseMessage());
                if (conn.getResponseCode() >= 200 || conn.getResponseCode() < 300 )
                {
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    myresult = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        myresult.append(line);
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String sentResult;

            if (myresult==null)
            {
                sentResult="connection_error";
            }
            else
                sentResult=myresult.toString();
            return sentResult;
        }
    }
}
