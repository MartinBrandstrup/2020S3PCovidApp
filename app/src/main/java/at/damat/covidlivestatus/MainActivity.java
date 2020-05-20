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

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static at.damat.covidlivestatus.RequestHandler.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private final static int GOOD_STAT = 0;
    private final static int BAD_STAT = 1;
    private final static int NEUTRAL_STAT = 2;
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

    private void editStat(EditText editText, int statType) {
        switch (statType) {
            case GOOD_STAT:
                editText.setTypeface(editText.getTypeface(), Typeface.BOLD);
                editText.setTextColor(getColor(android.R.color.holo_green_dark));
                break;
            case BAD_STAT:
                editText.setTypeface(editText.getTypeface(), Typeface.BOLD);
                editText.setTextColor(getColor(android.R.color.holo_red_light));
                break;
            default:
                editText.setTypeface(editText.getTypeface(), Typeface.NORMAL);
                editText.setTextColor(getColor(android.R.color.darker_gray));
        }
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
                structuraCountries.add(new StructuraCountry(allCountriesJSON.getJSONObject(i).getString("countryName"), allCountriesJSON.getJSONObject(i).getString("countryCode")));
                countries.add(allCountriesJSON.getJSONObject(i).getString("countryName"));
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
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
                    String selectedCountry = (String)adapterView.getItemAtPosition(i);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Country selected :: " + selectedCountry);
                    StructuraCountry.Pair<Boolean, Integer> countryPosition = StructuraCountry.GetPositionByCountry(structuraCountries, selectedCountry);
                    if (countryPosition.getFound()) {
                        JSONArray countrySummaryJSON = new JSONArray();
                        try {
                            countrySummaryJSON = new RequestAsync(getString(R.string.API_URL) + getString(R.string.API_Summary) + "/" + structuraCountries.get(countryPosition.getPosition()).getIsod2().toLowerCase(), "GET", null, progressBar).execute().get();
                            Calendar cal = Calendar.getInstance();
                            if (countrySummaryJSON.length() == 1) {
                                if (countrySummaryJSON.getJSONObject(0).has("date")) {
                                    cal.setTime(new Date(countrySummaryJSON.getJSONObject(0).getString("date")));
                                    structuraSummaries.clear();
                                    structuraSummaries.add(new StructuraSummary(countrySummaryJSON.getJSONObject(0).getString("countryName")
                                            , countrySummaryJSON.getJSONObject(0).getInt("population")
                                            , countrySummaryJSON.getJSONObject(0).getInt("newConfirmedInfected")
                                            , countrySummaryJSON.getJSONObject(0).getInt("totalConfirmedInfected")
                                            , countrySummaryJSON.getJSONObject(0).getInt("newDeaths")
                                            , countrySummaryJSON.getJSONObject(0).getInt("totalDeaths")
                                            , countrySummaryJSON.getJSONObject(0).getInt("newRecovered")
                                            , countrySummaryJSON.getJSONObject(0).getInt("totalRecovered")
                                            , cal));
                                    EditText totalPopulation = findViewById(R.id.edit_population);
                                    totalPopulation.setText(NumberFormat.getInstance().format(structuraSummaries.get(0).population));
                                    EditText totalConfirmed = findViewById(R.id.edit_totalconf);
                                    totalConfirmed.setText(NumberFormat.getInstance().format(structuraSummaries.get(0).totalConfirmed));
                                    EditText newConfirmed = findViewById(R.id.edit_newconf);
                                    newConfirmed.setText(NumberFormat.getInstance().format(structuraSummaries.get(0).newConfirmed));
                                    EditText totalDeaths = findViewById(R.id.edit_totaldeath);
                                    totalDeaths.setText(NumberFormat.getInstance().format(structuraSummaries.get(0).totalDeaths));
                                    EditText newDeaths = findViewById(R.id.edit_newdeath);
                                    newDeaths.setText(NumberFormat.getInstance().format(structuraSummaries.get(0).newDeaths));
                                    EditText totalRecovered = findViewById(R.id.edit_totalrec);
                                    totalRecovered.setText(NumberFormat.getInstance().format(structuraSummaries.get(0).totalRecovered));
                                    EditText newRecovered = findViewById(R.id.edit_newrec);
                                    newRecovered.setText(NumberFormat.getInstance().format(structuraSummaries.get(0).newRecovered));
                                    TextView lastUpdate = findViewById(R.id.tv_lasupdate);
                                    lastUpdate.setText(structuraSummaries.get(0).lastUpdate.getTime().toString());
                                    for (EditText editText : new EditText[]{totalConfirmed, newConfirmed, totalDeaths, newDeaths, totalRecovered, newRecovered}) {
                                        editStat(editText, NEUTRAL_STAT);
                                    }
                                    for (EditText edittext : new EditText[]{newConfirmed, newDeaths}) {
                                        if (Integer.parseInt(edittext.getText().toString()) > 0) {
                                            editStat(edittext, BAD_STAT);
                                        } else {
                                            editStat(edittext, NEUTRAL_STAT);
                                        }
                                    }
                                    if (Integer.parseInt(newRecovered.getText().toString()) > 0) {
                                        editStat(newRecovered, GOOD_STAT);
                                    } else {
                                        editStat(newRecovered, NEUTRAL_STAT);
                                    }
                                } else {
                                    EditText totalPopulation = findViewById(R.id.edit_population);
                                    totalPopulation.setText(NumberFormat.getInstance().format(countrySummaryJSON.getJSONObject(0).getInt("population")));
                                    EditText totalConfirmed = findViewById(R.id.edit_totalconf);
                                    EditText newConfirmed = findViewById(R.id.edit_newconf);
                                    EditText totalDeaths = findViewById(R.id.edit_totaldeath);
                                    EditText newDeaths = findViewById(R.id.edit_newdeath);
                                    EditText totalRecovered = findViewById(R.id.edit_totalrec);
                                    EditText newRecovered = findViewById(R.id.edit_newrec);
                                    for (EditText editText : new EditText[]{totalConfirmed, newConfirmed, totalDeaths, newDeaths, totalRecovered, newRecovered}) {
                                        editText.setText("NO DATA");
                                        editStat(editText, BAD_STAT);
                                    }
                                }
                            } else {
                                new MyVibrator(MainActivity.this);
                                Snackbar.make(findViewById(android.R.id.content), "Country data could not be read", Snackbar.LENGTH_LONG).setAction("No action", null).show();
                            }
                        } catch (ExecutionException | InterruptedException | JSONException ex) {
                            if (BuildConfig.DEBUG) Log.e(TAG, "Exception :: " + ex.getMessage(), ex);
                        }
                    } else {
                        new MyVibrator(MainActivity.this);
                        Snackbar.make(findViewById(android.R.id.content), "Country not found", Snackbar.LENGTH_LONG).setAction("No action", null).show();
                    }
                }
            });
        } catch (JSONException | InterruptedException | ExecutionException ex) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Exception :: " + ex.getMessage(), ex);
        }
    }
}
