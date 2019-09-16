package com.svobnick.thisorthat.presenters

import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.svobnick.thisorthat.app.ThisOrThatApp
import com.svobnick.thisorthat.model.Question
import com.svobnick.thisorthat.service.getMyQuestions
import com.svobnick.thisorthat.view.MyQuestionsView
import org.json.JSONArray
import org.json.JSONObject

@InjectViewState
class MyQuestionsPresenter(
    private val app: ThisOrThatApp,
    private val requestQueue: RequestQueue
) : MvpPresenter<MyQuestionsView>() {
    private val TAG = this::class.java.name

    val LIMIT = 30L

    fun getMyQuestions(offset: Long) {
        val json = JSONObject()
            .put("token", app.authToken)
            .put("limit", LIMIT.toString())
            .put("offset", offset.toString())
        requestQueue.add(
            getMyQuestions(
                json,
                Response.Listener { response ->
                    val items = (JSONObject(response)["result"] as JSONObject)["items"] as JSONArray
                    val questions = mutableListOf<Question>()
                    for (i in 0 until items.length()) {
                        val json = items.get(i) as JSONObject
                        questions.add(
                            Question(
                                (json["item_id"] as String).toLong(),
                                json["first_text"] as String,
                                json["last_text"] as String,
                                json["first_vote"] as Int,
                                json["last_vote"] as Int,
                                null
                            )
                        )
                    }
                    Log.i(TAG, "Receive my questions")
                    viewState.setMyQuestions(questions)
                },
                Response.ErrorListener {
                    val errData = JSONObject(String(it.networkResponse.data)).toString()
                    Log.e(TAG, errData)
                    viewState.showError(errData)
                })
        )
    }

}