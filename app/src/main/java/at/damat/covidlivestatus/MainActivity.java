package at.damat.covidlivestatus;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static at.damat.covidlivestatus.RequestHandler.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private ArrayList<StructuraCountry> structuraCountries = new ArrayList<>();
    private ArrayList<StructuraSummary> structuraSummaries = new ArrayList<>();
    private ArrayList<String> countries = new ArrayList<>();

    protected void checkPermissions() throws PackageManager.NameNotFoundException {
        final List<String> missingPermissions = new ArrayList<String>();
        PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
        String[] requiredPermissions = info.requestedPermissions;
        for (final String permission : requiredPermissions) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            final String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[requiredPermissions.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, requiredPermissions, grantResults);
        }
    }

    private void editBad(EditText editText) {
        editText.setTypeface(editText.getTypeface(), Typeface.BOLD);
        editText.setTextColor(getColor(android.R.color.holo_red_light));
    }

    private void editGood(EditText editText) {
        editText.setTypeface(editText.getTypeface(), Typeface.BOLD);
        editText.setTextColor(getColor(android.R.color.holo_green_dark));
    }

    private void editNeutral(EditText editText) {
        editText.setTypeface(editText.getTypeface(), Typeface.NORMAL);
        editText.setTextColor(android.R.attr.editTextColor);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            checkPermissions();
        } catch (PackageManager.NameNotFoundException ex) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Exception :: " + ex.getMessage(), ex);
        }
        final ProgressBar progressBar = findViewById(R.id.progressbar);
        try {
            JSONArray allCountriesJSON = new JSONArray();
            allCountriesJSON = new RequestAsync(getString(R.string.API_URL) + getString(R.string.API_Countries), "GET", null, progressBar).execute().get();
            for (int i = 0; i < allCountriesJSON.length(); i++) {
                structuraCountries.add(new StructuraCountry(allCountriesJSON.getJSONObject(i).getString("Country"), allCountriesJSON.getJSONObject(i).getString("Slug"), allCountriesJSON.getJSONObject(i).getString("ISO2")));
                countries.add(allCountriesJSON.getJSONObject(i).getString("Country"));
            }
            ArrayAdapter<String> countriesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, countries);
            final AutoCompleteTextView countrySelect = findViewById(R.id.ac_country);
            countrySelect.setAdapter(countriesAdapter);
            countrySelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    countrySelect.showDropDown();
                }
            });
            countrySelect.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                    if (BuildConfig.DEBUG) Log.d(TAG, "Item # selected :: " + i);
//                    String selectedCountry = countries.get(i);
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
                    String selectedCountry = (String)adapterView.getItemAtPosition(i);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Country selected :: " + selectedCountry);
                    StructuraCountry.Pair<Boolean, Integer> countryPosition = StructuraCountry.GetPositionByCountry(structuraCountries, selectedCountry);
                    if (countryPosition.getFound()) {
                        int position = 0;
                        JSONArray countrySummaryJSON = new JSONArray();
                        try {
                            countrySummaryJSON = new RequestAsync(getString(R.string.API_URL) + getString(R.string.API_Summary), "GET_SUM", null, progressBar).execute().get();
                            structuraSummaries.clear();
                            Calendar cal = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
                            String lastCountry = new String();
                            for (int j = 0; j < countrySummaryJSON.length(); j++) {
                                cal.setTime(sdf.parse(countrySummaryJSON.getJSONObject(j).getString("Date").replace("Z", "+0000")));
                                lastCountry = countrySummaryJSON.getJSONObject(j).getString("Country");
                                structuraSummaries.add(new StructuraSummary(lastCountry, countrySummaryJSON.getJSONObject(j).getString("CountryCode"), countrySummaryJSON.getJSONObject(j).getString("Slug"), countrySummaryJSON.getJSONObject(j).getInt("NewConfirmed"), countrySummaryJSON.getJSONObject(j).getInt("TotalConfirmed"), countrySummaryJSON.getJSONObject(j).getInt("NewDeaths"), countrySummaryJSON.getJSONObject(j).getInt("TotalDeaths"), countrySummaryJSON.getJSONObject(j).getInt("NewRecovered"), countrySummaryJSON.getJSONObject(j).getInt("TotalRecovered"), cal));
                                if (lastCountry.equals(selectedCountry)) {
                                    position = j;
                                    break;
                                }
                            }
                            if (position != 0) {
                                EditText totalConfirmed = findViewById(R.id.edit_totalconf);
                                totalConfirmed.setText(String.valueOf(structuraSummaries.get(position).totalConfirmed));
                                EditText newConfirmed = findViewById(R.id.edit_newconf);
                                newConfirmed.setText(String.valueOf(structuraSummaries.get(position).newConfirmed));
                                if (structuraSummaries.get(position).newConfirmed > 0) {
                                    newConfirmed.setTypeface(newConfirmed.getTypeface(), Typeface.BOLD);
                                    newConfirmed.setTextColor(getColor(android.R.color.holo_red_light));
                                } else {
                                    new MyVibrator(MainActivity.this);
                                    Snackbar.make(findViewById(android.R.id.content), "Country not found", Snackbar.LENGTH_LONG).setAction("No action", null).show();
                                }
                                EditText totalDeaths = findViewById(R.id.edit_totaldeath);
                                totalDeaths.setText(String.valueOf(structuraSummaries.get(position).totalDeaths));
                                EditText newDeaths = findViewById(R.id.edit_newdeath);
                                newDeaths.setText(String.valueOf(structuraSummaries.get(position).newDeaths));
                                EditText totalRecovered = findViewById(R.id.edit_totalrec);
                                totalRecovered.setText(String.valueOf(structuraSummaries.get(position).totalRecovered));
                                EditText newRecovered = findViewById(R.id.edit_newrec);
                                newRecovered.setText(String.valueOf(structuraSummaries.get(position).newRecovered));
                                TextView lastUpdate = findViewById(R.id.tv_lasupdate);
                                lastUpdate.setText(structuraSummaries.get(position).lastUpdate.getTime().toString());
                                for (EditText edittext : new EditText[]{newConfirmed, newDeaths}) {
                                    if (Integer.parseInt(edittext.getText().toString()) > 0) {
                                        editBad(edittext);
                                    } else {
                                        editNeutral(edittext);
                                    }
                                }
                                if (Integer.parseInt(newRecovered.getText().toString()) > 0) {
                                    editGood(newRecovered);
                                } else {
                                    editNeutral(newRecovered);
                                }
                            }
                        } catch (ExecutionException | InterruptedException | JSONException | ParseException ex) {
                            if (BuildConfig.DEBUG) Log.e(TAG, "Exception :: " + ex.getMessage(), ex);
                        }
                    }
                }
            });
        } catch (JSONException | InterruptedException | ExecutionException ex) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Exception :: " + ex.getMessage(), ex);
        }
    }
}
