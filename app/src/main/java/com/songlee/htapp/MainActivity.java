package com.songlee.htapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    private ListView infoListView;
    private List<Map<String, Object>> list = new ArrayList<>();
    private String url_first_half = "http://xjh.haitou.cc/wh/uni-";
    private String url_second_half = "/after/hold/page-";
    private String next_page_url = "";
    private int currentPage = 1;
    private int currentPosition;
    private String url;
    private ProgressDialog dialog;
    // popupWindow
    private PopupWindow popupWindow;
    private ListView menuListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // 显示宣讲会信息的ListView
        infoListView = (ListView) findViewById(R.id.info_list_view);

        // 初始化popupWindow
        initPopupWindow();

    }


    // 将数据填充到ListView中
    private void show() {
        if(list.isEmpty()) {
            TextView message = (TextView)findViewById(R.id.message);
            message.setText(R.string.message);
        } else {
            SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.my_list_item,
                    new String[]{"company", "time", "address"},
                    new int[]{R.id.company, R.id.time, R.id.address});
            infoListView.setAdapter(adapter);
        }
        dialog.dismiss();  // 关闭窗口
    }


    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Connection conn = Jsoup.connect(url);
            // 修改http包中的header,伪装成浏览器进行抓取
            conn.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/    20100101 Firefox/32.0");
            Document doc = null;
            try {
                doc = conn.get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 获取下一页的链接
            Elements link = doc.select("li.paginate_button").select("li.next");
            next_page_url = link.select("a").attr("href");
            // 获取tbody元素下的所有tr元素
            Elements elements = doc.select("tbody tr");
            for(Element element : elements) {
                String companyName = element.getElementsByTag("company").text();
                String time = element.select("td.text-center").first().text();
                String address = element.getElementsByClass("preach-tbody-addre").text();

                Map<String, Object> map = new HashMap<>();
                map.put("company", companyName);
                map.put("time", time);
                map.put("address", address);
                list.add(map);
            }
            // 执行完毕后给handler发送一个空消息
            handler.sendEmptyMessage(0);
        }
    };


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // 收到消息后执行handler
            show();
        }
    };


    // 判断是否有可用的网络连接
    public boolean isNetworkAvailable(Activity activity)
    {
        Context context = activity.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;
        else
        {   // 获取所有NetworkInfo对象
            NetworkInfo[] networkInfo = cm.getAllNetworkInfo();
            if (networkInfo != null && networkInfo.length > 0)
            {
                for (int i = 0; i < networkInfo.length; i++)
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED)
                        return true;  // 存在可用的网络连接
            }
        }
        return false;
    }


    // 重新抓取
    public void switchOver(final int position) {
        if(isNetworkAvailable(MainActivity.this)) {
            // 显示“正在加载”窗口
            dialog = new ProgressDialog(this);
            dialog.setMessage("正在抓取数据...");
            dialog.setCancelable(false);
            dialog.show();

            // 初始时，抓取并显示；切换学校时，重新抓取并显示
            url = url_first_half + (position+1) + url_second_half + currentPage + "/";
            list.clear();
            new Thread(runnable).start();  // 子线程
        } else {
            // 弹出提示框
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("当前没有网络连接！")
                    .setPositiveButton("重试",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switchOver(position);
                        }
                    }).setNegativeButton("退出",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);  // 退出程序
                        }
                    }).show();
        }
    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
        currentPosition = position;  // 保存当前位置（学校）
        // 切换学校
        currentPage = 1;   // 重置1
        switchOver(position);
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    // 刷新
    public void refresh() {
        if(isNetworkAvailable(MainActivity.this)) {
            // 显示“正在刷新”窗口
            dialog = new ProgressDialog(this);
            dialog.setMessage("正在刷新...");
            dialog.setCancelable(false);
            dialog.show();
            // 重新抓取
            list.clear();
            new Thread(runnable).start();  // 子线程
        } else {
            // 弹出提示框
            new AlertDialog.Builder(this)
                    .setTitle("刷新")
                    .setMessage("当前没有网络连接！")
                    .setPositiveButton("重试",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            refresh();
                        }
                    }).setNegativeButton("退出",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);  // 退出程序
                        }
                    }).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.refresh:
                refresh();
                return true;
            case R.id.more:
                if(popupWindow.isShowing())
                    popupWindow.dismiss();
                else
                    popUp();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //实例化PopupWindow创建菜单
    private void initPopupWindow(){
        View view = getLayoutInflater().inflate(R.layout.popup_window, null);
        menuListView = (ListView)view.findViewById(R.id.popup_list_view);
        popupWindow = new PopupWindow(view, 160,WindowManager.LayoutParams.WRAP_CONTENT);
        // 数据
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("menu_text", "上一页");
        data.add(map);
        map = new HashMap<>();
        map.put("menu_text", "下一页");
        data.add(map);
        // 创建适配器
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.menu_list_item,
                new String[]{"menu_text"}, new int[]{R.id.menu_text});
        menuListView.setAdapter(adapter);
        // 添加Item点击响应
        menuListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        prePage();  // 上一页
                        break;
                    case 1:
                        nextPage(); // 下一页
                }
            }
        });
        // 在popupWindow以外的区域点击后关闭popupWindow
        popupWindow.setFocusable(true);
        popupWindow.setTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
    }


    //显示PopupWindow菜单
    private void popUp(){
        //设置位置
        popupWindow.showAsDropDown(this.findViewById(R.id.more), 0, 2);
    }


    // 上一页
    public void prePage() {
        if(isNetworkAvailable(MainActivity.this)) {
            if(currentPage == 1)
                Toast.makeText(MainActivity.this, "已经是第一页了", Toast.LENGTH_SHORT).show();
            else {
                --currentPage;
                switchOver(currentPosition);
            }
        } else {
            // 弹出提示框
            new AlertDialog.Builder(this)
                    .setTitle("上一页")
                    .setMessage("当前没有网络连接！")
                    .setPositiveButton("重试",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prePage();
                        }
                    }).setNegativeButton("退出",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);  // 退出程序
                }
            }).show();
        }
    }

    // 下一页
    public void nextPage() {
        if(isNetworkAvailable(MainActivity.this)) {
            if(next_page_url.equals("#"))
                Toast.makeText(MainActivity.this, "已经是最后一页了", Toast.LENGTH_SHORT).show();
            else {
                ++currentPage;
                switchOver(currentPosition);
            }
        } else {
            // 弹出提示框
            new AlertDialog.Builder(this)
                    .setTitle("下一页")
                    .setMessage("当前没有网络连接！")
                    .setPositiveButton("重试",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            nextPage();
                        }
                    }).setNegativeButton("退出",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);  // 退出程序
                }
            }).show();
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
