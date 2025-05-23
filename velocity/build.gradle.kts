plugins {
  id("pq.platform-conventions")
}

dependencies {
  implementation(projects.pistonqueueShared)
  compileOnly(projects.pistonqueueBuildData)

  implementation("net.pistonmaster:PistonUtils:1.4.0")
  implementation("org.bstats:bstats-velocity:3.1.0")

  // ← hier hinzugefügt:
  implementation("net.kyori:adventure-api:4.14.0")
  implementation("net.kyori:adventure-text-minimessage:4.14.0")

  implementation("com.velocitypowered:velocity-api:3.1.1")
  annotationProcessor("com.velocitypowered:velocity-api:3.1.1")

  compileOnly("net.luckperms:api:5.4")                     // oder deine LP-Version
  implementation("org.spongepowered:configurate-yaml:4.1.2")
  implementation("com.google.guava:guava:31.1-jre")
  implementation("net.kyori:adventure-api:4.14.0")
  implementation("net.kyori:adventure-text-minimessage:4.14.0")
}
