val lwjglVersion: String by project.extra
val lwjglTarget: String by project.extra
val jomlVersion: String by project.extra
val gsonVersion: String by project.extra

dependencies {
    // LWJGL Core
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    // HIER KORRIGIERT: "natives-$lwjglTarget" statt nur "$lwjglTarget"
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-$lwjglTarget")

    // GLFW (Fenster & Input)
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-$lwjglTarget")

    // OpenGL (Grafik)
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-$lwjglTarget")

    // OpenAL (Audio-Thread)
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-$lwjglTarget")

    // Mathematik & JSON-Serialisierung
    implementation("org.joml:joml:$jomlVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
}