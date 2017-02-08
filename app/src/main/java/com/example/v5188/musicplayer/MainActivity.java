package com.example.v5188.musicplayer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static List<Music> list = new ArrayList<>();
    private Handler handler = new Handler();
    private ViewPager mViewPager;
    private MyBaseAdapter mMyBaseAdapter;
    private PopupWindow mPopupWindow;
    private boolean isUpdateThreadFalg = true;
    private SeekBar mSeekBar;
    private int curentTime;
    private ObjectAnimator mAnimator;
    private FrameLayout mFrameLayout_donghuan;
    private TextView mCurrentTimeTxt, mTotalTimeTxt, mTextView_zhutigeming, mTextView_zhutigeshou;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
    private ImageView mImageView_xiaoyuandian_1, mImageView_xiaoyuandian_2, mImageView_caidan, mImageView_baofang,
            mImageView_shang, mImageView_xia, mImageView_donghuatupian, mImageView_moshi;
    private static final int LOOPING_MODEL = 1;//单曲循环播放
    private static final int ORDERING_MODEL = 2;//顺序播放
    private static final int ALL_LOOPING_MODEL = 3;//全部循环播放
    private static final int RANDOM_MODEL = 4;//随机播放
    public int CURRENT_PLAY_MODEL = ALL_LOOPING_MODEL;//当前播放模式
    private MusicService.MusicPlayerService musicPlayerService;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicPlayerService = (MusicService.MusicPlayerService) iBinder;
            jieMian();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicPlayerService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.hide();


        mViewPager = (ViewPager) findViewById(R.id.ViewPager);
        mImageView_xiaoyuandian_1 = (ImageView) findViewById(R.id.ImageView_xiaoyuandian_1);
        mImageView_xiaoyuandian_2 = (ImageView) findViewById(R.id.ImageView_xiaoyuandian_2);
        mImageView_caidan = (ImageView) findViewById(R.id.ImageView_caidan);
        mImageView_baofang = (ImageView) findViewById(R.id.ImageView_baofang);
        mImageView_shang = (ImageView) findViewById(R.id.ImageView_shang);
        mImageView_xia = (ImageView) findViewById(R.id.ImageView_xia);
        mImageView_moshi = (ImageView) findViewById(R.id.ImageView_moshi);
        mSeekBar = (SeekBar) findViewById(R.id.SeekBar);
        mCurrentTimeTxt = (TextView) findViewById(R.id.TextView_dangqianshijian);
        mTotalTimeTxt = (TextView) findViewById(R.id.TextView_zongshijian);
        mTextView_zhutigeming = (TextView) findViewById(R.id.TextView_zhutigeming);
        mTextView_zhutigeshou = (TextView) findViewById(R.id.TextView_zhutigeshou);
        setViewPager();
        setPopupWindow();
        dongHuan();
        setSeekBar();
        new UpdateMusicThread().start();
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        mImageView_caidan.setOnClickListener(this);
        mImageView_baofang.setOnClickListener(this);
        mImageView_shang.setOnClickListener(this);
        mImageView_xia.setOnClickListener(this);
        mImageView_moshi.setOnClickListener(this);
    }

    /**
     * 按钮监听事件
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ImageView_caidan:
                if (mPopupWindow.isShowing()) {
                    mPopupWindow.dismiss();
                } else {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    mPopupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0], location[1] - mPopupWindow.getHeight());
                }
                break;
            case R.id.ImageView_baofang:
                musicPlayerService.IPlayerMusic();
                jieMian();
                break;
            case R.id.ImageView_shang:
                musicPlayerService.shangPlayMusic();
                jieMian();
                break;
            case R.id.ImageView_xia:
                musicPlayerService.xiaPlayMusic();
                jieMian();
                break;
            case R.id.ImageView_moshi:
                setPlayMode();
                break;
        }
    }

    /**
     * PopupWindow弹窗
     */
    public void setPopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v1 = inflater.inflate(R.layout.gedanlistview_layout, null);
        ListView listView = (ListView) v1.findViewById(R.id.ListView);
        mMyBaseAdapter = new MyBaseAdapter(this);
        listView.setAdapter(mMyBaseAdapter);
        handler();
        mPopupWindow = new PopupWindow(this);
        mPopupWindow.setContentView(v1);
        mPopupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        int[] Window = getScreenSize();
        mPopupWindow.setHeight(Window[0]);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setOutsideTouchable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                musicPlayerService.IinintMusic(i);
                musicPlayerService.IPlayerMusic();
                jieMian();
            }
        });
    }

    /**
     * popupWindow获取屏幕信息
     */
    public int[] getScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        // 获取屏幕信息
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeigh = dm.heightPixels;
        return new int[]{screenWidth, screenHeigh};
    }

    /**
     * 线程设置更新时间
     */
    class UpdateMusicThread extends Thread {
        @Override
        public void run() {
            while (isUpdateThreadFalg) {
                if (musicPlayerService != null && musicPlayerService.IIsPlayering()) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            curentTime = musicPlayerService.ICurrentTime();
                            int totalTime = musicPlayerService.ITotalTime();
                            mSeekBar.setMax(totalTime);
                            mSeekBar.setProgress(curentTime);
                            mCurrentTimeTxt.setText(simpleDateFormat.format(new Date(curentTime)));
                            mTotalTimeTxt.setText(simpleDateFormat.format(new Date(totalTime)));
                        }
                    });
                    SystemClock.sleep(1000);
                }
            }
        }
    }

    /**
     * setSeekBar拖动进度条
     */
    public void setSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                curentTime = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicPlayerService.IseekBar(curentTime);
                mCurrentTimeTxt.setText(simpleDateFormat.format(new Date(curentTime)));
            }
        });
    }

    /**
     * 线程设置更新数据源
     */
    public void handler() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        List<Music> list = ShuJuYuan();
                        mMyBaseAdapter.setList(list);
                    }
                });
            }
        }).start();
    }

    /**
     * 数据源
     */
    public List<Music> ShuJuYuan() {
        // 查询所有媒体数据
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            // 如果不是音乐
            String isMusic = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) continue;

            String geming = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String geshou = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

            if (isRepeat(geming, geshou)) continue;

            Music music = new Music();
            music.setTupian(getAlbumImage(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))));
            music.setGeming(geming);
            music.setGeshou(geshou + " | " + cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
            music.setDizhi(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
            list.add(music);
        }
        return list;
    }

    /**
     * 根据音乐名称和艺术家来判断是否重复包含了
     */
    private boolean isRepeat(String title, String artist) {
        for (Music music : list) {
            if (title.equals(music.getGeming()) && artist.equals(music.getGeshou())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据歌曲id获取图片
     */
    private String getAlbumImage(int anInt) {
        String result = "";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    Uri.parse("content://media/external/audio/albums/"
                            + anInt), new String[]{"album_art"}, null,
                    null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); ) {
                result = cursor.getString(0);
                break;
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null == result ? null : result;
    }

    /**
     * listview适配器
     */
    public class MyBaseAdapter extends BaseAdapter {
        List<Music> list = new ArrayList<>();
        LayoutInflater layoutInflater;

        MyBaseAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        public void setList(List<Music> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Msg msg;
            if (view == null) {
                view = layoutInflater.inflate(R.layout.listview_tupian, null);
                ImageView tupian = (ImageView) view.findViewById(R.id.profile_image);
                TextView geming = (TextView) view.findViewById(R.id.TextView_listview_geming);
                TextView geshou = (TextView) view.findViewById(R.id.TextView_listview_geshou);
                msg = new Msg();
                msg.tupian = tupian;
                msg.geming = geming;
                msg.geshou = geshou;
                view.setTag(msg);
            }
            msg = (Msg) view.getTag();
            Music music = (Music) getItem(i);
            Bitmap tupian = BitmapFactory.decodeFile(music.getTupian());
            msg.tupian.setImageBitmap(tupian == null ? BitmapFactory.decodeResource(getResources(), R.drawable.lay_protype_default) : tupian);
            msg.geming.setText(music.getGeming());
            msg.geshou.setText(music.getGeshou());
            return view;
        }

        class Msg {
            ImageView tupian;
            TextView geming;
            TextView geshou;
        }

    }

    /**
     * 设置 ViewPager
     */
    public void setViewPager() {
        //  解析listview布局
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View v1 = layoutInflater.inflate(R.layout.donghuaxuanzhuan_layout, null);
        View v2 = layoutInflater.inflate(R.layout.geci_layout, null);
        mFrameLayout_donghuan = (FrameLayout) v1.findViewById(R.id.FrameLayout_donghua);
        mImageView_donghuatupian = (ImageView) v1.findViewById(R.id.ImageView_donghuatupian);

        //  添加 listview布局到ViewAdapter适配器里面
        List<View> list = new ArrayList<>();
        list.add(v1);
        list.add(v2);
        MyPagerViewAdapter adapter = new MyPagerViewAdapter(list);
        mViewPager.setAdapter(adapter);
        mImageView_xiaoyuandian_1.setBackgroundResource(R.drawable.index_category_indicator_selected);
        mImageView_xiaoyuandian_2.setBackgroundResource(R.drawable.index_category_indicator_nor);
        //  viewPager监听事件实现
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        mImageView_xiaoyuandian_1.setBackgroundResource(R.drawable.index_category_indicator_selected);
                        mImageView_xiaoyuandian_2.setBackgroundResource(R.drawable.index_category_indicator_nor);
                        break;
                    case 1:
                        mImageView_xiaoyuandian_2.setBackgroundResource(R.drawable.index_category_indicator_selected);
                        mImageView_xiaoyuandian_1.setBackgroundResource(R.drawable.index_category_indicator_nor);
                        break;

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    /**
     * 自定义PagerAdapter适配器
     */
    public class MyPagerViewAdapter extends PagerAdapter {
        private List<View> pagerLists = new ArrayList<>();

        public MyPagerViewAdapter(List<View> list) {
            pagerLists = list;
        }

        @Override
        public int getCount() {
            return pagerLists.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = pagerLists.get(position);
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View v = pagerLists.get(position);
            container.removeView(v);
        }
    }

    /**
     * unbindService解绑
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null)
            unbindService(serviceConnection);
    }

    /**
     * 动画旋转
     */
    public void dongHuan() {
        mAnimator = ObjectAnimator.ofFloat(mFrameLayout_donghuan, "rotation", 0f, 360f);
        mAnimator.setDuration(20000);
        mAnimator.setInterpolator(new LinearInterpolator());//不停顿
        mAnimator.setRepeatCount(-1);//设置动画重复次数
        mAnimator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式
    }

    /**
     * 界面信息图片设置
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void jieMian() {
        Music music = list.get(musicPlayerService.Iitem());
        mTextView_zhutigeming.setText(music.getGeming());
        mTextView_zhutigeshou.setText(music.getGeshou());
        Bitmap ioc = BitmapFactory.decodeFile(music.getTupian());
        mImageView_donghuatupian.setImageBitmap(ioc == null ? BitmapFactory.decodeResource(getResources(), R.drawable.lay_protype_default) : ioc);
        mCurrentTimeTxt.setText(simpleDateFormat.format(new Date(musicPlayerService.ICurrentTime())));
        mTotalTimeTxt.setText(simpleDateFormat.format(new Date(musicPlayerService.ITotalTime())));
        if (musicPlayerService.IIsPlayering()) {
            mImageView_baofang.setImageResource(R.drawable.zanting);
            if (mAnimator.isRunning()) {
                mAnimator.resume();//恢复动画
            } else {
                mAnimator.start();//开始动画
            }
        } else {
            mImageView_baofang.setImageResource(R.drawable.bofang);
            mAnimator.pause();//暂停动画
        }
    }

    /**
     * 播放模式
     */
    public void setPlayMode() {
        musicPlayerService.ImoShi(CURRENT_PLAY_MODEL);
        switch (CURRENT_PLAY_MODEL) {
            case ORDERING_MODEL:
                CURRENT_PLAY_MODEL = ALL_LOOPING_MODEL;
                mImageView_moshi.setImageResource(R.drawable.shunxun);
                Toast.makeText(this, "顺序播放", Toast.LENGTH_SHORT).show();
                break;
            case ALL_LOOPING_MODEL:
                CURRENT_PLAY_MODEL = RANDOM_MODEL;
                mImageView_moshi.setImageResource(R.drawable.xunhuan);
                Toast.makeText(this, "循环播放", Toast.LENGTH_SHORT).show();
                break;
            case RANDOM_MODEL:
                CURRENT_PLAY_MODEL = LOOPING_MODEL;
                mImageView_moshi.setImageResource(R.drawable.suiji);
                Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT).show();
                break;
            case LOOPING_MODEL:
                CURRENT_PLAY_MODEL = ORDERING_MODEL;
                mImageView_moshi.setImageResource(R.drawable.danqu);
                Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 第一步 定义广播接收者
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("baofang1")) {
                jieMian();
            }
            if (intent.getAction().equals("baofang2")) {
                jieMian();
            }
        }
    };

    /**
     * 注册广播
     */
    @Override
    protected void onResume() {
        IntentFilter intentFilterBoFang1 = new IntentFilter("baofang1");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilterBoFang1);
        IntentFilter intentFilterBoFang2 = new IntentFilter("baofang2");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilterBoFang2);
        super.onResume();
    }

    /**
     * 注销广播
     */
    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }
}
