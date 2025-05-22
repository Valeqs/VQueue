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

  compileOnly("com.velocitypowered:velocity-api:3.1.1")
  annotationProcessor("com.velocitypowered:velocity-api:3.1.1")
}
