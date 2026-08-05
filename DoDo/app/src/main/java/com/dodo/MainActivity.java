package com.dodo;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends Activity {
	
	private WebView webview1;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		webview1 = findViewById(R.id.webview1);
		webview1.getSettings().setJavaScriptEnabled(true);
		webview1.getSettings().setSupportZoom(true);
		
		webview1.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView _param1, String _param2, Bitmap _param3) {
				final String _url = _param2;
				
				super.onPageStarted(_param1, _param2, _param3);
			}
			
			@Override
			public void onPageFinished(WebView _param1, String _param2) {
				final String _url = _param2;
				
				super.onPageFinished(_param1, _param2);
			}
		});
	}
	
	private void initializeLogic() {
		webview1.getSettings().setJavaScriptEnabled(true);
		webview1.getSettings().setDomStorageEnabled(true);
		webview1.getSettings().setAllowFileAccess(true);
		webview1.getSettings().setAllowContentAccess(true);
		webview1.getSettings().setAllowUniversalAccessFromFileURLs(true);
		webview1.getSettings().setAllowFileAccessFromFileURLs(true);
		
		android.webkit.CookieManager.getInstance().setAcceptCookie(true);
		android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webview1, true);
		
		webview1.loadUrl("file:///android_asset/index.html");
		
		webview1.setWebViewClient(new android.webkit.WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
				if (url.startsWith("mailto:")) {
					try {
						android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
						intent.setData(android.net.Uri.parse(url));
						startActivity(intent);
					} catch (Exception e) {
						try {
							android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
							intent.setType("message/rfc822");
							intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"yashansh@proton.me"});
							intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Productivity Hub - Feedback");
							startActivity(android.content.Intent.createChooser(intent, "Send Email"));
						} catch (Exception ex) {
							android.widget.Toast.makeText(getApplicationContext(), "No email app found", android.widget.Toast.LENGTH_SHORT).show();
						}
					}
					return true;
				}
				return false;
			}
		});
		
		webview1.addJavascriptInterface(new Object() {
			
			@android.webkit.JavascriptInterface
			public void saveData(String key, String value) {
				getSharedPreferences("todo_data", MODE_PRIVATE).edit().putString(key, value).apply();
			}
			
			@android.webkit.JavascriptInterface
			public String loadData(String key) {
				return getSharedPreferences("todo_data", MODE_PRIVATE).getString(key, "");
			}
			
			@android.webkit.JavascriptInterface
			public void openEmail(String email, String subject, String body) {
				try {
					android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
					intent.setData(android.net.Uri.parse("mailto:" + email));
					intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
					intent.putExtra(android.content.Intent.EXTRA_TEXT, body);
					startActivity(intent);
				} catch (Exception e) {
					try {
						android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
						intent.setType("message/rfc822");
						intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
						intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
						intent.putExtra(android.content.Intent.EXTRA_TEXT, body);
						startActivity(android.content.Intent.createChooser(intent, "Send Email"));
					} catch (Exception ex) {
						android.widget.Toast.makeText(getApplicationContext(), "No email app found", android.widget.Toast.LENGTH_SHORT).show();
					}
				}
			}
			
			@android.webkit.JavascriptInterface
			public String syncRequest(String url, String body) {
				try {
					java.net.CookieManager cookieManager = new java.net.CookieManager();
					java.net.CookieHandler.setDefault(cookieManager);
					String lastResponse = "ERR:No Response";
					
					for (int i = 0; i < 3; i++) {
						java.net.URL u = new java.net.URL(url);
						java.net.HttpURLConnection con = (java.net.HttpURLConnection) u.openConnection();
						con.setRequestMethod("POST");
						con.setDoOutput(true);
						con.setConnectTimeout(15000);
						con.setReadTimeout(15000);
						con.setInstanceFollowRedirects(true);
						
						con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
						con.setRequestProperty("Accept", "*/*");
						con.setRequestProperty("Connection", "close");
						con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						con.setRequestProperty("Expect", "");
						
						try {
							java.util.List<java.net.HttpCookie> cookies = cookieManager.getCookieStore().get(u.toURI());
							StringBuilder cookieHeader = new StringBuilder();
							for (java.net.HttpCookie c : cookies) {
								if(cookieHeader.length() > 0) cookieHeader.append("; ");
								cookieHeader.append(c.getName()).append("=").append(c.getValue());
							}
							if(cookieHeader.length() > 0) {
								con.setRequestProperty("Cookie", cookieHeader.toString());
							}
						} catch (Exception e) {}
						
						byte[] bodyBytes = body.getBytes("UTF-8");
						con.setFixedLengthStreamingMode(bodyBytes.length);
						
						java.io.OutputStream os = con.getOutputStream();
						os.write(bodyBytes);
						os.flush();
						os.close();
						
						int code = con.getResponseCode();
						java.io.InputStream is = (code == 200) ? con.getInputStream() : con.getErrorStream();
						if (is == null) is = con.getInputStream();
						java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = br.readLine()) != null) sb.append(line).append("\n");
						br.close();
						lastResponse = sb.toString();
						
						if (lastResponse.contains("__test=") && lastResponse.contains("slowAES.decrypt")) {
							java.util.regex.Matcher mA = java.util.regex.Pattern.compile("a=toNumbers\\(\"([a-f0-9]+)\"\\)").matcher(lastResponse);
							java.util.regex.Matcher mB = java.util.regex.Pattern.compile("b=toNumbers\\(\"([a-f0-9]+)\"\\)").matcher(lastResponse);
							java.util.regex.Matcher mC = java.util.regex.Pattern.compile("c=toNumbers\\(\"([a-f0-9]+)\"\\)").matcher(lastResponse);
							if (mA.find() && mB.find() && mC.find()) {
								String aHex = mA.group(1);
								byte[] aBytes = new byte[aHex.length() / 2];
								for (int j = 0; j < aBytes.length; j++) aBytes[j] = (byte) Integer.parseInt(aHex.substring(j * 2, j * 2 + 2), 16);
								
								String bHex = mB.group(1);
								byte[] bBytes = new byte[bHex.length() / 2];
								for (int j = 0; j < bBytes.length; j++) bBytes[j] = (byte) Integer.parseInt(bHex.substring(j * 2, j * 2 + 2), 16);
								
								String cHex = mC.group(1);
								byte[] cBytes = new byte[cHex.length() / 2];
								for (int j = 0; j < cBytes.length; j++) cBytes[j] = (byte) Integer.parseInt(cHex.substring(j * 2, j * 2 + 2), 16);
								
								javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/NoPadding");
								cipher.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(aBytes, "AES"), new javax.crypto.spec.IvParameterSpec(bBytes));
								byte[] dec = cipher.doFinal(cBytes);
								
								StringBuilder hexStr = new StringBuilder();
								for (byte b : dec) hexStr.append(String.format("%02x", b));
								
								java.net.HttpCookie cookie = new java.net.HttpCookie("__test", hexStr.toString());
								cookie.setDomain(u.getHost());
								cookie.setPath("/");
								cookieManager.getCookieStore().add(u.toURI(), cookie);
								continue; 
							}
						}
						return lastResponse; 
					}
					return "ERR:LoopExhausted\nLastResponse:\n" + lastResponse;
				} catch (Exception e) {
					return "ERR:" + e.getClass().getSimpleName() + ": " + e.getMessage();
				}
			}
		}, "Android");
		try {
			Intent widgetUpdate = new Intent(getApplicationContext(), TodoWidgetProvider.class);
			widgetUpdate.setAction("UPDATE_TODO_WIDGET");
			sendBroadcast(widgetUpdate);
		} catch (Exception e) {}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		try {
			Intent widgetUpdate = new Intent(getApplicationContext(), TodoWidgetProvider.class);
			widgetUpdate.setAction("UPDATE_TODO_WIDGET");
			sendBroadcast(widgetUpdate);
		} catch (Exception e) {}
	}
}