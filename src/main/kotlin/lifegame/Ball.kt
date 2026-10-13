package lifegame

import java.awt.Color
import kotlin.math.max
import kotlin.math.ln
import kotlin.math.min

/** Represents one ball and updates its movement, age, size, and appearance. */
class Ball(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    val baseRadius: Double,
    val color: Color,
    val sizeScale: Double = 1.0,
    val sizeGrowthPerSecond: Double,
    var ageSeconds: Int = 0,
    val id: Long = nextId++
) {
    private var ageRemainder = 0.0
    private val initialSpeed = kotlin.math.hypot(vx, vy)

    var alive: Boolean = true
        private set

    var isLeavingScreen: Boolean = false
        private set

    val radius: Double
        get() = baseRadius * sizeScale *
            (1.0 + min(ageSeconds, GameSettings.sizeGrowthLimitSeconds) * sizeGrowthPerSecond)

    val speed: Double
        get() = kotlin.math.hypot(vx, vy)

    fun update(dt: Double, width: Int, height: Int) {
        if (!alive) return

        if (isLeavingScreen) {
            moveOutsideScreen(dt, width, height)
            return
        }

        ageRemainder += dt
        while (ageRemainder >= 1.0) {
            ageRemainder -= 1.0
            ageSeconds++

            // The death chance grows logarithmically with age and is capped.
            val deathChance = min(
                GameSettings.deathChancePerLogSecond * ln(ageSeconds + 1.0),
                GameSettings.maxDeathChance
            )
            if (Math.random() < deathChance) {
                isLeavingScreen = true
                break
            }
        }

        if (isLeavingScreen) {
            moveOutsideScreen(dt, width, height)
            return
        }

        val wantedSpeed = initialSpeed * speedFactor(ageSeconds)
        val currentSpeed = speed
        if (currentSpeed > 0.0) {
            vx *= wantedSpeed / currentSpeed
            vy *= wantedSpeed / currentSpeed
        }

        x += vx * dt
        y += vy * dt

        val r = radius
        if (x - r < 0.0) {
            x = r
            reflectFromWall(horizontalSign = 1)
        } else if (x + r > width) {
            x = width - r
            reflectFromWall(horizontalSign = -1)
        }

        if (y - r < 0.0) {
            y = r
            reflectFromWall(verticalSign = 1)
        } else if (y + r > height) {
            y = height - r
            reflectFromWall(verticalSign = -1)
        }
    }

    /** Reflect the ball away from a wall in a random direction. */
    private fun moveOutsideScreen(dt: Double, width: Int, height: Int) {
        x += vx * dt
        y += vy * dt
        val r = radius
        if (x + r < 0.0 || x - r > width || y + r < 0.0 || y - r > height) {
            alive = false
        }
    }

    private fun reflectFromWall(horizontalSign: Int = 0, verticalSign: Int = 0) {
        val angle = Math.random() * Math.PI * 2.0
        val magnitude = max(1.0, speed)
        vx = (if (horizontalSign == 0) kotlin.math.cos(angle) else horizontalSign * kotlin.math.abs(kotlin.math.cos(angle))) * magnitude
        vy = (if (verticalSign == 0) kotlin.math.sin(angle) else verticalSign * kotlin.math.abs(kotlin.math.sin(angle))) * magnitude
    }

    fun die() {
        alive = false
    }

    fun displayedColor(background: Color): Color {
        val fade = min(ageSeconds * GameSettings.colorFadePerSecond, GameSettings.maxColorFade)
        fun blend(from: Int, to: Int) =
            (from * (1.0 - fade) + to * fade).toInt().coerceIn(0, 255)

        return Color(
            blend(color.red, background.red),
            blend(color.green, background.green),
            blend(color.blue, background.blue),
            color.alpha
        )
    }

    companion object {
        private var nextId = 1L

        fun speedFactor(ageSeconds: Int): Double =
            if (ageSeconds <= GameSettings.speedPeakAgeSeconds) {
                1.0 + ageSeconds * GameSettings.speedIncreasePerSecond
            } else {
                max(
                    GameSettings.minimumSpeedFactor,
                    1.0 + GameSettings.speedPeakAgeSeconds * GameSettings.speedIncreasePerSecond -
                        (ageSeconds - GameSettings.speedPeakAgeSeconds) * GameSettings.speedDecreasePerSecond
                )
            }
    }
}





