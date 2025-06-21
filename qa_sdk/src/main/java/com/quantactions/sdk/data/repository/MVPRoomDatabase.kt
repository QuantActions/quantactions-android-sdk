/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quantactions.sdk.BuildConfig
import com.quantactions.sdk.data.entity.ActivityTransitionEntity
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.CognitiveTestEntity
import com.quantactions.sdk.data.entity.Cohort
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.entity.JournalEntryEntity
import com.quantactions.sdk.data.entity.JournalEntryJoinsJournalEventEntity
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.entity.Questionnaire
import com.quantactions.sdk.data.entity.QuestionnaireResponseEntity
import com.quantactions.sdk.data.entity.SleepSummaryEntity
import com.quantactions.sdk.data.entity.StatisticEntity
import com.quantactions.sdk.data.entity.StatisticStringEntity
import com.quantactions.sdk.data.entity.TrendEntity
import net.sqlcipher.database.SQLiteDatabase.getBytes
import net.sqlcipher.database.SupportFactory


// Annotates class to be a Room Database with tables (entities)
@Database(
    entities = [
        StatisticEntity::class,
        StatisticStringEntity::class,
        Questionnaire::class,
        QuestionnaireResponseEntity::class,
        Cohort::class,
        JournalEventEntity::class,
        JournalEntryEntity::class,
        JournalEntryJoinsJournalEventEntity::class,
        TapDataParsed::class,
        DeviceHealthParsed::class,
        HourlyTapsEntity::class,
        CodeOfApp::class,
        SleepSummaryEntity::class,
        TrendEntity::class,
        ActivityTransitionEntity::class,
        CognitiveTestEntity::class
    ],
    version = 12, exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MVPRoomDatabase : RoomDatabase() {

    abstract fun mvpDao(): MVPDao
    abstract fun cognitiveTestDao(): CognitiveTestDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: MVPRoomDatabase? = null

        private var DB_NAME = "stat_database"

        fun getDatabase(context: Context): MVPRoomDatabase {
//            val tempInstance = INSTANCE
//            if (tempInstance != null) {
//                return tempInstance
//            }
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {

                    // Can I check the status of the DB before opening it
                    if (!BuildConfig.DEBUG) {
                        val state = SQLCipherUtils.getDatabaseState(context, DB_NAME)
                        if (state == SQLCipherUtils.State.UNENCRYPTED) {
                            SQLCipherUtils.encrypt(
                                context,
                                DB_NAME,
                                BuildConfig.QA_UUID.toCharArray()
                            )
                        }
                    }

                    // builder
                    val builder = Room.databaseBuilder(
                        context.applicationContext,
                        MVPRoomDatabase::class.java,
                        DB_NAME
                    )
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        // To v7
                        .addMigrations(MIGRATION_1_7)  // TapCounter
                        .addMigrations(MIGRATION_2_7)  // TapCounter
                        .addMigrations(MIGRATION_3_7)  // TapCounter
                        .addMigrations(MIGRATION_4_7)  // TapCounter
                        .addMigrations(MIGRATION_3_8)  // TapCounter
                        .addMigrations(MIGRATION_4_8)  // TapCounter
                        // NOTE: version 5 was only an internal version never in production
                        .addMigrations(MIGRATION_6_7)  // QA Recharge
                        .addMigrations(MIGRATION_7_8)  // QA Recharge
                        .addMigrations(MIGRATION_6_8)  // QA Recharge
                        // Adding Trends
                        .addMigrations(MIGRATION_8_9)  // QA Recharge
                        // Adding activity rec
                        .addMigrations(MIGRATION_7_10)  // QA Recharge
                        .addMigrations(MIGRATION_8_10)  // QA Recharge
                        .addMigrations(MIGRATION_9_10)  // QA Recharge
                        // Adding cognitive tests
                        .addMigrations(MIGRATION_10_11)  // QA Recharge
                        // Adding completion time to questionnaires
                        .addMigrations(MIGRATION_11_12)  // TapCounter

                    // Adding encryption of DB if not debug
                    if (!BuildConfig.DEBUG) {
                        val factory = SupportFactory(getBytes(BuildConfig.QA_UUID.toCharArray()))
                        builder.openHelperFactory(factory)
                    }

                    instance = builder.build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'reset' INTEGER NOT NULL DEFAULT 0;")
    }
}


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE stat_string_table ADD COLUMN 'reset' INTEGER NOT NULL DEFAULT 0;")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
//        migrateQuestionnaires(database)
        database.execSQL("CREATE TABLE questionnaires ('id' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qDescription' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qStudy' TEXT NOT NULL, 'qBody' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL("CREATE TABLE questionnaire_responses ('id' INTEGER NOT NULL, 'qFullID' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qDate' INTEGER NOT NULL, 'qResponse' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE studies ('studyId' TEXT NOT NULL, 'privacyPolicy' TEXT, " +
                    "'studyTitle' TEXT, 'dataPattern' TEXT, 'gpsResolution' INTEGER NOT NULL, 'canWithdraw' INTEGER NOT NULL," +
                    "'syncOnScreenOff' INTEGER, 'perimeterCheck' INTEGER, 'permAppId' INTEGER, " +
                    "'permDrawOver' INTEGER, 'permLocation' INTEGER, 'permContact' INTEGER, PRIMARY KEY('studyId'))"
        )
    }
}

// Migrate to 7

val MIGRATION_1_7 = object : Migration(1, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // from 1 -> 2
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'reset' INTEGER NOT NULL DEFAULT 0;")
        // from 2 -> 3
        database.execSQL("ALTER TABLE stat_string_table ADD COLUMN 'reset' INTEGER NOT NULL DEFAULT 0;")
        // from 3 -> 4
        database.execSQL("CREATE TABLE questionnaires ('id' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qDescription' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qStudy' TEXT NOT NULL, 'qBody' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL("CREATE TABLE questionnaire_responses ('id' INTEGER NOT NULL, 'qFullID' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qDate' INTEGER NOT NULL, 'qResponse' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE studies ('studyId' TEXT NOT NULL, 'privacyPolicy' TEXT, " +
                    "'studyTitle' TEXT, 'dataPattern' TEXT, 'gpsResolution' INTEGER NOT NULL, 'canWithdraw' INTEGER NOT NULL," +
                    "'syncOnScreenOff' INTEGER, 'perimeterCheck' INTEGER, 'permAppId' INTEGER, " +
                    "'permDrawOver' INTEGER, 'permLocation' INTEGER, 'permContact' INTEGER, PRIMARY KEY('studyId'))"
        )

        // from 4 -> 7
//        migrateToJournalEntries(database)
        database.execSQL(
            "CREATE TABLE journal_entry ('id' TEXT NOT NULL, " +
                    "'note' TEXT NOT NULL, " +
                    "'device_id' TEXT NOT NULL, " +
                    "'created' TEXT NOT NULL, " +
                    "'modified' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "'deleted' INTEGER NOT NULL DEFAULT 0, " +
                    "'old_id' TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY('id'))"
        )

        database.execSQL("CREATE TABLE journal_event ('id' TEXT NOT NULL, 'public_name' TEXT NOT NULL, 'icon_name' TEXT NOT NULL, 'created' TEXT NOT NULL, 'modified' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE journal_entry_joins_journal_event ('id' TEXT NOT NULL, " +
                    "'journal_entry_id' TEXT NOT NULL, " +
                    "'journal_event_id' TEXT NOT NULL, " +
                    "'rating' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
//        migrateToConfidenceIntervals(database)
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

//        migrateToParsedData(database)
        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_2_7 = object : Migration(2, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // from 2 -> 3
        database.execSQL("ALTER TABLE stat_string_table ADD COLUMN 'reset' INTEGER NOT NULL DEFAULT 0;")
        // from 3 -> 4
//        migrateQuestionnaires(database)
        database.execSQL("CREATE TABLE questionnaires ('id' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qDescription' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qStudy' TEXT NOT NULL, 'qBody' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL("CREATE TABLE questionnaire_responses ('id' INTEGER NOT NULL, 'qFullID' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qDate' INTEGER NOT NULL, 'qResponse' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE studies ('studyId' TEXT NOT NULL, 'privacyPolicy' TEXT, " +
                    "'studyTitle' TEXT, 'dataPattern' TEXT, 'gpsResolution' INTEGER NOT NULL, 'canWithdraw' INTEGER NOT NULL," +
                    "'syncOnScreenOff' INTEGER, 'perimeterCheck' INTEGER, 'permAppId' INTEGER, " +
                    "'permDrawOver' INTEGER, 'permLocation' INTEGER, 'permContact' INTEGER, PRIMARY KEY('studyId'))"
        )

        // from 4 -> 7
//        migrateToJournalEntries(database)
        database.execSQL(
            "CREATE TABLE journal_entry ('id' TEXT NOT NULL, " +
                    "'note' TEXT NOT NULL, " +
                    "'device_id' TEXT NOT NULL, " +
                    "'created' TEXT NOT NULL, " +
                    "'modified' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "'deleted' INTEGER NOT NULL DEFAULT 0, " +
                    "'old_id' TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY('id'))"
        )

        database.execSQL("CREATE TABLE journal_event ('id' TEXT NOT NULL, 'public_name' TEXT NOT NULL, 'icon_name' TEXT NOT NULL, 'created' TEXT NOT NULL, 'modified' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE journal_entry_joins_journal_event ('id' TEXT NOT NULL, " +
                    "'journal_entry_id' TEXT NOT NULL, " +
                    "'journal_event_id' TEXT NOT NULL, " +
                    "'rating' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

//        migrateToConfidenceIntervals(database)
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

//        migrateToParsedData(database)

        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

    }
}

val MIGRATION_2_8 = object : Migration(2, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // from 2 -> 3
        database.execSQL("ALTER TABLE stat_string_table ADD COLUMN 'reset' INTEGER NOT NULL DEFAULT 0;")
        // from 3 -> 4
//        migrateQuestionnaires(database)
        database.execSQL("CREATE TABLE questionnaires ('id' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qDescription' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qStudy' TEXT NOT NULL, 'qBody' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL("CREATE TABLE questionnaire_responses ('id' INTEGER NOT NULL, 'qFullID' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qDate' INTEGER NOT NULL, 'qResponse' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE studies ('studyId' TEXT NOT NULL, 'privacyPolicy' TEXT, " +
                    "'studyTitle' TEXT, 'dataPattern' TEXT, 'gpsResolution' INTEGER NOT NULL, 'canWithdraw' INTEGER NOT NULL," +
                    "'syncOnScreenOff' INTEGER, 'perimeterCheck' INTEGER, 'permAppId' INTEGER, " +
                    "'permDrawOver' INTEGER, 'permLocation' INTEGER, 'permContact' INTEGER, PRIMARY KEY('studyId'))"
        )

        // from 4 -> 7
//        migrateToJournalEntries(database)
        database.execSQL(
            "CREATE TABLE journal_entry ('id' TEXT NOT NULL, " +
                    "'note' TEXT NOT NULL, " +
                    "'device_id' TEXT NOT NULL, " +
                    "'created' TEXT NOT NULL, " +
                    "'modified' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "'deleted' INTEGER NOT NULL DEFAULT 0, " +
                    "'old_id' TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY('id'))"
        )

        database.execSQL("CREATE TABLE journal_event ('id' TEXT NOT NULL, 'public_name' TEXT NOT NULL, 'icon_name' TEXT NOT NULL, 'created' TEXT NOT NULL, 'modified' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE journal_entry_joins_journal_event ('id' TEXT NOT NULL, " +
                    "'journal_entry_id' TEXT NOT NULL, " +
                    "'journal_event_id' TEXT NOT NULL, " +
                    "'rating' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

//        migrateToConfidenceIntervals(database)
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

//        migrateToParsedData(database)

        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        // 7 -> 8
        database.execSQL(
            "CREATE TABLE hourly_taps ('id' INTEGER NOT NULL, " +
                    "'date_tap' TEXT NOT NULL, " +
                    "'hour' INTEGER NOT NULL, " +
                    "'num_taps' INTEGER NOT NULL, " +
                    "'speed' FLOAT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE code_of_app ('id' INTEGER NOT NULL, " +
                    "'app_name' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE sleep_summary_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'sleep_start' INTEGER NOT NULL, " +
                    "'sleep_end' INTEGER NOT NULL, " +
                    "'int_start' TEXT NOT NULL, " +
                    "'int_stop' TEXT NOT NULL, " +
                    "'int_ntaps' TEXT NOT NULL, " +
                    "'time_zone_id' TEXT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

    }
}

val MIGRATION_3_7 = object : Migration(3, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // from 3 -> 4
//        migrateQuestionnaires(database)
        database.execSQL("CREATE TABLE questionnaires ('id' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qDescription' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qStudy' TEXT NOT NULL, 'qBody' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL("CREATE TABLE questionnaire_responses ('id' INTEGER NOT NULL, 'qFullID' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qDate' INTEGER NOT NULL, 'qResponse' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE studies ('studyId' TEXT NOT NULL, 'privacyPolicy' TEXT, " +
                    "'studyTitle' TEXT, 'dataPattern' TEXT, 'gpsResolution' INTEGER NOT NULL, 'canWithdraw' INTEGER NOT NULL," +
                    "'syncOnScreenOff' INTEGER, 'perimeterCheck' INTEGER, 'permAppId' INTEGER, " +
                    "'permDrawOver' INTEGER, 'permLocation' INTEGER, 'permContact' INTEGER, PRIMARY KEY('studyId'))"
        )

        // from 4 -> 7
//        migrateToJournalEntries(database)
        database.execSQL(
            "CREATE TABLE journal_entry ('id' TEXT NOT NULL, " +
                    "'note' TEXT NOT NULL, " +
                    "'device_id' TEXT NOT NULL, " +
                    "'created' TEXT NOT NULL, " +
                    "'modified' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "'deleted' INTEGER NOT NULL DEFAULT 0, " +
                    "'old_id' TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY('id'))"
        )

        database.execSQL("CREATE TABLE journal_event ('id' TEXT NOT NULL, 'public_name' TEXT NOT NULL, 'icon_name' TEXT NOT NULL, 'created' TEXT NOT NULL, 'modified' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE journal_entry_joins_journal_event ('id' TEXT NOT NULL, " +
                    "'journal_entry_id' TEXT NOT NULL, " +
                    "'journal_event_id' TEXT NOT NULL, " +
                    "'rating' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
//        migrateToConfidenceIntervals(database)
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

//        migrateToParsedData(database)
        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_3_8 = object : Migration(3, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // from 3 -> 4
//        migrateQuestionnaires(database)
        database.execSQL("CREATE TABLE questionnaires ('id' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qDescription' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qStudy' TEXT NOT NULL, 'qBody' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL("CREATE TABLE questionnaire_responses ('id' INTEGER NOT NULL, 'qFullID' TEXT NOT NULL, 'qName' TEXT NOT NULL, 'qCode' TEXT NOT NULL, 'qDate' INTEGER NOT NULL, 'qResponse' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE studies ('studyId' TEXT NOT NULL, 'privacyPolicy' TEXT, " +
                    "'studyTitle' TEXT, 'dataPattern' TEXT, 'gpsResolution' INTEGER NOT NULL, 'canWithdraw' INTEGER NOT NULL," +
                    "'syncOnScreenOff' INTEGER, 'perimeterCheck' INTEGER, 'permAppId' INTEGER, " +
                    "'permDrawOver' INTEGER, 'permLocation' INTEGER, 'permContact' INTEGER, PRIMARY KEY('studyId'))"
        )

        // from 4 -> 7
//        migrateToJournalEntries(database)
        database.execSQL(
            "CREATE TABLE journal_entry ('id' TEXT NOT NULL, " +
                    "'note' TEXT NOT NULL, " +
                    "'device_id' TEXT NOT NULL, " +
                    "'created' TEXT NOT NULL, " +
                    "'modified' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "'deleted' INTEGER NOT NULL DEFAULT 0, " +
                    "'old_id' TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY('id'))"
        )

        database.execSQL("CREATE TABLE journal_event ('id' TEXT NOT NULL, 'public_name' TEXT NOT NULL, 'icon_name' TEXT NOT NULL, 'created' TEXT NOT NULL, 'modified' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE journal_entry_joins_journal_event ('id' TEXT NOT NULL, " +
                    "'journal_entry_id' TEXT NOT NULL, " +
                    "'journal_event_id' TEXT NOT NULL, " +
                    "'rating' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
//        migrateToConfidenceIntervals(database)
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

//        migrateToParsedData(database)
        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        // 7 -> 8
        database.execSQL(
            "CREATE TABLE hourly_taps ('id' INTEGER NOT NULL, " +
                    "'date_tap' TEXT NOT NULL, " +
                    "'hour' INTEGER NOT NULL, " +
                    "'num_taps' INTEGER NOT NULL, " +
                    "'speed' FLOAT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE code_of_app ('id' INTEGER NOT NULL, " +
                    "'app_name' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE sleep_summary_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'sleep_start' INTEGER NOT NULL, " +
                    "'sleep_end' INTEGER NOT NULL, " +
                    "'int_start' TEXT NOT NULL, " +
                    "'int_stop' TEXT NOT NULL, " +
                    "'int_ntaps' TEXT NOT NULL, " +
                    "'time_zone_id' TEXT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_4_7 = object : Migration(4, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE journal_entry ('id' TEXT NOT NULL, " +
                    "'note' TEXT NOT NULL, " +
                    "'device_id' TEXT NOT NULL, " +
                    "'created' TEXT NOT NULL, " +
                    "'modified' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "'deleted' INTEGER NOT NULL DEFAULT 0, " +
                    "'old_id' TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY('id'))"
        )

        database.execSQL("CREATE TABLE journal_event ('id' TEXT NOT NULL, 'public_name' TEXT NOT NULL, 'icon_name' TEXT NOT NULL, 'created' TEXT NOT NULL, 'modified' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE journal_entry_joins_journal_event ('id' TEXT NOT NULL, " +
                    "'journal_entry_id' TEXT NOT NULL, " +
                    "'journal_event_id' TEXT NOT NULL, " +
                    "'rating' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
//        migrateToConfidenceIntervals(database)
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

//        migrateToParsedData(database)
        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_4_8 = object : Migration(4, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE journal_entry ('id' TEXT NOT NULL, " +
                    "'note' TEXT NOT NULL, " +
                    "'device_id' TEXT NOT NULL, " +
                    "'created' TEXT NOT NULL, " +
                    "'modified' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "'deleted' INTEGER NOT NULL DEFAULT 0, " +
                    "'old_id' TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY('id'))"
        )

        database.execSQL("CREATE TABLE journal_event ('id' TEXT NOT NULL, 'public_name' TEXT NOT NULL, 'icon_name' TEXT NOT NULL, 'created' TEXT NOT NULL, 'modified' TEXT NOT NULL, PRIMARY KEY('id'))")

        database.execSQL(
            "CREATE TABLE journal_entry_joins_journal_event ('id' TEXT NOT NULL, " +
                    "'journal_entry_id' TEXT NOT NULL, " +
                    "'journal_event_id' TEXT NOT NULL, " +
                    "'rating' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
//        migrateToConfidenceIntervals(database)
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

//        migrateToParsedData(database)
        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE hourly_taps ('id' INTEGER NOT NULL, " +
                    "'date_tap' TEXT NOT NULL, " +
                    "'hour' INTEGER NOT NULL, " +
                    "'num_taps' INTEGER NOT NULL, " +
                    "'speed' FLOAT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE code_of_app ('id' INTEGER NOT NULL, " +
                    "'app_name' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE sleep_summary_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'sleep_start' INTEGER NOT NULL, " +
                    "'sleep_end' INTEGER NOT NULL, " +
                    "'int_start' TEXT NOT NULL, " +
                    "'int_stop' TEXT NOT NULL, " +
                    "'int_ntaps' TEXT NOT NULL, " +
                    "'time_zone_id' TEXT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
//        migrateToConfidenceIntervals(database)
//        migrateToParsedData(database)

        database.query("")

        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_6_8 = object : Migration(6, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.query("")

        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_l' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'ci_h' REAL;")
        database.execSQL("ALTER TABLE stat_table ADD COLUMN 'conf' REAL;")

        database.execSQL(
            "CREATE TABLE taps_table ('id' INTEGER NOT NULL, " +
                    "'taps' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'orientations' TEXT NOT NULL, " +
                    "'appIds0' TEXT NOT NULL, " +
                    "'appIds1' TEXT NOT NULL, " +
                    "'appIds2' TEXT NOT NULL, " +
                    "'tapsSession' INTEGER NOT NULL, " +
                    "'lengthSession' INTEGER NOT NULL, " +
                    "'timeZone' TEXT NOT NULL, " +
                    "'inCharge' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE health_table ('id' INTEGER NOT NULL, " +
                    "'timestamps' TEXT NOT NULL, " +
                    "'charge' TEXT NOT NULL, " +
                    "'event' TEXT NOT NULL, " +
                    "'start' INTEGER NOT NULL, " +
                    "'stop' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE hourly_taps ('id' INTEGER NOT NULL, " +
                    "'date_tap' TEXT NOT NULL, " +
                    "'hour' INTEGER NOT NULL, " +
                    "'num_taps' INTEGER NOT NULL, " +
                    "'speed' FLOAT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE code_of_app ('id' INTEGER NOT NULL, " +
                    "'app_name' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE sleep_summary_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'sleep_start' INTEGER NOT NULL, " +
                    "'sleep_end' INTEGER NOT NULL, " +
                    "'int_start' TEXT NOT NULL, " +
                    "'int_stop' TEXT NOT NULL, " +
                    "'int_ntaps' TEXT NOT NULL, " +
                    "'time_zone_id' TEXT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE hourly_taps ('id' INTEGER NOT NULL, " +
                    "'date_tap' TEXT NOT NULL, " +
                    "'hour' INTEGER NOT NULL, " +
                    "'num_taps' INTEGER NOT NULL, " +
                    "'speed' FLOAT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE code_of_app ('id' INTEGER NOT NULL, " +
                    "'app_name' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE sleep_summary_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'sleep_start' INTEGER NOT NULL, " +
                    "'sleep_end' INTEGER NOT NULL, " +
                    "'int_start' TEXT NOT NULL, " +
                    "'int_stop' TEXT NOT NULL, " +
                    "'int_ntaps' TEXT NOT NULL, " +
                    "'time_zone_id' TEXT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE trend_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'trend' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'diff2W' REAL, " +
                    "'stat2W' REAL, " +
                    "'sign2W' REAL, " +
                    "'diff6W' REAL, " +
                    "'stat6W' REAL, " +
                    "'sign6W' REAL, " +
                    "'diff1Y' REAL, " +
                    "'stat1Y' REAL, " +
                    "'sign1Y' REAL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_7_10 = object : Migration(7, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {


        database.execSQL(
            "CREATE TABLE trend_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'trend' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'diff2W' REAL, " +
                    "'stat2W' REAL, " +
                    "'sign2W' REAL, " +
                    "'diff6W' REAL, " +
                    "'stat6W' REAL, " +
                    "'sign6W' REAL, " +
                    "'diff1Y' REAL, " +
                    "'stat1Y' REAL, " +
                    "'sign1Y' REAL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE activity_transition_table (" +
                    "'id' INTEGER NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'action' TEXT NOT NULL, " +
                    "'transition' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )


        database.execSQL(
            "CREATE TABLE hourly_taps ('id' INTEGER NOT NULL, " +
                    "'date_tap' TEXT NOT NULL, " +
                    "'hour' INTEGER NOT NULL, " +
                    "'num_taps' INTEGER NOT NULL, " +
                    "'speed' FLOAT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE code_of_app ('id' INTEGER NOT NULL, " +
                    "'app_name' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE sleep_summary_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'sleep_start' INTEGER NOT NULL, " +
                    "'sleep_end' INTEGER NOT NULL, " +
                    "'int_start' TEXT NOT NULL, " +
                    "'int_stop' TEXT NOT NULL, " +
                    "'int_ntaps' TEXT NOT NULL, " +
                    "'time_zone_id' TEXT NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

    }


}

val MIGRATION_8_10 = object : Migration(8, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE trend_table (" +
                    "'id' TEXT NOT NULL, " +
                    "'trend' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'diff2W' REAL, " +
                    "'stat2W' REAL, " +
                    "'sign2W' REAL, " +
                    "'diff6W' REAL, " +
                    "'stat6W' REAL, " +
                    "'sign6W' REAL, " +
                    "'diff1Y' REAL, " +
                    "'stat1Y' REAL, " +
                    "'sign1Y' REAL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "CREATE TABLE activity_transition_table (" +
                    "'id' INTEGER NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'action' TEXT NOT NULL, " +
                    "'transition' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE activity_transition_table (" +
                    "'id' INTEGER NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'action' TEXT NOT NULL, " +
                    "'transition' INTEGER NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE cognitive_test_results (" +
                    "'id' INTEGER NOT NULL, " +
                    "'testType' TEXT NOT NULL, " +
                    "'results' TEXT NOT NULL, " +
                    "'timestamp' INTEGER NOT NULL, " +
                    "'localTime' TEXT NOT NULL, " +
                    "'sync' INTEGER NOT NULL, " +
                    "PRIMARY KEY('id')" +
                    ")"
        )

        database.execSQL(
            "ALTER TABLE studies " +
                    "ADD COLUMN enableCognitiveTest INTEGER NOT NULL DEFAULT 0"
        )

        database.execSQL("DROP TABLE questionnaires")

        database.execSQL("CREATE TABLE questionnaires " +
                "(" +
                "'id' TEXT NOT NULL, " +
                "'qName' TEXT NOT NULL, " +
                "'qDescription' TEXT NOT NULL, " +
                "'qCode' TEXT NOT NULL, " +
                "'qStudy' TEXT NOT NULL, " +
                "'qBody' TEXT NOT NULL, " +
                "PRIMARY KEY('id', 'qStudy'))")

    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "ALTER TABLE questionnaires " +
                    "ADD COLUMN completionTimeMinutes INTEGER NOT NULL DEFAULT 5"
        )
    }
}
