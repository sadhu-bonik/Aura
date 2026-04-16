package com.aura.app.ui.feed

import com.aura.app.data.model.PortfolioItem

interface ActiveVideoCallback {
    fun attachPlayer(target: VideoPageViewHolder, item: PortfolioItem)
    fun detachPlayer(target: VideoPageViewHolder)
    fun togglePlayback()
}
