package com.fri.chmelar.controller

import com.fri.chmelar.model.MontyHall
import com.fri.chmelar.model.MontyHallDecision
import com.fri.chmelar.model.MontyHallExperiment
import com.github.thomasnield.rxkotlinfx.observeOnFx
import com.github.thomasnield.rxkotlinfx.subscribeOnFx
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.scene.chart.XYChart
import tornadofx.*
import java.math.BigDecimal
import java.math.RoundingMode

class SimulationController : Controller() {

    private lateinit var changeDoor: MontyHall
    private lateinit var keepDoor: MontyHall

    val changeDoorData = observableArrayList<XYChart.Data<Number, Number>>()!!
    val keepDoorData = observableArrayList<XYChart.Data<Number, Number>>()!!

    val changeDoorModel = MontyHallExperimentModel()
    val keepDoorModel = MontyHallExperimentModel()

    val decisionProperty = SimpleObjectProperty<MontyHallDecision>(MontyHallDecision.ChangeDoor)
    var decision by decisionProperty


    val simulationConfigurationModel = SimulationConfigurationModel().apply {
        item = SimulationConfiguration(replicationCount = 100_000_000, numberOfDoors = 3)
    }

    val simulationRunningProperty = SimpleBooleanProperty(false)
    var simulationRunning by simulationRunningProperty

    val pausedProperty = SimpleBooleanProperty(false)
    var paused by pausedProperty

    val lowerBoundProperty = SimpleDoubleProperty(0.0)
    var lowerBound by lowerBoundProperty


    val upperBoundProperty = SimpleDoubleProperty(0.0)
    var upperBound by upperBoundProperty


    private val simulations = mutableListOf<Disposable>()

    // prvych 30% hodnot nebude v grafe zobrazenych.
    // zo sto milionov pokusov zobrazim 2 tisic hodnot
    fun simulate() = runAsync {

        with(simulationConfigurationModel.item) {
            changeDoor = MontyHall(numberOfDoors, replicationCount, MontyHallDecision.ChangeDoor)
            keepDoor = MontyHall(numberOfDoors, replicationCount, MontyHallDecision.KeepDoor)
        }

        listOf(changeDoor.simulation(), keepDoor.simulation()).forEach { simulation ->
            simulations += simulation
                    .skip(1)
                    .observeOn(Schedulers.io())
                    .filter {
                        it.iteration % (simulationConfigurationModel.item.replicationCount / 1000.0).toInt() == 0 &&
                                it.changeDecision == decision
                    }
                    .observeOnFx()
                    .doOnSubscribe { simulationRunning = true }
                    .doOnComplete(::finalize)
                    .subscribeOnFx()
                    .subscribe { experiment ->
                        when (experiment.changeDecision) {
                            MontyHallDecision.KeepDoor -> {
                                keepDoorData.add(experiment.iteration to experiment.probabilityOfWin)
                                keepDoorModel.item = experiment
                            }

                            MontyHallDecision.ChangeDoor -> {
                                changeDoorData.add(experiment.iteration to experiment.probabilityOfWin)
                                changeDoorModel.item = experiment
                            }
                        }

                        upperBound = (round(experiment.probabilityOfWin, 1) * 1.004)
                        lowerBound = (round(experiment.probabilityOfWin, 1) * 0.995)
                    }
        }


    }

    fun pause() {
        keepDoor.pause()
        changeDoor.pause()
        paused = true
        println("pause end")
    }

    fun resume() {
        keepDoor.resume()
        changeDoor.resume()
        paused = false
        println("resume end")
    }

    fun clear() {
        changeDoorData.clear()
        keepDoorData.clear()
        changeDoorModel.item = MontyHallExperiment(0, 0.0, 0.0, 0.0, MontyHallDecision.ChangeDoor)
        keepDoorModel.item = MontyHallExperiment(0, 0.0, 0.0, 0.0, MontyHallDecision.KeepDoor)
        println("clear")
    }

    fun stop() {
        simulations.forEach {
            try {
                it.dispose()
            } catch (e: Throwable) {
                println("Errorcek")
            }
        }
        finalize()
    }

    private fun finalize() {
        simulationRunning = false
        simulations.clear()
        println("Koniec")
    }

}

data class SimulationConfiguration(val replicationCount: Int, val numberOfDoors: Int)

class SimulationConfigurationModel : ItemViewModel<SimulationConfiguration>() {
    val replicationCount = bind(SimulationConfiguration::replicationCount)
    val numberOfDoors = bind(SimulationConfiguration::numberOfDoors)

    override fun onCommit() {
        item = SimulationConfiguration(replicationCount.value ?: 10000, numberOfDoors.value ?: 3)
    }
}

class MontyHallExperimentModel : ItemViewModel<MontyHallExperiment>() {
    val probabilityOfWin = bind(MontyHallExperiment::probabilityOfWin)
}

fun <A, B> ObservableList<XYChart.Data<Number, Number>>.add(p: Pair<A, B>) = with(p) { add(XYChart.Data(first as Number, second as Number)) }

fun onUi(f: () -> Unit) = Platform.runLater(f)

fun round(value: Double, places: Int): Double {
    if (places < 0) throw IllegalArgumentException()

    var bd = BigDecimal(value)
    bd = bd.setScale(places, RoundingMode.HALF_UP)
    return bd.toDouble()
}