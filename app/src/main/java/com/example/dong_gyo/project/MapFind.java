package com.example.dong_gyo.project;


import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MapFind extends ActionBarActivity {

    GoogleMap mMap; // Might be null if Google Play services APK is not available.
    LocationManager lm;
    String locationProvider;
    Location location;
    Button mapListbut;
    Button mapSearchbut;
    ListView mapList;
    HorizontalScrollView hsv;
    LinearLayout scrl;
    ArrayList arr;
    ArrayAdapter mapadapter;
    android.support.v7.app.ActionBar actionBar;

    double center_latitude = 0;
    double center_longitude = 0;

    final double LATDISTANCE = 0.0045050118256;
    final double LONGDISTANCE = 0.005659725956;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_find);

        startActivity(new Intent(this, LoadingApp.class));

        actionBar = getSupportActionBar();
        actionBar.setTitle("지도로 검색하기");
        actionBar.show();

        setUpMapIfNeeded();

        hsv = (HorizontalScrollView)findViewById(R.id.mapHori);
        scrl = (LinearLayout)findViewById(R.id.sclayout);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        ArrayList<Button> but = new ArrayList<Button>();
        arr = new ArrayList();
        for(int i=0; i<10; i++) {
            but.add(new Button(this));
            but.get(i).setText("버튼 " + (i+1));
            but.get(i).setLayoutParams(params);
            scrl.addView(but.get(i));
        }

        mapListbut = (Button)findViewById(R.id.showMapList);
        mapSearchbut = (Button)findViewById(R.id.findShop);

        mapList = (ListView)findViewById(R.id.mapList);
        mapList.setVisibility(View.INVISIBLE);

        for(int i=0; i<10; i++) {
            arr.add("레스토랑 " + (i+1));
        }

        mapadapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arr);

        mapList.setAdapter(mapadapter);

        mapList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mapList.getItemAtPosition(position).toString().equals("레스토랑 1")) {
                    Intent it = new Intent(MapFind.this, ResMain2.class);
                    startActivity(it);
                } else {
                    Bundle resInfo = new Bundle();
                    Intent it = new Intent(MapFind.this, ResMain.class);
                    startActivity(it);
                }
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mapList.isShown()) {
                    mapList.setVisibility(View.INVISIBLE);
                    mapListbut.setSelected(false);
                }
            }
        });

        mapListbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mapList.isShown()) {
                    mapList.setVisibility(View.VISIBLE);
                    mapListbut.setSelected(true);
                } else if (mapList.isShown()) {
                    mapList.setVisibility(View.INVISIBLE);
                    mapListbut.setSelected(false);
                }
            }
        });

        mapSearchbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(center_latitude != 0) {
                    LatLng now = new LatLng(center_latitude, center_longitude);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(now, 16));
                } else {

                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap(LatLng LOC)} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

            locationProvider = lm.getBestProvider(new Criteria(), true);

            location = lm.getLastKnownLocation(locationProvider);

            double latitude;
            double longitude;

            if (location == null) {


                AlertDialog.Builder agreeLoc = new AlertDialog.Builder(MapFind.this);
                agreeLoc.setMessage("위치정보 제공에 동의하고 있지 않습니다.\n" +
                        "위치정보제공에 동의하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                                return;
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                return;
                            }
                        });
                AlertDialog alert = agreeLoc.create();
                alert.show();



                latitude = 37.5665;
                longitude = 126.978;

            } else {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            mMap.setMyLocationEnabled(true);
            UiSettings uiSettings = mMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(true);

            final LatLng LOC = new LatLng(latitude, longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LOC, 16));

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

                    /*
                     * double nw_latitude; //븍서쪽 좌표 double nw_longitude; double
                     * se_latitude; //남동쪽 좌표 double se_longitude;
                     */
                    Double[] location = new Double[4];

                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {

                        LatLng location_center = cameraPosition.target;
                        center_latitude = cameraPosition.target.latitude;
                        center_longitude = cameraPosition.target.longitude;

                        location[0] = center_latitude + LATDISTANCE;
                        location[1] = center_longitude - LONGDISTANCE;
                        location[2] = center_latitude - LATDISTANCE;
                        location[3] = center_longitude + LONGDISTANCE;

                    }
                });
            }

            LocationListener locationListener = new LocationListener() {

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProviderEnabled(String provider) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProviderDisabled(String provider) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onLocationChanged(Location location) {
                    // TODO Auto-generated method stub

                    updateMap(location);

                }
            };
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap(LatLng LOC) {
        mMap.addMarker(new MarkerOptions().position(LOC));
    }

    public void updateMap(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        final LatLng LOC = new LatLng(latitude, longitude);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LOC, 16));
        /*
        Marker mk = mMap.addMarker(new MarkerOptions()
                .position(LOC)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("나의 위치 ").snippet("김영준"));

        mk.showInfoWindow();
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);

        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
        if(searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String arg0) {
                    return false;
                }


                @Override
                public boolean onQueryTextChange(String arg0) {
                    return false;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    class RestaurantList extends AsyncTask<Void, Void, Void> {

        Context mContext;
        JSONObject _jobj;
        String result = "";
        JSONObject receivedJSON;
        InputStream inputStream;
        Bitmap bmp;

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub

            Task();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            JSONObject resultJSON;
            super.onPostExecute(result);
        }

        public void Task() {

            HttpClient httpClient = new DefaultHttpClient();

            try {
                URI _url = null;

                _url = new URI("http://183.96.25.221:1338");

                HttpPost httpPost = new HttpPost(_url);
                String json = "";
                json = _jobj.toString();
                StringEntity se = new StringEntity(json);
                httpPost.setEntity(se);
                httpPost.setHeader("Content-Type", "application/json");
                HttpResponse response = httpClient.execute(httpPost);


                BufferedReader bufReader =  new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent(),"utf-8" )
                );
                String line = null;

                while ((line = bufReader.readLine())!=null){
                    result +=line;
                }

            }
            catch(URISyntaxException e) {
                System.out.println("1");
                e.printStackTrace();
            }
            catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                System.out.println("2");
                e.printStackTrace();
            }

            catch (IOException e) {
                System.out.println("3");
                e.printStackTrace();
            }
            return;
        }
    }
}
