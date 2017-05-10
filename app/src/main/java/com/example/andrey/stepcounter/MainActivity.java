package com.example.andrey.stepcounter;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private GoogleApiClient mClient = null;
    public static final String TAG = "BasicSensorsApi";
    ArrayList<Item> deviceSensors;
    RecyclerView sensorsView;
    LinearLayoutManager linearLayoutManager;
    TextView steps, speed, distance, totalSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorsView = (RecyclerView) findViewById(R.id.sensorsView);
        linearLayoutManager = new LinearLayoutManager(this);
        sensorsView.setLayoutManager(linearLayoutManager);
        steps = (TextView) findViewById(R.id.stepsCountTextView);
        speed = (TextView) findViewById(R.id.speedTextView);
        distance = (TextView) findViewById(R.id.distanceCountTextView);
        totalSteps = (TextView) findViewById(R.id.totalSteps);

        buildGoogleFitClient();

    }

    private void buildGoogleFitClient() {
        mClient = new GoogleApiClient.Builder(this).addApi(Fitness.SENSORS_API)
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.i(TAG, "Connected!!!");
                        subscribe();
                        getDeviceSensors();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                            Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                        } else if (i
                                == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                            Log.i(TAG,
                                    "Connection lost.  Reason: Service Disconnected");
                        }
                    }
                }).enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(TAG, "Google Play services connection failed. Cause: " +
                                result.toString());

                    }
                }).build();
    }

    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        // [START subscribe_to_datatype]
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }

                            new VerifyDataTask().execute();
                        } else {
                            Log.i(TAG, "There was a problem subscribing.");
                        }
                    }
                });
        // [END subscribe_to_datatype]
    }

    private class VerifyDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            long total = 0;

            PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA);
            DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
            if (totalResult.getStatus().isSuccess()) {
                DataSet totalSet = totalResult.getTotal();
                total = totalSet.isEmpty()
                        ? 0
                        : totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                final long finalTotal = total;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        totalSteps.setText(String.valueOf(finalTotal));
                    }
                });

            } else {
                Log.w(TAG, "There was a problem getting the step count.");
            }

            Log.i(TAG, "Total steps: " + total);

            return null;
        }
    }

    public void getDeviceSensors() {
        Fitness.SensorsApi.findDataSources(mClient, new DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA/*,
                        DataType.AGGREGATE_DISTANCE_DELTA,
                        DataType.TYPE_SPEED*/)
                .setDataSourceTypes(DataSource.TYPE_RAW, DataSource.TYPE_DERIVED).build())
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        deviceSensors = new ArrayList<>();
                        for (final DataSource dataSource : dataSourcesResult.getDataSources()) {
                            Log.i(TAG, "Data source found: " + dataSource.toString());
                            Log.i(TAG, "Data Source type: " + dataSource.getDataType().getName());
                            Device device = dataSource.getDevice();
                            deviceSensors.add(new Item(device.getManufacturer() + " " + device.getModel(), dataSource.toString()
                                    .split("2" + Pattern.quote("}:"))[1].split(":")[0],
                                    dataSource.getDataType().getName()));

                            if (dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)/* ||
                                    dataSource.getDataType().equals(DataType.TYPE_DISTANCE_DELTA) ||
                                    dataSource.getDataType().equals(DataType.TYPE_SPEED)*/) {
                                Log.i(TAG, "Data source for TYPE_STEP_COUNT_DELTA found!  Registering.");
                                Fitness.SensorsApi.add(mClient,
                                        new SensorRequest.Builder()
                                                .setDataSource(dataSource) // Optional but recommended for custom data sets.
                                                .setDataType(dataSource.getDataType()) // Can't be omitted.
                                                .setSamplingRate(1, TimeUnit.SECONDS)
                                                .build(),
                                        new OnDataPointListener() {
                                            @Override
                                            public void onDataPoint(DataPoint dataPoint) {
                                                for (final Field field : dataPoint.getDataType().getFields()) {
                                                    final Value val = dataPoint.getValue(field);
                                                    Log.i(TAG, field.getName() + ":" + dataPoint.getValue(field));

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (field.toString().equals("steps(i)")) {
                                                                final int sc = Integer.parseInt(steps.getText().toString());
                                                                steps.setText(String.valueOf(sc + val.asInt()));
                                                                totalSteps.setText(String.valueOf(
                                                                        Integer.parseInt(
                                                                                totalSteps.getText().toString())
                                                                                + val.asInt()));
                                                            } /*else if (field.toString().equals("distance(f)")) {
                                                                final int sc = Integer.parseInt(distance.getText().toString());
                                                                distance.setText(String.valueOf(sc + val.asInt()));
                                                            } else if (field.toString().equals("speed(f)")) {
                                                                final int sc = Integer.parseInt(speed.getText().toString());
                                                                speed.setText(String.valueOf(sc + val.asInt()));
                                                            }*/
                                                        }
                                                    });
                                                }
                                            }
                                        })
                                        .setResultCallback(new ResultCallback<Status>() {
                                            @Override
                                            public void onResult(Status status) {
                                                if (status.isSuccess()) {
                                                    Log.i(TAG, "Listener for " + dataSource.getDataType().getName() + " registered!");
                                                } else {
                                                    Log.i(TAG, "Listener not registered.");
                                                }
                                            }
                                        });

                            }

                            RVAdapter adapter = new RVAdapter(deviceSensors);
                            sensorsView.setAdapter(adapter);
                        }
                    }
                });

    }
}
