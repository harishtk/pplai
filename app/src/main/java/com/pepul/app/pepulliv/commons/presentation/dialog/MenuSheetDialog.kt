package com.pepul.app.pepulliv.commons.presentation.dialog

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.commons.presentation.dialog.MenuItemColor.*
import com.pepul.app.pepulliv.databinding.DialogMenuBinding
import com.pepul.app.pepulliv.databinding.ItemMenuBinding

class MenuSheetDialog constructor(
    context: Context,
    private val menuList: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : BottomSheetDialog(context) {

    private lateinit var binding: DialogMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentView = layoutInflater.inflate(R.layout.dialog_menu, null, false)
        binding = DialogMenuBinding.bind(contentView)
        setContentView(contentView)

        val bgColor = ResourcesCompat.getColor(context.resources, R.color.bg_dark_primary, null)
        val radius = context.resources.getDimensionPixelSize(R.dimen.default_bottom_sheet_radius)

        val adapter = MenuAdapter {
            dismiss()
            onItemClick(it)
        }
        adapter.submitList(menuList)
        binding.listView.adapter = adapter

        setOnShowListener {
            val bottomSheet: FrameLayout = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener
            bottomSheet.background = getRoundedDrawable(radius, bgColor)
        }
    }

    private fun getRoundedDrawable(@Px radius: Int, bgColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            cornerRadii = floatArrayOf(
                radius.toFloat(), radius.toFloat(), // top left
                radius.toFloat(), radius.toFloat(), // top right
                0F, 0F, // bottom right
                0F, 0F  // bottom left
            )
        }
    }
}

class MenuAdapter(
    private val onItemClick: (MenuItem) -> Unit
) : androidx.recyclerview.widget.ListAdapter<MenuItem, MenuAdapter.MenuViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        return MenuViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    class MenuViewHolder private constructor(
        private val binding: ItemMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem, onItemClick: (MenuItem) -> Unit) = with(binding) {
            settingsIcons.setImageResource(item.icon)
            settingsText.text = item.title

            val tintColorRes = when (item.color) {
                DEFAULT -> R.color.white
                ALT -> R.color.tag_second_color
                DISTINCT -> R.color.red_700
            }
            val tintColor = ResourcesCompat.getColor(binding.root.resources, tintColorRes, binding.root.context.theme)
            settingsText.setTextColor(tintColor)
            settingsIcons.imageTintList = ColorStateList.valueOf(tintColor)

            root.setOnClickListener { onItemClick(item) }
        }

        companion object {
            fun from(parent: ViewGroup): MenuViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_menu,
                    parent,
                    false
                )
                val binding = ItemMenuBinding.bind(itemView)
                return MenuViewHolder(binding)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MenuItem>() {
            override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

data class MenuItem(
    @DrawableRes val icon: Int,
    val title: String,
    val id: Int,
    val color: MenuItemColor = DEFAULT
)

enum class MenuItemColor { DEFAULT, ALT, DISTINCT }