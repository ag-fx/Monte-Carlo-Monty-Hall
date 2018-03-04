package com.fri.chmelar.view

import com.fri.chmelar.controller.SimulationController
import com.fri.chmelar.controller.SimulationState
import com.fri.chmelar.model.MontyHallDecision
import com.github.thomasnield.rxkotlinfx.bind
import com.sun.javafx.binding.ContentBinding.bind
import com.sun.javafx.css.converters.EnumConverter
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
    private val estimateAxis = NumberAxis(0.00, 100.0, 0.1)
    private var lineChart by singleAssign<LineChart<Number, Number>>()
    private var changeDoorSeries by singleAssign<XYChart.Series<Number, Number>>()
    private var keepDoorSeries by singleAssign<XYChart.Series<Number, Number>>()

    override val root = vbox {
        vboxConstraints {
            padding = Insets(20.0)
        }
        hbox {
            checkbox("Zmeniť dvere") {
                disableProperty().bind(controller.simulationRunningProperty)
                bind(controller.showChangeDoorDataProperty)
            }
            checkbox("Ponechať dvere") {
                disableProperty().bind(controller.simulationRunningProperty)
                bind(controller.showKeepDoorDataProperty)
            }
        }
        spacer()
        label {
            bind(controller.simulationStateProperty, converter = SimStateConverter())
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
                lowerBoundProperty().bindBidirectional(controller.lowerBoundProperty)
                upperBoundProperty().bindBidirectional(controller.upperBoundProperty)
                tickUnitProperty().bindBidirectional(controller.tickProperty)
                autoRangingProperty().bind(controller.autoRangingProperty)
            }

            with(xAxis) {
                xAxis.label = "Iterácia"
                isAutoRanging = true
            }

        }

        spacer()
        vbox {
            controls
            spacer()
            buttons
        }
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
        hbox {
            label("Počet dverí") { paddingRight = 12.0 }
            spacer()
            textfield {
                promptText = "Počet dverí"
                bind(controller.simulationConfigurationModel.numberOfDoors)
            }

        }

        spacer()
        hbox {
            label("Počet replikácii") { paddingRight = 12.0 }
            spacer()
            textfield {
                promptText = "Počet replikácii"
                bind(controller.simulationConfigurationModel.replicationCount)
            }
        }
        spacer()
    }

    private val buttons = hbox {
        paddingTop = 12.0
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

class SimStateConverter : StringConverter<SimulationState>() {
    override fun toString(`object`: SimulationState?) = `object`.toString()

    override fun fromString(string: String?) = SimulationState.values().first { string == it.name }

}