package com.myday.dailyfocus.ui.components

/**
 * "1 session" / "2 sessions". English-only helper for the redesigned screens;
 * move these strings to <plurals> resources when the new UI is localized.
 */
fun countLabel(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"
