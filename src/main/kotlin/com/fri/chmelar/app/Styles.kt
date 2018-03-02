package com.fri.chmelar.app

import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class Styles : Stylesheet() {
    companion object {
        val heading by cssclass()
    }

    init {
        importStylesheet("bootstrapfx.css")
        label and heading {
            padding  = box(10.px)
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
        }
        root {
            prefWidth = 600.px
            prefHeight = 400.px
        }
    }
}