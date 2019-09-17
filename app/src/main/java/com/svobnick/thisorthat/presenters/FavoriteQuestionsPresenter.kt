package com.svobnick.thisorthat.presenters

import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.svobnick.thisorthat.app.ThisOrThatApp
import com.svobnick.thisorthat.model.Question
import com.svobnick.thisorthat.service.deleteFavoriteRequest
import com.svobnick.thisorthat.service.getFavoriteRequest
import com.svobnick.thisorthat.view.FavoriteQuestionsView
import org.json.JSONArray
import org.json.JSONObject

@InjectViewState
class FavoriteQuestionsPresenter(
    private val app: ThisOrThatApp,
    private val requestQueue: RequestQueue
) : MvpPresenter<FavoriteQuestionsView>() {
    private val TAG = this::class.java.name
    internal val LIMIT = 100L

    fun getFavoriteQuestions(offset: Long) {
        val json = JSONObject()
            .put("token", app.authToken)
            .put("limit", LIMIT.toString())
            .put("offset", offset.toString())
        requestQueue.add(
            getFavoriteRequest(
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
                    viewState.setFavoriteQuestions(questions)
                },
                Response.ErrorListener {
                    val errData = JSONObject(String(it.networkResponse.data)).toString()
                    viewState.showError(errData)
                })
        )
    }

    fun deleteFavoriteQuestion(itemId: Long) {
        requestQueue.add(
            deleteFavoriteRequest(
                app.authToken,
                itemId.toString(),
                Response.Listener { response ->
                    Log.i(TAG, response.toString())
                },
                Response.ErrorListener {
                    val errData = JSONObject(String(it.networkResponse.data)).toString()
                    viewState.showError(errData)
                })
        )
    }
}