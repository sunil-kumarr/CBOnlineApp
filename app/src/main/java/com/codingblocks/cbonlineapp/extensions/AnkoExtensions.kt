package com.codingblocks.cbonlineapp.extensions

import android.view.ViewManager
import android.widget.RatingBar
import androidx.annotation.StyleRes
import com.codingblocks.cbonlineapp.widgets.ViewPagerCustomDuration
import de.hdodenhof.circleimageview.CircleImageView
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.styledRatingBar(@StyleRes style: Int, init: RatingBar.() -> Unit = {}): RatingBar = ankoView({ RatingBar(it, null, 0, style) }, 0, init = init)

inline fun ViewManager.circleImageView(theme: Int = 0, init: CircleImageView.() -> Unit) = ankoView({ CircleImageView(it) }, theme, init)

inline fun ViewManager.customViewPager(theme: Int = 0, init: ViewPagerCustomDuration.() -> Unit) = ankoView({
    ViewPagerCustomDuration(it)
}, theme, init)
