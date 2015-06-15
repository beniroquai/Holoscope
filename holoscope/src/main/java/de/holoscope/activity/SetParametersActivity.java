package de.holoscope.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;




import de.holoscope.datasets.Dataset;
import de.holoscope.R;

/**
 * Created by Bene on 19.04.2015.
 */
public class SetParametersActivity extends MainActivity{

    EditText wavelength;
    EditText z_min;
    EditText z_max;
    EditText z_samples;
    EditText z_resolution;
    String DEFAULT = "0";

    String z_min_val;
    String z_max_val;
    String z_resolution_val;
    String wavelength_val;
    String z_samples_val;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        final Button saveParameters;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.parameters_view);


        wavelength = (EditText) findViewById(R.id.editWavelength);
        z_min = (EditText) findViewById(R.id.editZMin);
        z_max = (EditText) findViewById(R.id.editZMax);
        z_samples= (EditText) findViewById(R.id.editZSamples);
        z_resolution = (EditText) findViewById(R.id.editZResolution);

        SharedPreferences sharedPreferences = getSharedPreferences("MyData", 0);


        // Check if Preference File is empty!
        if(sharedPreferences.getString("z_min", DEFAULT)=="0"){
            z_min_val = ""+ Dataset.ZMIN;
            z_max_val = ""+ Dataset.ZMAX;
            z_resolution_val = ""+ Dataset.ZINC;
            wavelength_val = ""+ Dataset.WAVELENGTH;
            z_samples_val = ""+ Dataset.Z_SAMPLES ;

            Log.e("SharedPref: ", "Was empty! : "+sharedPreferences.getString("z_min", DEFAULT));
        }
        else{
            z_min_val = sharedPreferences.getString("z_min", DEFAULT);
            z_max_val = sharedPreferences.getString("z_max", DEFAULT);
            z_resolution_val = sharedPreferences.getString("z_resolution", DEFAULT);
            wavelength_val = sharedPreferences.getString("wavelength", DEFAULT);
            z_samples_val = sharedPreferences.getString("z_samples", DEFAULT);
        }

        z_min.setText(z_min_val);
        z_max.setText(z_max_val);
        z_resolution.setText(z_resolution_val);
        wavelength.setText(wavelength_val);
        z_samples.setText(z_samples_val);




//TODO SAFE DATA once Save Button pressed, get back saved data once this activity was created globally


        Dataset.ZMIN =  (Float.valueOf(z_min_val));
        Dataset.ZMAX = Float.valueOf(z_max_val);
        Dataset.ZINC = Float.valueOf(z_resolution_val);
        Dataset.WAVELENGTH = Float.valueOf(wavelength_val);
        Dataset.Z_SAMPLES = Integer.parseInt(z_samples_val);


        saveParameters = (Button) findViewById(R.id.saveValue);
        saveParameters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                Intent intent = new Intent(v.getContext(), MainActivity.class);
                startActivity(intent);
            }
        });

    }



public void save(){
    SharedPreferences sharedPreferences = getSharedPreferences("MyData", 0);
    SharedPreferences.Editor editor=sharedPreferences.edit();
    editor.putString("z_samples", z_samples.getText().toString());
    editor.putString("z_resolution", z_resolution.getText().toString());
    editor.putString("z_min", z_min.getText().toString());
    editor.putString("z_max", z_max.getText().toString());
    editor.putString("wavelength", wavelength.getText().toString());


    Dataset.ZMIN = Float.valueOf(z_min.getText().toString());
    Dataset.ZMAX = Float.valueOf(z_max.getText().toString());
    Dataset.ZINC = Float.valueOf(z_resolution.getText().toString());
    Dataset.WAVELENGTH = Float.valueOf(wavelength.getText().toString());
    Dataset.Z_SAMPLES = Integer.parseInt(z_samples.getText().toString());


    editor.commit();

    Toast.makeText(this, "Data was saved successfully", Toast.LENGTH_LONG).show();
}

}
