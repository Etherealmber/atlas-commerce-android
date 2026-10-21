package ma.etherealmber.atlascommerce.di

import javax.inject.Qualifier

/** Marks the anonymous OkHttp client used to probe a candidate server address. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProbeClient
