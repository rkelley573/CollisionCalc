package com.collisioncalc.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CaseRow::class,
        VehicleRow::class,
        OccupantRow::class,
        UnitRow::class,
        NoteRow::class,
        CalcRow::class,
        CalcValueRow::class,
        CalcStepRow::class,
        CalcAttribUnitRow::class,
        CalcAttribVehicleRow::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CollisionCalcDatabase : RoomDatabase() {

    abstract fun caseDao(): CaseDao

    companion object {
        @Volatile private var INSTANCE: CollisionCalcDatabase? = null

        fun get(context: Context): CollisionCalcDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CollisionCalcDatabase::class.java,
                    "collisioncalc.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}