package com.example.coviddefeat;

//This class contains all the database implementation

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

public class DatabaseActivity
{
    String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CovidDefeat/";
    String db_name = "coviddefeat_logger.sql";
    public static String TAG = "Debug_Database_Activity";
    final String table_name = "CovidDefeat";
    SQLiteDatabase db;

    // This function creates the database
    public void create_database()
    {

        try
        {
            db = SQLiteDatabase.openOrCreateDatabase(folder_path + db_name, null);
            db.beginTransaction();
            db.setTransactionSuccessful();
        }
        catch (SQLiteException e)
        {
            Log.d(TAG, e.getMessage());
        }
        finally
        {
            db.endTransaction();
        }
    }


    // This function creates the table into the database
    public void create_table()
    {
        int check = check_table_exists();

        if (check == 0) {
            try {
                db.beginTransaction();
                String make_table_query = "CREATE TABLE " + table_name + "("
                        + "recID integer PRIMARY KEY autoincrement, "
                        + "heart_rate real,"
                        + "resp_rate real,"
                        + "fever  real,"
                        + "cough real,"
                        + "breath_problem real,"
                        + "fatigue real,"
                        + "body_ache real,"
                        + "headache real,"
                        + "loss_of_taste real,"
                        + "sore_throat real,"
                        + "congestion real,"
                        + "nausea real,"
                        + "diarrhea real);";
                db.execSQL(make_table_query);
                db.setTransactionSuccessful();
            }
            catch (SQLiteException e) {

            } finally {
                db.endTransaction();
            }
        }

    }

    // This function checks if the table already exists or not
    public int check_table_exists()
    {

        int check = 0;
        String sql_table_check = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table_name + "'";
        Cursor mCursor = db.rawQuery(sql_table_check, null);

        if (mCursor.getCount() > 0) {
            check = 1;
        }

        return check;
    }


    // Function to upload the heart and respiratory rate into the database
    public int upload_hr_resp_rate(double hr, double resp_rate)
    {
        try
        {

            db.beginTransaction();
            String insert_query = "INSERT into " + table_name
                    + "(heart_rate, resp_rate, fever, cough, breath_problem, fatigue, body_ache, headache, loss_of_taste, sore_throat, congestion, nausea, diarrhea) values ("
                    + hr + ',' + resp_rate + ',' + "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0);";

            db.execSQL(insert_query);
            db.setTransactionSuccessful();

        }
        catch (SQLiteException e)
        {
            return 0;
        }
        finally {

            db.endTransaction();
            return 1;
        }
    }

    // Function to upload all the Symptoms
    public int upload_symptoms(HashMap<String, Float> ratings)
    {
        create_database();
        int p_id = get_rows();
        Log.d("Rows: ", ""+p_id);
        try
        {
            db.beginTransaction();
            String update_query = "UPDATE " + table_name + "\n"
                    + "SET fever = " + ratings.get("Fever or chills") + ','
                    + "cough = " + ratings.get("Cough") + ','
                    + "breath_problem = " + ratings.get("Shortness of breath or difficulty breathing") + ','
                    + "fatigue = " + ratings.get("Fatigue") + ','
                    + "body_ache = " + ratings.get("Muscle or body aches") + ','
                    + "headache = " + ratings.get("Headache") + ','
                    + "loss_of_taste = " + ratings.get("New loss of taste or smell") + ','
                    + "sore_throat = " + ratings.get("Sore throat") + ','
                    + "congestion = " + ratings.get("Congestion or runny nose") + ','
                    + "nausea = " + ratings.get("Nausea or vomiting") + ','
                    + "diarrhea = " + ratings.get("Diarrhea")
                    + " where recID = " + p_id + ";";

            db.execSQL(update_query);
            db.setTransactionSuccessful();

        }
        catch (SQLiteException e)
        {
            return 0;
        }
        finally
        {
            db.endTransaction();
            return 1;
        }

    }

    // Function to get the total number of rows in the database to insert in the last entry
    public int get_rows()
    {
        int row_count=0;
        try
        {
            db.beginTransaction();
            String rows = "SELECT * FROM " + table_name;
            Cursor cursor = db.rawQuery(rows, null);
            db.setTransactionSuccessful();
            row_count = cursor.getCount();

            return row_count;
        }
        catch (SQLiteException e)
        {
            return 0;
        }
        finally
        {
            db.endTransaction();
            return row_count;
        }

    }
}
