package com.example.tenma.wolkapp;

import android.app.Fragment;
import android.content.pm.PackageManager;
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

    private SoundPool soundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ジャイロセンサー起動　歩数計測スタート
        start = (ImageButton) findViewById(R.id.imageButton4);
        start.setOnClickListener(this);

        //ジャイロセンサー停止　歩数計測ストップ
        stop = (ImageButton) findViewById(R.id.imageButton5);
        stop.setOnClickListener(this);

        //ジャイロセンサー取得数値消去
        reset = (ImageButton) findViewById(R.id.imageButton6);
        reset.setOnClickListener(this);

        mStepCounterText = (TextView) findViewById(R.id.pedometer);

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


    //↓無くてもいいらしいですが、サイトのコードをコピーした時についてきたので念のため残しておきます
    /*
    protected void onPause() {
        super.onPause();
        // リリース
        soundPool.release();
    }
    private void playFromSoundPool() {
        // 再生
        soundPool.play(soundId, 1.0F, 1.0F, 0, 0, 1.0F);
    }
    */

    private void setStepCounterListener() {
        if (mStepCounterSensor != null) {
            //ここでセンサーリスナーを登録する
            mSensorManager.registerListener(mStepCountListener, mStepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private SensorEvent se;
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
    float stopdust = 0;
    //ストップが押されている間の歩数（不必要歩数）
    //(スタートが押された瞬間の[センサの値]) - stopdust で求める
    float stopsteps = 0;



    private final SensorEventListener mStepCountListener = new SensorEventListener() {

        //センサーから歩数を取得し、表示するメソッド
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            se = sensorEvent;

            //センサーから取得した値をテキストビューに表示する

            //アプリ起動直後の処理
            //[0歩]もしくは、[前回の累積歩数]を表示
            if(first == 0) {
                //必要ないセンサの累積歩数を入れる
                dust = se.values[0];
                //最初に表示したい歩数の計算
                steps = se.values[0] - dust;
                //歩数の表示
                mStepCounterText.setText(String.format(Locale.US, "%f", steps));

                //状態の初期化
                startflag = false;
                stopflag = true;
                resetflag = false;

                //（起動時なので）ストップボタンを押している状態にする
                stopdust = se.values[0];

                //初回起動時の処理のため、以降このif文に入らないようにする
                first++;
            }
            //スタートボタンが押されている時
            else if(startflag) {

                //歩数表示を増加させる
                //wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww
                steps = se.values[0] - dust;
                mStepCounterText.setText(String.format(Locale.US, "%f", steps));
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
            case R.id.imageButton4:
                //ストップが押されたとき（かつリセットは押されてない）に押せる
                if(stopflag && !resetflag) {
                    //ボタンの音
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    //Toast.makeText(this, "スタート！", Toast.LENGTH_SHORT).show();

                    //スタート・ストップボタンの画像変更
                    teststart = (ImageButton) findViewById(R.id.imageButton4);
                    teststart.setImageResource(R.drawable.start2);
                    teststart = (ImageButton) findViewById(R.id.imageButton5);
                    teststart.setImageResource(R.drawable.stop);

                    //状態変更
                    startflag = true;
                    stopflag = false;
                    resetflag = false;

                    Log.d("testt", "dustの値：" + dust);

                    //歩数計算
                    stopsteps = se.values[0] - stopdust;
                    dust += stopsteps;

                    Log.d("testt", stopsteps + " = " + se.values[0] + " - " + stopdust);

                    Log.d("testt", "変化後のdustの値：" + dust);
                    Log.d("testt", "変化後のdustの値：" + dust);
                    Log.d("testt", "歩数の値：" + steps);

                }
                /*
                //リセットが押されたときに押せる
                if (stopflag && resetflag) {
                    //ボタンの音
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    //Toast.makeText(this, "スタート！", Toast.LENGTH_SHORT).show();

                    //スタート・ストップボタンの画像変更
                    teststart = (ImageButton) findViewById(R.id.imageButton4);
                    teststart.setImageResource(R.drawable.start2);
                    teststart = (ImageButton) findViewById(R.id.imageButton5);
                    teststart.setImageResource(R.drawable.stop);

                    //状態変更
                    startflag = true;
                    stopflag = false;
                    resetflag = false;

                    Log.d("testt", "dustの値：" + dust);

                    //歩数計算
                    stopsteps = se.values[0] - stopdust;
                    dust += stopsteps;

                    Log.d("testt", stopsteps + " = " + se.values[0] + " - " + stopdust);

                    Log.d("testt", "変化後のdustの値：" + dust);
                    Log.d("testt", "変化後のdustの値：" + dust);
                    Log.d("testt", "歩数の値：" + steps);
                }
                */
                break;

            //ストップボタン
            case R.id.imageButton5:
                if(startflag) {
                    //ボタンの音
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    //Toast.makeText(this, "ストップ！", Toast.LENGTH_SHORT).show();

                    //スタート・ストップ・リセットボタンの画像変更
                    teststart = (ImageButton) findViewById(R.id.imageButton4);
                    teststart.setImageResource(R.drawable.start);
                    teststart = (ImageButton) findViewById(R.id.imageButton5);
                    teststart.setImageResource(R.drawable.stop2);
                    teststart = (ImageButton) findViewById(R.id.imageButton6);
                    teststart.setImageResource(R.drawable.reset);

                    //状態変更
                    startflag = false;
                    stopflag = true;
                    resetflag = true;

                    //歩数計算
                    stopdust = se.values[0];

                }
                break;

            //リセットボタン
            case R.id.imageButton6:
                if(resetflag) {
                    //ボタンの音
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    //Toast.makeText(this, "リセット！", Toast.LENGTH_SHORT).show();

                    //スタート・リセットボタンの画像変更
                    teststart = (ImageButton) findViewById(R.id.imageButton4);
                    teststart.setImageResource(R.drawable.start);
                    teststart = (ImageButton) findViewById(R.id.imageButton6);
                    teststart.setImageResource(R.drawable.reset2);

                    //状態変更
                    startflag = false;
                    stopflag = true;
                    resetflag = false;

                    //歩数計算
                    dust = se.values[0];
                    steps = se.values[0] - dust;
                    mStepCounterText.setText(String.format(Locale.US, "%f", steps));

                }
                break;
        }


    }
}
