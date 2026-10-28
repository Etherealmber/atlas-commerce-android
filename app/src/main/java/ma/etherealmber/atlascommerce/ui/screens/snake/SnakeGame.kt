package ma.etherealmber.atlascommerce.ui.screens.snake

import kotlin.random.Random

internal data class Cell(val x: Int, val y: Int)
internal enum class Direction(val dx: Int, val dy: Int) { UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0) }
internal const val BOARD_SIZE = 12
internal data class SnakeState(
    val body: List<Cell> = listOf(Cell(4, 5), Cell(3, 5), Cell(2, 5)),
    val direction: Direction = Direction.RIGHT,
    val queued: Direction = Direction.RIGHT,
    val food: Cell = Cell(8, 5),
    val score: Int = 0,
    val over: Boolean = false,
    val won: Boolean = false,
)
internal fun SnakeState.turn(next: Direction): SnakeState {
    if (over || queued != direction || (next.dx == -direction.dx && next.dy == -direction.dy)) return this
    return copy(queued = next)
}
internal fun SnakeState.step(random: Random = Random.Default): SnakeState {
    if (over) return this
    val head = body.first()
    val next = Cell(head.x + queued.dx, head.y + queued.dy)
    val eating = next == food
    val occupied = if (eating) body else body.dropLast(1)
    if (next.x !in 0 until BOARD_SIZE || next.y !in 0 until BOARD_SIZE || next in occupied) return copy(over = true)
    val newBody = listOf(next) + if (eating) body else body.dropLast(1)
    if (!eating) return copy(body = newBody, direction = queued)
    val free = (0 until BOARD_SIZE).flatMap { y -> (0 until BOARD_SIZE).map { x -> Cell(x, y) } }.filterNot { it in newBody }
    return if (free.isEmpty()) copy(body = newBody, direction = queued, score = score + 1, over = true, won = true)
    else copy(body = newBody, direction = queued, food = free[random.nextInt(free.size)], score = score + 1)
}
