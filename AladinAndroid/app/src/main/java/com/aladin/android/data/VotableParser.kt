package com.aladin.android.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Minimal VOTable parser: extracts rows from the first <TABLE> in TABLEDATA format.
 * Returns each row as Map<fieldName, value>. Sufficient for Simbad.
 */
object VotableParser {
    fun parse(text: String): List<Map<String, String>> {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(text.reader())
        val fields = ArrayList<String>()       // field names, in order
        val rows = ArrayList<Map<String, String>>()
        var inTable = false
        var inData = false
        var inTr = false
        var inField = false
        var currentValues = ArrayList<String>()
        var fieldName: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "TABLE" -> { inTable = true; fields.clear() }
                    "FIELD" -> {
                        inField = true
                        fieldName = parser.getAttributeValue(null, "name")
                    }
                    "DATA" -> if (inTable) inData = true
                    "TR" -> if (inData) { inTr = true; currentValues.clear() }
                    "TD" -> if (inTr) currentValues.add(parser.nextText())
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "TABLE" -> { inTable = false; inData = false }
                    "FIELD" -> {
                        inField = false
                        fields.add(fieldName ?: "col${fields.size + 1}")
                    }
                    "DATA" -> inData = false
                    "TR" -> if (inData) {
                        inTr = false
                        if (currentValues.isNotEmpty()) {
                            val map = LinkedHashMap<String, String>()
                            for (i in fields.indices) {
                                map[fields[i]] = if (i < currentValues.size) currentValues[i] else ""
                            }
                            rows.add(map)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }
}