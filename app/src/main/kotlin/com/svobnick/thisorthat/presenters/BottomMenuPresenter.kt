package com.svobnick.thisorthat.presenters

import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.svobnick.thisorthat.app.ThisOrThatApp
import com.svobnick.thisorthat.service.BottomMenuState
import com.svobnick.thisorthat.view.BottomMenuView
import javax.inject.Inject

@InjectViewState
class BottomMenuPresenter(private val application: ThisOrThatApp) : MvpPresenter<BottomMenuView>() {

    @Inject
    lateinit var menuState: BottomMenuState

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        application.injector.inject(this)
        viewState.updateUI(menuState.currentMenuItem)
    }

    fun switchFragment(fragmentNumber: Int) {
        menuState.switchFragment(fragmentNumber)
    }
}