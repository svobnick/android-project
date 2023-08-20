package com.svobnick.thisorthat.fragments

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.svobnick.thisorthat.R
import com.svobnick.thisorthat.activities.CommentsActivity
import com.svobnick.thisorthat.activities.HistoryChoiceActivity
import com.svobnick.thisorthat.app.ThisOrThatApp
import com.svobnick.thisorthat.databinding.FragmentChoiceBinding
import com.svobnick.thisorthat.databinding.FragmentChoiceMenuBinding
import com.svobnick.thisorthat.databinding.FragmentHeaderMenuBinding
import com.svobnick.thisorthat.databinding.PopupReportChoiceBinding
import com.svobnick.thisorthat.databinding.PopupReportResultBinding
import com.svobnick.thisorthat.model.Question
import com.svobnick.thisorthat.presenters.ChoicePresenter
import com.svobnick.thisorthat.utils.PopupUtils.dimBackground
import com.svobnick.thisorthat.utils.computeQuestionsPercentage
import com.svobnick.thisorthat.view.ChoiceView
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

@RuntimePermissions
class ChoiceFragment : MvpAppCompatFragment(), ChoiceView {
    private val TAG = this::class.java.name

    private val ANALYTICS_SCREEN_NAME = "Questionnaire"

    private lateinit var state: STATE
    private lateinit var reportChoiceWindow: PopupWindow
    private lateinit var reportResultWindow: PopupWindow

    private lateinit var menuBinding: FragmentChoiceMenuBinding
    private lateinit var headerBinding: FragmentHeaderMenuBinding
    private var _binding: FragmentChoiceBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var currentQuestion: Question
    private var isFavorite: Boolean = false
    private var questionsSessionCounter = 0

    @InjectPresenter
    lateinit var choicePresenter: ChoicePresenter

    @ProvidePresenter
    fun provideChoicePresenter(): ChoicePresenter {
        return ChoicePresenter(requireActivity().application as ThisOrThatApp)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity().application as ThisOrThatApp).injector.inject(this)
        super.onCreate(savedInstanceState)
        choicePresenter.attachView(this)

        this.state = STATE.QUESTION

        _binding = FragmentChoiceBinding.inflate(inflater, container, false)
        menuBinding = FragmentChoiceMenuBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.firstText.setOnClickListener(this::onChoiceClick)
        binding.lastText.setOnClickListener(this::onChoiceClick)
        binding.reportButton.setOnClickListener(this::reportQuestion)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.reportChoiceWindow = setupReportPopupWindow()
        this.reportResultWindow = setupResponsePopupWindow()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity !is HistoryChoiceActivity) {
            choicePresenter.setNextQuestion()
        }
    }

    override fun onStart() {
        super.onStart()
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, ANALYTICS_SCREEN_NAME)
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS,ANALYTICS_SCREEN_NAME)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun onStop() {
        val params = Bundle()
        params.putString("count", questionsSessionCounter.toString())
        firebaseAnalytics.logEvent("question_answered", params)
        questionsSessionCounter = 0
        super.onStop()
    }

    override fun onChoiceClick(choice: View) {
        if (!this::currentQuestion.isInitialized) {
            return
        }

        if (state == STATE.RESULT) {
            choicePresenter.setNextQuestion()
        } else {
            val clickedText = requireActivity().findViewById<TextView>(choice.id)
            currentQuestion.choice =
                if (clickedText.text.toString() == currentQuestion.firstText) Question.Choices.FIRST else Question.Choices.LAST
            choicePresenter.saveChoice(currentQuestion)
            setResultToView(currentQuestion, isFavorite)
        }

        state = changeState()
    }

    override fun setNewQuestion(question: Question) {
        questionsSessionCounter.inc()
        requireActivity().runOnUiThread {
            currentQuestion = question
            binding.firstText.text = currentQuestion.firstText
            binding.lastText.text = currentQuestion.lastText
            isFavorite = false
            menuBinding.switchFavoriteButton.setImageResource(R.drawable.icon_favorite_off)
            binding.firstCardGroup.alpha = 1f
            binding.firstText.alpha = 1f
            binding.lastCardGroup.alpha = 1f
            binding.lastText.alpha = 1f
            hideResults()
        }
    }

    override fun setResultToView(question: Question, favorite: Boolean) {
        currentQuestion = question
        binding.firstText.text = question.firstText
        binding.lastText.text = question.lastText
        val firstRate = question.firstRate
        val lastRate = question.lastRate
        if (favorite) {
            menuBinding.switchFavoriteButton.setImageResource(R.drawable.icon_favorite)
            isFavorite = true
        }
        val (firstPercent, lastPercent) = computeQuestionsPercentage(firstRate, lastRate)
        (childFragmentManager.findFragmentById(R.id.first_stat)!! as ChoiceStatFragment).setStat(
            firstPercent,
            firstRate,
            question.choice == Question.Choices.FIRST
        )
        (childFragmentManager.findFragmentById(R.id.last_stat)!! as ChoiceStatFragment).setStat(
            lastPercent,
            lastRate,
            question.choice == Question.Choices.LAST
        )
        if (question.choice == Question.Choices.FIRST) {
            binding.lastCardGroup.alpha = 0.75f
            binding.lastText.alpha = 0.75f
        } else {
            binding.firstCardGroup.alpha = 0.75f
            binding.firstText.alpha = 0.75f
        }

        if (Question.Status.NEW == question.status) {
            binding.moderationStatus.visibility = View.VISIBLE
        } else {
            binding.moderationStatus.visibility = View.INVISIBLE
        }

        showResults()
    }

    private fun onReportClickHandler(selected: View) {
        val reportReason = when (selected.id) {
            R.id.clone -> "clone"
            R.id.abuse -> "abuse"
            R.id.typo -> "typo"
            else -> throw IllegalArgumentException("type ${selected.id} is not allowed here")
        }
        choicePresenter.reportQuestion(currentQuestion, reportReason)
        reportResult()
        choicePresenter.setNextQuestion()
    }

    private fun reportResult() {
        reportChoiceWindow.dismiss()
        reportResultWindow.showAtLocation(
            requireActivity().findViewById(R.id.main_screen_root),
            Gravity.CENTER,
            0,
            0
        )
        dimBackground(requireActivity(), reportResultWindow.contentView.rootView)
    }

    override fun reportQuestion(selected: View) {
        if (!this::currentQuestion.isInitialized) {
            return
        }

        reportChoiceWindow.showAtLocation(
            requireActivity().findViewById(R.id.main_screen_root),
            Gravity.CENTER,
            0,
            0
        )
        dimBackground(requireActivity(), reportChoiceWindow.contentView.rootView)
    }

    private fun hideReportResult() {
        reportResultWindow.dismiss()
    }

    override fun openComments() {
        if (!this::currentQuestion.isInitialized) {
            return
        }

        val params = Bundle()
        params.putString("question_id", currentQuestion.id.toString())
        firebaseAnalytics.logEvent("open_comments", params)
        val intent = Intent(context, CommentsActivity::class.java)
        intent.putExtra("id", currentQuestion.id)
        intent.putExtra("firstText", currentQuestion.firstText)
        intent.putExtra("lastText", currentQuestion.lastText)
        intent.putExtra("firstRate", currentQuestion.firstRate.toString())
        intent.putExtra("lastRate", currentQuestion.lastRate.toString())
        intent.putExtra("choice", currentQuestion.choice)
        val (firstPercent, lastPercent) = computeQuestionsPercentage(
            currentQuestion.firstRate,
            currentQuestion.lastRate
        )
        intent.putExtra("firstPercent", firstPercent.toString())
        intent.putExtra("lastPercent", lastPercent.toString())
        startActivity(intent)
    }

    override fun switchFavoriteQuestion() {
        if (isFavorite) {
            deleteFavoriteQuestion()
        } else {
            addFavoriteQuestion()
        }
    }

    private fun addFavoriteQuestion() {
        if (!this::currentQuestion.isInitialized) {
            return
        }

        choicePresenter.addFavoriteQuestion(currentQuestion.id.toString())
        menuBinding.switchFavoriteButton.setImageResource(R.drawable.icon_favorite)
        isFavorite = true
    }

    private fun deleteFavoriteQuestion() {
        if (!this::currentQuestion.isInitialized) {
            return
        }

        choicePresenter.deleteFavoriteQuestion(currentQuestion.id.toString())
        menuBinding.switchFavoriteButton.setImageResource(R.drawable.icon_favorite_off)
        isFavorite = false
    }

    @NeedsPermission(value = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun shareQuestion() {
        firebaseAnalytics.logEvent("share_instagram", null)
        val filename = UUID.randomUUID().toString() + ".png"

        val imageOutStream: OutputStream
        var uri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES
            )
            uri = requireContext().contentResolver!!.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
            imageOutStream = requireContext().contentResolver!!.openOutputStream(uri)!!
        } else {
            val imagePath =
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
            val image = File(imagePath, filename)
            try {
                uri = FileProvider.getUriForFile(requireContext(), "com.svobnick.thisorthat.fileprovider", image)
            } catch (e: Exception) {
                // strange hook for problem with huawei devices
                // more info at https://stackoverflow.com/a/41309223
                if ("Huawei" == Build.MANUFACTURER) {
                    uri = Uri.fromFile(image)
                }
            }

            imageOutStream = FileOutputStream(image)
        }

        imageOutStream.use {
            combineBitmaps(getViewBitmap(binding.choiceView), getViewBitmap(headerBinding.headerLogo))
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        val intent = Intent("com.instagram.share.ADD_TO_STORY")
        intent.type = "image/*"
        intent.putExtra("interactive_asset_uri", uri)
        intent.putExtra("content_url", "https://thisorthat.ru")
        intent.putExtra("top_background_color", "#312F5A")
        intent.putExtra("bottom_background_color", "#110F26")

        val activity: Activity = requireActivity()
        activity.grantUriPermission(
            "com.instagram.android",
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        if (activity.packageManager?.resolveActivity(intent, 0) != null) {
            activity.startActivityForResult(intent, 0)
        }
    }

    private fun getViewBitmap(view: View): Bitmap {
        val result = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(result))
        return result
    }

    private fun combineBitmaps(choiceBitmap: Bitmap, logoBitmap: Bitmap): Bitmap {
        val width = choiceBitmap.width + (choiceBitmap.width / 2)
        val height = logoBitmap.height + choiceBitmap.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val comboImage = Canvas(result)
        comboImage.drawColor(0, PorterDuff.Mode.CLEAR)

        comboImage.drawBitmap(
            logoBitmap,
            ((result.width - logoBitmap.width) / 2).toFloat(),
            0f,
            null
        )
        comboImage.drawBitmap(
            choiceBitmap,
            ((result.width - choiceBitmap.width) / 2).toFloat(),
            logoBitmap.height.toFloat(),
            null
        )

        return result
    }

    override fun showError(errorMsg: String) {
        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
    }

    private fun setupReportPopupWindow(): PopupWindow {
        val popupWindow = PopupWindow(context)
        val reportView = PopupReportChoiceBinding.inflate(LayoutInflater.from(context))
        popupWindow.contentView = reportView.root
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true
        popupWindow.update()
        reportView.typo.setOnClickListener(this::onReportClickHandler)
        reportView.abuse.setOnClickListener(this::onReportClickHandler)
        reportView.clone.setOnClickListener(this::onReportClickHandler)
        return popupWindow
    }

    private fun setupResponsePopupWindow(): PopupWindow {
        val popupWindow = PopupWindow(context)
        val responseView = PopupReportResultBinding.inflate(LayoutInflater.from(context))
        popupWindow.contentView = responseView.root
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true
        popupWindow.update()
        responseView.reportResultOk.setOnClickListener { hideReportResult() }
        return popupWindow
    }

    private fun hideResults() {
        childFragmentManager.beginTransaction()
            .hide(childFragmentManager.findFragmentById(R.id.first_stat)!!)
            .hide(childFragmentManager.findFragmentById(R.id.last_stat)!!)
            .commit()
    }

    private fun showResults() {
        childFragmentManager.beginTransaction()
            .show(childFragmentManager.findFragmentById(R.id.first_stat)!!)
            .show(childFragmentManager.findFragmentById(R.id.last_stat)!!)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun changeState() = if (state == STATE.QUESTION) STATE.RESULT else STATE.QUESTION

    enum class STATE {
        QUESTION,
        RESULT
    }
}