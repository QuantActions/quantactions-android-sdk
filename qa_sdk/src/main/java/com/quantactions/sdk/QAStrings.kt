/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

/**
 * Various codes for QA SDK. Some functions of the SDK return these flags for you information.
 * @suppress
 * Created By Enea Ceolini 11/2018
 * Contact: enea.ceolini@quantactions.com
 */

interface QAStrings {
    companion object {

        const val QA_FOREGROUND_SERVICE_ID = 67676767

        /** The permission has already been granted */
        const val QA_ALREADY_GRANTED = 776

        /** Code for requested of usage permission  */
        const val QA_PERMISSION_REQUEST_USAGE = 777

        /** Code for requested of overlay permission */
        const val QA_PERMISSION_REQUEST_OVERLAY = 778

        /** Code for requested of Location permission */
        const val QA_PERMISSION_REQUEST_GPS = 779

        /***/
        const val NOTIFY_DRAW = 227777

        /***/
        const val NOTIFY_USAGE = 337777

        /***/
        const val NOTIFY_VANILLA = 44777

        /***/
        const val NOTIFY_UPDATE = 44788

        /***/
        const val NOTIFY_QUESTIONNAIRE = 45988

        const val QA_REQUEST = "qa_request"

        const val QA_ERROR = "error"

        const val STUDY_ID = "STUDY_ID"
        const val PERM_APP_ID = "PERMISSION_APP_ID"
        const val PERM_DRAW = "PERMISSION_DRAW_OVER"
        const val PERM_LOC = "PERMISSION LOCATION"
        const val PERM_CONTACT = "PERMISSION CONTACTS"
        const val DEVICE_ID = "DEVICE_ID"
        const val STUDY_TITLE = "STUDY_ID"
        const val CAN_WITHDRAW = "CAN_WITHDRAW"
        const val PRIVACY_POLICY = "PRIVACY_POLICY"
        const val DEV_PART_ID = "DEVICE_PARTICIPATION_ID"
        const val PART_ID = "PARTICIPATION_ID"

        const val VERBOSE_LOW = 100
        const val VERBOSE_MEDIUM = 200
        const val VERBOSE_HIGH = 300

    }

}
