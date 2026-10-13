package com.neuroflow.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.EnergyPredictionDao
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.dao.UlyssesContractDao
import com.neuroflow.app.data.local.dao.WoopDao
import com.neuroflow.app.data.sync.CloudBackupManager
import com.neuroflow.app.data.sync.FirestoreBackupRepository
import com.neuroflow.app.data.sync.GoogleSignInManager
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideGoogleSignInManager(auth: FirebaseAuth): GoogleSignInManager =
        GoogleSignInManager(auth)

    @Provides
    @Singleton
    fun provideFirestoreBackupRepository(firestore: FirebaseFirestore): FirestoreBackupRepository =
        FirestoreBackupRepository(firestore)

    @Provides
    @Singleton
    fun provideCloudBackupManager(
        taskDao: TaskDao,
        goalDao: GoalDao,
        timeSessionDao: TimeSessionDao,
        woopDao: WoopDao,
        ulyssesContractDao: UlyssesContractDao,
        sleepLogDao: SleepLogDao,
        energyPredictionDao: EnergyPredictionDao,
        hyperFocusSessionDao: HyperFocusSessionDao,
        noteDao: NoteDao,
        firestoreRepo: FirestoreBackupRepository,
        signInManager: GoogleSignInManager,
        preferencesDataStore: UserPreferencesDataStore,
        @ApplicationContext context: Context
    ): CloudBackupManager = CloudBackupManager(
        taskDao = taskDao,
        goalDao = goalDao,
        timeSessionDao = timeSessionDao,
        woopDao = woopDao,
        ulyssesContractDao = ulyssesContractDao,
        sleepLogDao = sleepLogDao,
        energyPredictionDao = energyPredictionDao,
        hyperFocusSessionDao = hyperFocusSessionDao,
        noteDao = noteDao,
        firestoreRepo = firestoreRepo,
        signInManager = signInManager,
        preferencesDataStore = preferencesDataStore,
        context = context
    )
}
