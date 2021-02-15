package com.example.coviddefeat;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class SymptomActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CovidDefeat/";
    String db_name = "covid_sym_logger.sql";
    DatabaseActivity db_act;

    // Declare all the Sympotms
    String[] sym = {"Fever or chills",
            "Cough",
            "Shortness of breath or difficulty breathing",
            "Fatigue",
            "Muscle or body aches",
            "Headache",
            "New loss of taste or smell",
            "Sore throat",
            "Congestion or runny nose",
            "Nausea or vomiting",
            "Diarrhea" };

    RatingBar ratingBar;
    HashMap<String, Float> ratingHash;
    TextView inst;

    Spinner spin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_activity);

        db_act = new DatabaseActivity();
        inst = (TextView)findViewById(R.id.textView5);
        String instructions = "Enter the Ratings of the Symptoms you feel";
        inst.setText(instructions);

        ratingHash = new HashMap<String, Float>();
        for(int i = 0; i < sym.length; i++){
            ratingHash.put(sym[i], (float) 0.0);
        }

        // Setting up the dropdown menu
        spin = (Spinner) findViewById(R.id.spinner2);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sym);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        spin.setOnItemSelectedListener(this);


        // Listening to the rating bar
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                String spin_text = spin.getSelectedItem().toString();
                ratingHash.put(spin_text, rating);
            }
        });

        // Button to upload the symptoms rating into the database
        Button upload_btn = (Button)findViewById(R.id.up_symp);
        upload_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {

                int success = db_act.upload_symptoms(ratingHash);
                if (success == 1)
                {
                    Toast.makeText(SymptomActivity.this, "Symptoms Uploaded", Toast.LENGTH_LONG).show();
                    ratingBar.setRating(0);
                    for(int i = 0; i < sym.length; i++)
                    {
                        ratingHash.put(sym[i], (float) 0.0);
                    }

                }


            }
        });


    }

    // Listener for the dropdown menu
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        String selected_item = (String) arg0.getItemAtPosition(position);
        Float rating = ratingHash.get(selected_item);
        ratingBar.setRating(rating);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}