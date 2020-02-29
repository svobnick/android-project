package com.svobnick.thisorthat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.svobnick.thisorthat.R
import com.svobnick.thisorthat.fragments.ChoiceStatFragment
import com.svobnick.thisorthat.model.Question
import com.svobnick.thisorthat.utils.computeQuestionsPercentage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.my_question_single_view.*

class MyQuestionsAdapter(private val context: Context) : RecyclerView.Adapter<MyQuestionsAdapter.QuestionListViewHolder>(), QuestionsListAdapter {

    private val myQuestionsList = ArrayList<Question>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.my_question_single_view, parent, false)
        return QuestionListViewHolder(context, view)
    }

    override fun getItemCount(): Int {
        return myQuestionsList.size
    }

    override fun onBindViewHolder(holder: QuestionListViewHolder, position: Int) {
        holder.bind(myQuestionsList[position])
    }

    override fun getItemId(position: Int): Long {
        return myQuestionsList[position].id
    }

    override fun addQuestions(questions: List<Question>) {
        myQuestionsList.addAll(questions)
        notifyDataSetChanged()
    }

    class QuestionListViewHolder(context: Context, itemView: View) : RecyclerView.ViewHolder(itemView),
        LayoutContainer {
        override val containerView: View get() = itemView
        private val fragmentManager = (context as AppCompatActivity).supportFragmentManager
        private val firstStat = fragmentManager.findFragmentById(R.id.m_first_stat) as ChoiceStatFragment
        private val lastStat = fragmentManager.findFragmentById(R.id.m_last_stat) as ChoiceStatFragment

        fun bind(question: Question) {
            val (firstPercent, lastPercent) = computeQuestionsPercentage(
                question.firstRate,
                question.lastRate
            )
            m_first_text.text = question.firstText
            firstStat.setStat(firstPercent, question.firstRate, true)
            m_last_text.text = question.lastText
            lastStat.setStat(lastPercent, question.lastRate, true)
        }
    }
}