package com.svobnick.thisorthat.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.svobnick.thisorthat.R

class MenuActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics_menu)
    }

    fun onHistoryButtonClick(selected: View) {
        val intent = Intent(this, AnsweredQuestionsActivity::class.java)
        startActivity(intent)
    }

    fun onMyQuestionsButtonClick(selected: View) {

    }

    fun onCommentedButtonClick(selected: View) {

    }

    fun onNewQuestionButtonClick(selected: View) {
        val intent = Intent(this, NewQuestionActivity::class.java)
        startActivity(intent)
    }
}