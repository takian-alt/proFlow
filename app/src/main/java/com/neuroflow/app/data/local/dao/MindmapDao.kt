package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.MindmapEdgeEntity
import com.neuroflow.app.data.local.entity.MindmapNodePositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MindmapDao {
    // Node Positions
    @Query("SELECT * FROM mindmap_node_positions")
    fun observeAllPositions(): Flow<List<MindmapNodePositionEntity>>

    @Query("SELECT * FROM mindmap_node_positions")
    suspend fun getAllPositions(): List<MindmapNodePositionEntity>

    @Query("SELECT * FROM mindmap_node_positions WHERE entityId = :entityId LIMIT 1")
    suspend fun getPositionByEntityId(entityId: String): MindmapNodePositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosition(position: MindmapNodePositionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPositions(positions: List<MindmapNodePositionEntity>)

    @Query("DELETE FROM mindmap_node_positions WHERE entityId = :entityId")
    suspend fun deletePositionByEntityId(entityId: String)

    // Mindmap Edges
    @Query("SELECT * FROM mindmap_edges ORDER BY createdAt ASC")
    fun observeAllEdges(): Flow<List<MindmapEdgeEntity>>

    @Query("SELECT * FROM mindmap_edges ORDER BY createdAt ASC")
    suspend fun getAllEdges(): List<MindmapEdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: MindmapEdgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<MindmapEdgeEntity>)

    @Delete
    suspend fun deleteEdge(edge: MindmapEdgeEntity)

    @Query("DELETE FROM mindmap_edges WHERE id = :edgeId")
    suspend fun deleteEdgeById(edgeId: String)

    @Query("DELETE FROM mindmap_edges WHERE sourceId = :entityId OR targetId = :entityId")
    suspend fun deleteEdgesForEntity(entityId: String)
}
