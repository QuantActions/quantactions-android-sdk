/*
 * Copyright (C) Quantactions, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, September 2016
 *
 */

package com.quantactions.sdktestapp.utils

import android.content.Context
import androidx.work.*
import com.quantactions.sdk.QA
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.*
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object QAModule {

    @Singleton
    @Provides
    fun provideQA(@ApplicationContext context: Context): QA {
        return QA.getInstance(context)
    }

}