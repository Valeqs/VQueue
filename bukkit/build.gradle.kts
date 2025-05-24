plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation("net.pistonmaster:PistonUtils:1.4.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("net.kyori:adventure-api:4.14.0")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("com.google.guava:guava:31.1-jre")

    compileOnly ("net.luckperms:api:5.4")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.14.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.14.0")
}
