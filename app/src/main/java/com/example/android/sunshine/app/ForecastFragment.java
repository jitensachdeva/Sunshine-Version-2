package com.example.android.sunshine.app;

/**
 * Created by jitendrasachdeva on 24/11/15.
 */

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.text.format.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Forecast fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragement, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh){
            FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
            fetchWeatherTask.execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] data = {};
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
        fetchWeatherTask.execute("94043");

        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));
        mForecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);

        ListView forecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastList.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{


        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            String[] weatherData = null;

            final String MODE_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";
            final String QUERY_PARAM = "q";


            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null ;
            String mode = "json";
            String units = "metric";
            int numberOfDays = 7;

            if (params.length == 0){
                return null;
            }

            try {
                Log.v(LOG_TAG, "cityid-" + params[0]);
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(MODE_PARAM, mode)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numberOfDays))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();


                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if(inputStream == null){
                    return null ;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while( (line=reader.readLine()) != null){
                    buffer.append(line + "\n");
                }

                if (buffer.length() ==0){
                    return  null;
                }

                forecastJsonStr = buffer.toString();

            }catch(IOException e){
                Log.e(LOG_TAG, "Error -" + e.getMessage(), e);
            }finally {
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
                if (reader != null){
                    try {
                        reader.close();
                    }catch (final IOException e){
                        Log.e(LOG_TAG, "error closing stream", e);
                    }
                }
            }
            try {
                weatherData = getWeatherDataFromJson(forecastJsonStr, numberOfDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG,"JSON Exception -"+ e.getMessage(), e);
                e.printStackTrace();
            }

            return weatherData;
        }

        @Override
        protected void onPostExecute(String[] results) {
            if (results !=null){
                mForecastAdapter.clear();
                for(String dayForecastStr: results){
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }

        @NonNull
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numberOfDays) throws JSONException {

            final String OWM_LIST = "list";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();

            String[] resultStr = new String[numberOfDays];

            for(int i=0; i< weatherArray.length(); i++){
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String  highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);
                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                JSONArray weatherObject = dayForecast.getJSONArray("weather");
                JSONObject object = (JSONObject) weatherObject.get(0);
                description = object.getString("description");

                JSONObject temperature = dayForecast.getJSONObject("temp");
                Double low = temperature.getDouble("min");
                Double high = temperature.getDouble("max");

                highAndLow = formatHighLows(high,low);
                resultStr[i] = day + ", " + description + ", "+ highAndLow ;
            }
            return  resultStr ;
        }

        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

    }
}