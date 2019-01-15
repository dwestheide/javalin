/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestErrorMapper {

    @Test
    fun `error-mapper works for 404`() = TestUtil.test { app, http ->
        app.error(404) { ctx -> ctx.result("Custom 404 page") }
        assertThat(http.getBody("/unmapped"), `is`("Custom 404 page"))
    }

    @Test
    fun `error-mapper works for 500`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception"), `is`("Custom 500 page"))
    }

    @Test
    fun `error-mapper runs after exception-mapper`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
                .exception(Exception::class.java) { _, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception"), `is`("Custom 500 page"))
    }

    @Test
    fun `error-mapper can throw exceptions`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
                .exception(Exception::class.java) { _, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx ->
                    ctx.result("Custom 500 page")
                    throw RuntimeException()
                }
        assertThat(http.getBody("/exception"), `is`("Exception handled!"))
    }

    @Test
    fun `exception-mapper-does-not-trump-error-handler`() = TestUtil.test { app, http->
        app.exception(Exception::class.java) { _, ctx -> ctx.status(500).result("boom") }
            .error(404) { ctx -> ctx.result("custom-404-page") }
        val response = http.get("/doesntexist")
        assertThat(response.status, `is`(404))
        assertThat(response.body, `is`("custom-404-page"))
    }

    @Test
    fun `exception-mapper-doesnt-override-404-from-missing-route`() = TestUtil.test { app, http->
        app.exception(Exception::class.java) { _, ctx -> ctx.status(500).result("boom") }
        val response = http.get("/doesntexist")
        assertThat(response.status, `is`(404))
    }

    @Test
    fun `exception-mapper-doesnt-override-explicitly-set-status-codes`() = TestUtil.test { app, http->
        app.get("/") { ctx -> ctx.status(404) }
            .exception(Exception::class.java) { _, ctx -> ctx.status(500).result("boom") }
        val response = http.get("/")
        assertThat(response.status, `is`(404))
    }

}
