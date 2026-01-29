package com.etraksolutions.speedsign.di

import com.etraksolutions.speedsign.data.detection.SpeedSignDetector
import com.etraksolutions.speedsign.data.repository.DetectionRepositoryImpl
import com.etraksolutions.speedsign.domain.repository.DetectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the SpeedSignDetector instance.
     */
    @Provides
    @Singleton
    fun provideSpeedSignDetector(): SpeedSignDetector {
        return SpeedSignDetector()
    }

    /**
     * Provides the DetectionRepository implementation.
     */
    @Provides
    @Singleton
    fun provideDetectionRepository(
        speedSignDetector: SpeedSignDetector
    ): DetectionRepository {
        return DetectionRepositoryImpl(speedSignDetector)
    }
}
