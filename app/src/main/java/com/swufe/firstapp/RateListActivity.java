package com.swufe.firstapp;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class RateListActivity extends ListActivity implements Runnable {
    Handler handler;
    private String logDate = "";
    private final String DATE_SP_KEY = "lastRateDateStr";

    String data[] = {"wait..."};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sp = getSharedPreferences("myrate", Context.MODE_PRIVATE);
        logDate = sp.getString(DATE_SP_KEY, "");
        Log.i("List", "lastRateDateStr=" + logDate);

        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_rate_list);
        List<String> list1 = new ArrayList<String>();
        for (int i = 1; i < 100; i++) {
            list1.add("item" + i);
        }
        ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data);
        setListAdapter(adapter);

        Thread thread = new Thread(this);
        thread.start();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 7) {
                    List<String> list2 = (List<String>) msg.obj;
                    ListAdapter adapter = new ArrayAdapter<String>(RateListActivity.this, android.R.layout.simple_list_item_1, list2);
                    setListAdapter(adapter);
                }

                super.handleMessage(msg);
            }
        };
    }

    public void run() {
        List<String> relist = new ArrayList<String>();
        String curDateStr = (new SimpleDateFormat("yyyy-M-dd")).format(new Date());
        Log.i("run", "curDateStr: " + curDateStr + "logDate: " + logDate);
        if (curDateStr.equals(logDate)) {
//如果相等，则不从网络中获取数据
            Log.i("run", "日期相等，从数据库中获取数据");
            RateManager manager=new RateManager(this);
            for (RateItem item:manager.listAll () ) {
                relist.add(item.getCurName() + "-> " + item.getCurRate());
            }
        } else {
                    Log.i ("run", "日期不等，从网络中获取在线数据");
            Document doc = null;
            try {
                doc = Jsoup.connect("https://www.boc.cn/sourcedb/whbj").get();
                //doc = Jsoup.parse(html);
                Log.i(TAG, "run: " + doc.title());

                Elements tables = doc.getElementsByTag("table");
                Element table2 = tables.get(1);
                Elements tds = table2.getElementsByTag("td");
                List<RateItem> rateList = new ArrayList<RateItem>();
                //获取TD中的元素
                for (int i = 0; i < tds.size(); i += 8) {
                    Element td1 = tds.get(i);
                    Element td2 = tds.get(i + 5);
                    String str1 = td1.text();
                    String val = td2.text();
                    Log.i(TAG, "run: " + str1 + "==>" + val);
                    relist.add(str1 + "==>" + val);
                    rateList.add(new RateItem(str1, val));
                }
                //把数据写入到数据库中
                RateManager manager = new RateManager(this);
                manager.deleteAll();
                manager.addAll(rateList);
                //更新记录日期
                SharedPreferences sp = getSharedPreferences("myrate", Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                edit.putString(DATE_SP_KEY, curDateStr);
                edit.commit();
                Log.i("run", "更新日期结束:" + curDateStr);


            } catch (IOException e) {
                e.printStackTrace();
            }
            Message msg = handler.obtainMessage(7);//取出来一个消息队列
            msg.obj = relist;
            handler.sendMessage(msg);//将msg发送到队列里
        }
    }
}