package com.botpa.turbophotos.gallery.search

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.widget.EditText
import androidx.activity.BackEventCompat
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.util.BackAnimationEvent
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Ease
import com.botpa.turbophotos.util.Orion

@SuppressLint("NotifyDataSetChanged")
class ListSearchHelper<T> {

    //Helper
    private lateinit var activity: Activity
    private lateinit var backManager: BackManager
    private lateinit var navbarLayout: View
    private lateinit var searchLayout: View
    private lateinit var searchInput: EditText
    private lateinit var list: RecyclerView
    private lateinit var onBeforeFilter: (isFiltering: Boolean, query: String) -> Boolean
    private lateinit var onFilter: (isFiltering: Boolean, query: String) -> MutableList<T>
    private lateinit var onAfterFilter: (isFiltering: Boolean, query: String, list: MutableList<T>) -> Unit

    //Search
    var isSearching: Boolean = false
        private set
    var currentQuery: String = ""
        private set
    val isFiltered: Boolean get() = currentQuery.isNotEmpty()


    //Actions
    fun init(
        activity: Activity,
        backManager: BackManager,
        navbarLayout: View,
        searchLayout: View,
        searchInput: EditText,
        list: RecyclerView,
        onBeforeFilter: (isFiltering: Boolean, query: String) -> Boolean,
        onFilter: (isFiltering: Boolean, query: String) -> MutableList<T>,
        onAfterFilter: (isFiltering: Boolean, query: String, list: MutableList<T>) -> Unit
    ) {
        this.activity = activity
        this.backManager = backManager
        this.navbarLayout = navbarLayout
        this.searchLayout = searchLayout
        this.searchInput = searchInput
        this.list = list
        this.onBeforeFilter = onBeforeFilter
        this.onFilter = onFilter
        this.onAfterFilter = onAfterFilter
    }

    fun toggleLayout(show: Boolean) {
        if (show) {
            //Searching
            if (isSearching) return

            //Toggle search
            Orion.animateHide(navbarLayout) { Orion.animateShow(searchLayout) }

            //Focus text & show keyboard
            searchInput.requestFocus()
            searchInput.selectAll()
            Orion.showKeyboard(activity)

            //Back button
            backManager.register("searchMenu") { toggleLayout(false) }
        } else {
            //Clear text & hide keyboard
            Orion.hideKeyboard(activity)
            Orion.clearFocus(activity)

            //Toggle search
            Orion.animateHide(searchLayout) { Orion.animateShow(navbarLayout) }

            //Back button
            backManager.unregister("searchMenu")
        }
    }

    fun filter(query: String = "") {
        //Check if filtering
        val fixedQuery = query.trim()
        val isFiltering = fixedQuery.isNotEmpty()

        //On before filter
        val shouldFilter = onBeforeFilter.invoke(isFiltering, query)
        if (!shouldFilter) return

        //Update search info
        isSearching = true
        currentQuery = query

        //Update back manager
        if (isFiltering) {
            //Register back event
            backManager.register("search", object : BackAnimationEvent {
                override fun onProgress(backEvent: BackEventCompat) {
                    //Get info
                    val easeOut = Ease.outCubic(backEvent.progress)

                    //Animate
                    list.alpha = 1.0f - easeOut * 0.8f
                }

                override fun onInvoked() {
                    //Clear filter
                    filter()
                }
            })

            //Move search menu event before the search cancel one
            backManager.moveFirst("searchMenu")
        } else {
            //Unregister back event
            backManager.unregister("search")
        }

        //Filter
        Thread {
            //On filter
            val items = onFilter.invoke(isFiltering, query)

            //Prepare on after filter
            val onAfterFilterRunnable = Runnable {
                //On after filter
                onAfterFilter.invoke(isFiltering, query, items)

                //Update albums
                list.adapter?.notifyDataSetChanged()

                //Scroll to top
                list.stopScroll()
                list.scrollToPosition(0)

                //Finish searching
                isSearching = false
            }

            //Update items
            activity.runOnUiThread {
                when (list.alpha) {
                    1f -> {
                        //On after filter
                        onAfterFilterRunnable.run()
                    }
                    0f -> {
                        //On after filter
                        onAfterFilterRunnable.run()

                        //Show list
                        list.animate()
                            .alpha(1.0f)
                            .setDuration(Orion.DEFAULT_ANIMATION_DURATION.toLong())
                            .start()
                    }
                    else -> {
                        //Hide list, update items & show list again
                        list.animate()
                            .alpha(0f)
                            .setDuration((Orion.DEFAULT_ANIMATION_DURATION * list.alpha).toLong())
                            .withEndAction {
                                //On after filter
                                onAfterFilterRunnable.run()

                                //Show list
                                list.animate()
                                    .alpha(1.0f)
                                    .setDuration(Orion.DEFAULT_ANIMATION_DURATION.toLong())
                                    .start()
                            }
                            .start()
                    }
                }
            }
        }.start()
    }

    fun refresh() {
        //Reapply current filter
        filter(currentQuery)
    }

}