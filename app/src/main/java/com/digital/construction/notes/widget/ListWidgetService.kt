package com.digital.construction.notes.widget

import android.content.Intent
import android.widget.RemoteViewsService

class ListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ListRemoteViewsFactory(this, intent)
    }
}