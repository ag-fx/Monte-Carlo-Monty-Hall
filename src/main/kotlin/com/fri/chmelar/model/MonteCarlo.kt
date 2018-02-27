package com.fri.chmelar.model

import hu.akarnokd.rxjava2.operators.FlowableTransformers
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.processors.PublishProcessor


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
abstract class MonteCarlo<T>(private val numberOfReplications:Int) {

    private val valve =  PublishProcessor.create<Boolean>()

    fun pause()  = valve.onNext(false)

    fun resume() = valve.onNext(true)


    fun simulation(): Flowable<T> = Flowable
            .range      (0, numberOfReplications)
            .compose    (FlowableTransformers.valve(valve, true))
            .observeOn  (Schedulers.newThread())
            .doOnNext   { event(it)  }
            .map        { toExperiment(it) }

    abstract fun event(iteration: Int)

    abstract fun toExperiment(iteration: Int): T

    abstract fun clear()

}