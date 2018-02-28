package com.fri.chmelar.model

import hu.akarnokd.rxjava2.operators.FlowableTransformers
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers


/**
@param T
class that represents a state of experiment
@param toExperiment
function that returns an experiment object
@param event
represents an event in simulation

Every simulation runs on new thread thanks to Rx schedulesres
in event function you'll get an iteration number everytime it's called

toExperiment function has to return T as a result of one exper
 */
abstract class MonteCarlo<T>(private val numberOfReplications: Int) {

    private val valve = PublishProcessor.create<Boolean>()
    abstract var isRunning: Boolean

    fun pause() {
        isRunning = false
        valve.onNext(false)
    }

    fun resume() {
        isRunning = true
        valve.onNext(true)
    }

    fun simulation(): Flowable<T> = Flowable
            .fromIterable (1..numberOfReplications)
            .observeOn    (Schedulers.newThread())
            .compose      (FlowableTransformers.valve(valve, isRunning))
            .doOnNext     { event(it) }
            .map          { toExperiment(it) }

    abstract fun event(iteration: Int)

    abstract fun toExperiment(iteration: Int): T

    abstract fun clear()

}

