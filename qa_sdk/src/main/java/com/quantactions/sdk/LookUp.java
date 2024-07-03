/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, July 2024
 * *******************************************************************************
 */

package com.quantactions.sdk;


/**
 * Created By Enea Ceolini 2015
 * Contact: enea.ceolini@quantactions.com
 * @hide
 */

public interface LookUp {

    String TABLE_RECORDS = "records";
    String TABLE_APP_CODE = "codeofapp";
    String TABLE_SYNC = "sync";
    String TABLE_TAPS = "taps";
    String TABLE_PENDING_SYNC = "pending_sync";
    String TABLE_STUDIES = "studies";
    String TABLE_TAP_STATS = "taps_stats";
    String TABLE_QUESTIONNAIRES = "questionnaires";
    String TABLE_QUESTIONNAIRES_RESPONSES = "questionnaires_resp";

    String COLUMN_ID = "_id";
    String COLUMN_STEP = "msteplen";
    String COLUMN_MEAN = "mean";
    String COLUMN_UUI = "uui";
    String COLUMN_APP_NAME = "appname";
    String COLUMN_STEP_ON = "stepon";
    String COLUMN_TAP_ON = "tapon";
    String COLUMN_DT = "dt";
    String COLUMN_LAST_APP_ID = "last_app_id";
    String COLUMN_DATE = "date_tap";
    String NUMBER_TAPS = "num_taps";
    String COLUMN_HOUR = "hour";
    String GENDER = "gender";
    String AGE = "age";
    String SELF_DECLARED_HEALTHY = "msteplen";
    String COLUMN_DATA = "data_to_sync";
    String COLUMN_SPEED = "speed";
    String AS_TURNED_ON = "as_on";
    String TABLE_GPS = "gps";
    String COLUMN_LATITUDE = "latitude";
    String COLUMN_LONGITUDE = "longitude";
    String COLUMN_CITY = "city";
    String COLUMN_PRIVACY = "privacy_policy";
    String COLUMN_WITHDRAW = "withdraw";
    String COLUMN_STUDY_ID = "study_id";
    String COLUMN_DATA_PATTERN = "data_pattern";
    String COLUMN_STUDY_TITLE = "study_title";
    String COLUMN_HIGH_GPS = "high_gps";
    String EMAIL = "email";
    String COLUMN_GPS = "gps";
    String COLUMN_SYNC_SCREEN_OFF = "sync_screen_off";
    String COLUMN_PERIMETER_CHECK = "permiter_check";
    String COLUMN_IS_SYNC = "is_synched";
    String COLUMN_PERM_APP_ID = "perm_app_id";
    String COLUMN_PERM_DRAW = "perm_draw";
    String COLUMN_PERM_LOC = "perm_loc";
    String COLUMN_PERM_CONTACT = "perm_contact";

    String COLUMN_STATS_DATE = "date";
    String COLUMN_STATS_TZ = "time_zone";
    String COLUMN_STATS_H_DATA = "hourly_data";
    String COLUMN_STATS_H_SPEED_DATA = "hourly_speed_data";
    String COLUMN_STATS_D_AVG = "daily_average";
    String COLUMN_STATS_D_AVG_SPEED = "daily_average_speed";
    String COLUMN_STATS_D_TAPS = "daily_taps";
    String COLUMN_STATS_VER = "app_version";
    String COLUMN_AUTH_CODE = "auth_code";

    String JSON_STATS_DATE = "date";
    String JSON_STATS_TZ = "timeZone";
    String JSON_STATS_H_DATA = "hourlyData";
    String JSON_STATS_H_SPEED_DATA = "hourlySpeedData";
    String JSON_STATS_D_AVG = "dailyAverage";
    String JSON_STATS_D_AVG_SPEED = "dailyAverageSpeed";
    String JSON_STATS_D_TAPS = "dailyTaps";
    String JSON_STATS_VER = "tapCounterVersion";

    String COLUMN_Q_NAME = "qName";
    String COLUMN_Q_DESCRIPTION = "qDescription";
    String COLUMN_Q_CODE = "qCode";
    String COLUMN_Q_DATE = "qDate";
    String COLUMN_Q_RESPONSE = "qResponse";
    String COLUMN_Q_BODY = "qBody";
    String COLUMN_Q_STUDY = "qStudy";
    String COLUMN_Q_FULL_ID = "qFullID";

    // DEPRECATED
    String COLUMN_PART_ID = "participation_id";
    String COLUMN_DEV_PART_ID = "deviceParticipationId";
    String FIREBASE = "firebase";
}
