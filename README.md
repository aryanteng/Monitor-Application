# Step Up: indoor walking tracker (Android)

A Kotlin Android app (2023) that tracks a walk with only the phone's own sensors, with no GPS needed indoors.

- **Steps:** detected from peaks in the accelerometer signal, with a threshold and a low-pass filter to reject jitter.
- **Heading:** the azimuth, from the accelerometer and magnetometer together.
- **Trajectory:** each step advances the position by a stride length along the heading, and the path is drawn live on a canvas along with the distance walked.
- **Stairs or lift:** vertical acceleration tells whether you're taking the stairs or a lift, with a debounced on-screen notice.

Open the project in Android Studio and run it on a physical device (emulators have no real accelerometer or magnetometer). Archived: kept for reference, not maintained.
