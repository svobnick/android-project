package com.svobnick.thisorthat.view

import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.svobnick.thisorthat.model.Question

@StateStrategyType(value = OneExecutionStateStrategy::class)
interface AnsweredQuestionsView: MvpView {

    fun setAnsweredQuestions(it: List<Question>)
    fun updateQuestions()
    fun showError(errorMsg: String)
}