package com.homework.google;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.SpatialRelationUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button InsideBtn, OutsideBtn;
    private Button btnDraw, btnStart;

    private TextView currentpa, insidepa, outsidepa, differentpa;

    private float current = 0;
    private float in = 0;
    private float out = 0;

    private SensorManager mSensorManager = null;
    private Sensor mSensor = null;

    //map
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    LatLng choosePoint, myPos, myPosGPS;
    BitmapDescriptor bdA = BitmapDescriptorFactory
            .fromResource(R.mipmap.ic_launcher);


    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;

    Button requestLocButton;
    boolean isFirstLoc = true;
    private MyLocationData locData;

    private int drawFlag; //0-不绘图 1-绘图中
    private int locFlag;  //0-围栏之外 1-围栏之内
    private int dataFlag; //0-关闭采集 1-开启采集

    private float outShow, inShow;

    private List<LatLng> rangePosList = new ArrayList<>();
    private Chronometer chronometer2;

    private List<Float> inDatas = new ArrayList<>();
    private List<Float> outDatas = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        InsideBtn = (Button) findViewById(R.id.inside);
        OutsideBtn = (Button) findViewById(R.id.outside);
        btnDraw = (Button) findViewById(R.id.button);
        btnStart = (Button) findViewById(R.id.button2);
        currentpa = (TextView) findViewById(R.id.currentpa);
        insidepa = (TextView) findViewById(R.id.insidepa);
        outsidepa = (TextView) findViewById(R.id.outsidepa);
        differentpa = (TextView) findViewById(R.id.different);

        initMap();
        initView();

        /*get sensor services（SENSOR_SERVICE）return SensorManager */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        /*using SensorManager to get （pressure sensor*/
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mSensor == null) {
            Toast.makeText(getApplication(), "No pressure sensor", Toast.LENGTH_LONG).show();
        } else {
            mSensorManager.registerListener(mSensorEventListener, mSensor
                    , SensorManager.SENSOR_DELAY_UI);
        }
        InsideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float tempall = 0;
                float temp;
                for (int i = 0; i < 5; i++) {
                    temp = current;
                    tempall += temp;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                in = tempall / 5;
                insidepa.setText("Inside: " + String.valueOf(in) + " hPa");
                differentpa.setText("Different: " + (in - out) + " hPa");
            }
        });

        OutsideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float tempall = 0;
                float temp = 0;
                for (int i = 0; i < 5; i++) {
                    temp = current;
                    tempall += temp;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                out = tempall / 5;
                outsidepa.setText("Outside: " + String.valueOf(out) + " hPa");
                differentpa.setText("Different: " + (in - out) + " hPa");
            }
        });

        btnDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawFlag == 0) {
                    drawFlag = 1;
                    btnDraw.setText("Save aire");
                    btnStart.setVisibility(View.GONE);
                    mBaiduMap.clear();
                    rangePosList.clear();
                    Toast.makeText(MainActivity.this, "Set 4 point for drawing the area", Toast.LENGTH_LONG).show();
                } else if (drawFlag == 1) {
                    drawFlag = 0;
                    btnDraw.setText("Draw the Area");
                    btnStart.setVisibility(View.VISIBLE);
                    if (rangePosList.size() == 4) {
                        String res = "";
                        for (int i = 0; i < rangePosList.size(); i++) {
                            res += rangePosList.get(i).latitude + ":" + rangePosList.get(i).longitude + ":";
                        }
                        res = res.substring(0, res.length() - 1);
                        setDataString(res);
                    } else {
                        Toast.makeText(MainActivity.this, "围栏点数量不对，无法保存", Toast.LENGTH_LONG).show();
                        mBaiduMap.clear();
                        DrawInit();
                    }
                }
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rangePosList.size() == 4 && myPos!=null)
                {
                    if (dataFlag == 0)
                    {
                        dataFlag = 1;
                        btnStart.setText("Stop");
                        btnDraw.setVisibility(View.GONE);
                        initDatas();
                    }
                    else if (dataFlag == 1)
                    {
                        dataFlag = 0;
                        btnStart.setText("Start");
                        btnDraw.setVisibility(View.VISIBLE);

                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Area error", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /*saying using a SensorEventListener to use Sensor，and reload onSensorChanged class*/
    private final SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
//            System.out.println("type："+event.sensor.getType());
//            if(event.sensor.getType()==Sensor.TYPE_PRESSURE){
//                /*return the value of pressure sensor，unit is hectopascal（hPa）*/
//                float pressure=event.values[0];
//                showpa.setText(String.valueOf(pressure)+" hPa");
//            }

            System.out.println("type：" + event.sensor.getType());
            if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                /*return，unit is hectopascal（hPa）*/
                ;
                current = event.values[0];
                currentpa.setText("Current: " + String.valueOf(current) + " hPa");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    };

    //map
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mMapView.onDestroy();
        mSensorManager.unregisterListener(mSensorEventListener, mSensor);
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mMapView.onResume();
    }


    private void initMap() {
//        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        mBaiduMap.setMyLocationEnabled(true);
        mMapView.showScaleControl(false);
        mMapView.showZoomControls(false);
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); //
        option.setCoorType("bd09ll"); //
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (drawFlag == 1 && rangePosList.size() < 4) {
                    rangePosList.add(latLng);
                    DrawRange();
                }
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });

        DrawInit();
    }

    public void DrawInit() {
        String res = getDataString();
        if (!TextUtils.isEmpty(res)) {
            rangePosList.clear();
            String[] datas = res.split(":");
            for (int i = 0; i < datas.length; i += 2) {
                rangePosList.add(new LatLng(Double.valueOf(datas[i]), Double.valueOf(datas[i + 1])));
            }
            DrawRange();
        }
    }

    private void initDatas()
    {
        inShow = 0;
        outShow = 0;
        inDatas.clear();
        outDatas.clear();
    }
    private void initView() {
        chronometer2 = (Chronometer) findViewById(R.id.chronometer2);
        chronometer2.start();
        chronometer2.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                current = (float) (4*Math.random()+6);

                if (dataFlag == 1)
                {
                    isIn();
                    if (locFlag == 0)
                    {
                        outDatas.add(current);
                        if (outDatas.size()>5)
                        {
                            outDatas.remove(0);
                        }

                        float out = 0;
                        for (int i=0;i<outDatas.size();i++)
                        {
                            out+=outDatas.get(i);
                        }
                        outShow = out/outDatas.size();
                        outsidepa.setText("Outside: " + String.valueOf(outShow) + " hPa");
                        differentpa.setText("Different: " + (inShow - outShow) + " hPa");
                    }
                    else if (locFlag == 1)
                    {
                        inDatas.add(current);
                        if (inDatas.size()>5)
                        {
                            inDatas.remove(0);
                        }

                        float in = 0;
                        for (int i=0;i<inDatas.size();i++)
                        {
                            in+=inDatas.get(i);
                        }
                        inShow = in/inDatas.size();
                        insidepa.setText("Inside: " + String.valueOf(inShow) + " hPa");
                        differentpa.setText("Different: " + (inShow - outShow) + " hPa");
                    }
                }
            }
        });
    }

    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            if (location == null || mMapView == null) {
                return;
            }
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())

                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            myPos = new LatLng(location.getLatitude(), location.getLongitude());

            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    public String getDataString() {
        String res = "";
        SharedPreferences pre = getSharedPreferences("setting", Context.MODE_PRIVATE);
        res = pre.getString("pos", null);
        return res;
    }

    public void setDataString(String con) {
        SharedPreferences.Editor edi =
                getSharedPreferences("setting", Context.MODE_PRIVATE).edit();
        edi.putString("pos", con);
        edi.commit();
    }

    public void DrawRange() {
        if (rangePosList.size() <= 4 && rangePosList.size() >= 3) {
            mBaiduMap.clear();
            OverlayOptions ooPolygon = new PolygonOptions().points(rangePosList)
                    .stroke(new Stroke(5, 0xAA00FF00)).fillColor(0xAAFFFF00);
            mBaiduMap.addOverlay(ooPolygon);
        } else if (rangePosList.size() <= 2 && rangePosList.size() >= 1) {
            mBaiduMap.clear();
            for (int i = 0; i < rangePosList.size(); i++) {
                OverlayOptions ooDot = new DotOptions().center(rangePosList.get(i)).radius(6)
                        .color(0xAA00FF00);
                mBaiduMap.addOverlay(ooDot);
            }

        }
    }

    public void isIn() {
        if (rangePosList.size() == 4) {
            if (SpatialRelationUtil.isPolygonContainsPoint(rangePosList, myPos)) {
                locFlag = 1;
            } else {
                locFlag = 0;
            }
        }

    }
}
