package ma.etherealmber.atlascommerce

import kotlin.random.Random
import ma.etherealmber.atlascommerce.ui.screens.snake.BOARD_SIZE
import ma.etherealmber.atlascommerce.ui.screens.snake.CROWN_EVERY_APPLES
import ma.etherealmber.atlascommerce.ui.screens.snake.Cell
import ma.etherealmber.atlascommerce.ui.screens.snake.Direction
import ma.etherealmber.atlascommerce.ui.screens.snake.SnakeState
import ma.etherealmber.atlascommerce.ui.screens.snake.step
import ma.etherealmber.atlascommerce.ui.screens.snake.turn
import ma.etherealmber.atlascommerce.ui.components.nextSurprise
import org.junit.Assert.*
import org.junit.Test

class SnakeGameTest {
    @Test fun applesGrowSnakeAndPlaceNextAppleOnFreeSquare() {
        val state = SnakeState(food = Cell(9, 10)).step(Random(1))
        assertEquals(4, state.body.size)
        assertEquals(1, state.score)
        assertFalse(state.food in state.body)
        assertTrue(state.food.x in 0 until BOARD_SIZE)
        assertTrue(state.food.y in 0 until BOARD_SIZE)
    }

    @Test fun crownAppearsAfterFiveApplesAndIsAVisibleBonusCollectible() {
        val state = SnakeState(food = Cell(9, 10), score = CROWN_EVERY_APPLES - 1).step(Random(7))
        assertEquals(CROWN_EVERY_APPLES, state.score)
        assertNotNull(state.crown)
        assertFalse(state.crown!! in state.body)
        assertNotEquals(state.food, state.crown)
        assertTrue(state.crownTicksLeft > 0)

        val crownRun = SnakeState(
            body = listOf(Cell(4, 5), Cell(3, 5), Cell(2, 5)),
            food = Cell(10, 10),
            crown = Cell(5, 5),
            crownTicksLeft = 10,
        ).step(Random(1))
        assertEquals(1, crownRun.crowns)
        assertEquals(5, crownRun.body.size)
        assertNull(crownRun.crown)
    }

    @Test fun crownExpiresAndDailyTargetCompletesRun() {
        val expired = SnakeState(
            body = listOf(Cell(4, 5)),
            food = Cell(10, 10),
            crown = Cell(9, 9),
            crownTicksLeft = 1,
        ).step(Random(3))
        assertNull(expired.crown)
        assertEquals(0, expired.crownTicksLeft)

        val completed = SnakeState(food = Cell(9, 10), score = 2, targetScore = 3).step(Random(1))
        assertTrue(completed.over)
        assertTrue(completed.won)
        assertEquals(3, completed.score)
    }

    @Test fun wallsEndRunUnlessWrapModeIsEnabledAndSelfCollisionEndsRun() {
        val rightEdge = SnakeState(body = listOf(Cell(BOARD_SIZE - 1, 5))).step()
        assertTrue(rightEdge.over)
        assertFalse(rightEdge.won)

        val wrapped = SnakeState(
            body = listOf(Cell(BOARD_SIZE - 1, 5)),
            wrapWalls = true,
        ).step()
        assertFalse(wrapped.over)
        assertEquals(Cell(0, 5), wrapped.body.first())

        val state = SnakeState(body = listOf(Cell(4, 4), Cell(4, 5), Cell(5, 5), Cell(5, 4), Cell(6, 4)))
        assertTrue(state.step().over)
    }

    @Test fun tailMayVacateAndReverseTurnsAreRejected() {
        val state = SnakeState(body = listOf(Cell(4, 4), Cell(4, 5), Cell(5, 5), Cell(5, 4)))
        assertFalse(state.step().over)
        assertEquals(Direction.RIGHT, SnakeState().turn(Direction.LEFT).queued)
        assertEquals(Direction.UP, SnakeState().turn(Direction.UP).turn(Direction.LEFT).queued)
    }

    @Test fun surprisesDoNotRepeatImmediately() {
        val random = Random(1)
        repeat(50) { assertNotEquals(2, nextSurprise(2, 5, random)) }
        assertEquals(0, nextSurprise(0, 1, random))
    }
}
