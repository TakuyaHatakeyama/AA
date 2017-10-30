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

    ImageButton start,stop,reset;

    float steps;
    int count = 0;

    private SoundPool soundPool;
    private TextView mStepCounterText;
    private SensorManager mSensorManager;
    private Sensor  mStepCounterSensor;

    SensorEvent se;

    private int soundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (ImageButton) findViewById(R.id.imageButton4);
        start.setOnClickListener(this);
        //ジャイロセンサー起動　歩数計測スタート

        stop = (ImageButton) findViewById(R.id.imageButton5);
        stop.setOnClickListener(this);
        //ジャイロセンサー停止　歩数計測ストップ

        reset = (ImageButton) findViewById(R.id.imageButton6);
        reset.setOnClickListener(this);
        //ジャイロセンサー取得数値消去

        mStepCounterText = (TextView) findViewById(R.id.pedometer);
        steps = 0;




    }
    protected void onResume() {
        super.onResume();
        // 予め音声データを読み込む
        soundPool = new SoundPool(50, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load(getApplicationContext(), R.raw.clicksound1, 1);


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

    float first = 0;
    boolean stopflag = false;
    float stopfirst = 0;
    float stopsteps = 0;



    private final SensorEventListener mStepCountListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            se = sensorEvent;

            //センサーから取得した値をテキストビューに表示する
            if(count == 0){
                first = se.values[0];
                steps = se.values[0] - first;
                mStepCounterText.setText(String.format(Locale.US, "%f", steps));
                count++;
            }else {
                if(!stopflag) {

                    //wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww
                    steps = se.values[0] - first;
                    mStepCounterText.setText(String.format(Locale.US, "%f", steps));
                    //wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww

                }else {

                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };


    //連続で同じボタンを押された場合の処理に使う変数
    int startB;
    int stopB = 1;


    ImageButton teststart;

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.imageButton4:

                //画像を変更する
                //ストップが押されたときに押せる
                if(startB == 0) {
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    teststart = (ImageButton) findViewById(R.id.imageButton4);
                    teststart.setImageResource(R.drawable.start2);
                    //Toast.makeText(this, "スタート！", Toast.LENGTH_SHORT).show();

                    teststart = (ImageButton) findViewById(R.id.imageButton5);
                    teststart.setImageResource(R.drawable.stop);

                    startB = 1;
                    stopB = 0;

                    //
                    stopflag = false;
                    stopsteps = se.values[0] - stopfirst;

                    Log.d("testt", "firstの値：" + first);
                    Log.d("testt", "センサの値：" + se.values[0]);
                    Log.d("testt", "stopfirstの値：" + stopfirst);



                }
                //リセットが押されたときに押せる
                if (stopB == 0) {
                    teststart = (ImageButton) findViewById(R.id.imageButton6);
                    teststart.setImageResource(R.drawable.reset2);
                }




                break;

            case R.id.imageButton5:
                //画像を変更する
                if(stopB == 0) {
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    teststart = (ImageButton) findViewById(R.id.imageButton4);
                    teststart.setImageResource(R.drawable.start);
                    teststart = (ImageButton) findViewById(R.id.imageButton5);
                    teststart.setImageResource(R.drawable.stop2);
                    teststart = (ImageButton) findViewById(R.id.imageButton6);
                    teststart.setImageResource(R.drawable.reset);
                    //Toast.makeText(this, "ストップ！", Toast.LENGTH_SHORT).show();

                    startB = 0;
                    stopB = 1;

                    //
                    stopflag = true;
                    stopfirst = se.values[0];



                }else{

                }
                break;

            case R.id.imageButton6:
                //画像を変更する
                if(stopB == 1) {
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1);    //音の大きさは0fから1fで調整できる
                    teststart = (ImageButton) findViewById(R.id.imageButton6);
                    teststart.setImageResource(R.drawable.reset2);
                    teststart = (ImageButton) findViewById(R.id.imageButton4);
                    teststart.setImageResource(R.drawable.start);
                    //Toast.makeText(this, "リセット！", Toast.LENGTH_SHORT).show();

                    first = se.values[0];
                    stopsteps = 0;
                    stopfirst = 0;
                    steps = se.values[0] - first;
                    mStepCounterText.setText(String.format(Locale.US, "%f", steps));



                    startB = 0;
                    stopB = 1;
                }else{

                }
                break;
        }


    }
}
