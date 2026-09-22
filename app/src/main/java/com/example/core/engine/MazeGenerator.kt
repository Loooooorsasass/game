package com.example.core.engine

import com.example.data.local.MazeLevelEntity
import kotlin.random.Random

/**
 * Dữ liệu cấu trúc logic của một màn chơi mê cung
 * Bao gồm kích thước, vị trí các bức tường (bitmask), điểm xuất phát và điểm đích.
 */
data class MazeLevelData(
    val levelId: String,
    val width: Int,
    val height: Int,
    val targetSteps: Int,
    val start: Point,
    val end: Point,
    val walls: IntArray,              // Mảng bitmask tường của từng ô (NORTH=1, EAST=2, SOUTH=4, WEST=8)
    val optimalPath: List<Point>,    // Chuỗi tọa độ đường đi tối ưu
    val seed: Long
) {
    /**
     * Lấy giá trị bitmask tường của ô (x, y)
     */
    fun cellAt(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        return walls[y * width + x]
    }

    /**
     * Kiểm tra ô (x, y) có thể đi theo hướng (dx, dy) hay không
     * Trả về true nếu không có bức tường chắn và tọa độ đích nằm trong phạm vi lưới
     */
    fun isTileWalkable(x: Int, y: Int, dx: Int, dy: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        val nx = x + dx
        val ny = y + dy
        if (nx !in 0 until width || ny !in 0 until height) return false

        val dir = when {
            dx == 1 && dy == 0 -> EAST
            dx == -1 && dy == 0 -> WEST
            dx == 0 && dy == 1 -> NORTH
            dx == 0 && dy == -1 -> SOUTH
            else -> return false
        }

        val mask = cellAt(x, y)
        return (mask and dir) != 0
    }

    /**
     * Chuyển đổi sang Maze model dùng cho engine hiển thị
     */
    fun toMaze(): Maze {
        return Maze(
            w = width,
            h = height,
            masks = walls,
            start = start,
            goal = end,
            spine = optimalPath,
            target = targetSteps,
            seedStr = seed.toString()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MazeLevelData) return false
        return levelId == other.levelId && width == other.width && height == other.height && seed == other.seed
    }

    override fun hashCode(): Int {
        var result = levelId.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + seed.hashCode()
        return result
    }
}

/**
 * Lớp MazeGenerator: Quản lý và xử lý cấu trúc logic của các màn chơi mê cung,
 * bao gồm vị trí các bức tường, điểm bắt đầu (start) và kết thúc (end) của người chơi.
 */
object MazeGenerator {

    /**
     * Tạo màn chơi theo số thứ tự level
     */
    fun generateLevel(levelNumber: Int, customSeed: Long? = null): MazeLevelData {
        val levelDef = MazeConfig.generateLevelDef(levelNumber)
        val seed = customSeed ?: (levelNumber * 1000003L + 1234567L)
        return generateFromDef(levelDef, seed)
    }

    /**
     * Tạo màn chơi với kích thước và mục tiêu tùy biến
     */
    fun generateCustom(
        levelId: String,
        width: Int,
        height: Int,
        targetSteps: Int,
        customSeed: Long? = null
    ): MazeLevelData {
        val seed = customSeed ?: Random.nextLong()
        val maze = MazeBuilder.buildMaze(
            w = width,
            h = height,
            target = targetSteps,
            seedBig = seed.toULong()
        )

        return MazeLevelData(
            levelId = levelId,
            width = width,
            height = height,
            targetSteps = targetSteps,
            start = maze.start,
            end = maze.goal,
            walls = maze.masks,
            optimalPath = maze.spine,
            seed = seed
        )
    }

    /**
     * Tạo cấu trúc màn chơi từ LevelDef
     */
    fun generateFromDef(def: LevelDef, seed: Long): MazeLevelData {
        val maze = MazeBuilder.buildMaze(
            w = def.w,
            h = def.h,
            target = def.target,
            seedBig = seed.toULong()
        )

        return MazeLevelData(
            levelId = def.id,
            width = def.w,
            height = def.h,
            targetSteps = def.target,
            start = maze.start,
            end = maze.goal,
            walls = maze.masks,
            optimalPath = maze.spine,
            seed = seed
        )
    }

    /**
     * Kiểm tra va chạm: Xem nhân vật có thể đi từ ô (fromX, fromY) sang (toX, toY) hay không
     */
    fun canMove(
        levelData: MazeLevelData,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        val dx = toX - fromX
        val dy = toY - fromY
        return levelData.isTileWalkable(fromX, fromY, dx, dy)
    }

    /**
     * Chuyển đổi đối tượng MazeLevelData thành Room Entity để lưu xuống database
     */
    fun toEntity(data: MazeLevelData): MazeLevelEntity {
        val wallsCsv = data.walls.joinToString(separator = ",")
        val pathCsv = data.optimalPath.joinToString(separator = ";") { "${it.x},${it.y}" }

        return MazeLevelEntity(
            levelId = data.levelId,
            width = data.width,
            height = data.height,
            targetSteps = data.targetSteps,
            startX = data.start.x,
            startY = data.start.y,
            endX = data.end.x,
            endY = data.end.y,
            wallsData = wallsCsv,
            optimalPath = pathCsv,
            seed = data.seed,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Phục hồi cấu trúc màn chơi từ Room Entity
     */
    fun fromEntity(entity: MazeLevelEntity): MazeLevelData {
        val wallsArray = if (entity.wallsData.isNotEmpty()) {
            entity.wallsData.split(",").map { it.trim().toIntOrNull() ?: 0 }.toIntArray()
        } else {
            IntArray(entity.width * entity.height) { 0 }
        }

        val optimalList = if (entity.optimalPath.isNotEmpty()) {
            entity.optimalPath.split(";").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size == 2) {
                    val x = parts[0].trim().toIntOrNull()
                    val y = parts[1].trim().toIntOrNull()
                    if (x != null && y != null) Point(x, y) else null
                } else null
            }
        } else {
            emptyList()
        }

        return MazeLevelData(
            levelId = entity.levelId,
            width = entity.width,
            height = entity.height,
            targetSteps = entity.targetSteps,
            start = Point(entity.startX, entity.startY),
            end = Point(entity.endX, entity.endY),
            walls = wallsArray,
            optimalPath = optimalList,
            seed = entity.seed
        )
    }
}
