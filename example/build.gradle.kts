plugins { kotlin("jvm"); application }
kotlin { jvmToolchain(21) }
dependencies { implementation(project(":")) }
application { mainClass.set("MainKt") }
