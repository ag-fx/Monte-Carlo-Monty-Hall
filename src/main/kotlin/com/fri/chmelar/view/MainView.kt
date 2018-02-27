package com.fri.chmelar.view

import com.fri.chmelar.controller.SimulationController
import javafx.geometry.Insets
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import tornadofx.*

class MainView : View("Monty Hall via Monte Carlo") {

    private val controller: SimulationController by inject()
    private val iterationAxis = NumberAxis()
    private val estimateAxis = NumberAxis(0.0, 100.0, 5.0)

    override val root = vbox {
        vboxConstraints {
            padding = Insets(20.0)
        }
        linechart(title = "", x = iterationAxis, y = estimateAxis) {
            autosize()
            series("Pravedpodonost vyhry so zmenou", controller.changeDoorData)
            series("Pravedpodonost vyhry bez zmeny", controller.keepDoorData)
            createSymbols = false
            yAxis.label = "Pravdepodbnost[%]"
            xAxis.label = "Iterácia"
        }
        spacer()
        hbox {
            textfield {
                promptText = "Počet dverí"
                bind(controller.simulationConfigurationModel.numberOfDoors)
            }
            spacer()
            textfield {
                promptText = "Počet replikácii"
                bind(controller.simulationConfigurationModel.replicationCount)
            }
            spacer()
            button("Simuluj") { action { again()             } }
            spacer()
            button("Pause ")  { action { controller.pause()  } }
            spacer()
            button("Resume ") { action { controller.resume() } }
        }
        spacer()
    }

    fun again() {
        controller.changeDoorData.clear()
        controller.keepDoorData.clear()
        controller.simulationConfigurationModel.commit()
        controller.simulate()
    }

}