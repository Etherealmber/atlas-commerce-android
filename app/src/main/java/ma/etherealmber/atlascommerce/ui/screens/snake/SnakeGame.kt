package ma.etherealmber.atlascommerce.ui.screens.snake

import kotlin.random.Random

internal data class Cell(val x: Int, val y: Int)
internal enum class Direction(val dx: Int, val dy: Int) { UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0) }

/** Fixed grid, like a classic Snake board: the snake grows; the playfield does not. */
internal const val BOARD_SIZE = 20
internal const val CROWN_EVERY_APPLES = 5
internal const val CROWN_LIFETIME_TICKS = 42

internal data class SnakeState(
    val body: List<Cell> = listOf(Cell(8, 10), Cell(7, 10), Cell(6, 10)),
    val direction: Direction = Direction.RIGHT,
    val queued: Direction = Direction.RIGHT,
    val food: Cell = Cell(14, 10),
    val score: Int = 0,
    val crowns: Int = 0,
    val crown: Cell? = null,
    val crownTicksLeft: Int = 0,
    val wrapWalls: Boolean = false,
    val targetScore: Int? = null,
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
    val rawX = head.x + queued.dx
    val rawY = head.y + queued.dy
    if (!wrapWalls && (rawX !in 0 until BOARD_SIZE || rawY !in 0 until BOARD_SIZE)) {
        return copy(over = true)
    }
    val next = if (wrapWalls) {
        Cell((rawX + BOARD_SIZE) % BOARD_SIZE, (rawY + BOARD_SIZE) % BOARD_SIZE)
    } else {
        Cell(rawX, rawY)
    }

    val eatingFood = next == food
    val eatingCrown = next == crown
    val growing = eatingFood || eatingCrown
    val occupied = if (growing) body else body.dropLast(1)
    if (next in occupied) return copy(over = true)

    // Atlas logos add one link; the rare Etherealmber bonus logo adds two. The old tail is allowed
    // to vacate its cell on an ordinary move, matching familiar Snake rules.
    val newBody = when {
        eatingCrown -> listOf(next) + body + body.last()
        eatingFood -> listOf(next) + body
        else -> listOf(next) + body.dropLast(1)
    }
    val newScore = score + if (eatingFood) 1 else 0
    val newCrowns = crowns + if (eatingCrown) 1 else 0
    val targetReached = targetScore?.let { newScore >= it } == true

    var nextCrown = when {
        eatingCrown -> null
        crown != null && crownTicksLeft > 1 -> crown
        else -> null
    }
    var nextCrownTicks = when {
        eatingCrown -> 0
        crown != null && crownTicksLeft > 1 -> crownTicksLeft - 1
        else -> 0
    }

    val occupiedByItems = newBody.toMutableSet()
    nextCrown?.let { occupiedByItems.add(it) }
    val openCells = allCells().filterNot { it in occupiedByItems }
    val nextFood = if (eatingFood) openCells.randomOrNull(random) else food
    val foodCouldNotFit = eatingFood && nextFood == null

    if (!targetReached && !foodCouldNotFit && eatingFood && newScore % CROWN_EVERY_APPLES == 0 && nextCrown == null) {
        val crownCells = allCells().filter { it !in newBody && it != nextFood }
        nextCrown = crownCells.randomOrNull(random)
        nextCrownTicks = if (nextCrown == null) 0 else CROWN_LIFETIME_TICKS
    }

    val fullBoard = foodCouldNotFit || newBody.size >= BOARD_SIZE * BOARD_SIZE
    return copy(
        body = newBody,
        direction = queued,
        food = nextFood ?: food,
        score = newScore,
        crowns = newCrowns,
        crown = nextCrown,
        crownTicksLeft = nextCrownTicks,
        over = targetReached || fullBoard,
        won = targetReached || fullBoard,
    )
}

private fun allCells(): List<Cell> = buildList(BOARD_SIZE * BOARD_SIZE) {
    for (y in 0 until BOARD_SIZE) for (x in 0 until BOARD_SIZE) add(Cell(x, y))
}
