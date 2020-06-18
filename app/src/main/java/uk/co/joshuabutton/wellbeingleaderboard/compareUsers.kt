package uk.co.joshuabutton.wellbeingleaderboard

import uk.co.joshuabutton.wellbeingleader.userItem

class compareUsers {
    companion object : Comparator<userItem> {

        override fun compare(a: userItem, b: userItem): Int = when {
            a.appTime.toLong() != b.appTime.toLong() -> (a.appTime.toLong() - b.appTime.toLong()).toInt()
            else -> 0
        }
    }
}