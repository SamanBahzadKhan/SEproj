package com.fridge.caps.views.adapters

import android.animation.ValueAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fridge.caps.R
import com.fridge.caps.ai.AiRepository
import com.fridge.caps.ai.AiViewModel
import java.util.Locale

class AiChatAdapter(
    private val onCounselorClick: (counselorId: String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<AiViewModel.ChatMessage>()

    fun submitList(list: List<AiViewModel.ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is AiViewModel.ChatMessage.UserMessage -> VT_USER
        is AiViewModel.ChatMessage.LoadingMessage -> VT_LOADING
        is AiViewModel.ChatMessage.AssistantMessage -> {
            val a = items[position] as AiViewModel.ChatMessage.AssistantMessage
            if (a.isCrisis) VT_CRISIS else VT_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            VT_USER -> UserVH(inf.inflate(R.layout.item_ai_chat_user, parent, false))
            VT_LOADING -> LoadingVH(inf.inflate(R.layout.item_ai_chat_loading, parent, false))
            VT_CRISIS -> CrisisVH(inf.inflate(R.layout.item_ai_chat_crisis, parent, false))
            else -> AssistantVH(inf.inflate(R.layout.item_ai_chat_assistant, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            when (val msg = items[position]) {
                is AiViewModel.ChatMessage.UserMessage -> (holder as UserVH).bind(msg.text)
                is AiViewModel.ChatMessage.LoadingMessage -> (holder as LoadingVH).bind()
                is AiViewModel.ChatMessage.AssistantMessage -> {
                    if (msg.isCrisis) {
                        (holder as CrisisVH).bind(msg.text)
                    } else {
                        (holder as AssistantVH).bind(msg, onCounselorClick)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AI_DEBUG", "Adapter crash at position $position", e)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is LoadingVH) {
            holder.cancelAnimation()
        }
        super.onViewRecycled(holder)
    }

    class UserVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(text: String) {
            tv.text = text
        }
    }

    class CrisisVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(text: String) {
            tv.text = text
        }
    }

    class LoadingVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dots = listOf(
            itemView.findViewById<TextView>(R.id.tvDot1),
            itemView.findViewById<TextView>(R.id.tvDot2),
            itemView.findViewById<TextView>(R.id.tvDot3),
        )
        private var animator: ValueAnimator? = null

        fun bind() {
            cancelAnimation()
            animator = ValueAnimator.ofFloat(0.35f, 1f).apply {
                duration = 500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { va ->
                    val v = va.animatedValue as Float
                    dots.forEachIndexed { i, d ->
                        d.alpha = (v - i * 0.15f).coerceIn(0.25f, 1f)
                    }
                }
                start()
            }
        }

        fun cancelAnimation() {
            animator?.cancel()
            animator = null
            dots.forEach { it.alpha = 1f }
        }
    }

    class AssistantVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val rvCounsellors: RecyclerView = itemView.findViewById(R.id.rvCounsellors)
        private val officeCard: View = itemView.findViewById(R.id.officeCard)
        private val tvOfficeName: TextView = itemView.findViewById(R.id.tvOfficeName)
        private val tvOfficeLocation: TextView = itemView.findViewById(R.id.tvOfficeLocation)
        private val tvOfficeContact: TextView = itemView.findViewById(R.id.tvOfficeContact)

        private val counsellorMiniAdapter = CounsellorMiniAdapter()

        init {
            rvCounsellors.layoutManager = LinearLayoutManager(
                itemView.context,
                RecyclerView.HORIZONTAL,
                false,
            )
            rvCounsellors.adapter = counsellorMiniAdapter
            rvCounsellors.isNestedScrollingEnabled = false
        }

        fun bind(
            msg: AiViewModel.ChatMessage.AssistantMessage,
            onCounselorClick: (String) -> Unit,
        ) {
            tvMessage.text = msg.text

            val office = msg.officeInfo
            if (office != null && (
                    office.office.isNotEmpty() ||
                        office.location.isNotEmpty() ||
                        office.contact.isNotEmpty()
                    )
            ) {
                officeCard.visibility = View.VISIBLE
                tvOfficeName.text = office.office
                tvOfficeLocation.text = office.location
                tvOfficeContact.text = office.contact
            } else {
                officeCard.visibility = View.GONE
            }

            if (msg.counsellors.isNotEmpty()) {
                rvCounsellors.visibility = View.VISIBLE
                counsellorMiniAdapter.submit(msg.counsellors, onCounselorClick)
            } else {
                rvCounsellors.visibility = View.GONE
                counsellorMiniAdapter.submit(emptyList(), onCounselorClick)
            }
        }
    }

    companion object {
        private const val VT_USER = 0
        private const val VT_ASSISTANT = 1
        private const val VT_CRISIS = 2
        private const val VT_LOADING = 3
    }
}

/** Horizontal row of counsellor recommendation cards (nested in assistant message). */
private class CounsellorMiniAdapter : RecyclerView.Adapter<CounsellorMiniAdapter.MiniVH>() {

    private var items = emptyList<AiRepository.Counsellor>()
    private var onRowClick: (String) -> Unit = {}

    fun submit(list: List<AiRepository.Counsellor>, onClick: (String) -> Unit) {
        items = list
        onRowClick = onClick
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_counselor_mini, parent, false)
        return MiniVH(v)
    }

    override fun onBindViewHolder(holder: MiniVH, position: Int) {
        try {
            holder.bind(items[position], onRowClick)
        } catch (e: Exception) {
            Log.e("AI_DEBUG", "Counsellor mini adapter crash at $position", e)
        }
    }

    override fun getItemCount(): Int = items.size

    class MiniVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvSpec: TextView = itemView.findViewById(R.id.tvSpec)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)

        fun bind(c: AiRepository.Counsellor, onClick: (String) -> Unit) {
            tvName.text = c.name
            tvSpec.text = c.specialization
            tvRating.text = String.format(
                Locale.US,
                "%.1f  (%d)",
                c.rating,
                c.reviewCount,
            )
            if (c.counselorId.isNotEmpty()) {
                itemView.isClickable = true
                itemView.setOnClickListener { onClick(c.counselorId) }
            } else {
                itemView.isClickable = false
                itemView.setOnClickListener(null)
            }
        }
    }
}
