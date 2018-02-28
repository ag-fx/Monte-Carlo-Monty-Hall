package com.fri.chmelar.model

import tornadofx.*
import java.util.*

class MontyHall(private val numberOfDoors: Int, numberOfIterations: Int, private val changeDecision: Boolean) : MonteCarlo<MontyHallExperiment>(numberOfIterations) {

    private var wins = 0.0
    private var loss = 0.0
    override var isRunning = true

    private fun generateDoors() = Collections
            .nCopies(numberOfDoors - 1, Door(0, Prize.Animal))
            .plus(Door(0, Prize.Car))
            .shuffled()
            .mapIndexed { index, door -> Door(index, door.prize) }

    override fun toExperiment(iteration: Int) = MontyHallExperiment(iteration, (wins / iteration) * 100, wins, loss, changeDecision)

    override fun event(iteration: Int) {
        val doors = generateDoors()
        val firstGuess = Random().nextInt(numberOfDoors)
        val openedDoor = doors
                .filter { it.index != firstGuess && it.prize != Prize.Car }
                .let { it[Random().nextInt(it.size)] }

        if (changeDecision) {
            val secondGuess = doors
                    .filter { it.index != firstGuess && it.index != openedDoor.index }
                    .let { it[Random().nextInt(it.size)] }
            if (secondGuess.prize == Prize.Car)
                wins++
            else
                loss++
        } else {
            if (doors[firstGuess].prize == Prize.Car)
                wins++
            else
                loss++
        }
    }

    override fun clear() {
        wins = 0.0
    }
}

data class MontyHallExperiment(val iteration: Int, val probabilityOfWin: Double, val wins:Double, val loss:Double ,  val changeDecision: Boolean){
    override fun equals(other: Any?) = other is MontyHallExperiment && wins==other.wins && other.loss==loss
}

data class Door(val index: Int, val prize: Prize)

enum class Prize { Car, Animal }