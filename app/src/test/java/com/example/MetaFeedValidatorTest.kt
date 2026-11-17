package com.example

import com.example.meta.MetaFeedValidator
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test

class MetaFeedValidatorTest {
    private fun feed(entries: String) = JSONObject("""{"schema":1,"version":"test","strategies":$entries}""")

    @Test fun acceptsValidExample() {
        MetaFeedValidator.validate(feed("""[{"id":"funnel","th_min":7,"th_max":19,"name":"Funnel","status":"example-unverified","guidance":["Test only"]}]"""))
    }

    @Test fun rejectsDuplicateIds() {
        val entry = """{"id":"same","th_min":7,"th_max":19,"name":"Funnel","status":"experimental","guidance":["Test"]}"""
        assertThrows(IllegalArgumentException::class.java) { MetaFeedValidator.validate(feed("[$entry,$entry]")) }
    }

    @Test fun rejectsInvalidTownHallRange() {
        assertThrows(IllegalArgumentException::class.java) {
            MetaFeedValidator.validate(feed("""[{"id":"bad","th_min":20,"th_max":7,"name":"Invalid","status":"verified","guidance":["Test"]}]"""))
        }
    }

    @Test fun rejectsMissingVerificationStatus() {
        assertThrows(IllegalArgumentException::class.java) {
            MetaFeedValidator.validate(feed("""[{"id":"bad","th_min":7,"th_max":10,"name":"Invalid","guidance":["Test"]}]"""))
        }
    }
}
