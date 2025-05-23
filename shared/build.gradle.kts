plugins {
    id("pq.java-conventions")
}

dependencies {
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    compileOnly("net.pistonmaster:pistonmotd-api:5.2.4")
    compileOnly("com.google.guava:guava:33.4.8-jre")
    implementation("org.apiguardian:apiguardian-api:1.1.0")
}
