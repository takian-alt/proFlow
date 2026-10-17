package com.neuroflow.app.di

import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskNaturalLanguageParser
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.TaskNaturalLanguageParser
import com.neuroflow.app.domain.service.TaskSchedulerService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainServiceModule {

    @Binds
    @Singleton
    abstract fun bindCircadianEnergyService(
        impl: DefaultCircadianEnergyService
    ): CircadianEnergyService

    @Binds
    @Singleton
    abstract fun bindTaskSchedulerService(
        impl: DefaultTaskSchedulerService
    ): TaskSchedulerService

    @Binds
    @Singleton
    abstract fun bindTaskNaturalLanguageParser(
        impl: DefaultTaskNaturalLanguageParser
    ): TaskNaturalLanguageParser

    @Binds
    @Singleton
    abstract fun bindBehavioralSupportService(
        impl: DefaultBehavioralSupportService
    ): BehavioralSupportService

    @Binds
    @Singleton
    abstract fun bindCalendarSyncRepository(
        impl: com.neuroflow.app.data.calendar.CalendarSyncDataSource
    ): com.neuroflow.app.data.calendar.CalendarSyncRepository

    @Binds
    @Singleton
    abstract fun bindAndroidCalendarSyncRepository(
        impl: com.neuroflow.app.data.calendar.CalendarSyncDataSource
    ): com.neuroflow.app.data.calendar.AndroidCalendarSyncRepository

    @Binds
    @Singleton
    abstract fun bindSleepTelemetryRepository(
        impl: com.neuroflow.app.data.sleep.DefaultSleepTelemetryRepository
    ): com.neuroflow.app.data.sleep.SleepTelemetryRepository
}
