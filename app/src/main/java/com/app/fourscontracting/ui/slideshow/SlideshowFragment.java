package com.app.fourscontracting.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.app.fourscontracting.R;
import com.app.fourscontracting.User;
import com.app.fourscontracting.UserLocalStore;
import com.app.fourscontracting.UserProject;
import com.app.fourscontracting.LoadingManager;
import com.app.fourscontracting.WebviewActivity;

public class SlideshowFragment extends Fragment {

    public WebView mWebView;
    UserLocalStore userLocalStore;
    private boolean hasLoadError = false;
    private android.net.ConnectivityManager.NetworkCallback networkCallback;
    private boolean isOffline = false; // Track state to trigger reload

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        userLocalStore = new UserLocalStore(getActivity());
        User user = userLocalStore.getLoggedInUser();
        String val = user.username != null ? user.username : "";

        UserProject userproject = userLocalStore.getLoggedInUserProject();
        String projname = userproject.projectname != null ? userproject.projectname : "";
        if (projname.isEmpty() && getContext() != null) {
            com.app.fourscontracting.SessionPrefs sp = new com.app.fourscontracting.SessionPrefs(getContext());
            projname = sp.getProjectName();
            if (projname == null || projname.isEmpty()) {
                projname = sp.getProjectId();
            }
        }

        String[] val_list = UserLocalStore.parseUserInfo(val);
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);
        mWebView = (WebView) root.findViewById(R.id.webview);
        mWebView.setVisibility(View.INVISIBLE); // Keep it hidden while building
        mWebView.setAlpha(0f);

        final View blueprintView = root.findViewById(R.id.blueprint_skeleton);
        final View errorView = root.findViewById(R.id.webview_error_container);
        final android.widget.Button btnRetryPage = errorView != null ? (android.widget.Button) errorView.findViewById(R.id.btn_retry_page) : null;

        if (btnRetryPage != null) {
            btnRetryPage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hasLoadError = false;
                    if (errorView != null) {
                        errorView.setVisibility(View.GONE);
                    }
                    if (blueprintView != null) {
                        blueprintView.setVisibility(View.VISIBLE);
                        blueprintView.setAlpha(1f);
                    }
                    mWebView.reload();
                }
            });
        }

        if (blueprintView != null) {
            android.view.animation.AlphaAnimation pulse = new android.view.animation.AlphaAnimation(0.5f, 1.0f);
            pulse.setDuration(1200);
            pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
            pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
            blueprintView.startAnimation(pulse);

            View ring = blueprintView.findViewById(R.id.verify_progress);
            View icon = blueprintView.findViewById(R.id.construction_icon);
            if (ring != null && icon != null) {
                ring.startAnimation(android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.anim_rotate_loading));
                icon.startAnimation(android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.anim_pulse_loading));
            }
        }

        // Enable Javascript & DOM Storage for absolute cross-device web engine support
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void hideLoader() {
                final android.app.Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!hasLoadError) {
                                crossFade(blueprintView, mWebView);
                            }
                        }
                    });
                }
            }

            @JavascriptInterface
            public void closeWebView() {
                final android.app.Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.finish();
                        }
                    });
                }
            }

            @JavascriptInterface
            public void showToast(final String msg) {
                final android.app.Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @JavascriptInterface
            public void showImagePreview(final String url) {
                final android.app.Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String absoluteUrl = url;
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    absoluteUrl = "https://4scontracting.com/SMCS_APP/subcontractor/" + url;
                                }
                                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
                                android.webkit.WebView dialogWebView = new android.webkit.WebView(activity);
                                dialogWebView.getSettings().setJavaScriptEnabled(true);
                                dialogWebView.getSettings().setSupportZoom(true);
                                dialogWebView.getSettings().setBuiltInZoomControls(true);
                                dialogWebView.getSettings().setDisplayZoomControls(false);
                                dialogWebView.getSettings().setUseWideViewPort(true);
                                dialogWebView.getSettings().setLoadWithOverviewMode(true);
                                dialogWebView.setWebViewClient(new android.webkit.WebViewClient());
                                dialogWebView.loadUrl(absoluteUrl);

                                builder.setView(dialogWebView);
                                builder.setPositiveButton("Close", null);
                                builder.show();
                            } catch (Exception e) {
                                Toast.makeText(activity, "Error opening preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

            @JavascriptInterface
            public void showFooter(final boolean visible) {
                final android.app.Activity activity = getActivity();
                if (activity instanceof WebviewActivity) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            View bottomNav = activity.findViewById(R.id.bottom_nav_view);
                            if (bottomNav != null) {
                                bottomNav.setVisibility(visible ? View.VISIBLE : View.GONE);
                            }
                        }
                    });
                }
            }
        }, "AndroidBridge");
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // At 80%, the "structure" is ready. Swap layers.
                if (newProgress >= 80) {
                    if (!hasLoadError) {
                        crossFade(blueprintView, mWebView);
                    }
                }
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                hasLoadError = false;
                if (errorView != null) {
                    errorView.setVisibility(View.GONE);
                }
                if (blueprintView != null) {
                    blueprintView.setVisibility(View.VISIBLE);
                    blueprintView.setAlpha(1f);
                }
                mWebView.setVisibility(View.INVISIBLE);
                mWebView.setAlpha(0f);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                if (!hasLoadError) {
                    crossFade(blueprintView, mWebView);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // JavaScript check to detect if page body contains error signatures
                view.evaluateJavascript("document.body ? document.body.innerText : ''", new android.webkit.ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (value != null) {
                            String lowerVal = value.toLowerCase();
                            if (lowerVal.contains("504 gateway") || lowerVal.contains("502 bad gateway") || 
                                lowerVal.contains("504 gateway time-out") || lowerVal.contains("504 gateway timeout") ||
                                lowerVal.contains("gateway timeout") || lowerVal.contains("gateway time-out") ||
                                lowerVal.contains("500 internal server error") || lowerVal.contains("404 not found") ||
                                lowerVal.contains("nginx/")) {
                                
                                hasLoadError = true;
                                mWebView.setVisibility(View.INVISIBLE);
                                mWebView.setAlpha(0f);
                                if (blueprintView != null) {
                                    blueprintView.clearAnimation();
                                    blueprintView.setVisibility(View.GONE);
                                }
                                if (errorView != null) {
                                    errorView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                });

                String title = view.getTitle();
                if (title != null) {
                    String lowerTitle = title.toLowerCase();
                    if (lowerTitle.contains("504") || lowerTitle.contains("502") || lowerTitle.contains("503") || 
                        lowerTitle.contains("500") || lowerTitle.contains("404") || lowerTitle.contains("gateway") || 
                        lowerTitle.contains("timeout") || lowerTitle.contains("time-out") || 
                        lowerTitle.contains("webpage not available") || lowerTitle.contains("site can't be reached")) {
                        
                        hasLoadError = true;
                        mWebView.setVisibility(View.INVISIBLE);
                        mWebView.setAlpha(0f);
                        if (blueprintView != null) {
                            blueprintView.clearAnimation();
                            blueprintView.setVisibility(View.GONE);
                        }
                        if (errorView != null) {
                            errorView.setVisibility(View.VISIBLE);
                        }
                    }
                }

                if (!hasLoadError) {
                    crossFade(blueprintView, mWebView);
                } else if (blueprintView != null) {
                    blueprintView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                hasLoadError = true;
                mWebView.setVisibility(View.INVISIBLE);
                mWebView.setAlpha(0f);
                if (blueprintView != null) {
                    blueprintView.setVisibility(View.GONE);
                }
                if (errorView != null) {
                    errorView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    if (request.isForMainFrame()) {
                        hasLoadError = true;
                        mWebView.setVisibility(View.INVISIBLE);
                        mWebView.setAlpha(0f);
                        if (blueprintView != null) {
                            blueprintView.setVisibility(View.GONE);
                        }
                        if (errorView != null) {
                            errorView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @android.annotation.TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (request.isForMainFrame()) {
                    hasLoadError = true;
                    mWebView.setVisibility(View.INVISIBLE);
                    mWebView.setAlpha(0f);
                    if (blueprintView != null) {
                        blueprintView.setVisibility(View.GONE);
                    }
                    if (errorView != null) {
                        errorView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        String uid = val_list.length > 0 ? val_list[0] : "";
        if (uid.isEmpty() || uid.equals("0")) {
            android.util.Log.e("4S_AUTH", "Invalid Session Parameters: UID=" + uid + " Project=" + projname);
            hasLoadError = true;
            mWebView.setVisibility(View.INVISIBLE);
            mWebView.setAlpha(0f);
            if (blueprintView != null) {
                blueprintView.setVisibility(View.GONE);
            }
            if (errorView != null) {
                errorView.setVisibility(View.VISIBLE);
            }
        } else {
            String projId = userLocalStore != null ? userLocalStore.getLoggedInUserProjectID() : "";
            if (android.text.TextUtils.isEmpty(projId) && getContext() != null) {
                projId = new com.app.fourscontracting.SessionPrefs(getContext()).getProjectId();
            }
            if (android.text.TextUtils.isEmpty(projId)) projId = projname;
            String encUid = android.net.Uri.encode(uid);
            String encProjName = android.net.Uri.encode(projname != null ? projname : "");
            String encProjId = android.net.Uri.encode(projId != null ? projId : "");
            String url = "https://4scontracting.com/SMCS_APP/subcontractor/leaveapproval.php"
                    + "?uid=" + encUid
                    + "&id=" + encUid
                    + "&projname=" + encProjName
                    + "&project_id=" + encProjId
                    + "&projid=" + encProjId;
            mWebView.loadUrl(url);
        }
        startNetworkMonitoring();
        return root;
    }

    private void startNetworkMonitoring() {
        android.content.Context context = getContext();
        if (context == null) return;
        final android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;
        
        networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull android.net.Network network) {
                final android.app.Activity activity = getActivity();
                if (activity != null && isOffline) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isOffline = false;
                            if (isResumed()) {
                                hideOfflineBar();
                                if (mWebView != null) {
                                    mWebView.reload();
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onLost(@NonNull android.net.Network network) {
                final android.app.Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isOffline = true;
                            if (isResumed()) {
                                showOfflineBar();
                            }
                        }
                    });
                }
            }
        };

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private void showOfflineBar() {
        android.app.Activity activity = getActivity();
        if (activity == null) return;
        final View errorBar = activity.findViewById(R.id.noInternetBar); 
        if (errorBar != null && errorBar.getVisibility() != View.VISIBLE) {
            errorBar.setVisibility(View.VISIBLE);
            errorBar.post(new Runnable() {
                @Override
                public void run() {
                    float height = errorBar.getHeight();
                    if (height == 0) height = 150f;
                    errorBar.setTranslationY(-height);
                    errorBar.animate().translationY(0f).alpha(1f).setDuration(300).start();
                }
            });
        }
    }

    private void hideOfflineBar() {
        android.app.Activity activity = getActivity();
        if (activity == null) return;
        final View errorBar = activity.findViewById(R.id.noInternetBar);
        if (errorBar != null && errorBar.getVisibility() == View.VISIBLE) {
            float height = errorBar.getHeight();
            if (height == 0) height = 150f;
            errorBar.animate().translationY(-height)
                .alpha(0f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        errorBar.setVisibility(View.GONE);
                    }
                })
                .start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkCallback != null) {
            android.content.Context context = getContext();
            if (context != null) {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.unregisterNetworkCallback(networkCallback);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isOffline) {
            showOfflineBar();
        } else {
            hideOfflineBar();
            if (hasLoadError) {
                hasLoadError = false;
                View root = getView();
                if (root != null) {
                    View blueprintView = root.findViewById(R.id.blueprint_skeleton);
                    View errorView = root.findViewById(R.id.webview_error_container);
                    if (errorView != null) {
                        errorView.setVisibility(View.GONE);
                    }
                    if (blueprintView != null) {
                        blueprintView.setVisibility(View.VISIBLE);
                        blueprintView.setAlpha(1f);
                    }
                }
                if (mWebView != null) {
                    mWebView.reload();
                }
            }
        }
    }



    // Smooth Cross-Fade Method
    private void crossFade(final View skeleton, final View content) {
        if (content.getVisibility() == View.VISIBLE && content.getAlpha() == 1f) return; // Already swapped

        skeleton.clearAnimation(); // Clear any running infinite animations
        skeleton.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                skeleton.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                content.animate().alpha(1f).setDuration(300).start();
            }
        }).start();
    }

    public void onProjectChanged(String newProjName) {
        User user = userLocalStore.getLoggedInUser();
        String val = user.username != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        String uid = val_list.length > 0 ? val_list[0] : "";
        if (!uid.isEmpty() && !uid.equals("0")) {
            String projId = userLocalStore != null ? userLocalStore.getLoggedInUserProjectID() : "";
            if (android.text.TextUtils.isEmpty(projId) && getContext() != null) {
                projId = new com.app.fourscontracting.SessionPrefs(getContext()).getProjectId();
            }
            if (android.text.TextUtils.isEmpty(projId)) projId = newProjName;
            String encUid = android.net.Uri.encode(uid);
            String encProjName = android.net.Uri.encode(newProjName != null ? newProjName : "");
            String encProjId = android.net.Uri.encode(projId != null ? projId : "");
            String url = "https://4scontracting.com/SMCS_APP/subcontractor/leaveapproval.php"
                    + "?uid=" + encUid
                    + "&id=" + encUid
                    + "&projname=" + encProjName
                    + "&project_id=" + encProjId
                    + "&projid=" + encProjId;
            mWebView.loadUrl(url);
        }
    }
}