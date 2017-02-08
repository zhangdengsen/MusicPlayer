package com.example.v5188.musicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MusicService extends Service {
    private List<Music> list = MainActivity.list;
    private MediaPlayer mMediaPlayer;
    private int item = 0;
    private boolean flag = false;
    private NotificationManager mBoFang;
    private static final int LOOPING_MODEL = 1;//单曲循环播放
    private static final int ORDERING_MODEL = 2;//顺序播放
    private static final int ALL_LOOPING_MODEL = 3;//全部循环播放
    private static final int RANDOM_MODEL = 4;//随机播放
    private int CURRENT_PLAY_MODEL = ORDERING_MODEL;//当前播放模式

    interface MusicPlayerService {
        void IinintMusic(int number);//初始化音乐

        boolean IIsPlayering(); //是否正在播放

        void IPlayerMusic();    //播放暂停音乐

        int ICurrentTime();  //当前播放时间

        int ITotalTime();  //播放总时间

        void shangPlayMusic();//上一首

        void xiaPlayMusic();//下一首

        void IseekBar(int totalTime);//拖动时间

        int Iitem();//返回的每项

        void ImoShi(int moshi);//模式
    }

    class MusicPlayerBinder extends Binder implements MusicPlayerService {

        @Override
        public void IinintMusic(int number) {
                item = number;
                inintMusic(item);//初始化音乐
        }

        @Override
        public boolean IIsPlayering() {
            if (mMediaPlayer != null) {
                return mMediaPlayer.isPlaying(); //是否正在播放
            }
            return false;
        }

        @Override
        public void IPlayerMusic() {
            if (mMediaPlayer != null) {
                if (IIsPlayering()) {
                    mMediaPlayer.pause();//播放暂停音乐
                } else {
                    mMediaPlayer.start();
                }
                boFang();
            }
        }

        @Override
        public int ICurrentTime() {
            return mMediaPlayer.getCurrentPosition();//当前播放时间
        }

        @Override
        public int ITotalTime() {
            return mMediaPlayer.getDuration();//播放总时间
        }

        @Override
        public void shangPlayMusic() {
            if (--item < 0) {
                item = list.size() - 1;//上一首
            }
            inintMusic(item);
            mMediaPlayer.start();
            boFang();
        }

        @Override
        public void xiaPlayMusic() {
            if (++item >= list.size()) {
                item = 0;//下一首
            }
            inintMusic(item);
            mMediaPlayer.start();
            boFang();
        }

        @Override
        public void IseekBar(int totalTime) {
            mMediaPlayer.seekTo(totalTime);//拖动时间
        }

        @Override
        public int Iitem() {
            return item;//返回的每项
        }

        @Override
        public void ImoShi(int moshi) {
            CURRENT_PLAY_MODEL = moshi;
        }
    }

    MusicPlayerBinder musicPlayerBinder = new MusicPlayerBinder();

    @Override
    public IBinder onBind(Intent intent) {
            inintMusic(item);
            mBoFang = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            boFang();
        return musicPlayerBinder;
    }

    /**
     * 初始化音乐
     */
    private void inintMusic(int item) {
        Music music = list.get(item);
        String dizhi = music.getDizhi();
        if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();
        try {
            if (dizhi != null) {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(dizhi);
                mMediaPlayer.prepare();
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        singleModel();
                        orderModel();
                        allLoopingModel();
                        randomModel();
                    }
                });
            }
            }catch(IOException e){
                e.printStackTrace();
            }
            if (flag) {
                Intent intentbofang = new Intent("baofang1");
                LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intentbofang);
            }
        }

    /**
     * 单曲播放
     */
    public void singleModel() {
        if (CURRENT_PLAY_MODEL == LOOPING_MODEL) {
            inintMusic(item);
            musicPlayerBinder.IPlayerMusic();
        }
    }

    /**
     * 顺序播放
     */
    public void orderModel() {
        if (CURRENT_PLAY_MODEL == ORDERING_MODEL) {
            if (++item >= list.size()) {
                Toast.makeText(this, "全部播完", Toast.LENGTH_SHORT).show();
                item = 0;
                inintMusic(item);
            } else {
                inintMusic(item);
                musicPlayerBinder.IPlayerMusic();
            }
        }
    }

    /**
     * 全部循环
     */
    public void allLoopingModel() {

        if (CURRENT_PLAY_MODEL == ALL_LOOPING_MODEL) {
            if (++item >= list.size()) {
                item = 0;
            }
            inintMusic(item);
            musicPlayerBinder.IPlayerMusic();
        }
    }


    /**
     * 随机播放
     */
    public void randomModel() {

        if (CURRENT_PLAY_MODEL == RANDOM_MODEL) {
            int numbers = list.size();
            Random random = new Random();
            item = random.nextInt(numbers);
            inintMusic(item);
            musicPlayerBinder.IPlayerMusic();
        }
    }


    /**
     * 第一步 定义广播接收者
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("bofang")) {
                musicPlayerBinder.IPlayerMusic();
                boFang();
                Intent intentbofang = new Intent("baofang2");
                LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intentbofang);
            }
            if (intent.getAction().equals("shang")) {
                musicPlayerBinder.shangPlayMusic();
            }
            if (intent.getAction().equals("xia")) {
                musicPlayerBinder.xiaPlayMusic();
            }
        }
    };

    @Override
    public void onCreate() {
        IntentFilter intentFilterBoFang = new IntentFilter("bofang");
        registerReceiver(receiver, intentFilterBoFang);
        IntentFilter intentFilterShang = new IntentFilter("shang");
        registerReceiver(receiver, intentFilterShang);
        IntentFilter intentFilterXia = new IntentFilter("xia");
        registerReceiver(receiver, intentFilterXia);
        super.onCreate();
    }

    /**
     * 自定义通知栏信息
     */
    public void boFang() {
        flag = true;
        Music music = list.get(item);
        Bitmap ioc = BitmapFactory.decodeFile(music.getTupian());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.lay_protype_default);
        builder.setContentText("1条新消息");
        builder.setTicker("音乐播放器通知");
//        builder.setDefaults(Notification.DEFAULT_ALL);
//        builder.setWhen(System.currentTimeMillis());
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.zidingyi_tongzhilan);
        builder.setContent(remoteViews);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //通知栏图片
        remoteViews.setOnClickPendingIntent(R.id.ImageView_tongzhi_tupian, pendingIntent);
        remoteViews.setImageViewBitmap(R.id.ImageView_tongzhi_tupian, ioc == null ? BitmapFactory.decodeResource(getResources(), R.drawable.lay_protype_default) : ioc);
        //通知栏歌名
        remoteViews.setOnClickPendingIntent(R.id.TextView_tongzhigeming, pendingIntent);
        remoteViews.setTextViewText(R.id.TextView_tongzhigeming, music.getGeming());
        //通知栏歌手
        remoteViews.setOnClickPendingIntent(R.id.TextView_tongzhigeshou, pendingIntent);
        remoteViews.setTextViewText(R.id.TextView_tongzhigeshou, music.getGeshou());
        //通知栏播放暂停
        Intent intentBoFang = new Intent("bofang");
        PendingIntent pendingIntentBoFang = PendingIntent.getBroadcast(this, 101, intentBoFang, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.ImageView_tongzhi_bofangzanting, pendingIntentBoFang);
        if (mMediaPlayer.isPlaying()) {
            remoteViews.setImageViewResource(R.id.ImageView_tongzhi_bofangzanting, R.drawable.tongzhilan_zanting);
        } else {
            remoteViews.setImageViewResource(R.id.ImageView_tongzhi_bofangzanting, R.drawable.tongzhilan_bofang);
        }
        //通知栏上一首
        Intent intentShang = new Intent("shang");
        PendingIntent pendingIntentShang = PendingIntent.getBroadcast(this, 101, intentShang, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.ImageView_tongzhi_shang, pendingIntentShang);
        //通知栏下一首
        Intent intentXia = new Intent("xia");
        PendingIntent pendingIntentXia = PendingIntent.getBroadcast(this, 101, intentXia, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.ImageView_tongzhi_xia, pendingIntentXia);
        Notification notification = builder.build();
        mBoFang.notify(11, notification);
    }
}
