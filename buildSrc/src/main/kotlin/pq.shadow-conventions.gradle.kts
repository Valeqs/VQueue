import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("pq.java-conventions")
    id("com.gradleup.shadow")
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        exclude("META-INF/SPONGEPO.SF", "META-INF/SPONGEPO.DSA", "META-INF/SPONGEPO.RSA")
        minimize()
        configureRelocations()
    }

    build {
        dependsOn(shadowJar)
    }
}

fun ShadowJar.configureRelocations() {
    relocate("org.bstats", "net.pistonmaster.pistonqueue.shadow.bstats")
    relocate("org.spongepowered.configurate", "net.pistonmaster.pistonqueue.shadow.configurate")
    relocate("io.leangen.geantyref", "net.pistonmaster.pistonqueue.shadow.geantyref")
    relocate("org.checkerframework", "net.pistonmaster.pistonqueue.shadow.checkerframework")
    relocate("org.yaml.snakeyaml", "net.pistonmaster.pistonqueue.shadow.snakeyaml")
    relocate("com.google.errorprone", "net.pistonmaster.pistonqueue.shadow.google.errorprone")
    relocate("com.google.gson", "net.pistonmaster.pistonqueue.shadow.gson")
    relocate("org.jetbrains.annotations", "net.pistonmaster.pistonqueue.shadow.annotations.jetbrains")
    relocate("net.kyori.option", "net.pistonmaster.pistonqueue.shadow.kyori.option")
    relocate("net.pistonmaster.pistonutils", "net.pistonmaster.pistonqueue.shadow.pistonutils")
}
