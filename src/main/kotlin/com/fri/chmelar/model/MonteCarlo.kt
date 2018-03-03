package com.fri.chmelar.model

import hu.akarnokd.rxjava2.operators.FlowableTransformers
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import java.util.*


/**
 *  @param T class that represents a state of experiment
 */
abstract class MonteCarlo<Experiment>(private val numberOfReplications: Int) {

    abstract var isRunning: Boolean

    private val seedGenerator = Random()

    protected fun random() =  Random(seedGenerator.nextLong())

    /**
     * Flow of simulation results that can be subscribed on
     */
    fun simulation(): Flowable<Experiment> = Flowable
            .fromIterable (1..numberOfReplications)
            .observeOn    (Schedulers.newThread())
            .compose      (FlowableTransformers.valve(valve, isRunning))
            .doOnNext     { event() }
            .map          { toExperiment(it) }

    /**
     * This class represents one event that occurs in simulation that we are replicating [numberOfReplications] times
     */
    abstract fun event()

    /**
     * @param  iteration  this method will receive the current number of iteration
     * @return object of experiment i.e. data class Experiment(val iteration:Int, probability:Double)
     */
    abstract fun toExperiment(iteration: Int): Experiment

    abstract fun clear()

    fun pause() {
        isRunning = false
        valve.onNext(false)
    }

    fun resume() {
        isRunning = true
        valve.onNext(true)
    }

    private val valve = PublishProcessor.create<Boolean>()

}