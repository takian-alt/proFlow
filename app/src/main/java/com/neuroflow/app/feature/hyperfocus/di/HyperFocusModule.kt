package com.neuroflow.app.feature.hyperfocus.di

import android.content.Context
import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStoreImpl
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusSessionDao
import com.neuroflow.app.feature.hyperfocus.data.UnlockCodeDao
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManagerImpl
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HyperFocusModule {

    @Binds
    @Singleton
    abstract fun bindUnlockCodeRepository(impl: UnlockCodeRepositoryImpl): UnlockCodeRepository

    @Binds
    @Singleton
    abstract fun bindHyperFocusManager(impl: HyperFocusManagerImpl): HyperFocusManager

    companion object {
        @Provides
        @Singleton
        fun provideHyperFocusDataStore(@ApplicationContext context: Context): HyperFocusDataStore =
            HyperFocusDataStoreImpl(context)

        @Provides
        fun provideUnlockCodeDao(database: NeuroFlowDatabase): UnlockCodeDao = database.unlockCodeDao()

        @Provides
        fun provideHyperFocusSessionDao(database: NeuroFlowDatabase): HyperFocusSessionDao =
            database.hyperFocusSessionDao()
    }
}
