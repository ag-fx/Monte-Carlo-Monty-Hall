package com.fri.chmelar.app

import com.fri.chmelar.view.MainView
import com.jfoenix.controls.JFXDecorator
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.App

class MyApp: App(MainView::class, Styles::class)