package com.cy.compiler.utils


fun String.capitalizeFirstLetter(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}