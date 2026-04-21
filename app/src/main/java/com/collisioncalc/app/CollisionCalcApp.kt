package com.collisioncalc.app

import android.app.Application
import com.collisioncalc.app.data.db.CollisionCalcDatabase
import com.collisioncalc.app.data.lookups.NhtsaVinDecoder
import com.collisioncalc.app.data.lookups.VinDecoder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@HiltAndroidApp
class CollisionCalcApp : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): CollisionCalcDatabase =
        CollisionCalcDatabase.get(app)

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideVinDecoder(): VinDecoder = NhtsaVinDecoder()
}