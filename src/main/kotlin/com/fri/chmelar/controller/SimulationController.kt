package com.fri.chmelar.controller

import com.fri.chmelar.model.MontyHall
import com.github.thomasnield.rxkotlinfx.subscribeOnFx
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.scene.chart.XYChart
import tornadofx.*


class SimulationController : Controller() {

    val changeDoorData = observableArrayList<XYChart.Data<Number, Number>>()!!
    val keepDoorData   = observableArrayList<XYChart.Data<Number, Number>>()!!
    val simulationConfigurationModel = SimulationConfigurationModel()

    private val changeDoor by lazy {
        val conf = simulationConfigurationModel.item
        MontyHall(conf.numberOfDoors, conf.replicationCount, changeDecision = true)
    }

    private val keepDoor by lazy {
        val conf = simulationConfigurationModel.item
        MontyHall(conf.numberOfDoors, conf.replicationCount, changeDecision = false)
    }

    fun simulate() = runAsync {
        changeDoor.clear()
        keepDoor.clear()
        keepDoorData.clear()
        changeDoorData.clear()
        listOf(changeDoor.simulation(), keepDoor.simulation()).forEach { simulation ->
            simulation
                    .subscribeOnFx()
                    .skip(1)
                    .filter { it.iteration % 10 == 0 }
                    .doOnEach { Thread.sleep(100) }
                    .doOnComplete { println("KONIEEEC") }
                    .subscribe { experiment ->
                        println(experiment)
                        ui {
                            if (experiment.changeDecision)
                                changeDoorData.add(experiment.iteration to experiment.probabilityOfWin)
                            else
                                keepDoorData  .add(experiment.iteration to experiment.probabilityOfWin)
                        }
                    }
        }
    }


    fun pause() {
        keepDoor.pause()
        changeDoor.pause()
    }

    fun resume() {
        keepDoor.resume()
        changeDoor.resume()
    }
}

data class SimulationConfiguration(val replicationCount: Int = 4000, val numberOfDoors: Int = 4)

class SimulationConfigurationModel : ItemViewModel<SimulationConfiguration>() {
    val replicationCount = bind(SimulationConfiguration::replicationCount)
    val numberOfDoors = bind(SimulationConfiguration::numberOfDoors)
    override fun onCommit() {
        item = SimulationConfiguration(replicationCount.value ?: 4000, numberOfDoors.value ?: 3)
    }
}

fun <A, B> ObservableList<XYChart.Data<Number, Number>>.add(p: Pair<A, B>) = with(p) { add(XYChart.Data(first as Number, second as Number)) }