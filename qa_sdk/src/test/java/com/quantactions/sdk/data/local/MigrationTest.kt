/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MVPRoomDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate12To13() {
        // Create the database in version 12
        var db = helper.createDatabase(TEST_DB, 12)
        
        // Prepare some data
        db.execSQL("INSERT INTO code_of_app (id, app_name, sync) VALUES (1, 'com.example.app', 0)")
        db.close()

        // Re-open the database with version 13 and provide MIGRATION_12_13
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            13,
            true,
            MIGRATION_12_13
        )
        
        // Verify migration
        val cursor = db.query("SELECT * FROM code_of_app WHERE id = 1")
        assert(cursor.moveToFirst())
        val categoryIndex = cursor.getColumnIndex("category")
        assert(categoryIndex != -1) { "Column 'category' should exist in version 13" }
        assert(cursor.getString(categoryIndex) == null) { "New category column should be null by default" }
        cursor.close()
    }
}
