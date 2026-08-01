package ru.artem.alaverdyan.vspmlauncher

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}