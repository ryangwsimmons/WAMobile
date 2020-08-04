package io.github.ryangwsimmons.wamobile

data class DropdownOption(val name: String, val value: String) {
    override fun toString(): String {
        return this.name
    }
}