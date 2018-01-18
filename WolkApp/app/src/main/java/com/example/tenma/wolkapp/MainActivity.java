package com.example.tenma.wolkapp;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageButton start,stop,reset;

    private TextView mStepCounterText;
    private SensorManager mSensorManager;
    private Sensor  mStepCounterSensor;

    //前回の不必要歩数
    float beforedust;
    static float beforestopfirst;

    private SoundPool soundPool;

    //Android終了時・起動時の処理
    public final static class mReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            SharedPreferences pref = context.getSharedPreferences("file", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            String action = intent.getAction();
            if(action.equals("android.intent.action.ACTION_SHUTDOWN")) {
                Log.v("testt", "-----SHUTDOWN-----");
                //Toast.makeText(context, "-----SHUTDOWN-----", Toast.LENGTH_SHORT).show();

                if(pref.getFloat("beforestopfirst", -1) == -1) {

                    Log.v("testt", "スタートが押された状態でシャットダウン");

                    editor.putFloat("beforestopfirst", se.values[0]);
                    editor.apply();

                    editor.putBoolean("shutdown", true);
                    editor.apply();

                }
                Log.v("testt", "[センサ]" + se.values[0]);
                Log.v("testt", "[beforestopfirst]" + pref.getFloat("beforestopfirst", -1));
                Log.v("testt", "-----SHUTDOWN-----");

            }
            if(action.equals("android.intent.action.BOOT_COMPLETED")) {
                Log.v("testt", "BOOT_COMPLETED");
                //Toast.makeText(context, "-----BOOT_COMPLETED-----", Toast.LENGTH_SHORT).show();

                if(pref.getBoolean("shutdown", false)) {

                    Log.v("testt", "& shutdownのPreferenceが成功");

                    editor.putBoolean("bootcompleted", true);
                    editor.apply();

                    editor.putBoolean("shutdown", false);
                    editor.apply();

                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ジャイロセンサー起動　歩数計測スタート
        start = (ImageButton) findViewById(R.id.IBstart);
        start.setOnClickListener(this);

        //ジャイロセンサー停止　歩数計測ストップ
        stop = (ImageButton) findViewById(R.id.IBstop);
        stop.setOnClickListener(this);

        //ジャイロセンサー取得数値消去
        reset = (ImageButton) findViewById(R.id.IBreset);
        reset.setOnClickListener(this);

        //歩数
        mStepCounterText = (TextView) findViewById(R.id.pedometer);

        //プリファレンスのインスタンス取得
        //前回不必要歩数の取得（歩数stepsがある場合）
        SharedPreferences pref = getSharedPreferences("file", MODE_PRIVATE);
        beforedust = pref.getFloat("beforedust", -1);
        //ストップの間に加算された歩数があり、終了した場合
        beforestopfirst = pref.getFloat("beforestopfirst", -1);

        if(pref.getBoolean("bootcompleted", false)) {

            Log.v("testt", "Android起動！！！！！！！！！！！！！！！");

            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("bootcompleted", false);
            editor.apply();
        }
    }

    protected void onResume() {
        super.onResume();

        //KITKAT以上かつTYPE_STEP_COUNTERが有効ならtrue
        boolean isTarget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);

        if (isTarget) {
            //TYPE_STEP_COUNTERが有効な場合の処理
            Log.d("hasStepCounter", "STEP-COUNTER is available!!!");
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mStepCounterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

            setStepCounterListener();
        } else {
            //TYPE_STEP_COUNTERが無効な場合の処理
            Log.d("hasStepCounter", "STEP-COUNTER is NOT available.");
            mStepCounterText.setText("STEP-COUNTER is NOT available.");
        }

        // 予め音声データを読み込む
        soundPool = new SoundPool(50, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load(getApplicationContext(), R.raw.clicksound1, 1);

    }

    boolean onstopflag = false;

    protected  void  onStart() {
        super.onStart();

        if(onstopflag) {
            onstopflag = false;
        }
    }

    protected void onStop() {
        super.onStop();

        //onDestroy()時にonStop()が呼び出されてしまうのでflag処理
        if(!onstopflag) {

            Log.v("testt", "-----onStop()が呼ばれました-----");
            Log.v("testt", "[steps]" + steps);
            Log.v("testt", "[stopfirst]" + stopfirst);

            //歩数がある場合の保存
            if(steps > 0) {
                beforedust = dust;
            }else {
                beforedust = -1;
            }
            //増やした数を再び保存
            SharedPreferences pref = getSharedPreferences("file", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putFloat("beforedust", beforedust);
            editor.apply();
            Log.v("testt", "[beforedust]" + beforedust);

            //ストップの間に加算された歩数があり、終了した場合
            if(stopflag) {
                Log.v("testt", "※ストップが押されてる状態");
                beforestopfirst = stopfirst;
            }else {
                beforestopfirst = -1;
            }
            //増やした数を再び保存
            editor = pref.edit();
            editor.putFloat("beforestopfirst", beforestopfirst);
            editor.apply();

            Log.v("testt", "[beforestopfirst]" + beforestopfirst);
            Log.v("testt", "[センサ]" + se.values[0]);
            Log.v("testt", "-----onStop()が呼ばれました[終了]-----");

            onstopflag = true;
        }
    }

//    //呼び出されないことがあるので使用しない
//    @Override
//    protected void onDestroy() {
//       super.onDestroy();
//
//    }

    private void setStepCounterListener() {
        if (mStepCounterSensor != null) {
            //ここでセンサーリスナーを登録する
            mSensorManager.registerListener(mStepCountListener, mStepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private static SensorEvent se;
    private int soundId;

    //スタート・ストップ・リセットの状態
    boolean startflag = false;
    boolean stopflag = false;
    boolean resetflag = false;

    //現在の歩数
    private float steps = 0;

    //起動時を表す（1度だけ使用）
    int first = 0;

    //アプリ起動以前に記録された歩数、および不必要歩数の総和
    float dust = 0;
    //ストップが押された時の[センサの値]
    float stopfirst = 0;
    //ストップが押されている間の歩数（不必要歩数）
    //(スタートが押された瞬間の[センサの値]) - stopdust で求める
    float stopsteps = 0;



    private final SensorEventListener mStepCountListener = new SensorEventListener() {

        //センサーから歩数を取得し、表示するメソッド
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            se = sensorEvent;

            Log.v("testt", "[センサ]：" + se.values[0]);

            //アプリ起動直後の処理
            //[0歩]もしくは、[前回の累積歩数]を表示
            if(first == 0) {
                //必要ないセンサの累積歩数を入れる（起動時は必ず）
                dust = se.values[0];
                //不要歩数の上書き（前回終了時に歩数stepsがある場合）
                if(beforedust > 0) {
                    dust = beforedust;
                }
                Log.v("testt", "前回の不必要歩数の総和[beforedust]" + beforedust);

                //起動時、ストップボタンを押している状態にする
                stopfirst = se.values[0];
                //ストップボタンを押し、加算された状態でアプリ終了した場合
                if(beforestopfirst > 0) {
                    //前回、ストップボタンを押した時のセンサの値
                    stopfirst = beforestopfirst;
                    Log.v("testt", "※※※ストップを押して終了※※※");
                    Log.v("testt", "ストップを押した時[beforestopfirst]：" + beforestopfirst);

                    stopsteps = se.values[0] - stopfirst;
                    dust += stopsteps;

                    //初期化(この処理をしないとスタートボタンを押した時に重複処理になる)
                    stopfirst = se.values[0];

                    Log.v("testt", "stop中に増えた歩数[stopsteps]：" + stopsteps);
                    Log.v("testt", "stopstepsを足す[dust(起動時)]：" + dust);
                    Log.v("testt", "※※※※※※");

                }

                //最初に表示したい歩数の計算
                steps = se.values[0] - dust;

                //歩数の表示
                mStepCounterText.setText(String.format(Locale.US, "%d", (int)steps));

                //状態の初期化（ストップを押している状態）
                startflag = false;
                stopflag = true;
                resetflag = true;

                //（起動2回目以降）スタートが押された状態で終了
                if(beforedust > 0 && beforestopfirst < 0) {
                    startflag = true;
                    stopflag = false;
                    resetflag = true;

                    teststart = (ImageButton) findViewById(R.id.IBstart);
                    teststart.setImageResource(R.drawable.start2);
                    teststart = (ImageButton) findViewById(R.id.IBstop);
                    teststart.setImageResource(R.drawable.stop);
                    teststart = (ImageButton) findViewById(R.id.IBreset);
                    teststart.setImageResource(R.drawable.reset2);
                }

                //初回起動時の処理のため、以降このif文に入らないようにする
                first++;
            }
            //スタートボタンが押されている時
            else if(startflag) {

                //歩数表示を増加させる
                //wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww
                steps = se.values[0] - dust;
                mStepCounterText.setText(String.format(Locale.US, "%d", (int)steps));
                //wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww

            }
            //ストップボタンが押されている時
            else if(stopflag) {

                //歩数表示は変化させず維持する（ストップが押される直前の歩数を表示し続けるだけ）

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

    };

    ImageButton teststart;

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            //スタートボタン
            case R.id.IBstart:
                //ストップが押されたときに押せる
                if(stopflag) {
                    //ボタンの音
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    //Toast.makeText(this, "スタート！", Toast.LENGTH_SHORT).show();

                    //スタート・ストップボタンの画像変更
                    teststart = (ImageButton) findViewById(R.id.IBstart);
                    teststart.setImageResource(R.drawable.start2);
                    teststart = (ImageButton) findViewById(R.id.IBstop);
                    teststart.setImageResource(R.drawable.stop);
                    teststart = (ImageButton) findViewById(R.id.IBreset);
                    teststart.setImageResource(R.drawable.reset2);

                    Log.v("testt", "-----スタートボタンが押されました-----");
                    Log.v("testt", "いらない歩数[dust(変化前)]" + dust);

                    //歩数計算
                    stopsteps = se.values[0] - stopfirst;
                    dust += stopsteps;  //不必要歩数

                    Log.v("testt", "stop中に増えた歩数[stopsteps]" + stopsteps + " = [センサ]" + se.values[0] + " - [stopfirst]" + stopfirst);

                    Log.v("testt", "いらない歩数[dust(変化後)]" + dust + " = [dust(変化前)]" + (dust - stopsteps) + " + [stopsteps]" + stopsteps);

                    Log.v("testt", "歩数[steps]" + steps + " = [センサ]" + se.values[0] + " - [dust(変化後)]" + dust);

                    Log.v("testt", "~~~スタートボタンが押されました[終了]~~~");

                    //状態変更
                    startflag = true;
                    stopflag = false;
                    resetflag = false;

                }
                break;

            //ストップボタン
            case R.id.IBstop:
                if(startflag) {
                    //ボタンの音
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    //Toast.makeText(this, "ストップ！", Toast.LENGTH_SHORT).show();

                    //スタート・ストップ・リセットボタンの画像変更
                    teststart = (ImageButton) findViewById(R.id.IBstart);
                    teststart.setImageResource(R.drawable.start);
                    teststart = (ImageButton) findViewById(R.id.IBstop);
                    teststart.setImageResource(R.drawable.stop2);
                    teststart = (ImageButton) findViewById(R.id.IBreset);
                    teststart.setImageResource(R.drawable.reset);

                    //歩数計算
                    stopfirst = se.values[0];

                    //状態変更
                    startflag = false;
                    stopflag = true;
                    resetflag = true;
                }
                break;

            //リセットボタン
            case R.id.IBreset:
                if(resetflag) {
                    //ボタンの音
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    //Toast.makeText(this, "リセット！", Toast.LENGTH_SHORT).show();

                    //スタート・リセットボタンの画像変更
                    teststart = (ImageButton) findViewById(R.id.IBstart);
                    teststart.setImageResource(R.drawable.start);
                    teststart = (ImageButton) findViewById(R.id.IBreset);
                    teststart.setImageResource(R.drawable.reset2);

                    //歩数計算
                    dust = se.values[0];
                    steps = se.values[0] - dust;
                    mStepCounterText.setText(String.format(Locale.US, "%d", (int)steps));

                    //状態変更
                    startflag = false;
                    stopflag = true;
                    resetflag = false;

                }
                break;
        }
    }
}
