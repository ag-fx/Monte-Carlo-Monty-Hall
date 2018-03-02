package com.fri.chmelar.view

import com.fri.chmelar.controller.SimulationController
import com.fri.chmelar.model.MontyHallDecision
import javafx.geometry.Insets
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.ScatterChart
import javafx.scene.chart.XYChart
import javafx.util.StringConverter
import tornadofx.*
import java.text.DecimalFormat
import java.util.*


class MainView : View("Monty Hall via Monte Carlo") {

    private val controller: SimulationController by inject()

    private val iterationAxis = NumberAxis()
    private val estimateAxis  = NumberAxis(0.00,100.0,0.1)
    private var lineChart        by singleAssign<LineChart<Number, Number>>()
    private var changeDoorSeries by singleAssign<XYChart.Series<Number, Number>>()
    private var keepDoorSeries   by singleAssign<XYChart.Series<Number, Number>>()

    override val root = vbox {
        vboxConstraints {
            padding = Insets(20.0)
        }
        hbox{
            togglegroup {
                MontyHallDecision.values().forEach {
                    radiobutton(it.name, value = it) {
                        disableProperty().bind(controller.simulationRunningProperty)
                    }
                }
                bind(controller.decisionProperty)
            }
        }

        lineChart = linechart(x = iterationAxis, y = estimateAxis) {
            this.autosize()
            changeDoorSeries = series("", controller.changeDoorData) {
                nameProperty().bindBidirectional(controller.changeDoorModel.probabilityOfWin, Converter("Pravedpodonost vyhry so zmenou"))
            }

            keepDoorSeries = series("", controller.keepDoorData) {
                nameProperty().bindBidirectional(controller.keepDoorModel.probabilityOfWin, Converter("Pravedpodonost vyhry bez zmeny"))

            }

            createSymbols = false
            with(yAxis as NumberAxis) {
                label = "Pravdepodbnost[%]"
             //   isAutoRanging = true
                lowerBoundProperty().bindBidirectional(controller.lowerBoundProperty)
                upperBoundProperty().bindBidirectional(controller.upperBoundProperty)
             //   tickLabelFormatter = DecimalTickConverter()
            }

            with(xAxis) {
                xAxis.label = "Iterácia"
                isAutoRanging = true
            }

        }

        spacer()
        controls
        spacer()
    }

    fun pause() {
        lineChart.animated = false
        controller.pause()
    }

    fun stop() {
        lineChart.animated = false
        controller.stop()
    }

    fun clearPlot() {
        controller.clear()
        with(lineChart) {
            animated = false
            data.clear()
            animated = true
            data.add(keepDoorSeries)
            data.add(changeDoorSeries)
        }
    }

    fun again() {
        clearPlot()
        controller.simulationConfigurationModel.commit()
        controller.simulate()
    }

    private val controls = hbox {
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
            action { again() }
            disableProperty().bind(controller.simulationRunningProperty)
        }
        spacer()
        button("Pause") {
            enableWhen(controller.simulationRunningProperty)
            action(::pause)
        }
        spacer()
        button("Resume") {
            enableWhen(controller.pausedProperty)
            action { controller.resume() }
        }
        spacer()
        button("Stop") {
            enableWhen(controller.simulationRunningProperty)
            action(::stop)
        }
        spacer()
        button("Clear") {
            action(::clearPlot)
            disableProperty().bind(controller.simulationRunningProperty)
        }

    }

}


class Converter(val text: String) : StringConverter<Double>() {
    override fun toString(`object`: Double?) = "$text ${format(`object`)}%"

    override fun fromString(string: String?) = string?.split(" ")?.last()?.toDouble() ?: 0.000

    private fun format(double: Double?) = double?.let { DecimalFormat("#0.0000").format(double) } ?: 0.0000
}
