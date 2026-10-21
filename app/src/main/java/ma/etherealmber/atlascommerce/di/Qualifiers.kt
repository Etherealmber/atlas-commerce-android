package ma.etherealmber.atlascommerce.di

import javax.inject.Qualifier

/** Application-lifetime coroutine scope (IO) for fire-and-forget persistence. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
