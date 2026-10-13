package lifegame

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

/**
 * Central place for the main game settings.
 * Change these values to tune the simulation.
 */
object GameSettings {
    const val windowSizeX = 1000
    const val windowSizeY = 700
    const val startingBalls = 50
    const val maxBalls = 180

    const val minBallRadius = 10.0
    const val maxBallRadius = 17.0
    const val minStartingSpeed = 55.0
    const val maxStartingSpeed = 145.0
    const val spawnMargin = 30.0
    const val smallBallSizeScale = 0.40
    const val minSmallBallRandomScale = 0.50
    const val maxSmallBallRandomScale = 1.00

    const val timerIntervalMs = 16
    const val maxFrameDeltaSeconds = 0.05
    const val randomColorChannelMinimum = 0

    const val deathChancePerLogSecond = 0.01
    const val maxDeathChance = 0.25
    const val colorFadePerSecond = 0.006
    const val maxColorFade = 0.58

    const val sizeGrowthLimitSeconds = 20
    const val minSizeGrowthPerSecond = 0.05
    const val maxSizeGrowthPerSecond = 0.10
    const val speedPeakAgeSeconds = 50
    const val speedIncreasePerSecond = 0.018
    const val speedDecreasePerSecond = 0.012
    const val minimumSpeedFactor = 0.45

    const val noSpawnAtPopulation = 180
    const val onePercentSpawnAtPopulation = 170
    const val fivePercentSpawnAtPopulation = 160
    const val fivePercentUntilPopulation = 100
    const val tenPercentSpawnAtPopulation = 50

    val backgroundColor = Color(15, 18, 27)

    fun collisionSpawnChance(ballCount: Int): Double = when {
        ballCount >= noSpawnAtPopulation -> 0.0
        ballCount >= onePercentSpawnAtPopulation -> 0.01
        ballCount >= fivePercentSpawnAtPopulation -> 0.05
        ballCount >= fivePercentUntilPopulation -> 0.05
        ballCount >= tenPercentSpawnAtPopulation -> 0.10
        else -> 0.20
    }
}

class LifeGamePanel : JPanel() {
    private val balls = mutableListOf<Ball>()
    private val random = Random.Default
    private var paused = false
    private var lastTick = System.nanoTime()
    private var elapsed = 0.0

    init {
        preferredSize = Dimension(GameSettings.windowSizeX, GameSettings.windowSizeY)
        background = GameSettings.backgroundColor
        isFocusable = true

        repeat(GameSettings.startingBalls) {
            spawnBall(
                random.nextDouble(GameSettings.spawnMargin, GameSettings.windowSizeX - GameSettings.spawnMargin),
                random.nextDouble(GameSettings.spawnMargin, GameSettings.windowSizeY - GameSettings.spawnMargin),
                sizeScale = randomSmallBallSizeScale()
            )
        }

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                spawnBall(e.x.toDouble(), e.y.toDouble())
                requestFocusInWindow()
            }
        })

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_SPACE -> paused = !paused
                    KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS, KeyEvent.VK_ADD ->
                        spawnBall(width / 2.0, height / 2.0)
                    KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT ->
                        if (balls.isNotEmpty()) balls.removeLast()
                    KeyEvent.VK_C -> balls.clear()
                }
            }
        })

        Timer(GameSettings.timerIntervalMs) {
            val now = System.nanoTime()
            val dt = ((now - lastTick) / 1_000_000_000.0)
                .coerceIn(0.0, GameSettings.maxFrameDeltaSeconds)
            lastTick = now
            if (!paused) {
                elapsed += dt
                step(dt)
            }
            repaint()
        }.start()
    }

    private fun spawnBall(
        x: Double,
        y: Double,
        color: Color = randomColor(),
        sizeScale: Double = 1.0
    ) {
        if (balls.size >= GameSettings.maxBalls) return

        val angle = random.nextDouble(0.0, Math.PI * 2.0)
        val speed = random.nextDouble(GameSettings.minStartingSpeed, GameSettings.maxStartingSpeed)
        val radius = random.nextDouble(GameSettings.minBallRadius, GameSettings.maxBallRadius)
        val safeWidth = max(width, preferredSize.width)
        val safeHeight = max(height, preferredSize.height)

        balls += Ball(
            x = x.coerceIn(radius, max(radius, safeWidth - radius)),
            y = y.coerceIn(radius, max(radius, safeHeight - radius)),
            vx = kotlin.math.cos(angle) * speed,
            vy = kotlin.math.sin(angle) * speed,
            baseRadius = radius,
            color = color,
            sizeScale = sizeScale,
            sizeGrowthPerSecond = random.nextDouble(
                GameSettings.minSizeGrowthPerSecond,
                GameSettings.maxSizeGrowthPerSecond
            )
        )
    }

    private fun randomSmallBallSizeScale() =
        GameSettings.smallBallSizeScale *
            random.nextDouble(GameSettings.minSmallBallRandomScale, GameSettings.maxSmallBallRandomScale)

    private fun randomColor() = Color(
        random.nextInt(GameSettings.randomColorChannelMinimum, 256),
        random.nextInt(GameSettings.randomColorChannelMinimum, 256),
        random.nextInt(GameSettings.randomColorChannelMinimum, 256)
    )

    private fun step(dt: Double) {
        balls.forEach { it.update(dt, width, height) }
        balls.removeAll { !it.alive }

        for (i in 0 until balls.size) {
            for (j in i + 1 until balls.size) {
                val first = balls[i]
                val second = balls[j]
                if (first.isLeavingScreen || second.isLeavingScreen) continue
                val dx = second.x - first.x
                val dy = second.y - first.y
                val distance = hypot(dx, dy)
                val minDistance = first.radius + second.radius
                if (distance >= minDistance) continue

                val safeDistance = max(distance, 0.001)
                val nx = dx / safeDistance
                val ny = dy / safeDistance
                val overlap = minDistance - safeDistance

                // Separate overlapping balls so the same collision is not processed every frame.
                first.x -= nx * overlap * 0.5
                first.y -= ny * overlap * 0.5
                second.x += nx * overlap * 0.5
                second.y += ny * overlap * 0.5

                val relativeAlongNormal = (second.vx - first.vx) * nx + (second.vy - first.vy) * ny
                if (relativeAlongNormal < 0.0) {
                    reflectDirection(first, -nx, -ny)
                    reflectDirection(second, nx, ny)

                    if (random.nextDouble() < GameSettings.collisionSpawnChance(balls.size)) {
                        val childColor = randomColor()
                        val childScale = randomSmallBallSizeScale()
                        spawnBall(
                            (first.x + second.x) / 2.0,
                            (first.y + second.y) / 2.0,
                            childColor,
                            childScale
                        )
                    }
                }
            }
        }
    }

    private fun reflectDirection(ball: Ball, nx: Double, ny: Double) {
        val magnitude = ball.speed
        val dot = ball.vx * nx + ball.vy * ny
        if (dot < 0.0) {
            ball.vx -= 2.0 * dot * nx
            ball.vy -= 2.0 * dot * ny
            val newSpeed = hypot(ball.vx, ball.vy)
            if (newSpeed > 0.0) {
                ball.vx *= magnitude / newSpeed
                ball.vy *= magnitude / newSpeed
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        for (ball in balls) {
            val radius = ball.radius
            val diameter = radius * 2.0
            g2.color = ball.displayedColor(GameSettings.backgroundColor)
            g2.fillOval(
                (ball.x - radius).toInt(),
                (ball.y - radius).toInt(),
                diameter.toInt(),
                diameter.toInt()
            )
            g2.color = Color(255, 255, 255, 70)
            g2.stroke = BasicStroke(1.2f)
            g2.drawOval(
                (ball.x - radius).toInt(),
                (ball.y - radius).toInt(),
                diameter.toInt(),
                diameter.toInt()
            )
        }

        g2.color = Color(235, 240, 250, 220)
        g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        val status = if (paused) "PAUSED" else "RUNNING"
        g2.drawString(
            "Balls: ${balls.size}/${GameSettings.maxBalls}   Time: ${elapsed.toInt()} s   $status",
            16,
            24
        )
        g2.color = Color(185, 195, 215, 190)
        g2.drawString("Click: spawn   Space: pause   +: add   -: remove   C: clear", 16, 45)
        g2.dispose()
    }
}

private fun createWindow() {
    val frame = JFrame("LifeGame — Kotlin")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane = LifeGamePanel()
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
}

fun main() {
    SwingUtilities.invokeLater(::createWindow)
}









