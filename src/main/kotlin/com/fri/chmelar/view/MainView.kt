package com.fri.chmelar.view

import com.fri.chmelar.controller.SimulationController
import javafx.geometry.Insets
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.util.StringConverter
import tornadofx.*
import java.text.DecimalFormat

class Converter(val text:String) : StringConverter<Double>(){
    override fun toString(`object`: Double?) = "$text ${format(`object`)}%"

    override fun fromString(string: String?) = string?.split(" ")?.last()?.toDouble() ?: 0.0

    private fun format(double: Double?) = double?.let {DecimalFormat("#0.00").format(double)} ?: 0.00

}
class MainView : View("Monty Hall via Monte Carlo") {

    private val controller: SimulationController by inject()
    private val iterationAxis = NumberAxis()
    private val estimateAxis = NumberAxis(0.0, 100.0, 5.0)
    private var lineacher by singleAssign<LineChart<Number,Number>>()
    override val root = vbox {

        vboxConstraints {
            padding = Insets(20.0)
        }
        lineacher = linechart(x = iterationAxis, y = estimateAxis) {
            autosize()
            series("", controller.changeDoorData){
             nameProperty().bindBidirectional(controller.changeDoorModel.probabilityOfWin, Converter("Pravedpodonost vyhry so zmenou"))
            }
            series("", controller.keepDoorData){
                nameProperty().bindBidirectional(controller.keepDoorModel.probabilityOfWin, Converter("Pravedpodonost vyhry bez zmeny"))
            }
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
            button("Simuluj") {
                action { again()             }
                disableProperty().bind(controller.simulationRunningProperty)
            }
            spacer()
            button("Pause")  {
                enableWhen(controller.simulationRunningProperty)
                action { controller.pause()  }
            }
            spacer()
            button("Resume") {
                enableWhen(controller.simulationRunningProperty)
                action { controller.resume() }
            }
            spacer()
            button("Stop") {
                enableWhen(controller.simulationRunningProperty)
                action { controller.stop() }
            }
            spacer()

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

