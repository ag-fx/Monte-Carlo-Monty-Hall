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
import tornadofx.getValue
import tornadofx.setValue

class SimulationController : Controller() {

    private lateinit var changeDoor: MontyHall
    private lateinit var keepDoor: MontyHall

    val changeDoorData = observableArrayList<XYChart.Data<Number, Number>>()!!
    val keepDoorData = observableArrayList<XYChart.Data<Number, Number>>()!!

    val changeDoorModel = MontyHallExperimentModel()
    val keepDoorModel = MontyHallExperimentModel()


    val simulationConfigurationModel = SimulationConfigurationModel().apply {
        item = SimulationConfiguration(replicationCount = 100_000_000, numberOfDoors = 3)
    }

    val showKeepDoorDataProperty = SimpleBooleanProperty(false)
    var showKeepDoorData by showKeepDoorDataProperty

    val showChangeDoorDataProperty = SimpleBooleanProperty(true)
    var showChangeDoorData by showChangeDoorDataProperty

    val simulationRunningProperty = SimpleBooleanProperty(false)
    var simulationRunning by simulationRunningProperty

    val pausedProperty = SimpleBooleanProperty(false)
    var paused by pausedProperty

    val lowerBoundProperty = SimpleDoubleProperty(0.0)
    var lowerBound by lowerBoundProperty

    val tickProperty = SimpleDoubleProperty(0.1)
    var tick by tickProperty

    val upperBoundProperty = SimpleDoubleProperty(0.0)
    var upperBound by upperBoundProperty

    val simulationStateProperty = SimpleObjectProperty<SimulationState>(SimulationState.NotRunning)
    var simulationState by simulationStateProperty

    private val simulations = mutableListOf<Disposable>()

    fun simulate() = runAsync {

        with(simulationConfigurationModel.item) {
            changeDoor = MontyHall(numberOfDoors, replicationCount, MontyHallDecision.ChangeDoor)
            keepDoor = MontyHall(numberOfDoors, replicationCount, MontyHallDecision.KeepDoor)
        }


        listOf(changeDoor.simulation(), keepDoor.simulation()).forEach { simulation ->
            simulations += simulation
                    .skip(1)
                    .observeOn(Schedulers.io())
                    .filter { it.iteration % 10_000 == 0 }
                    .skip(600)
                    .observeOnFx()
                    .doOnSubscribe {
                        simulationRunning = true
                        simulationState = SimulationState.WarmingUp
                        upperBound = 100.0
                        lowerBound = 0.0
                    }
                    .doOnComplete(::finalize)
                    .subscribeOnFx()
                    .subscribe { experiment ->
                       // println(experiment)
                        simulationState = SimulationState.Running
                        when (experiment.changeDecision) {
                            MontyHallDecision.KeepDoor -> {
                                if (showKeepDoorData) {
                                    keepDoorData.add(experiment.iteration to experiment.probabilityOfWin)
                                    keepDoorModel.item = experiment
                                }

                            }

                            MontyHallDecision.ChangeDoor -> {
                                if (showChangeDoorData) {
                                    changeDoorData.add(experiment.iteration to experiment.probabilityOfWin)
                                    changeDoorModel.item = experiment
                                }
                            }
                        }

                    }
        }


    }

    fun pause() {
        keepDoor.pause()
        changeDoor.pause()
        paused = true
        simulationState = SimulationState.NotRunning

        println("pause end")
    }

    fun resume() {
        keepDoor.resume()
        changeDoor.resume()
        paused = false
        simulationState = SimulationState.Running

        println("resume end")
    }

    fun clear() {
        changeDoorData.clear()
        keepDoorData.clear()
        changeDoorModel.item = MontyHallExperiment(0, 0.0, 0.0, 0.0, MontyHallDecision.ChangeDoor)
        keepDoorModel.item = MontyHallExperiment(0, 0.0, 0.0, 0.0, MontyHallDecision.KeepDoor)
        upperBound = 100.0
        lowerBound = 0.0
        tick = 10.0
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
        simulationState = SimulationState.NotRunning

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

enum class SimulationState { NotRunning, WarmingUp, Running }

fun <A, B> ObservableList<XYChart.Data<Number, Number>>.add(p: Pair<A, B>) = with(p) { add(XYChart.Data(first as Number, second as Number)) }

fun onUi(f: () -> Unit) = Platform.runLater(f)

fun round(value: Double, places: Int): Double {
    if (places < 0) throw IllegalArgumentException()

    var bd = BigDecimal(value)
    bd = bd.setScale(places, RoundingMode.HALF_UP)
    return bd.toDouble()
}
