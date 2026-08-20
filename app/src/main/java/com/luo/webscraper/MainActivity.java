package com.luo.webscraper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private EditText urlInput;
    private Button btnFetch;
    private TextView titleView;
    private TextView contentView;
    private Button btnCopy;
    private Button btnShare;

    private String currentTitle = "";
    private String currentContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlInput = findViewById(R.id.url_input);
        btnFetch = findViewById(R.id.btn_fetch);
        titleView = findViewById(R.id.title_view);
        contentView = findViewById(R.id.content_view);
        btnCopy = findViewById(R.id.btn_copy);
        btnShare = findViewById(R.id.btn_share);

        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetch();
            }
        });

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                fetch();
                return true;
            }
            return false;
        });

        btnCopy.setOnClickListener(v -> {
            if (currentContent.isEmpty()) {
                Toast.makeText(this, "没有可复制的内容", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("scrape", currentTitle + "\n\n" + currentContent));
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            if (currentContent.isEmpty()) {
                Toast.makeText(this, "没有可分享的内容", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, currentTitle + "\n\n" + currentContent);
            startActivity(Intent.createChooser(intent, "分享到"));
        });
    }

    private void fetch() {
        String input = urlInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入网址", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = input.startsWith("http://") || input.startsWith("https://")
                ? input : "https://" + input;

        btnFetch.setEnabled(false);
        btnFetch.setText("抓取中…");
        contentView.setText("正在抓取 " + url + " …");

        new FetchTask().execute(url);
    }

    private class FetchTask extends AsyncTask<String, Void, String> {

        private String title;

        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Linux; Android 7.0) AppleWebKit/537.36")
                        .timeout(15000)
                        .followRedirects(true)
                        .get();

                title = doc.title();

                StringBuilder sb = new StringBuilder();

                // 正文文字（去脚本、样式、导航、广告）
                doc.select("script, style, noscript, nav, header, footer, aside, iframe, form").remove();

                String bodyText = doc.body() != null ? doc.body().text() : doc.text();
                sb.append("【正文】\n").append(bodyText).append("\n\n");

                // 所有链接
                sb.append("【链接】\n");
                int count = 0;
                for (org.jsoup.nodes.Element a : doc.select("a[href]")) {
                    String href = a.absUrl("href");
                    String text = a.text().trim();
                    if (href.startsWith("http")) {
                        sb.append(count + 1).append(". ");
                        sb.append(text.isEmpty() ? href : text).append("\n");
                        sb.append("   ").append(href).append("\n");
                        count++;
                        if (count >= 50) break;
                    }
                }
                if (count == 0) {
                    sb.append("（无链接）\n");
                }

                return sb.toString();
            } catch (IOException e) {
                return "抓取失败：" + e.getMessage();
            } catch (Exception e) {
                return "抓取失败：" + e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            btnFetch.setEnabled(true);
            btnFetch.setText("抓取");

            currentTitle = title != null ? title : "";
            currentContent = result;

            if (currentTitle.isEmpty()) {
                titleView.setVisibility(View.GONE);
            } else {
                titleView.setVisibility(View.VISIBLE);
                titleView.setText(currentTitle);
            }
            contentView.setText(result);
        }
    }
}
