package com.example.mydailyroutine.platform

import android.content.res.Resources
import com.example.mydailyroutine.R

/** Core calendar titles are dataset keys, not hardcoded Android presentation copy. */
fun Resources.calendarTitle(key: String): String = when (key) {
    "Začetek pouka" -> getString(R.string.calendar_label_0)
    "Jesenske počitnice" -> getString(R.string.calendar_label_1)
    "Novoletni oddih" -> getString(R.string.calendar_label_2)
    "Zimske počitnice · Zahod" -> getString(R.string.calendar_label_3)
    "Pouka prost dan" -> getString(R.string.calendar_label_4)
    "Prvomajski oddih" -> getString(R.string.calendar_label_5)
    "Konec pouka · zaključni letniki" -> getString(R.string.calendar_label_6)
    "Konec pouka · ostali letniki" -> getString(R.string.calendar_label_7)
    "Poletne počitnice" -> getString(R.string.calendar_label_8)
    "Dan reformacije" -> getString(R.string.calendar_label_9)
    "Dan spomina na mrtve" -> getString(R.string.calendar_label_10)
    "Božič" -> getString(R.string.calendar_label_11)
    "Dan samostojnosti in enotnosti" -> getString(R.string.calendar_label_12)
    "Novo leto" -> getString(R.string.calendar_label_13)
    "Prešernov dan" -> getString(R.string.calendar_label_14)
    "Dan upora proti okupatorju" -> getString(R.string.calendar_label_15)
    "Praznik dela" -> getString(R.string.calendar_label_16)
    "Dan državnosti" -> getString(R.string.calendar_label_17)
    "Marijino vnebovzetje" -> getString(R.string.calendar_label_18)
    "Priključitev Primorske k matični domovini" -> getString(R.string.calendar_label_19)
    "Dan slovenskega športa" -> getString(R.string.calendar_label_20)
    "Dan suverenosti" -> getString(R.string.calendar_label_21)
    "Dan znanosti" -> getString(R.string.calendar_label_22)
    "Dan Rudolfa Maistra" -> getString(R.string.calendar_label_23)
    "Dan Primoža Trubarja" -> getString(R.string.calendar_label_24)
    "Združitev prekmurskih Slovencev z matičnim narodom" -> getString(R.string.calendar_label_25)
    "Velikonočna nedelja" -> getString(R.string.calendar_label_26)
    "Velikonočni ponedeljek" -> getString(R.string.calendar_label_27)
    "Binkošti" -> getString(R.string.calendar_label_28)
    else -> key // Preserve titles imported/entered by the user.
}
