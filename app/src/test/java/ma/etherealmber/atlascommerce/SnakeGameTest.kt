package ma.etherealmber.atlascommerce

import kotlin.random.Random
import ma.etherealmber.atlascommerce.ui.screens.snake.BOARD_SIZE
import ma.etherealmber.atlascommerce.ui.screens.snake.Cell
import ma.etherealmber.atlascommerce.ui.screens.snake.Direction
import ma.etherealmber.atlascommerce.ui.screens.snake.SnakeState
import ma.etherealmber.atlascommerce.ui.screens.snake.step
import ma.etherealmber.atlascommerce.ui.screens.snake.turn
import ma.etherealmber.atlascommerce.ui.components.nextSurprise
import org.junit.Assert.*
import org.junit.Test

class SnakeGameTest {
    @Test fun growthPlacesFoodOnFreeSquare() {
        val state = SnakeState(food = Cell(5, 5)).step(Random(1))
        assertEquals(4, state.body.size)
        assertEquals(1, state.score)
        assertFalse(state.food in state.body)
        assertTrue(state.food.x in 0 until BOARD_SIZE)
        assertTrue(state.food.y in 0 until BOARD_SIZE)
    }

    @Test fun edgesWrapAndSelfCollisionEndsGame() {
        val rightEdge = SnakeState(body = listOf(Cell(BOARD_SIZE - 1, 5))).step()
        assertFalse(rightEdge.over)
        assertEquals(Cell(0, 5), rightEdge.body.first())

        val leftEdge = SnakeState(body = listOf(Cell(0, 5)), direction = Direction.LEFT, queued = Direction.LEFT).step()
        assertFalse(leftEdge.over)
        assertEquals(Cell(BOARD_SIZE - 1, 5), leftEdge.body.first())

        val topEdge = SnakeState(body = listOf(Cell(5, 0)), direction = Direction.UP, queued = Direction.UP).step()
        assertFalse(topEdge.over)
        assertEquals(Cell(5, BOARD_SIZE - 1), topEdge.body.first())

        val bottomEdge = SnakeState(body = listOf(Cell(5, BOARD_SIZE - 1)), direction = Direction.DOWN, queued = Direction.DOWN).step()
        assertFalse(bottomEdge.over)
        assertEquals(Cell(5, 0), bottomEdge.body.first())

        val state = SnakeState(body = listOf(Cell(4, 4), Cell(4, 5), Cell(5, 5), Cell(5, 4), Cell(6, 4)))
        assertTrue(state.step().over)
    }

    @Test fun tailVacatingIsAllowedAndReverseIsRejected() {
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
