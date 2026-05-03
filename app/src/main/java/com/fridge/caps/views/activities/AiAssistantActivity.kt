/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */

package com.fridge.caps.views.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fridge.caps.R
import com.fridge.caps.ai.AiViewModel
import com.fridge.caps.views.adapters.AiChatAdapter

class AiAssistantActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AiAssistant"
    }

    private val viewModel: AiViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var chipScroll: View
    private lateinit var chipRow: LinearLayout

    private val adapter: AiChatAdapter = AiChatAdapter { counselorId ->
        if (counselorId.isEmpty()) {
            Toast.makeText(this, "Profile unavailable for this recommendation.", Toast.LENGTH_SHORT)
                .show()
            return@AiChatAdapter
        }
        startActivity(
            Intent(this, CounselorProfileActivity::class.java).apply {
                putExtra("counselorId", counselorId)
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        rvChat = findViewById(R.id.rvChat)
        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        chipScroll = findViewById(R.id.chipScroll)
        chipRow = findViewById(R.id.chipRow)

        findViewById<ImageButton>(R.id.btnBackAi).setOnClickListener { finish() }

        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        viewModel.messages.observe(this) { list ->
            try {
                adapter.submitList(list)
                rvChat.post {
                    val n = adapter.itemCount
                    if (n > 0) {
                        rvChat.scrollToPosition(n - 1)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chat list update failed", e)
                Toast.makeText(this, "Could not refresh the chat.", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.showSuggestedPrompts.observe(this) { show ->
            chipScroll.visibility = if (show == true) View.VISIBLE else View.GONE
        }

        setupSuggestedChips()

        btnSend.setOnClickListener { sendCurrentInput() }
        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentInput()
                true
            } else {
                false
            }
        }
    }

    private fun setupSuggestedChips() {
        chipRow.removeAllViews()
        val prompts = listOf(
            getString(R.string.ai_chip_anxiety),
            getString(R.string.ai_chip_academic),
            getString(R.string.ai_chip_transcript),
        )
        val padH = (12 * resources.displayMetrics.density).toInt()
        val padV = (10 * resources.displayMetrics.density).toInt()
        val marginEnd = (8 * resources.displayMetrics.density).toInt()
        for (text in prompts) {
            val chip = TextView(this).apply {
                setText(text)
                setBackgroundResource(R.drawable.bg_ai_chip)
                setPadding(padH, padV, padH, padV)
                setTextColor(getColor(R.color.caps_palette_neutral_dark))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                setOnClickListener {
                    etInput.setText(text)
                    sendWithText(text)
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.marginEnd = marginEnd
            chipRow.addView(chip, lp)
        }
    }

    private fun sendCurrentInput() {
        val t = etInput.text?.toString()?.trim() ?: ""
        if (t.isEmpty()) {
            Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show()
            return
        }
        sendWithText(t)
    }

    private fun sendWithText(text: String) {
        try {
            val query = text.trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show()
                return
            }

            etInput.setText("")
            btnSend.isEnabled = false

            viewModel.sendMessage(query)

            btnSend.postDelayed({ btnSend.isEnabled = true }, 2000L)
        } catch (e: Exception) {
            Log.e("AI_DEBUG", "Crash in send", e)
            btnSend.isEnabled = true
        }
    }
}
