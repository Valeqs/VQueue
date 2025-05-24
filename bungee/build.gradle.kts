plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation(projects.pistonqueueShared)

    implementation("net.pistonmaster:PistonUtils:1.4.0")
    implementation("org.bstats:bstats-bungeecord:3.1.0")
  implementation("org.spongepowered:configurate-yaml:4.1.2")
  implementation("com.google.guava:guava:31.1-jre")

    compileOnly("net.md-5:bungeecord-api:1.21-R0.2")
}
