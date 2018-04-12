package com.homework.google;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.utils.SpatialRelationUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivityGoogle extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    LatLng latLng;
    GoogleMap mGoogleMap;
    SupportMapFragment mMapFragment;
    Marker mCurrLocation;
    private static final int REQUESTCODE = 6001;

    /**
     * not map begin
     **/
    private Button InsideBtn, OutsideBtn;
    private Button btnDraw, btnStart;

    private TextView currentpa, insidepa, outsidepa, differentpa;

    private float current = 0;
    private float in = 0;
    private float out = 0;

    private SensorManager mSensorManager = null;
    private Sensor mSensor = null;
    private Chronometer chronometer2;
    private int drawFlag; //
    private int locFlag;  //
    private int dataFlag; //
    private List<LatLng> rangePosList = new ArrayList<>();
    private LatLng myPos;//

    private float outShow, inShow;
    private List<Float> inDatas = new ArrayList<>();
    private List<Float> outDatas = new ArrayList<>();

    /**
     * not map end
     **/


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initSensor();
        initMap();
        initEvent();
        initTopButtonEvent();
        initChronometerEvent();

    }

    private void initTopButtonEvent() {
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

    }

    private void initChronometerEvent() {
        chronometer2 = (Chronometer) findViewById(R.id.chronometer2);
        chronometer2.start();
        chronometer2.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                current = (float) (4 * Math.random() + 6);
                if (dataFlag == 1) {
                    isIn();
                    if (locFlag == 0) {
                        outDatas.add(current);
                        if (outDatas.size() > 5) {
                            outDatas.remove(0);
                        }

                        float out = 0;
                        for (int i = 0; i < outDatas.size(); i++) {
                            out += outDatas.get(i);
                        }
                        outShow = out / outDatas.size();
                        outsidepa.setText("Outside: " + String.valueOf(outShow) + " hPa");
                        differentpa.setText("Different: " + (inShow - outShow) + " hPa");
                    } else if (locFlag == 1) {
                        inDatas.add(current);
                        if (inDatas.size() > 5) {
                            inDatas.remove(0);
                        }

                        float in = 0;
                        for (int i = 0; i < inDatas.size(); i++) {
                            in += inDatas.get(i);
                        }
                        inShow = in / inDatas.size();
                        insidepa.setText("Inside: " + String.valueOf(inShow) + " hPa");
                        differentpa.setText("Different: " + (inShow - outShow) + " hPa");
                    }
                }
            }
        });
    }

    public void isIn() {
        if (rangePosList.size() == 4) {
            //坐标进行转换 begin
            List<com.baidu.mapapi.model.LatLng> baiduRangePosList = new ArrayList<>();
            for (LatLng latLngFromGoogleItem : rangePosList) {
                baiduRangePosList.add(new com.baidu.mapapi.model.LatLng(latLngFromGoogleItem.latitude, latLngFromGoogleItem.longitude));
            }
            com.baidu.mapapi.model.LatLng baiduMyPos = new com.baidu.mapapi.model.LatLng(myPos.latitude, myPos.longitude);
            //坐标进行转换 end

//            if (SpatialRelationUtil.isPolygonContainsPoint(rangePosList, myPos)) {
            if (SpatialRelationUtil.isPolygonContainsPoint(baiduRangePosList, baiduMyPos)) {
                locFlag = 1;
            } else {
                locFlag = 0;
            }
        }

    }

    /**
     * init map event,add map some event listener
     */
    private void initMapEvent() {
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (drawFlag == 1 && rangePosList.size() < 4) {
                    rangePosList.add(latLng);
                    DrawRange();
                }
            }
        });
    }

    //init event
    private void initEvent() {
        /**1.drawButton and saveDrawBubbon clickEvent begin**/
        btnDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawFlag == 0) {
                    drawFlag = 1;
                    btnDraw.setText("Save area");
                    btnStart.setVisibility(View.GONE);
                    mGoogleMap.clear();
                    rangePosList.clear();
                    Toast.makeText(MainActivityGoogle.this, "Set 4 point for drawing the area", Toast.LENGTH_LONG).show();
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
//                        Toast.makeText(MainActivityGoogle.this, "Can't save", Toast.LENGTH_LONG).show();
                        Toast.makeText(MainActivityGoogle.this, "save fail,geoFencing count is not correct", Toast.LENGTH_LONG).show();
                        mGoogleMap.clear();
                        DrawInit();
                    }
                }
            }
        });
        /**drawButton and saveDrawBubbon clickEvent end**/
        /**2.startButton clickEvent begin**/
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rangePosList.size() == 4 && myPos != null) {
                    if (dataFlag == 0) {
                        dataFlag = 1;
                        btnStart.setText("Stop");
                        btnDraw.setVisibility(View.GONE);
                        initDatas();
                    } else if (dataFlag == 1) {
                        dataFlag = 0;
                        btnStart.setText("Start");
                        btnDraw.setVisibility(View.VISIBLE);

                    }
                } else {
                    Toast.makeText(MainActivityGoogle.this, "Area error", Toast.LENGTH_LONG).show();
                }
            }
        });
        /**2.startButton clickEvent end**/
    }

    private void initDatas() {
        inShow = 0;
        outShow = 0;
        inDatas.clear();
        outDatas.clear();
    }

    public void setDataString(String con) {
        SharedPreferences.Editor edi =
                getSharedPreferences("setting", Context.MODE_PRIVATE).edit();
        edi.putString("pos", con);
        edi.commit();
    }

    public String getDataString() {
        String res = "";
        SharedPreferences pre = getSharedPreferences("setting", Context.MODE_PRIVATE);
        res = pre.getString("pos", null);
        return res;
    }

    /**
     * make draw init
     */
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

    public void DrawRange() {
        if (rangePosList.size() <= 4 && rangePosList.size() >= 3) {
            mGoogleMap.clear();
            //BaiduMap
//            OverlayOptions ooPolygon = new PolygonOptions().points(rangePosList)
//                    .stroke(new Stroke(5, 0xAA00FF00)).fillColor(0xAAFFFF00);
//            mGoogleMap.addOverlay(ooPolygon);
            //googleMap
            PolygonOptions rectOptions = new PolygonOptions().addAll(rangePosList);
            Polygon polygon = mGoogleMap.addPolygon(rectOptions.strokeColor(Color.GREEN).fillColor(Color.BLUE));
        } else if (rangePosList.size() <= 2 && rangePosList.size() >= 1) {
            mGoogleMap.clear();
            for (int i = 0; i < rangePosList.size(); i++) {
//                OverlayOptions ooDot = new DotOptions().center(rangePosList.get(i)).radius(6)
//                        .color(0xAA00FF00);
//                mBaiduMap.addOverlay(ooDot);
                PolylineOptions rectOptionsOptions = new PolylineOptions().addAll(rangePosList);
                Polyline polyline = mGoogleMap.addPolyline(rectOptionsOptions.color(Color.GREEN));
            }
        }
    }

    /**
     * init sensor
     */
    private void initSensor() {
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
    }


    private void initView() {
        InsideBtn = (Button) findViewById(R.id.inside);
        OutsideBtn = (Button) findViewById(R.id.outside);
        btnDraw = (Button) findViewById(R.id.button);
        btnStart = (Button) findViewById(R.id.button2);
        currentpa = (TextView) findViewById(R.id.currentpa);
        insidepa = (TextView) findViewById(R.id.insidepa);
        outsidepa = (TextView) findViewById(R.id.outsidepa);
        differentpa = (TextView) findViewById(R.id.different);
    }

    /**
     * 初始化map
     */
    private void initMap() {
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mMapView);
        mMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        //地图加上点击事件
        initMapEvent();
        // 允许获取我的位置
        try {
            mGoogleMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Unregister for location callbacks:
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                }
            });
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Toast.makeText(this, "buildGoogleApiClient", Toast.LENGTH_SHORT).show();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        Location mLastLocation = null;
        try {
            Log.i("location", LocationServices.FusedLocationApi.getLocationAvailability(mGoogleApiClient) + "");
            Toast.makeText(this, "location:" + LocationServices.FusedLocationApi.getLocationAvailability(mGoogleApiClient), Toast.LENGTH_SHORT).show();
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            //TODO 这里可能需要修改下
            myPos = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        } catch (SecurityException e) {
            e.printStackTrace();
        }


        if (mLastLocation != null) {
            //place marker at current position
            mGoogleMap.clear();
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));
            // 添加marker，但是这里我们特意把marker弄成透明的
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.chat_loc_point));

//            mCurrLocation = mGoogleMap.addMarker(markerOptions);

            Log.i("location", mLastLocation + "1111111");
            Log.i("location", "newlocation getProvider " + mLastLocation.getProvider());
            Log.i("location", "newlocation getAccuracy " + mLastLocation.getAccuracy());
            Log.i("location", "newlocation getAltitude " + mLastLocation.getAltitude());
            Log.i("location", "newlocation Bearing() " + mLastLocation.getBearing());
            Log.i("location", "newlocation Extras() " + mLastLocation.getExtras());
            Log.i("location", "newlocation Latitude() " + mLastLocation.getLatitude());
            Log.i("location", "newlocation Longitude()() " + mLastLocation.getLongitude());
            Log.i("location", " =============  ");
//            TextView mTvAddress = (TextView) findViewById(R.id.mTvAddress);
            String address = getAddress(MainActivityGoogle.this, mLastLocation.getLatitude(), mLastLocation.getLongitude());
//            mTvAddress.setText(address);
            Toast.makeText(this, "address:" + address, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "address is null", Toast.LENGTH_SHORT).show();
        }


        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(5000); //5 seconds
        mLocationRequest.setFastestInterval(3000); //3 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

        //LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_SHORT).show();
    }


    /**
     * 逆地理编码 得到地址
     *
     * @param context
     * @param latitude
     * @param longitude
     * @return
     */
    public static String getAddress(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<android.location.Address> address = geocoder.getFromLocation(latitude, longitude, 1);
            Log.i("location", "getlocation" + address + "'\n"
                    + "经度：" + String.valueOf(address.get(0).getLongitude()) + "\n"
                    + "纬度：" + String.valueOf(address.get(0).getLatitude()) + "\n"
                    + "纬度：" + "国家：" + address.get(0).getCountryName() + "\n"
                    + "城市：" + address.get(0).getLocality() + "\n"
                    + "名称：" + address.get(0).getAddressLine(1) + "\n"
                    + "街道：" + address.get(0).getAddressLine(0)
            );
            return address.get(0).getAddressLine(0) + "  " + address.get(0).getLocality() + " " + address.get(0).getCountryName();
        } catch (Exception e) {
            e.printStackTrace();
            return "未知";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUESTCODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    buildGoogleApiClient();
                    mGoogleApiClient.connect();
                } else {
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(mSensorEventListener, mSensor);
    }

    /*saying using a SensorEventListener to use Sensor，and reload onSensorChanged class*/
    private final SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                current = event.values[0];
                currentpa.setText("Current: " + String.valueOf(current) + " hPa");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
}
