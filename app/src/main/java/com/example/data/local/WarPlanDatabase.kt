package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ManualMarker
import com.example.model.TacticalAnalysisResult
import com.example.model.TacticalJsonCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Entity(tableName = "war_plans")
data class WarPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val baseTitle: String,
    val townHall: String,
    val strategyName: String,
    val efficiencyScore: Int,
    val predictedStars: Int,
    val imagePath: String,
    val analysisJson: String,
    val markersJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface WarPlanDao {
    @Query("SELECT * FROM war_plans ORDER BY createdAt DESC")
    fun getAllWarPlans(): Flow<List<WarPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarPlan(plan: WarPlanEntity): Long

    @Query("DELETE FROM war_plans WHERE id = :id")
    suspend fun deleteWarPlanById(id: Long)
}

@Database(entities = [WarPlanEntity::class], version = 1, exportSchema = false)
abstract class WarPlanDatabase : RoomDatabase() {
    abstract fun warPlanDao(): WarPlanDao

    companion object {
        @Volatile
        private var INSTANCE: WarPlanDatabase? = null

        fun getInstance(context: Context): WarPlanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WarPlanDatabase::class.java,
                    "warstrat_plans.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class WarPlanRepository(
    private val context: Context,
    private val dao: WarPlanDao
) {
    val allPlans: Flow<List<WarPlanEntity>> = dao.getAllWarPlans()

    suspend fun savePlan(
        bitmap: Bitmap,
        result: TacticalAnalysisResult,
        markers: List<ManualMarker>
    ): Long {
        val savedPath = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "base_captures").apply { mkdirs() }
            val file = File(dir, "base_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            file.absolutePath
        }
        val entity = WarPlanEntity(
            baseTitle = result.baseArchetype,
            townHall = result.estimatedTownHall,
            strategyName = result.recommendedStrategyName,
            efficiencyScore = result.efficiencyScore,
            predictedStars = result.predictedStars,
            imagePath = savedPath,
            analysisJson = TacticalJsonCodec.encodeAnalysisResult(result),
            markersJson = TacticalJsonCodec.encodeManualMarkers(markers)
        )
        return dao.insertWarPlan(entity)
    }

    suspend fun loadBitmap(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun deletePlan(plan: WarPlanEntity) {
        withContext(Dispatchers.IO) {
            runCatching { File(plan.imagePath).delete() }
        }
        dao.deleteWarPlanById(plan.id)
    }
}
