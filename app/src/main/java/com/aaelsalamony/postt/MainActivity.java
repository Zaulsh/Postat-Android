package com.aaelsalamony.postt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eggheadgames.siren.Siren;
import com.eggheadgames.siren.SirenAlertType;
import com.eggheadgames.siren.SirenSupportedLocales;
import com.eggheadgames.siren.SirenVersionCheckType;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Build.*;
public class MainActivity extends AppCompatActivity {

    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;
    private WebView webview;
    private static final String SIREN_JSON_URL = "https://postat.com/upload/version/version.json";
    public static String FACEBOOK_URL = "https://www.facebook.com/";
    public static String FACEBOOK_PAGE_ID = "";
    List<Uri> mSelected;
    SwipeRefreshLayout mySwipeRefreshLayout;



    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setupDialogLoader();


        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_loader);
        dialog.show();
        new CountDownTimer(2000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
              dialog.dismiss();
            }
        }.start();



        checkCurrentAppVersion();
        webview = findViewById(R.id.webview);
        webview.loadUrl("https://postat.com/");
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mySwipeRefreshLayout = (SwipeRefreshLayout)this.findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        webview.reload();
                        mySwipeRefreshLayout.setRefreshing(false);
                    }
                }
        );

        //Runtime External storage permission for saving download files
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d("permission", "permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1);
            }
        }

        //handle downloading
        webview.setDownloadListener(new DownloadListener()
        {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading File...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                                url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
            }});


        webview.getSettings().setLoadWithOverviewMode(true);
        //webview.getSettings().setUserAgentString("Android Mozilla/5.0 AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setAllowContentAccess(true);
        webview.getSettings().supportZoom();
        webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);


        webview.addJavascriptInterface(new WebAppInterface(FirebaseInstanceId.getInstance().getToken(),getBaseContext()),"PrPostAtAndroid");

//       Log.d("tokeneeee",FirebaseInstanceId.getInstance().getToken());

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.getString("action") != null){
                //bundle must contain all info sent in "data" field of the notification
                webview.loadUrl("https://postat.com" + bundle.getString("action"));
            }
        }

        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("https://postat.com/user/logout")) {
                    Log.d("logot","logout done");
                    Intent intent=new Intent(MainActivity.this,DeleteTokenService.class);
                    MainActivity.this.startService(intent);
                }
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                if ( url.contains(".pdf")){
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(url), "application/pdf");
                    try{
                        view.getContext().startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        //user does not have a pdf viewer installed
                    }
                } else if(url.contains("instagram.com") || url.contains("facebook.com") || url.contains("twitter.com")
                        || url.contains("linkedin.com") || url.contains("tiktok.com") || url.contains("youtube.com") || url.contains("snapchat.com")){

                    if (url.contains("facebook.com")){
//                        Intent(Intent.ACTION_VIEW,Uri.parse("https://www.facebook.com/<user_name_here>"));
                        openSocialApp(url);//"fb://page/?id=0");
                    }else{
                        openSocialApp(url);
                    }


                    return true;
                } else{
                    webview.loadUrl(url);
                }
                return false; // then it is not handled by default action
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("error",description);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //hide progressbar here
                webview.loadUrl("javascript:window.PrPostAtAndroid.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

            }

        });
        webview.setWebChromeClient(new ChromeClient());
    }

    public String getFacebookPageURL(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            int versionCode = packageManager.getPackageInfo("com.facebook.katana", 0).versionCode;
            if (versionCode >= 3002850) { //newer versions of fb app
                return "fb://facewebmodal/f?href=" + FACEBOOK_URL;
            } else { //older versions of fb app
                return "fb://page/" + FACEBOOK_PAGE_ID;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return FACEBOOK_URL; //normal web url
        }
    }

    private void openSocialApp(String url){
        Uri uri = Uri.parse(url);
        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

        if (url.contains("instagram.com")) {
            likeIng.setPackage("com.instagram.android");
        }else if (url.contains("facebook.com")) {
//            likeIng.setPackage("com.facebook.android");

            Intent facebookIntent = new Intent(Intent.ACTION_VIEW);
            String facebookUrl = getFacebookPageURL(this);
            facebookIntent.setData(Uri.parse(facebookUrl));
            startActivity(facebookIntent);

            return;

        }else if (url.contains("twitter.com")) {
            likeIng.setPackage("com.twitter.android");
        }else if (url.contains("linkedin.com")) {
            likeIng.setPackage("com.linkedin.android");
        }else if (url.contains("tiktok.com")) {
            likeIng.setPackage("com.tiktok.android");
        }else if (url.contains("youtube.com")) {
            likeIng.setPackage("com.youtube.android");
        }else if (url.contains("snapchat.com")) {
//            likeIng.setPackage("com.snapchat.android");

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://snapchat.com/add/" + ""));
                intent.setPackage("com.snapchat.android");
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://snapchat.com/add/" + "")));
            }
            return;
        }

        try {
            startActivity(likeIng);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url)));
        }
    }

    private void checkCurrentAppVersion() {
        Siren siren = Siren.getInstance(getApplicationContext());
        siren.setMajorUpdateAlertType(SirenAlertType.FORCE);
        siren.setMinorUpdateAlertType(SirenAlertType.OPTION);
        siren.setPatchUpdateAlertType(SirenAlertType.OPTION);
        siren.setLanguageLocalization(SirenSupportedLocales.AR);
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY,SIREN_JSON_URL);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webview.canGoBack()) {
                        webview.goBack();

                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }


    public class ChromeClient extends WebChromeClient {

        // For Android 5.0
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
             // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent takePictureIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            takePictureIntent.addCategory(Intent.CATEGORY_OPENABLE);
            takePictureIntent.setType("video/*, image/*");

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e("ffdfggdfg", "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }


            Intent chooserIntent = new Intent(Intent.ACTION_PICK);
            chooserIntent.setType("*/*");
            chooserIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*"});

            chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;
        }
    }
}










