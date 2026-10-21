package ma.etherealmber.atlascommerce

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Atlas Commerce application.
 *
 * No analytics, no crash reporter, no third-party SDK that phones home: the app
 * only talks to the server the user chose.
 */
@HiltAndroidApp
class AtlasApp : Application()
