package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.MazeBuilder
import com.example.core.engine.MazeConfig
import com.example.core.engine.MazeSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Exact-Step Maze", appName)
  }

  @Test
  fun `test deterministic maze generation`() {
    val seed = 123456789UL
    val maze1 = MazeBuilder.buildMaze(w = 5, h = 5, target = 8, seedBig = seed)
    val maze2 = MazeBuilder.buildMaze(w = 5, h = 5, target = 8, seedBig = seed)

    assertEquals(maze1.start, maze2.start)
    assertEquals(maze1.goal, maze2.goal)
    assertEquals(maze1.target, 8)

    val dist = MazeSolver.computeDistances(maze1)
    val goalDist = dist[maze1.goal.y * maze1.w + maze1.goal.x]
    assertEquals(8, goalDist)
  }
}
