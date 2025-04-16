/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quantactions.sdk.data.repository.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MVPRoomDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate9To10() {
        var db = helper.createDatabase(TEST_DB, 9).apply {
            close()
        }
        db = helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
    }

    @Test
    @Throws(IOException::class)
    fun migrate10To11() {
        var db = helper.createDatabase(TEST_DB, 10).apply {
            close()
        }
        db = helper.runMigrationsAndValidate(TEST_DB, 11, true, MIGRATION_10_11)
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        var db = helper.createDatabase(TEST_DB, 7).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
//            execSQL(...)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        var db = helper.createDatabase(TEST_DB, 8).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
//            execSQL(...)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Test
    @Throws(IOException::class)
    fun migrate6To8() {
        var db = helper.createDatabase(TEST_DB, 6).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
//            execSQL(...)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_6_8)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        var db = helper.createDatabase(TEST_DB, 6).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
//            execSQL(...)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(TEST_DB, 3).apply {
            close()
        }
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To7() {
        helper.createDatabase(TEST_DB, 4).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_4_7)
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To7() {
        helper.createDatabase(TEST_DB, 3).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_3_7)
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To8() {
        helper.createDatabase(TEST_DB, 3).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_3_8)
    }
    @Test
    @Throws(IOException::class)
    fun migrate4To8() {
        helper.createDatabase(TEST_DB, 4).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_4_8)
    }
}