# PR_lifegame

A Kotlin/JVM and Swing ball life simulation.

## Run

JDK 21 is required. Gradle does not need to be installed separately; the wrapper downloads the configured Gradle version. From the project directory, run this in PowerShell:

    .\gradlew.bat run

## Controls

- Click: spawn a ball
- Space: pause or resume
- +: add a ball
- -: remove the last ball
- C: clear all balls

Balls start at a small random size, with random RGB colors and directions. Every new ball gets an independent random color with red, green, and blue channels from 0 to 255. They bounce away from the window edges and each other. Collision spawn chance scales with the population: 20% below 50 balls, 10% from 50 to 99, 5% from 100 to 169, 1% from 170 to 179, and 0% at 180 or more. When a ball reaches its death chance, it leaves the screen without bouncing and is removed only after it is fully off-screen. The chance of entering this state increases logarithmically (capped at 25%). Their colors fade, and their sizes grow. Each ball gets its own random size growth rate between 0.05 and 0.10 per second. Their speeds rise until age 50 before gradually slowing down.

## Main settings

The GameSettings object in Main.kt contains the main values to tune, including the window size, starting ball count, maximum ball count, movement speeds, and collision spawn chance.





