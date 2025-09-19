/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.Charset


object SQLCipherUtils {
    /**
     * Determine whether or not this database appears to be encrypted, based
     * on whether we can open it without a passphrase.
     *
     * @param context a Context
     * @param dbName the name of the database, as used with Room, SQLiteOpenHelper,
     * etc.
     * @return the detected state of the database
     */
    fun getDatabaseState(
        context: Context,
        dbName: String?
    ): State {
        System.loadLibrary("sqlcipher")
        return getDatabaseState(context.getDatabasePath(dbName))
    }

    /**
     * Determine whether or not this database appears to be encrypted, based
     * on whether we can open it without a passphrase.
     *
     * NOTE: You are responsible for ensuring that net.sqlcipher.database.SQLiteDatabase.loadLibs()
     * is called before calling this method. This is handled automatically with the
     * getDatabaseState() method that takes a Context as a parameter.
     *
     * @param dbPath a File pointing to the database
     * @return the detected state of the database
     */
    fun getDatabaseState(dbPath: File): State {
        if (dbPath.exists()) {
            var db: SQLiteDatabase? = null
            return try {
                db = SQLiteDatabase.openDatabase(
                    dbPath.absolutePath,
                    null, SQLiteDatabase.OPEN_READONLY
                )
                db.version
                State.UNENCRYPTED
            } catch (_: Exception) {
                State.ENCRYPTED
            } finally {
                db?.close()
            }
        }
        return State.DOES_NOT_EXIST
    }
    
    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     *
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param context a Context
     * @param dbName the name of the database, as used with Room, SQLiteOpenHelper,
     * etc.
     * @param passphrase the passphrase from the user
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encrypt(
        context: Context,
        dbName: String?,
        passphrase: String?
    ) {
        encrypt(
            context,
            context.getDatabasePath(dbName),
            passphrase?.toByteArray(Charset.forName("UTF-8"))
        )
    }

    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     *
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param context a Context
     * @param originalFile a File pointing to the database
     * @param passphrase the passphrase from the user
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encrypt(
        context: Context,
        originalFile: File,
        passphrase: ByteArray?
    ) {
        System.loadLibrary("sqlcipher")
        if (originalFile.exists()) {
            val newFile = File.createTempFile(
                "sqlcipherutils", "tmp",
                context.cacheDir
            )
            var db =
                SQLiteDatabase.openDatabase(
                    originalFile.absolutePath,
                    null, SQLiteDatabase.OPEN_READWRITE
                )
            val version = db.version
            db.close()
            db = SQLiteDatabase.openDatabase(
                newFile.absolutePath, passphrase,
                null, SQLiteDatabase.OPEN_READWRITE, null, null
            )
            val st =
                db.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''")
            st.bindString(1, originalFile.absolutePath)
            st.execute()
            db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            db.rawExecSQL("DETACH DATABASE plaintext")
            db.version = version
            st.close()
            db.close()
            originalFile.delete()
            newFile.renameTo(originalFile)
        } else {
            throw FileNotFoundException(originalFile.absolutePath + " not found")
        }
    }

    /**
     * The detected state of the database, based on whether we can open it
     * without a passphrase.
     */
    enum class State {
        DOES_NOT_EXIST, UNENCRYPTED, ENCRYPTED
    }
}