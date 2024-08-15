/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import timber.log.Timber;

/**
 * @hide
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes";
    private static final int DATABASE_VERSION = 10;
    private static DatabaseHelper mInstance = null;

    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    private DatabaseHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //create personal table
        String create = "";
        create += "CREATE TABLE " + LookUp.TABLE_RECORDS + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_STEP + " INTEGER,";
        create += "  " + LookUp.COLUMN_MEAN + " TEXT NOT NULL,";
        create += "  " + LookUp.COLUMN_STEP_ON + " INTEGER,";
        create += "  " + LookUp.COLUMN_TAP_ON + " INTEGER,";
        create += "  " + LookUp.COLUMN_UUI + " TEXT,";
        create += "  " + LookUp.GENDER + " INTEGER,";
        create += "  " + LookUp.AGE + " INTEGER,";
        create += "  " + LookUp.EMAIL + " TEXT,";
        create += "  " + LookUp.COLUMN_AUTH_CODE + " TEXT,";
        create += "  " + LookUp.AS_TURNED_ON + " INTEGER";
        create += ")";
        db.execSQL(create);

        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_APP_CODE + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_APP_NAME + " TEXT NOT NULL,";
        create += "  " + LookUp.COLUMN_IS_SYNC + " INTEGER";
        create += ")";
        db.execSQL(create);

        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_SYNC + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_DT + " INTEGER,";
        create += "  " + LookUp.COLUMN_LAST_APP_ID + " INTEGER";
        create += ")";
        db.execSQL(create);

        // Store sum per every day
        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_TAPS + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_DATE + " DATETIME,";
        create += "  " + LookUp.COLUMN_HOUR + " INTEGER,";
        create += "  " + LookUp.NUMBER_TAPS + " INTEGER,";
        create += "  " + LookUp.COLUMN_SPEED + " REAL";
        create += ")";
        db.execSQL(create);

        // Stores data to synchronize
        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_PENDING_SYNC + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_DATA + " TEXT,";
        create += "  " + LookUp.COLUMN_GPS + " TEXT";
        create += ")";
        db.execSQL(create);

        // GPS position
        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_GPS + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_DATE + " TEXT,";
        create += "  " + LookUp.COLUMN_LATITUDE + " TEXT,";
        create += "  " + LookUp.COLUMN_LONGITUDE + " TEXT,";
        create += "  " + LookUp.COLUMN_CITY + " TEXT";
        create += ")";
        db.execSQL(create);

        // list of studies
        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_STUDIES + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_STUDY_ID + " TEXT,";
        create += "  " + LookUp.COLUMN_PRIVACY + " TEXT,";
        create += "  " + LookUp.COLUMN_WITHDRAW + " INTEGER,";
        create += "  " + LookUp.COLUMN_DATA_PATTERN + " TEXT,";
        create += "  " + LookUp.COLUMN_STUDY_TITLE + " TEXT,";
        create += "  " + LookUp.COLUMN_HIGH_GPS + " INTEGER,";
        create += "  " + LookUp.COLUMN_SYNC_SCREEN_OFF + " INTEGER,";
        create += "  " + LookUp.COLUMN_PERIMETER_CHECK + " INTEGER,";
        create += "  " + LookUp.COLUMN_PERM_APP_ID + " INTEGER,";
        create += "  " + LookUp.COLUMN_PERM_DRAW + " INTEGER,";
        create += "  " + LookUp.COLUMN_PERM_LOC + " INTEGER,";
        create += "  " + LookUp.COLUMN_PERM_CONTACT + " INTEGER";
        create += ")";
        db.execSQL(create);

        // Stores stats to synchronize
        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_TAP_STATS + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_STATS_DATE + " INTEGER,";
        create += "  " + LookUp.COLUMN_STATS_TZ + " TEXT,";
        create += "  " + LookUp.COLUMN_STATS_H_DATA + " TEXT,";
        create += "  " + LookUp.COLUMN_STATS_H_SPEED_DATA + " TEXT,";
        create += "  " + LookUp.COLUMN_STATS_D_AVG + " FLOAT,";
        create += "  " + LookUp.COLUMN_STATS_D_AVG_SPEED + " FLOAT,";
        create += "  " + LookUp.COLUMN_STATS_D_TAPS + " INTEGER,";
        create += "  " + LookUp.COLUMN_STATS_VER + " TEXT";
        create += ")";
        db.execSQL(create);

        // New in version 10
        // Stores responses to questionnaires
        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_QUESTIONNAIRES + " (";
        create += "  " + LookUp.COLUMN_ID + " TEXT PRIMARY KEY,";  // ID = study_id + quest name
        create += "  " + LookUp.COLUMN_Q_NAME + " TEXT,";
        create += "  " + LookUp.COLUMN_Q_DESCRIPTION + " TEXT,";
        create += "  " + LookUp.COLUMN_Q_CODE + " TEXT,";
        create += "  " + LookUp.COLUMN_Q_STUDY + " TEXT,";
        create += "  " + LookUp.COLUMN_Q_BODY + " TEXT";
        create += ")";
        db.execSQL(create);

        // Stores responses to questionnaires
        create = "";
        create += "CREATE TABLE " + LookUp.TABLE_QUESTIONNAIRES_RESPONSES + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_Q_FULL_ID + " TEXT,";
        create += "  " + LookUp.COLUMN_Q_NAME + " TEXT,";
        create += "  " + LookUp.COLUMN_Q_CODE + " TEXT,";
        create += "  " + LookUp.COLUMN_Q_DATE + " INTEGER,";
        create += "  " + LookUp.COLUMN_Q_RESPONSE + " TEXT";
        create += ")";
        db.execSQL(create);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v2 added table7
        // v3 drop uui from table5
        // v4 add column email
        // v5 added column sync and perimeter to table 7
        // v6 added columns permissions for table 7
        // v10 removing stored identifiers + questionnaires

        if (oldVersion == 1 && newVersion > 1) {
            try {
                String create = "";
                create += "CREATE TABLE " + LookUp.TABLE_STUDIES + " (";
                create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
                create += "  " + LookUp.COLUMN_PART_ID + " TEXT,";
                create += "  " + LookUp.COLUMN_STUDY_ID + " TEXT,";
                create += "  " + LookUp.COLUMN_PRIVACY + " TEXT,";
                create += "  " + LookUp.COLUMN_WITHDRAW + " INTEGER,";
                create += "  " + LookUp.COLUMN_DATA_PATTERN + " TEXT,";
                create += "  " + LookUp.COLUMN_STUDY_TITLE + " TEXT,";
                create += "  " + LookUp.COLUMN_HIGH_GPS + " INTEGER";
                create += "  " + LookUp.COLUMN_SYNC_SCREEN_OFF + " INTEGER,";
                create += "  " + LookUp.COLUMN_PERIMETER_CHECK + " INTEGER,";
                create += "  " + LookUp.COLUMN_PERM_APP_ID + " INTEGER,";
                create += "  " + LookUp.COLUMN_PERM_DRAW + " INTEGER,";
                create += "  " + LookUp.COLUMN_PERM_LOC + " INTEGER,";
                create += "  " + LookUp.COLUMN_PERM_CONTACT + " INTEGER";
                create += ")";
                db.execSQL(create);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    FirebaseCrashlytics.getInstance().recordException(e);
                } catch (Exception ex) {
                    Timber.e("App does not integrate Firebase, cannot send crash!");
                }
            }
        }

        if ((oldVersion == 1 || oldVersion == 2) && newVersion >= 4) {
            try {
                String create;
                create = "ALTER TABLE " + LookUp.TABLE_RECORDS + " ADD COLUMN " + LookUp.EMAIL + " TEXT NOT NULL DEFAULT 'no_permission'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_RECORDS + " ADD COLUMN " + LookUp.FIREBASE + " TEXT NOT NULL DEFAULT 'no_token'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_PENDING_SYNC + " ADD COLUMN " + LookUp.COLUMN_GPS + " TEXT";

                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_APP_CODE + " ADD COLUMN " + LookUp.COLUMN_IS_SYNC + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    FirebaseCrashlytics.getInstance().recordException(e);
                } catch (Exception ex) {
                    Timber.e("App does not integrate Firebase, cannot send crash!");
                }
         }
        }

        if (oldVersion == 2 && newVersion >= 5) {
            createTable7(db);
        }

        if (oldVersion == 3 && newVersion >= 4) {
            String create;
            create = "ALTER TABLE " + LookUp.TABLE_RECORDS + " ADD COLUMN " + LookUp.EMAIL + " TEXT NOT NULL DEFAULT 'no_permission'";
            db.execSQL(create);
            create = "ALTER TABLE " + LookUp.TABLE_RECORDS + " ADD COLUMN " + LookUp.FIREBASE + " TEXT NOT NULL DEFAULT 'no_token'";
            db.execSQL(create);
            create = "ALTER TABLE " + LookUp.TABLE_PENDING_SYNC + " ADD COLUMN " + LookUp.COLUMN_GPS + " TEXT";
            db.execSQL(create);

            try {
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_SYNC_SCREEN_OFF + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERIMETER_CHECK + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_APP_ID + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_DRAW + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_LOC + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_CONTACT + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
            }catch(Exception e){
                createTable7(db);
            }
            create = "ALTER TABLE " + LookUp.TABLE_APP_CODE + " ADD COLUMN " + LookUp.COLUMN_IS_SYNC + " INTEGER NOT NULL DEFAULT '0'";
            db.execSQL(create);
        }


        if (oldVersion == 4 && newVersion >= 5) {
            try {
                String create;
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_SYNC_SCREEN_OFF + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERIMETER_CHECK + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_APP_ID + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_DRAW + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_LOC + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_CONTACT + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
                create = "ALTER TABLE " + LookUp.TABLE_APP_CODE + " ADD COLUMN " + LookUp.COLUMN_IS_SYNC + " INTEGER NOT NULL DEFAULT '0'";
                db.execSQL(create);
            }catch (Exception e){
                createTable7(db);
            }

        }

        if (oldVersion == 5 && newVersion == 6) {
            String create;
            create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_APP_ID + " INTEGER NOT NULL DEFAULT '0'";
            db.execSQL(create);
            create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_DRAW + " INTEGER NOT NULL DEFAULT '0'";
            db.execSQL(create);
            create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_LOC + " INTEGER NOT NULL DEFAULT '0'";
            db.execSQL(create);
            create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_PERM_CONTACT + " INTEGER NOT NULL DEFAULT '0'";
            db.execSQL(create);
        }

        if (newVersion == 7) {
            String create;
            create = "ALTER TABLE " + LookUp.TABLE_STUDIES + " ADD COLUMN " + LookUp.COLUMN_DEV_PART_ID + " TEXT NOT NULL DEFAULT 'empty'";
            db.execSQL(create);
        }

        if (newVersion == 8) {
            // Stores stats to synchronize
            String create = "";
            create += "CREATE TABLE " + LookUp.TABLE_TAP_STATS + " (";
            create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
            create += "  " + LookUp.COLUMN_STATS_DATE + " INTEGER,";
            create += "  " + LookUp.COLUMN_STATS_TZ + " TEXT,";
            create += "  " + LookUp.COLUMN_STATS_H_DATA + " TEXT,";
            create += "  " + LookUp.COLUMN_STATS_H_SPEED_DATA + " TEXT,";
            create += "  " + LookUp.COLUMN_STATS_D_AVG + " FLOAT,";
            create += "  " + LookUp.COLUMN_STATS_D_AVG_SPEED + " FLOAT,";
            create += "  " + LookUp.COLUMN_STATS_D_TAPS + " INTEGER,";
            create += "  " + LookUp.COLUMN_STATS_VER + " TEXT";
            create += ")";
            db.execSQL(create);
        }

        if (newVersion == 9) {
            String create;
            create = "ALTER TABLE " + LookUp.TABLE_RECORDS + " ADD COLUMN " + LookUp.COLUMN_AUTH_CODE + " TEXT NOT NULL DEFAULT 'empty'";
            db.execSQL(create);
        }

        if (newVersion == 10) {
            Log.d("DATABASE UPDATE", "REMOVING STUFF");
            // remove FB
            String create;
            create = "UPDATE " + LookUp.TABLE_RECORDS + " SET " + LookUp.FIREBASE + "=''";
            db.execSQL(create);

            // remove part IDS
            create = "UPDATE " + LookUp.TABLE_STUDIES + " SET " + LookUp.COLUMN_PART_ID + "=''," + LookUp.COLUMN_DEV_PART_ID + "=''";
            db.execSQL(create);

            // Questionnaire stuff
            // CREATE TABLE questionnaires (_id TEXT PRIMARY KEY, )
            create = "";
            create += "CREATE TABLE " + LookUp.TABLE_QUESTIONNAIRES + " (";
            create += "  " + LookUp.COLUMN_ID + " TEXT PRIMARY KEY,";  // ID = study_id + quest name
            create += "  " + LookUp.COLUMN_Q_NAME + " TEXT,";
            create += "  " + LookUp.COLUMN_Q_DESCRIPTION + " TEXT,";
            create += "  " + LookUp.COLUMN_Q_CODE + " TEXT,";
            create += "  " + LookUp.COLUMN_Q_STUDY + " TEXT,";
            create += "  " + LookUp.COLUMN_Q_BODY + " TEXT";
            create += ")";
            db.execSQL(create);

            // Stores responses to questionnaires
            create = "";
            create += "CREATE TABLE " + LookUp.TABLE_QUESTIONNAIRES_RESPONSES + " (";
            create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
            create += "  " + LookUp.COLUMN_Q_FULL_ID + " TEXT,";
            create += "  " + LookUp.COLUMN_Q_NAME + " TEXT,";
            create += "  " + LookUp.COLUMN_Q_CODE + " TEXT,";
            create += "  " + LookUp.COLUMN_Q_DATE + " INTEGER,";
            create += "  " + LookUp.COLUMN_Q_RESPONSE + " TEXT";
            create += ")";
            db.execSQL(create);

        }
    }

    private void createTable7(SQLiteDatabase db) {
        String create = "";
        create += "CREATE TABLE " + LookUp.TABLE_STUDIES + " (";
        create += "  " + LookUp.COLUMN_ID + " INTEGER PRIMARY KEY,";
        create += "  " + LookUp.COLUMN_PART_ID + " TEXT,";
        create += "  " + LookUp.COLUMN_DEV_PART_ID + " TEXT,";
        create += "  " + LookUp.COLUMN_STUDY_ID + " TEXT,";
        create += "  " + LookUp.COLUMN_PRIVACY + " TEXT,";
        create += "  " + LookUp.COLUMN_WITHDRAW + " INTEGER,";
        create += "  " + LookUp.COLUMN_DATA_PATTERN + " TEXT,";
        create += "  " + LookUp.COLUMN_STUDY_TITLE + " TEXT,";
        create += "  " + LookUp.COLUMN_HIGH_GPS + " INTEGER";
        create += "  " + LookUp.COLUMN_SYNC_SCREEN_OFF + " INTEGER NOT NULL DEFAULT '0',";
        create += "  " + LookUp.COLUMN_PERIMETER_CHECK + " INTEGER NOT NULL DEFAULT '0',";
        create += "  " + LookUp.COLUMN_PERM_APP_ID + " INTEGER NOT NULL DEFAULT '0',";
        create += "  " + LookUp.COLUMN_PERM_DRAW + " INTEGER NOT NULL DEFAULT '0',";
        create += "  " + LookUp.COLUMN_PERM_LOC + " INTEGER NOT NULL DEFAULT '0',";
        create += "  " + LookUp.COLUMN_PERM_CONTACT + " INTEGER NOT NULL DEFAULT '0'";
        create += ")";
        db.execSQL(create);
    }
}
