package com.fri.chmelar.controller

import com.fri.chmelar.model.MontyHall
import com.fri.chmelar.model.MontyHallExperiment
import com.github.thomasnield.rxkotlinfx.observeOnFx
import com.github.thomasnield.rxkotlinfx.subscribeOnFx
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.scene.chart.XYChart
import tornadofx.*


class SimulationController : Controller() {

    private lateinit var changeDoor: MontyHall
    private lateinit var keepDoor: MontyHall

    val changeDoorData = observableArrayList<XYChart.Data<Number, Number>>()!!
    val keepDoorData = observableArrayList<XYChart.Data<Number, Number>>()!!

    val changeDoorModel = MontyHallExperimentModel()
    val keepDoorModel = MontyHallExperimentModel()

    val simulationConfigurationModel = SimulationConfigurationModel().apply {
        item = SimulationConfiguration()
    }

    val simulationRunningProperty = SimpleBooleanProperty(false)
    var simulationRunning by simulationRunningProperty

    private val simulations = mutableListOf<Disposable>()

    fun simulate() = runAsync {

        with(simulationConfigurationModel.item) {
            changeDoor = MontyHall(numberOfDoors, replicationCount, changeDecision = true)
            keepDoor   = MontyHall(numberOfDoors, replicationCount, changeDecision = false)
        }

        listOf(changeDoor.simulation(), keepDoor.simulation()).forEach { simulation ->
            simulations += simulation
                    .skip(1)
                    .observeOn(Schedulers.io())
                    .filter { it.iteration % 20 == 0 }
                    .doOnEach { Thread.sleep(30) }
                    .observeOnFx()
                    .doOnSubscribe { simulationRunning = true }
                    .doOnComplete(::finalize)
                    .subscribeOnFx()
                    .subscribe { experiment ->
                        if (experiment.changeDecision) {
                            changeDoorData.add(experiment.iteration to experiment.probabilityOfWin)
                            changeDoorModel.item = experiment
                            println(experiment)
                        } else {
                            keepDoorData.add(experiment.iteration to experiment.probabilityOfWin)
                            keepDoorModel.item = experiment
                        }

                    }


        }
    }

    fun pause() {
        println("pause start")
        keepDoor.pause()
        changeDoor.pause()
        println("pause end")
    }

    fun resume() {
        println("resume start")
        keepDoor.resume()
        changeDoor.resume()
        println("resume end")

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
        simulationRunning = false
    }

    private fun finalize() {
        simulationRunning = false
        simulations.clear()
        println("Koniec")
    }

}

data class SimulationConfiguration(val replicationCount: Int = 5000, val numberOfDoors: Int = 3)

class SimulationConfigurationModel : ItemViewModel<SimulationConfiguration>() {
    val replicationCount = bind(SimulationConfiguration::replicationCount)
    val numberOfDoors = bind(SimulationConfiguration::numberOfDoors)

    override fun onCommit() {
        item = SimulationConfiguration(replicationCount.value ?: 4000, numberOfDoors.value ?: 3)
    }
}

class MontyHallExperimentModel : ItemViewModel<MontyHallExperiment>() {
    val iteration = bind(MontyHallExperiment::iteration)
    val probabilityOfWin = bind(MontyHallExperiment::probabilityOfWin)
    val changeDecision = bind(MontyHallExperiment::changeDecision)
}

fun <A, B> ObservableList<XYChart.Data<Number, Number>>.add(p: Pair<A, B>) = with(p) { add(XYChart.Data(first as Number, second as Number)) }

fun onUi(f: () -> Unit) = Platform.runLater(f)